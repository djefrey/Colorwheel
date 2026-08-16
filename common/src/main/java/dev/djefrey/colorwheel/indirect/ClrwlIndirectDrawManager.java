package dev.djefrey.colorwheel.indirect;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.iris.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.iris.ShaderPackAccessor;
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
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.engine.TextureBinder;
import dev.engine_room.flywheel.backend.engine.indirect.LightBuffers;
import dev.engine_room.flywheel.backend.engine.indirect.MatrixBuffer;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
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
							   ClrwlDepthPyramid gbuffersDepthPyramid,
							   @Nullable
							   ClrwlDepthPyramid shadowDepthPyramid)
	{
		public void delete()
		{
			programs.delete();
			framebuffers.delete();
			gbuffersDepthPyramid.delete();

			if (shadowDepthPyramid != null)
			{
				shadowDepthPyramid.delete();
			}
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

		setPhase(ClrwlRenderingPhase.STAGING_BUFFER_FLUSH, false);
		stagingBuffer.flush();
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
		var depthPyramid = !isShadow
			? pipelineData.gbuffersDepthPyramid()
			: pipelineData.shadowDepthPyramid();

		if (depthPyramid == null)
		{
			// Should never happen
			return;
		}

		ClrwlUniforms.bind(isShadow);

		setPhase(ClrwlRenderingPhase.INDIRECT_DEPTH_PYRAMID, isShadow);

		depthPyramid.generate();

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
		setPhase(ClrwlRenderingPhase.INDIRECT_CULL, isShadow);

		depthPyramid.bindForCull();

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
			group.dispatchApply(isShadow);
		}

		glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);

		if (cullingGroups.isEmpty())
		{
			return;
		}

		setPhase(ClrwlRenderingPhase.SOLID, isShadow);

		vao.bindForDraw();
		lightBuffers.bind();
		matrixBuffer.bind();

		for (var group : cullingGroups.values())
		{
			group.submitSolid(pipelinePrograms, framebuffers, irisPipeline, isShadow);
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	public void renderTranslucent(IrisRenderingPipeline irisPipeline, boolean isShadow)
	{
		//Mvar pipelineData = getPipelineData(irisPipeline);
//		if (cullingGroups.isEmpty())
//		{
//			return;
//		}
//
//		var pipelinePrograms = pipelineData.programs();
//		var framebuffers = pipelineData.framebuffers();
//		var depthPyramid = !isShadow
//				? pipelineData.gbuffersDepthPyramid()
//				: pipelineData.shadowDepthPyramid();
//
//		if (depthPyramid == null)
//		{
//			// Should never happen
//			return;
//		}
//
//		// TODO: regenerate depth pyramid as depth buffer may have changed. Safe to reuse as it's only used to cull instances.
//
//		boolean useOit = false;
//		for (var group : cullingGroups.values())
//		{
//			if (group.hasOitDraws())
//			{
//				useOit = true;
//				break;
//			}
//		}
//
//		if (useOit)
//		{
//			var program = !isShadow
//					? ClrwlProgramId.GBUFFERS_TRANSLUCENT
//					: ClrwlProgramId.SHADOW_TRANSLUCENT;
//
//			var isOitEnabled = GlCompat.SUPPORTS_OIT && ((ShaderPackAccessor) pack).colorwheel$getProperties().isOitEnabled(program.group());
//
//			if (isOitEnabled)
//			{
//				var maybeSrc = ((ProgramSetAccessor) programSet).colorwheel$getClrwlProgramSource(program);
//
//				if (maybeSrc.isEmpty())
//				{
//					return;
//				}
//
//				var framebuffer = framebuffers.getFramebuffer(program);
//				var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
//
//				var directives = maybeSrc.get().getDirectives();
//				var oitFramebuffer = framebuffers.getOitFramebuffers(program.group(), programs.getOitPrograms(), properties, directives);
//				var blendOverride = framebuffers.getBlendModeOverride(program).orElse(null);
//				var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(program);
//
//				if (framebuffer == null || oitFramebuffer == null)
//				{
//					return;
//				}
//
//				setPhase(ClrwlRenderingPhase.OIT_DEPTH_RANGE, isShadow);
//
//				oitFramebuffer.prepare();
//
//				oitFramebuffer.prepareDepthRange();
//				for (var group : cullingGroups.values())
//				{
//					group.submitTransparent(ClrwlPipelineCompiler.OitMode.DEPTH_RANGE);
//				}
//
//				if (oitFramebuffer.prepareRenderTransmittance())
//				{
//					setPhase(ClrwlRenderingPhase.OIT_COEFFICIENTS, isShadow);
//
//					for (var group : cullingGroups.values())
//					{
//						group.submitTransparent(ClrwlPipelineCompiler.OitMode.GENERATE_COEFFICIENTS);
//					}
//				}
//
////				oitFramebuffer.renderDepthFromTransmittance();
////
////				// Need to bind this again because we just drew a full screen quad for OIT.
////				vao.bindForDraw();
//
//				setPhase(ClrwlRenderingPhase.OIT_ACCUMULATE, isShadow);
//
//				oitFramebuffer.prepareAccumulate();
//				for (var group : cullingGroups.values())
//				{
//					group.submitTransparent(ClrwlPipelineCompiler.OitMode.EVALUATE);
//				}
//
//				setPhase(ClrwlRenderingPhase.OIT_COMPOSITE, isShadow);
//
//				oitFramebuffer.composite(framebuffer, blendOverride, bufferBlendOverrides);
//			}
//		}
//
//		ClrwlMaterialRenderState.reset();
//		TextureBinder.resetLightAndOverlay();
	}

	public void renderCrumbling(IrisRenderingPipeline irisPipeline, List<Engine.CrumblingBlock> crumblingBlocks)
	{
		// TODO
	}

	private PipelineData getPipelineData(IrisRenderingPipeline pipeline)
	{
		return pipelineData.computeIfAbsent(pipeline, this::createPipelineData);
	}

	private PipelineData createPipelineData(IrisRenderingPipeline irisPipeline)
	{
		ClrwlIndirectPrograms.PipelineProgramCache pipelinePrograms = programs.createPipelineProgramsCache();
		ClrwlFramebuffers framebuffers = new ClrwlFramebuffers(irisPipeline, pack, programSet);
		ClrwlDepthPyramid gbuffersDepthPyramid = new ClrwlDepthPyramid(ClrwlProgramGroup.GBUFFERS, programs, irisPipeline);
		ClrwlDepthPyramid shadowDepthPyramid = null;

		if (programSet.getPackDirectives().getShadowDirectives().isShadowEnabled().orElse(true))
		{
			shadowDepthPyramid = new ClrwlDepthPyramid(ClrwlProgramGroup.SHADOW, programs, irisPipeline);
		}

		Colorwheel.LOGGER.info("Created pipeline data for {}", irisPipeline);

		return new PipelineData(pipelinePrograms, framebuffers, gbuffersDepthPyramid, shadowDepthPyramid);
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
