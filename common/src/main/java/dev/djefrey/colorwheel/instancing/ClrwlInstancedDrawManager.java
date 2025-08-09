package dev.djefrey.colorwheel.instancing;

import dev.djefrey.colorwheel.engine.ClrwlMeshPool;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramId;
import dev.djefrey.colorwheel.ClrwlSamplers;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.ShaderPackAccessor;
import dev.djefrey.colorwheel.compile.ClrwlPipelineCompiler;
import dev.djefrey.colorwheel.compile.ClrwlProgram;
import dev.djefrey.colorwheel.compile.ClrwlPrograms;
import dev.djefrey.colorwheel.compile.ClrwlShaderKey;
import dev.djefrey.colorwheel.engine.*;
import dev.djefrey.colorwheel.engine.embed.EnvironmentStorage;
import dev.djefrey.colorwheel.engine.uniform.ClrwlUniforms;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.backend.Samplers;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.engine.*;
import dev.engine_room.flywheel.backend.engine.instancing.InstancedLight;
import dev.engine_room.flywheel.backend.gl.TextureBuffer;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.network.chat.Component;

import java.util.*;

public class ClrwlInstancedDrawManager extends ClrwlDrawManager<ClrwlInstancedInstancer<?>>
{
	private static final Comparator<ClrwlInstancedDraw> DRAW_COMPARATOR = Comparator.comparing(ClrwlInstancedDraw::bias)
			.thenComparing(ClrwlInstancedDraw::indexOfMeshInModel)
			.thenComparing(ClrwlInstancedDraw::material, MaterialRenderState.COMPARATOR);

	private final List<ClrwlInstancedDraw> allDraws = new ArrayList<>();
	private boolean needSort = false;

	private final List<ClrwlInstancedDraw> solidDraws = new ArrayList<>();
	private final List<ClrwlInstancedDraw> translucentDraws = new ArrayList<>();
	private final List<ClrwlInstancedDraw> oitDraws = new ArrayList<>();

	private final ClrwlPrograms programs;
	private final ClrwlProgramFramebuffers framebuffers;

	/**
	 * A map of vertex types to their mesh pools.
	 */
	private final ClrwlMeshPool meshPool;
	private final GlVertexArray vao;
	private final TextureBuffer instanceTexture;
	private final InstancedLight light;

	private final NamespacedId dimension;
	private final IrisRenderingPipeline irisPipeline;
	private final ShaderPack pack;
	private final ProgramSet programSet;

	public ClrwlInstancedDrawManager(NamespacedId dimension, IrisRenderingPipeline irisPipeline, ShaderPack pack, ClrwlPrograms programs)
	{
		this.dimension = dimension;
		this.irisPipeline = irisPipeline;
		this.pack = pack;
		this.programSet = pack.getProgramSet(dimension);

		this.programs = programs;
		this.framebuffers = new ClrwlProgramFramebuffers();

		meshPool = new ClrwlMeshPool();
		vao = GlVertexArray.create();
		instanceTexture = new TextureBuffer();
		light = new InstancedLight();

		meshPool.bind(vao);
	}

	@Override
	public void prepareFrame(LightStorage lightStorage, EnvironmentStorage environmentStorage)
	{
		super.prepareFrame(lightStorage, environmentStorage);

		this.instancers.values()
				.removeIf(instancer ->
				{
					if (instancer.instanceCount() == 0)
					{
						instancer.delete();
						return true;
					}
					else
					{
						instancer.updateBuffer();
						return false;
					}
				});

		// Remove the draw calls for any instancers we deleted.
		needSort |= allDraws.removeIf(ClrwlInstancedDraw::deleted);

		if (needSort)
		{
			allDraws.sort(DRAW_COMPARATOR);

			solidDraws.clear();
			translucentDraws.clear();
			oitDraws.clear();

			for (var draw : allDraws)
			{
				if (draw.material().transparency() == Transparency.TRANSLUCENT)
				{
					translucentDraws.add(draw);
				}
				else if (draw.material().transparency() == Transparency.ORDER_INDEPENDENT)
				{
					oitDraws.add(draw);
				}
				else
				{
					solidDraws.add(draw);
				}
			}

			needSort = false;
		}

		meshPool.flush();

		light.flush(lightStorage);
	}

