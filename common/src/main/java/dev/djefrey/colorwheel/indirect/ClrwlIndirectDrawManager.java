package dev.djefrey.colorwheel.indirect;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.iris.IrisRenderingPipelineAccessor;
import dev.djefrey.colorwheel.accessors.iris.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.iris.ShaderPackAccessor;
import dev.djefrey.colorwheel.accessors.iris.ShaderStorageBufferHolderAccessor;
import dev.djefrey.colorwheel.compile.ClrwlIndirectPrograms;
import dev.djefrey.colorwheel.compile.ClrwlPipelineCompiler;
import dev.djefrey.colorwheel.compile.oit.ClrwlOitPrograms;
import dev.djefrey.colorwheel.engine.*;
import dev.djefrey.colorwheel.engine.embed.EnvironmentStorage;
import dev.djefrey.colorwheel.engine.uniform.ClrwlUniforms;
import dev.djefrey.colorwheel.instancing.ClrwlInstancedDrawManager;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramGroup;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramId;
import dev.djefrey.colorwheel.util.GlCompat;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.backend.Samplers;
import dev.engine_room.flywheel.backend.engine.*;
import dev.engine_room.flywheel.backend.engine.indirect.*;
import dev.engine_room.flywheel.backend.engine.uniform.Uniforms;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferType;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL30.glBindBufferRange;
import static org.lwjgl.opengl.GL40.glDrawElementsIndirect;
import static org.lwjgl.opengl.GL42.GL_PIXEL_BUFFER_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BARRIER_BIT;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public class ClrwlIndirectDrawManager extends ClrwlDrawManager<ClrwlIndirectInstancer<?>>
{
	public record PipelineData(ClrwlIndirectPrograms.PipelineProgramCache programs,
							   ClrwlFramebuffers framebuffers,
							   ClrwlDepthPyramid depthPyramid)
	{
		public void delete()
		{
			programs.delete();
			framebuffers.delete();
			depthPyramid.delete();
		}
	}

	private final ClrwlIndirectPrograms programs;
	private final ClrwlMeshPool meshPool;
	private final GlVertexArray vao;
	private final Map<InstanceType<?>, ClrwlIndirectCullingGroup<?>> cullingGroups = new HashMap<>();
	private final GlBuffer crumblingDrawBuffer = new GlBuffer(GlBufferUsage.STREAM_DRAW);
	private final StagingBuffer stagingBuffer;
	private final ClrwlLightBuffers lightBuffers;
	private final ClrwlMatrixBuffer matrixBuffer;

	private final Map<IrisRenderingPipeline, PipelineData> pipelineData = new HashMap<>();

	private final ShaderPack pack;
	private final ProgramSet programSet;

	public ClrwlIndirectDrawManager(ShaderPack pack, ProgramSet programSet, ClrwlIndirectPrograms programs)
	{
		this.pack = pack;
		this.programSet = programSet;

		this.programs = programs;
		this.meshPool = new ClrwlMeshPool();
		this.vao = GlVertexArray.create();
		this.stagingBuffer = new StagingBuffer(programs.getFlwPrograms());
		this.lightBuffers = new ClrwlLightBuffers();
		this.matrixBuffer = new ClrwlMatrixBuffer();

		this.meshPool.bind(vao);
	}

	public static ClrwlIndirectDrawManager build(ShaderSources sources, ShaderPack pack, ProgramSet programSet, boolean isFallback)
	{
		var programs = ClrwlIndirectPrograms.build(sources, pack, programSet, isFallback);

		if (programs == null)
		{
			return null;
		}

		return new ClrwlIndirectDrawManager(pack, programSet, programs);
	}

	@Override
	protected <I extends Instance> ClrwlIndirectInstancer<?> create(ClrwlInstancerKey<I> key)
	{
		return new ClrwlIndirectInstancer<>(key, new ClrwlAbstractInstancer.Recreate<>(key, this));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <I extends Instance> void initialize(ClrwlInstancerKey<I> key, ClrwlIndirectInstancer<?> instancer)
	{
		var group = (ClrwlIndirectCullingGroup<I>) cullingGroups.computeIfAbsent(key.type(), t -> new ClrwlIndirectCullingGroup<>(t, programs, programSet));
		group.add((ClrwlIndirectInstancer<I>) instancer, key, meshPool);
	}

	@Override
	public void prepareFrame(LightStorage lightStorage, EnvironmentStorage environmentStorage)
	{
		super.prepareFrame(lightStorage, environmentStorage);

		// Flush instance counts, page mappings, and prune empty groups.
		cullingGroups.values()
				.removeIf(ClrwlIndirectCullingGroup::flushInstancers);

		// Instancers may have been emptied in the above call, now remove them here.
		instancers.values()
				.removeIf(instancer -> instancer.instanceCount() == 0);

		meshPool.flush();

		stagingBuffer.reclaim();

		// Genuinely nothing to do, we can just early out.
		// Still process the mesh pool and reclaim fenced staging regions though.
		if (cullingGroups.isEmpty())
		{
			return;
		}

		lightBuffers.flush(stagingBuffer, lightStorage);
		matrixBuffer.flush(stagingBuffer, environmentStorage);

		// Done in preparePass

		// setPhase(ClrwlRenderingPhase.STAGING_BUFFER_FLUSH, false);
		// stagingBuffer.flush();

		// glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
	}

	public void preparePass(IrisRenderingPipeline pipeline, boolean isShadow)
	{
		if (cullingGroups.isEmpty())
		{
			return;
		}

		stagingBuffer.reclaim();

		for (var group : cullingGroups.values())
		{
			group.upload(stagingBuffer);
		}

		setPhase(ClrwlRenderingPhase.STAGING_BUFFER_FLUSH, false);
		stagingBuffer.flush();

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
	}

	public void renderSolid(IrisRenderingPipeline irisPipeline, boolean isShadow)
	{
		if (cullingGroups.isEmpty())
		{
			return;
		}

		var pipelineData = getPipelineData(irisPipeline);
		var pipelinePrograms = pipelineData.programs();
		var framebuffers = pipelineData.framebuffers();

		if (!isShadow) // hiz cull
		{
			var depthPyramid = pipelineData.depthPyramid();

			setPhase(ClrwlRenderingPhase.INDIRECT_DEPTH_PYRAMID, isShadow);

			depthPyramid.generate();

			glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

			depthPyramid.bindForCull();
		}

		setPhase(ClrwlRenderingPhase.INDIRECT_CULL, isShadow);

		for (var group : cullingGroups.values())
		{
			group.dispatchCull(isShadow);
		}

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
		setPhase(ClrwlRenderingPhase.INDIRECT_CULL_APPLY, isShadow);

		programs.getApplyProgram()
				.bind();

		for (var group : cullingGroups.values())
		{
			group.dispatchApply();
		}

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

		setPhase(ClrwlRenderingPhase.SOLID, isShadow);

		TextureBinder.bindLightAndOverlay();
		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		lightBuffers.bind();
		matrixBuffer.bind();

		var irisSSBO = ((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$getSSBOHolder();

		if (irisSSBO != null)
		{
			((ShaderStorageBufferHolderAccessor) irisSSBO).colorwheel$setupBuffersWithIndexOffset(ClrwlBufferBindings.TOTAL_BINDING_COUNT);
		}

		try
		{
			for (var group : cullingGroups.values())
			{
				group.submitSolid(pipelinePrograms, framebuffers, irisPipeline, isShadow);
			}
		}
		catch (Exception e)
		{
			Colorwheel.LOGGER.error("Got error while rendering solid instances", e);
		}

		if (irisSSBO != null)
		{
			irisSSBO.setupBuffers();
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	public void renderTranslucent(IrisRenderingPipeline irisPipeline, boolean isShadow)
	{
		if (cullingGroups.isEmpty())
		{
			return;
		}

		var pipelineData = getPipelineData(irisPipeline);
		var pipelinePrograms = pipelineData.programs();
		var framebuffers = pipelineData.framebuffers();
		var program = !isShadow
				? ClrwlProgramId.GBUFFERS_TRANSLUCENT
				: ClrwlProgramId.SHADOW_TRANSLUCENT;

		setPhase(ClrwlRenderingPhase.TRANSLUCENT, isShadow);

		TextureBinder.bindLightAndOverlay();
		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		lightBuffers.bind();
		matrixBuffer.bind();

		var irisSSBO = ((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$getSSBOHolder();

		if (irisSSBO != null)
		{
			((ShaderStorageBufferHolderAccessor) irisSSBO).colorwheel$setupBuffersWithIndexOffset(ClrwlBufferBindings.TOTAL_BINDING_COUNT);
		}

		try
		{
			boolean hasOit = false;
			for (var group : cullingGroups.values())
			{
				group.submitTranslucent(pipelinePrograms, framebuffers, irisPipeline, isShadow);

				if (group.hasOitDraws())
				{
					hasOit = true;
				}
			}

top: 		if (hasOit)
			{
				var isOitEnabled = GlCompat.SUPPORTS_OIT && ((ShaderPackAccessor) pack).colorwheel$getProperties().isOitEnabled(program.group());

				if (isOitEnabled)
				{
					var maybeSrc = ((ProgramSetAccessor) programSet).colorwheel$getClrwlProgramSource(program);

					if (maybeSrc.isEmpty())
					{
						break top;
					}

					var framebuffer = framebuffers.getFramebuffer(program);
					var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();

					var directives = maybeSrc.get().getDirectives();
					var oitFramebuffer = framebuffers.getOitFramebuffers(program.group(), programs.getOitPrograms(), properties, programSet.getPackDirectives(), directives);
					var blendOverride = framebuffers.getBlendModeOverride(program).orElse(null);
					var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(program);

					if (framebuffer == null || oitFramebuffer == null)
					{
						break top;
					}

					setPhase(ClrwlRenderingPhase.OIT_DEPTH_RANGE, isShadow);

					oitFramebuffer.prepare();

					oitFramebuffer.prepareDepthRange();
					for (var group : cullingGroups.values())
					{
						group.submitOit(ClrwlPipelineCompiler.OitMode.DEPTH_RANGE, pipelinePrograms, framebuffers, irisPipeline, isShadow, currentRenderPhase);
					}

					if (oitFramebuffer.prepareRenderTransmittance())
					{
						setPhase(ClrwlRenderingPhase.OIT_COEFFICIENTS, isShadow);

						for (var group : cullingGroups.values())
						{
							group.submitOit(ClrwlPipelineCompiler.OitMode.GENERATE_COEFFICIENTS, pipelinePrograms, framebuffers, irisPipeline, isShadow, currentRenderPhase);
						}
					}

	//				oitFramebuffer.renderDepthFromTransmittance();
	//
	//				// Need to bind this again because we just drew a full screen quad for OIT.
	//				vao.bindForDraw();

					setPhase(ClrwlRenderingPhase.OIT_ACCUMULATE, isShadow);

					oitFramebuffer.prepareAccumulate();
					for (var group : cullingGroups.values())
					{
						group.submitOit(ClrwlPipelineCompiler.OitMode.EVALUATE, pipelinePrograms, framebuffers, irisPipeline, isShadow, currentRenderPhase);
					}

					setPhase(ClrwlRenderingPhase.OIT_COMPOSITE, isShadow);

					oitFramebuffer.composite(framebuffer, blendOverride, bufferBlendOverrides, isShadow);
				}
				else
				{
					for (var group : cullingGroups.values())
					{
						group.submitOitAsTranslucent(pipelinePrograms, framebuffers, irisPipeline, isShadow);
					}
				}
			}
		}
		catch (Exception e)
		{
			Colorwheel.LOGGER.error("Got error while rendering translucent instances", e);
		}

		if (irisSSBO != null)
		{
			irisSSBO.setupBuffers();
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	public void renderCrumbling(IrisRenderingPipeline irisPipeline, List<Engine.CrumblingBlock> crumblingBlocks)
	{
		var byType = doCrumblingSort(crumblingBlocks, ClrwlIndirectInstancer::fromState);

		if (byType.isEmpty())
		{
			return;
		}

		var pipelineData = getPipelineData(irisPipeline);
		var programs = pipelineData.programs();
		var framebuffers = pipelineData.framebuffers();

		var framebuffer = framebuffers.getFramebuffer(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK);

		if (framebuffer == null)
		{
			return;
		}

		setPhase(ClrwlRenderingPhase.CRUMBLING, false);

		framebuffer.bind();

		TextureBinder.bindLightAndOverlay();
		ClrwlUniforms.bind(false);
		vao.bindForDraw();

		var blendOverride = framebuffers.getBlendModeOverride(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK).orElse(null);
		var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK);

		var irisSSBO = ((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$getSSBOHolder();

		if (irisSSBO != null)
		{
			((ShaderStorageBufferHolderAccessor) irisSSBO).colorwheel$setupBuffersWithIndexOffset(ClrwlBufferBindings.TOTAL_BINDING_COUNT);
		}

		var crumblingMaterial = SimpleMaterial.builder();

		// Scratch memory for writing draw commands.
		var block = MemoryBlock.malloc(IndirectBuffers.DRAW_COMMAND_STRIDE);

		// Set up the crumbling program buffers. Nothing changes here between draws.
		GlBufferType.DRAW_INDIRECT_BUFFER.bind(crumblingDrawBuffer.handle());
		glBindBufferRange(GL_SHADER_STORAGE_BUFFER, ClrwlBufferBindings.DRAW, crumblingDrawBuffer.handle(), 0, ClrwlIndirectBuffers.DRAW_COMMAND_STRIDE);

		for (var groupEntry : byType.entrySet())
		{
			var byProgress = groupEntry.getValue();

			GroupKey<?> groupKey = groupEntry.getKey();
			ClrwlIndirectCullingGroup<?> cullingGroup = cullingGroups.get(groupKey.instanceType());

			if (cullingGroup == null)
			{
				continue;
			}

			for (var progressEntry : byProgress.int2ObjectEntrySet())
			{
				Samplers.CRUMBLING.makeActive();
				TextureBinder.bind(ModelBakery.BREAKING_LOCATIONS.get(progressEntry.getIntKey()));

				for (var instanceHandlePair : progressEntry.getValue())
				{
					ClrwlIndirectInstancer<?> instancer = instanceHandlePair.getFirst();
					int instanceIndex = instanceHandlePair.getSecond().index;

					for (ClrwlIndirectDraw draw : instancer.draws())
					{
						// Transform the material to be suited for crumbling.
						CommonCrumbling.applyCrumblingProperties(crumblingMaterial, draw.material());

						if (cullingGroup.bindForCrumbling(crumblingMaterial, programs, irisPipeline, blendOverride))
						{
							ClrwlMaterialRenderState.setup(crumblingMaterial, blendOverride, bufferBlendOverrides);

							// Upload the draw command.
							draw.writeWithOverrides(block.ptr(), instanceIndex, crumblingMaterial);
							crumblingDrawBuffer.upload(block);

							// Submit! Everything is already bound by here.
							glDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_INT, 0);
						}
					}
				}
			}
		}

		if (irisSSBO != null)
		{
			irisSSBO.setupBuffers();
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();

		block.free();
	}

	private PipelineData getPipelineData(IrisRenderingPipeline pipeline)
	{
		return pipelineData.computeIfAbsent(pipeline, this::createPipelineData);
	}

	private PipelineData createPipelineData(IrisRenderingPipeline irisPipeline)
	{
		ClrwlIndirectPrograms.PipelineProgramCache pipelinePrograms = programs.createPipelineProgramsCache();
		ClrwlFramebuffers framebuffers = new ClrwlFramebuffers(irisPipeline, pack, programSet);
		ClrwlDepthPyramid depthPyramid = new ClrwlDepthPyramid(ClrwlProgramGroup.GBUFFERS, programs, irisPipeline);

		Colorwheel.LOGGER.info("Created pipeline data for {}", irisPipeline);

		return new PipelineData(pipelinePrograms, framebuffers, depthPyramid);
	}

	public void onIrisPipelineDestroy(IrisRenderingPipeline irisPipeline)
	{
		var data = pipelineData.remove(irisPipeline);

		if (data != null)
		{
			data.delete();
		}

		Colorwheel.LOGGER.info("Deleted pipeline data for {}", irisPipeline);
	}

	private void deletePipelinesData()
	{
		for (var entry : pipelineData.entrySet())
		{
			entry.getValue().delete();
			Colorwheel.LOGGER.info("Deleted pipeline data for {}", entry.getKey());
		}

		pipelineData.clear();
	}

	@Override
	public void delete()
	{
		super.delete();

		cullingGroups.values()
				.forEach(ClrwlIndirectCullingGroup::delete);
		cullingGroups.clear();

		meshPool.delete();
		vao.delete();

		crumblingDrawBuffer.delete();

		lightBuffers.delete();
		matrixBuffer.delete();

		deletePipelinesData();
		programs.delete();
	}

	@Override
	public void triggerFallback()
	{
		Minecraft.getInstance().levelRenderer.allChanged();
	}
}
