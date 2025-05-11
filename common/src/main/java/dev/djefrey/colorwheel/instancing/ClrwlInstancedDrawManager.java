package dev.djefrey.colorwheel.instancing;

import dev.djefrey.colorwheel.ClrwlMeshPool;
import dev.djefrey.colorwheel.ClrwlSamplers;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.IrisRenderingPipelineAccessor;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.compile.ClrwlPipelineCompiler;
import dev.djefrey.colorwheel.compile.ClrwlProgram;
import dev.djefrey.colorwheel.compile.ClrwlPrograms;
import dev.djefrey.colorwheel.compile.ClrwlShaderKey;
import dev.djefrey.colorwheel.engine.ClrwlAbstractInstancer;
import dev.djefrey.colorwheel.engine.ClrwlDrawManager;
import dev.djefrey.colorwheel.engine.ClrwlOitFramebuffers;
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
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import org.jetbrains.annotations.Nullable;

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

	@Nullable
	private GlFramebuffer framebuffer;
	@Nullable
	private ClrwlOitFramebuffers oitFramebuffers;

	@Nullable
	private GlFramebuffer shadowFramebuffer;
	@Nullable
	private ClrwlOitFramebuffers shadowOitFramebuffers;

	public ClrwlInstancedDrawManager(NamespacedId dimension, IrisRenderingPipeline irisPipeline, ShaderPack pack, ClrwlPrograms programs)
	{
		this.dimension = dimension;
		this.irisPipeline = irisPipeline;
		this.pack = pack;
		this.programSet = pack.getProgramSet(dimension);

		programs.acquire();
		this.programs = programs;

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

	@Override
	public void renderAll()
	{
		renderSolid();
		renderTranslucent();
	}

	public void renderSolid()
	{
		if (solidDraws.isEmpty())
		{
			return;
		}

		var isShadow = ShadowRenderingState.areShadowsCurrentlyBeingRendered();

		if (isShadow && ((ProgramSetAccessor) programSet).colorwheel$getFlwShadow().isEmpty())
		{
			// No shadow shader, skip
			return;
		}

		var framebuffer = getCurrentFramebuffer(isShadow);

		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();
		light.bind();

		framebuffer.bind();

		submitDraws(solidDraws, isShadow);

		MaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	public void renderTranslucent()
	{
		if (translucentDraws.isEmpty() && oitDraws.isEmpty())
		{
			return;
		}

		var isShadow = ShadowRenderingState.areShadowsCurrentlyBeingRendered();

		if (isShadow && ((ProgramSetAccessor) programSet).colorwheel$getFlwShadow().isEmpty())
		{
			// No shadow shader, skip
			return;
		}

		var framebuffer = getCurrentFramebuffer(isShadow);

		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();
		light.bind();

		if (!translucentDraws.isEmpty())
		{
			framebuffer.bind();

			submitDraws(translucentDraws, isShadow);
		}

		if (!oitDraws.isEmpty())
		{
			var oitFramebuffer = getCurrentOitFramebuffer(isShadow);

			oitFramebuffer.prepare();

			oitFramebuffer.depthRange();

			submitOitDraws(isShadow, ClrwlPipelineCompiler.OitMode.DEPTH_RANGE);

			oitFramebuffer.renderTransmittance();

			submitOitDraws(isShadow, ClrwlPipelineCompiler.OitMode.GENERATE_COEFFICIENTS);

//			oitFramebuffer.renderDepthFromTransmittance();
//
//			// Need to bind this again because we just drew a full screen quad for OIT.
//			vao.bindForDraw();

			oitFramebuffer.accumulate();

			submitOitDraws(isShadow, ClrwlPipelineCompiler.OitMode.EVALUATE);

			oitFramebuffer.composite(framebuffer);
		}

		MaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	private GlFramebuffer getCurrentFramebuffer(boolean isShadow)
	{
		if (!isShadow)
		{
			if (((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$consumeFramebufferChanged() && framebuffer != null)
			{
				((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$destroyColorFramebuffer(framebuffer);
				framebuffer = null;
			}

			if (framebuffer == null)
			{
				ProgramSource source = ((ProgramSetAccessor) programSet).colorwheel$getFlwGbuffers().orElseThrow();
				framebuffer = ((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$createGbuffersFramebuffer(source);
			}

			return framebuffer;
		}
		else
		{
			if (shadowFramebuffer == null)
			{
				ProgramSource source = ((ProgramSetAccessor) programSet).colorwheel$getFlwShadow().orElseThrow();
				shadowFramebuffer = ((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$createShadowFramebuffer(source);
			}

			return shadowFramebuffer;
		}
	}

	private ClrwlOitFramebuffers getCurrentOitFramebuffer(boolean isShadow)
	{
		var oitPrograms = programs.getOitPrograms();

		if (!isShadow)
		{
			if (oitFramebuffers == null)
			{
				oitFramebuffers = new ClrwlOitFramebuffers(oitPrograms, irisPipeline, isShadow, programSet.getPackDirectives());
			}

			return oitFramebuffers;
		}
		else
		{
			if (shadowOitFramebuffers == null)
			{
				shadowOitFramebuffers = new ClrwlOitFramebuffers(oitPrograms, irisPipeline, isShadow, programSet.getPackDirectives());
			}

			return shadowOitFramebuffers;
		}
	}

	private final Set<ClrwlShaderKey> brokenShaders = new HashSet<>();

	private void submitDraws(List<ClrwlInstancedDraw> draws, boolean isShadow)
	{
		for (var drawCall : draws)
		{
			var material = drawCall.material();
			var groupKey = drawCall.groupKey;
			var environment = groupKey.environment();

			var key = new ClrwlShaderKey(groupKey.instanceType(), material, environment.contextShader(), pack, dimension, isShadow, ClrwlPipelineCompiler.OitMode.OFF);

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
				brokenShaders.add(key);
                Colorwheel.LOGGER.error("Could not compile shader: " + key.getPath(), e);
				continue;
			}

			program.bind(drawCall.mesh().baseVertex(), 0, material);
			environment.setupDraw(program.getProgram());
			MaterialRenderState.setup(material);

			ClrwlSamplers.INSTANCE_BUFFER.makeActive();

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

			var key = new ClrwlShaderKey(groupKey.instanceType(), material, environment.contextShader(), pack, dimension, isShadow, oit);

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
				brokenShaders.add(key);
				Colorwheel.LOGGER.error("Could not compile shader: " + key.getPath(), e);
				continue;
			}

			program.bind(drawCall.mesh().baseVertex(),0, material);
			environment.setupDraw(program.getProgram());
			MaterialRenderState.setupOit(material);

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

		var framebuffer = getCurrentFramebuffer(false);
		framebuffer.bind();

		ClrwlUniforms.bind(false);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();

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

						var shaderKey = new ClrwlShaderKey(key.instanceType(), crumblingMaterial, ContextShader.CRUMBLING, pack, dimension, false, ClrwlPipelineCompiler.OitMode.OFF);

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
							brokenShaders.add(shaderKey);
							Colorwheel.LOGGER.error("Could not compile shader: " + shaderKey.getPath(), e);
							continue;
						}

						program.bind(0, index, crumblingMaterial);
						MaterialRenderState.setup(crumblingMaterial);

						Samplers.INSTANCE_BUFFER.makeActive();

						draw.renderOne(instanceTexture);
					}
				}
			}
		}

		MaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
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
		programs.release();
		vao.delete();

		light.delete();

		if (framebuffer != null)
		{
			((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$destroyColorFramebuffer(framebuffer);
			framebuffer = null;
		}

		if (shadowFramebuffer != null)
		{
			((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$destroyShadowFramebuffer(shadowFramebuffer);
			shadowFramebuffer = null;
		}

		if (oitFramebuffers != null)
		{
			oitFramebuffers.delete();
			oitFramebuffers = null;
		}

		if (shadowOitFramebuffers != null)
		{
			shadowOitFramebuffers.delete();
			shadowOitFramebuffers = null;
		}

		super.delete();
	}

	@Override
	protected <I extends Instance> ClrwlInstancedInstancer<I> create(InstancerKey<I> key)
	{
		return new ClrwlInstancedInstancer<>(key, new ClrwlAbstractInstancer.Recreate<>(key, this));
	}

	@Override
	protected <I extends Instance> void initialize(InstancerKey<I> key, ClrwlInstancedInstancer<?> instancer) {
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
	public void triggerFallback() {
		ClrwlPrograms.kill();
		Minecraft.getInstance().levelRenderer.allChanged();
	}
}