	public void renderSolid()
	{
		if (solidDraws.isEmpty())
		{
			return;
		}

		var isShadow = ShadowRenderingState.areShadowsCurrentlyBeingRendered();

		if (isShadow && ((ProgramSetAccessor) programSet).colorwheel$getClrwlProgramSource(ClrwlProgramId.SHADOW).isEmpty())
		{
			// No base shadow shader, skip
			return;
		}

		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();
		light.bind();

		submitDraws(solidDraws, isShadow);

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	public void renderTranslucent()
	{
		if (translucentDraws.isEmpty() && oitDraws.isEmpty())
		{
			return;
		}

		var isShadow = ShadowRenderingState.areShadowsCurrentlyBeingRendered();

		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();
		light.bind();

		if (!translucentDraws.isEmpty())
		{
			submitDraws(translucentDraws, isShadow);
		}

		if (!oitDraws.isEmpty())
		{
			var program = !isShadow
					? ClrwlProgramId.GBUFFERS_TRANSLUCENT
					: ClrwlProgramId.SHADOW_TRANSLUCENT;

			var isOitEnabled = ((ShaderPackAccessor) pack).colorwheel$getProperties().isOitEnabled(program.group());

			if (isOitEnabled)
			{
				var maybeSrc = ((ProgramSetAccessor) programSet).colorwheel$getClrwlProgramSource(program);

				if (maybeSrc.isEmpty())
				{
					return;
				}

				var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
				var directives = maybeSrc.get().getDirectives();

				var framebuffer = framebuffers.getFramebuffer(program, irisPipeline, programSet);
				var oitFramebuffer = framebuffers.getOitFramebuffers(program.group(), programs.getOitPrograms(), irisPipeline, properties, directives);
				var blendOverride = framebuffers.getBlendModeOverride(program, pack, programSet).orElse(null);
				var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(program, pack, programSet);

				if (framebuffer == null || oitFramebuffer == null)
				{
					return;
				}

				oitFramebuffer.prepare();

				oitFramebuffer.prepareDepthRange();
				submitOitDraws(isShadow, ClrwlPipelineCompiler.OitMode.DEPTH_RANGE);

				if (oitFramebuffer.prepareRenderTransmittance())
				{
					submitOitDraws(isShadow, ClrwlPipelineCompiler.OitMode.GENERATE_COEFFICIENTS);
				}

//				oitFramebuffer.renderDepthFromTransmittance();
//
//				// Need to bind this again because we just drew a full screen quad for OIT.
//				vao.bindForDraw();

				oitFramebuffer.prepareAccumulate();
				submitOitDraws(isShadow, ClrwlPipelineCompiler.OitMode.EVALUATE);

				oitFramebuffer.composite(framebuffer, blendOverride, bufferBlendOverrides);
			}
			else
			{
				submitDraws(oitDraws, isShadow);
			}
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	private final Set<ClrwlShaderKey> brokenShaders = new HashSet<>();

	private void submitDraws(List<ClrwlInstancedDraw> draws, boolean isShadow)
	{
		for (var drawCall : draws)
		{
			var material = drawCall.material();
			var groupKey = drawCall.groupKey;
			var environment = groupKey.environment();

			var programId = ClrwlProgramId.fromTransparency(material.transparency(), isShadow);
			var framebuffer = framebuffers.getFramebuffer(programId, irisPipeline, programSet);

			if (framebuffer == null)
			{
				continue;
			}

			var key = ClrwlShaderKey.fromMaterial(groupKey.instanceType(), material, environment.contextShader(), isShadow, ClrwlPipelineCompiler.OitMode.OFF);

			if (brokenShaders.contains(key))
			{
				continue;
			}

			ClrwlProgram program;

			try
			{
				program = programs.get(key);
			}
			catch (Exception e)
			{
				handleBrokenShader(key, programId, e);
				continue;
			}

			var blendOverride = framebuffers.getBlendModeOverride(programId, pack, programSet).orElse(null);
			var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(programId, pack, programSet);

			program.bind(drawCall.mesh().baseVertex(), 0, material, drawCall.visual(), drawCall.mesh().boundingSphere());
			environment.setupDraw(program.getProgram());
			ClrwlMaterialRenderState.setup(material, blendOverride, bufferBlendOverrides);

			ClrwlSamplers.INSTANCE_BUFFER.makeActive();

			framebuffer.bind();
			drawCall.render(instanceTexture);

			program.unbind();
		}
	}

	private void submitOitDraws(boolean isShadow, ClrwlPipelineCompiler.OitMode oit)
	{
		for (var drawCall : oitDraws)
		{
			var material = drawCall.material();
			var groupKey = drawCall.groupKey;
			var environment = groupKey.environment();

			var key = ClrwlShaderKey.fromMaterial(groupKey.instanceType(), material, environment.contextShader(), isShadow, oit);

			if (brokenShaders.contains(key))
			{
				continue;
			}

			ClrwlProgram program;

			try
			{
				program = programs.get(key);
			}
			catch (Exception e)
			{
				handleBrokenShader(key, isShadow ? ClrwlProgramId.SHADOW_TRANSLUCENT : ClrwlProgramId.GBUFFERS_TRANSLUCENT, e);
				continue;
			}

			program.bind(drawCall.mesh().baseVertex(),0, material, drawCall.visual(), drawCall.mesh().boundingSphere());
			environment.setupDraw(program.getProgram());
			ClrwlMaterialRenderState.setupOit(material);

			Samplers.INSTANCE_BUFFER.makeActive();

			drawCall.render(instanceTexture);

			program.unbind();
		}
	}

	@Override
	public void renderCrumbling(List<Engine.CrumblingBlock> crumblingBlocks)
	{
		var isShadow = ShadowRenderingState.areShadowsCurrentlyBeingRendered();

		if (isShadow)
		{
			return;
		}

		// Sort draw calls into buckets, so we don't have to do as many shader binds.
		var byType = doCrumblingSort(crumblingBlocks, handle ->
		{
			// AbstractInstancer directly implement HandleState, so this check is valid.
			if (handle instanceof ClrwlInstancedInstancer<?> instancer)
			{
				return instancer;
			}
			// This rejects instances that were created by a different engine,
			// and also instances that are hidden or deleted.
			return null;
		});

		if (byType.isEmpty())
		{
			return;
		}

		var framebuffer = framebuffers.getFramebuffer(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK, irisPipeline, programSet);

		if (framebuffer == null)
		{
			return;
		}

		framebuffer.bind();

		ClrwlUniforms.bind(false);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();

		var blendOverride = framebuffers.getBlendModeOverride(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK, pack, programSet).orElse(null);
		var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK, pack, programSet);

		var crumblingMaterial = SimpleMaterial.builder();

		for (var groupEntry : byType.entrySet())
		{
			var byProgress = groupEntry.getValue();

			GroupKey<?> key = groupEntry.getKey();

			for (var progressEntry : byProgress.int2ObjectEntrySet())
			{
				Samplers.CRUMBLING.makeActive();
				TextureBinder.bind(ModelBakery.BREAKING_LOCATIONS.get(progressEntry.getIntKey()));

				for (var instanceHandlePair : progressEntry.getValue())
				{
					ClrwlInstancedInstancer<?> instancer = instanceHandlePair.getFirst();
					var index = instanceHandlePair.getSecond().index;

					for (ClrwlInstancedDraw draw : instancer.draws())
					{
						CommonCrumbling.applyCrumblingProperties(crumblingMaterial, draw.material());

						var shaderKey = ClrwlShaderKey.fromMaterial(key.instanceType(), crumblingMaterial, ContextShader.CRUMBLING, false, ClrwlPipelineCompiler.OitMode.OFF);

						if (brokenShaders.contains(shaderKey))
						{
							continue;
						}

						ClrwlProgram program;

						try
						{
							program = programs.get(shaderKey);
						}
						catch (Exception e)
						{
							handleBrokenShader(shaderKey, ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK, e);
							continue;
						}

						program.bind(0, index, crumblingMaterial, draw.visual(), draw.mesh().boundingSphere());
						ClrwlMaterialRenderState.setup(crumblingMaterial, blendOverride, bufferBlendOverrides);

						Samplers.INSTANCE_BUFFER.makeActive();

						draw.renderOne(instanceTexture);
					}
				}
			}
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	private void handleBrokenShader(ClrwlShaderKey key, ClrwlProgramId baseProgramId, Exception e)
	{
		if (brokenShaders.isEmpty() && Colorwheel.CONFIG.shouldAlertBrokenPack())
		{
			Colorwheel.sendWarnMessage(Component.translatable("colorwheel.alert.broken_pack"));
		}

		brokenShaders.add(key);

		ClrwlProgramId realProgramId = ((ProgramSetAccessor) programSet).colorwheel$getRealClrwlProgram(baseProgramId).orElse(baseProgramId);
		String shaderPath = realProgramId.programName() + "/" + key.getPath();

		Colorwheel.LOGGER.error("Could not compile shader: " + shaderPath, e);
	}

	@Override
	public void delete()
	{
		brokenShaders.clear();

		instancers.values()
				.forEach(ClrwlInstancedInstancer::delete);

		solidDraws.clear();
		translucentDraws.clear();
		oitDraws.clear();

		allDraws.forEach(ClrwlInstancedDraw::delete);
		allDraws.clear();

		meshPool.delete();
		instanceTexture.delete();
		programs.delete();
		vao.delete();

		light.delete();

		framebuffers.delete(irisPipeline);

		super.delete();
	}

	@Override
	protected <I extends Instance> ClrwlInstancedInstancer<I> create(ClrwlInstancerKey<I> key)
	{
		return new ClrwlInstancedInstancer<>(key, new ClrwlAbstractInstancer.Recreate<>(key, this));
	}

	@Override
	protected <I extends Instance> void initialize(ClrwlInstancerKey<I> key, ClrwlInstancedInstancer<?> instancer) {
		instancer.init();

		var meshes = key.model()
				.meshes();
		for (int i = 0; i < meshes.size(); i++) {
			var entry = meshes.get(i);
			var mesh = meshPool.alloc(entry.mesh());

			GroupKey<?> groupKey = new GroupKey<>(key.type(), key.environment());
			ClrwlInstancedDraw instancedDraw = new ClrwlInstancedDraw(instancer, mesh, groupKey, entry.material(), key.bias(), i);

			allDraws.add(instancedDraw);
			needSort = true;
			instancer.addDrawCall(instancedDraw);
		}
	}

	@Override
	public void triggerFallback()
	{
		Minecraft.getInstance().levelRenderer.allChanged();
	}

	private String getShaderPackName()
	{
		return Iris.getCurrentPackName();
	}
}
