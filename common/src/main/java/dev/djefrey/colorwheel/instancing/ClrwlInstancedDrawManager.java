package dev.djefrey.colorwheel.instancing;

import com.google.common.collect.ImmutableList;
import dev.djefrey.colorwheel.*;
import dev.djefrey.colorwheel.accessors.IrisRenderingPipelineAccessor;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.ShaderPackAccessor;
import dev.djefrey.colorwheel.compile.ClrwlPipelineCompiler;
import dev.djefrey.colorwheel.compile.ClrwlProgram;
import dev.djefrey.colorwheel.compile.ClrwlPrograms;
import dev.djefrey.colorwheel.compile.ClrwlShaderKey;
import dev.djefrey.colorwheel.engine.ClrwlAbstractInstancer;
import dev.djefrey.colorwheel.engine.ClrwlDrawManager;
import dev.djefrey.colorwheel.engine.ClrwlInstancerKey;
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
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

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
	private GlFramebuffer gbuffersFramebuffer;

	@Nullable
	private GlFramebuffer gbuffersTranslucentFramebuffer;
	@Nullable
	private ClrwlOitFramebuffers gbuffersTranslucentOitFramebuffers;

	@Nullable
	private GlFramebuffer shadowFramebuffer;
	@Nullable
	private ClrwlOitFramebuffers shadowOitFramebuffers;

	@Nullable
	private GlFramebuffer damagedblockFramebuffer;

	public ClrwlInstancedDrawManager(NamespacedId dimension, IrisRenderingPipeline irisPipeline, ShaderPack pack, ClrwlPrograms programs)
	{
		this.dimension = dimension;
		this.irisPipeline = irisPipeline;
		this.pack = pack;
		this.programSet = pack.getProgramSet(dimension);

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

	public void renderSolid()
	{
		if (solidDraws.isEmpty())
		{
			return;
		}

		var isShadow = ShadowRenderingState.areShadowsCurrentlyBeingRendered();

		if (isShadow && ((ProgramSetAccessor) programSet).colorwheel$getClrwlShadow().isEmpty())
		{
			// No shadow shader, skip
			return;
		}

		var framebuffer = getCurrentFramebuffer(isShadow ? ClrwlShaderPrograms.SHADOW : ClrwlShaderPrograms.GBUFFERS);

		if (framebuffer == null)
		{
			return;
		}

		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();
		light.bind();

		framebuffer.bind();

		var bufferBlendOff = isShadow ? getShadowBufferBlendOff() : getGbuffersBufferBlendOff();

		submitDraws(solidDraws, isShadow, bufferBlendOff);

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

		if (isShadow && ((ProgramSetAccessor) programSet).colorwheel$getClrwlShadow().isEmpty())
		{
			// No shadow shader, skip
			return;
		}

		var framebuffer = getCurrentFramebuffer(isShadow ? ClrwlShaderPrograms.SHADOW : ClrwlShaderPrograms.GBUFFERS_TRANSLUCENT);

		if (framebuffer == null)
		{
			return;
		}

		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();
		light.bind();

		var bufferBlendOff = isShadow ? getShadowBufferBlendOff() : getGbuffersTranslucentBufferBlendOff();

		if (!translucentDraws.isEmpty())
		{
			framebuffer.bind();
			submitDraws(translucentDraws, isShadow, bufferBlendOff);
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

			oitFramebuffer.composite(framebuffer, bufferBlendOff);
		}

		MaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	@Nullable
	private GlFramebuffer getCurrentFramebuffer(ClrwlShaderPrograms program)
	{
		if (((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$consumeFramebufferChanged())
		{
			if (gbuffersFramebuffer != null)
			{
				((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$destroyGbuffersFramebuffer(gbuffersFramebuffer);
				gbuffersFramebuffer = null;
			}

			if (gbuffersTranslucentFramebuffer != null)
			{
				((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$destroyGbuffersFramebuffer(gbuffersTranslucentFramebuffer);
				gbuffersTranslucentFramebuffer = null;
			}

			if (damagedblockFramebuffer != null)
			{
				((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$destroyGbuffersFramebuffer(damagedblockFramebuffer);
				damagedblockFramebuffer = null;
			}
		}

		var programAccessor = ((ProgramSetAccessor) programSet);

		switch (program)
		{
            case GBUFFERS ->
			{
				if (gbuffersFramebuffer == null)
				{
					Optional<ProgramSource> source = programAccessor.colorwheel$getClrwlGbuffers();

					gbuffersFramebuffer = source.map(src ->
									((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$createSolidGbuffersFramebuffer(src))
							.orElse(null);
				}

				return gbuffersFramebuffer;
            }
            case GBUFFERS_TRANSLUCENT ->
			{
				if (gbuffersTranslucentFramebuffer == null)
				{
					Optional<ProgramSource> source = programAccessor.colorwheel$getClrwlGbuffersTranslucent()
														.or(programAccessor::colorwheel$getClrwlGbuffers);

					gbuffersTranslucentFramebuffer = source.map(src ->
									((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$createTranslucentGbuffersFramebuffer(src))
							.orElse(null);
				}

				return gbuffersTranslucentFramebuffer;
            }
            case SHADOW ->
			{
				if (shadowFramebuffer == null)
				{
					Optional<ProgramSource> source = programAccessor.colorwheel$getClrwlShadow();

					shadowFramebuffer = source.map(src ->
									((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$createShadowFramebuffer(src))
							.orElse(null);
				}

				return shadowFramebuffer;
            }
            case DAMAGEDBLOCK ->
			{
				if (damagedblockFramebuffer == null)
				{
					Optional<ProgramSource> source = programAccessor.colorwheel$getClrwlDamagedblock();

					damagedblockFramebuffer = source.map(src ->
									((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$createSolidGbuffersFramebuffer(src))
							.orElse(null);
				}

				return damagedblockFramebuffer;
            }
        }

		throw new RuntimeException("Unknown shader program: " + program);
	}

	private ClrwlOitFramebuffers getCurrentOitFramebuffer(boolean isShadow)
	{
		var oitPrograms = programs.getOitPrograms();

		if (!isShadow)
		{
			if (gbuffersTranslucentOitFramebuffers == null)
			{
				gbuffersTranslucentOitFramebuffers = new ClrwlOitFramebuffers(oitPrograms, irisPipeline, isShadow, programSet.getPackDirectives());
			}

			return gbuffersTranslucentOitFramebuffers;
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

	private List<Integer> gbuffersBlendOffCache;
	private List<Integer> gbuffersTranslucentBlendOffCache;
	private List<Integer> shadowBlendOffCache;
	private List<Integer> damagedblockBlendOffCache;

	private List<Integer> getGbuffersBufferBlendOff()
	{
		if (gbuffersBlendOffCache != null)
		{
			return gbuffersBlendOffCache;
		}

		var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
		Optional<ProgramSource> source = ((ProgramSetAccessor) programSet).colorwheel$getClrwlGbuffers();

		if (source.isEmpty())
		{
			gbuffersBlendOffCache = Collections.emptyList();
			return gbuffersBlendOffCache;
		}

		var res = computeBufferBlendOff(source.get(), properties.getGbuffersBufferBlendOff());
		gbuffersBlendOffCache = res;
		return res;
	}

	private List<Integer> getGbuffersTranslucentBufferBlendOff()
	{
		if (gbuffersTranslucentBlendOffCache != null)
		{
			return gbuffersTranslucentBlendOffCache;
		}

		var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
		Optional<ProgramSource> source = ((ProgramSetAccessor) programSet).colorwheel$getClrwlGbuffersTranslucent();

		if (source.isEmpty())
		{
			return getGbuffersBufferBlendOff();
		}

		var res = computeBufferBlendOff(source.get(), properties.getGbuffersBufferBlendOff());
		gbuffersTranslucentBlendOffCache = res;
		return res;
	}

	private List<Integer> getShadowBufferBlendOff()
	{
		if (shadowBlendOffCache != null)
		{
			return shadowBlendOffCache;
		}

		var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
		Optional<ProgramSource> source = ((ProgramSetAccessor) programSet).colorwheel$getClrwlShadow();

		if (source.isEmpty())
		{
			shadowBlendOffCache = Collections.emptyList();
			return shadowBlendOffCache;
		}

		var res = computeBufferBlendOff(source.get(), properties.getShadowBufferBlendOff());
		shadowBlendOffCache = res;
		return res;
	}

	private List<Integer> getDamagedblockBufferBlendOff()
	{
		if (damagedblockBlendOffCache != null)
		{
			return damagedblockBlendOffCache;
		}

		var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
		Optional<ProgramSource> source = ((ProgramSetAccessor) programSet).colorwheel$getClrwlDamagedblock();

		if (source.isEmpty())
		{
			damagedblockBlendOffCache = Collections.emptyList();
			return damagedblockBlendOffCache;
		}

		var res = computeBufferBlendOff(source.get(), properties.getDamagedblockBufferBlendOff());
		damagedblockBlendOffCache = res;
		return res;
	}

	private List<Integer> computeBufferBlendOff(ProgramSource source, List<Integer> bufferBlendOff)
	{
		if (bufferBlendOff.isEmpty())
		{
			return Collections.emptyList();
		}

		var drawBuffers = source.getDirectives().getDrawBuffers();
		var list = new ArrayList<Integer>();

		for (int i = 0; i < drawBuffers.length; i++)
		{
			int buf = drawBuffers[i];

			if (bufferBlendOff.contains(buf))
			{
				list.add(i);
			}
		}

		return ImmutableList.copyOf(list);
	}

	private final Set<ClrwlShaderKey> brokenShaders = new HashSet<>();

	private void submitDraws(List<ClrwlInstancedDraw> draws, boolean isShadow, List<Integer> bufferBlendOff)
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
				if (brokenShaders.isEmpty() && Colorwheel.CONFIG.shouldAlertBrokenPack())
				{
					Colorwheel.sendWarnMessage(Component.translatable("colorwheel.alert.broken_pack"));
				}

				brokenShaders.add(key);
                Colorwheel.LOGGER.error("Could not compile shader: " + key.getPath(), e);
				continue;
			}

			program.bind(drawCall.mesh().baseVertex(), 0, material, drawCall.visual());
			environment.setupDraw(program.getProgram());
			MaterialRenderState.setup(material);

			for (var buffer : bufferBlendOff)
			{
				IrisRenderSystem.disableBufferBlend(buffer);
			}

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
				if (brokenShaders.isEmpty() && Colorwheel.CONFIG.shouldAlertBrokenPack())
				{
					Colorwheel.sendWarnMessage(Component.translatable("colorwheel.alert.broken_pack"));
				}

				brokenShaders.add(key);
				Colorwheel.LOGGER.error("Could not compile shader: " + key.getPath(), e);
				continue;
			}

			program.bind(drawCall.mesh().baseVertex(),0, material, drawCall.visual());
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

		var framebuffer = getCurrentFramebuffer(ClrwlShaderPrograms.DAMAGEDBLOCK);

		if (framebuffer == null)
		{
			return;
		}

		framebuffer.bind();

		ClrwlUniforms.bind(false);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();

		var bufferBlendOff = getDamagedblockBufferBlendOff();

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
							if (brokenShaders.isEmpty() && Colorwheel.CONFIG.shouldAlertBrokenPack())
							{
								Colorwheel.sendWarnMessage(Component.translatable("colorwheel.alert.broken_pack"));
							}

							brokenShaders.add(shaderKey);
							Colorwheel.LOGGER.error("Could not compile shader: " + shaderKey.getPath(), e);
							continue;
						}

						program.bind(0, index, crumblingMaterial, draw.visual());
						MaterialRenderState.setup(crumblingMaterial);

						for (var buffer : bufferBlendOff)
						{
							IrisRenderSystem.disableBufferBlend(buffer);
						}

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
		programs.delete();
		vao.delete();

		light.delete();

		if (gbuffersFramebuffer != null)
		{
			((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$destroyGbuffersFramebuffer(gbuffersFramebuffer);
			gbuffersFramebuffer = null;
		}

		if (gbuffersTranslucentFramebuffer != null)
		{
			((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$destroyGbuffersFramebuffer(gbuffersTranslucentFramebuffer);
			gbuffersTranslucentFramebuffer = null;
		}

		if (shadowFramebuffer != null)
		{
			((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$destroyShadowFramebuffer(shadowFramebuffer);
			shadowFramebuffer = null;
		}

		if (damagedblockFramebuffer != null)
		{
			((IrisRenderingPipelineAccessor) irisPipeline).colorwheel$destroyGbuffersFramebuffer(damagedblockFramebuffer);
			damagedblockFramebuffer = null;
		}

		if (gbuffersTranslucentOitFramebuffers != null)
		{
			gbuffersTranslucentOitFramebuffers.delete();
			gbuffersTranslucentOitFramebuffers = null;
		}

		if (shadowOitFramebuffers != null)
		{
			shadowOitFramebuffers.delete();
			shadowOitFramebuffers = null;
		}

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
}
