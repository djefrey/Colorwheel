package dev.djefrey.colorwheel.instancing;

import dev.djefrey.colorwheel.compile.ClrwlInstancedPrograms;
import dev.djefrey.colorwheel.compile.oit.ClrwlOitPrograms;
import dev.djefrey.colorwheel.engine.ClrwlMeshPool;
import dev.djefrey.colorwheel.shaderpack.ClrwlProgramId;
import dev.djefrey.colorwheel.ClrwlSamplers;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.iris.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.iris.ShaderPackAccessor;
import dev.djefrey.colorwheel.compile.ClrwlPipelineCompiler;
import dev.djefrey.colorwheel.compile.ClrwlProgram;
import dev.djefrey.colorwheel.compile.ClrwlShaderKey;
import dev.djefrey.colorwheel.engine.*;
import dev.djefrey.colorwheel.engine.embed.EnvironmentStorage;
import dev.djefrey.colorwheel.engine.uniform.ClrwlUniforms;
import dev.djefrey.colorwheel.util.GlCompat;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.backend.Samplers;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.engine.*;
import dev.engine_room.flywheel.backend.engine.instancing.InstancedLight;
import dev.engine_room.flywheel.backend.gl.TextureBuffer;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClrwlInstancedDrawManager extends ClrwlDrawManager<ClrwlInstancedInstancer<?>>
{
	public record PipelineData(ClrwlInstancedPrograms.PipelineProgramCache programs, ClrwlFramebuffers framebuffers)
	{
		public void delete()
		{
			programs.delete();
			framebuffers.delete();
		}
	}

	private static final Comparator<ClrwlInstancedDraw> DRAW_COMPARATOR = Comparator.comparing(ClrwlInstancedDraw::bias)
			.thenComparing(ClrwlInstancedDraw::indexOfMeshInModel)
			.thenComparing(ClrwlInstancedDraw::material, MaterialRenderState.COMPARATOR);

	private final List<ClrwlInstancedDraw> allDraws = new ArrayList<>();
	private boolean needSort = false;

	private final List<ClrwlInstancedDraw> solidDraws = new ArrayList<>();
	private final List<ClrwlInstancedDraw> translucentDraws = new ArrayList<>();
	private final List<ClrwlInstancedDraw> oitDraws = new ArrayList<>();

	private final Map<IrisRenderingPipeline, PipelineData> pipelineData = new HashMap<>();

	/**
	 * A map of vertex types to their mesh pools.
	 */
	private final ClrwlInstancedPrograms programs;
	private final ClrwlMeshPool meshPool;
	private final GlVertexArray vao;
	private final TextureBuffer instanceTexture;
	private final InstancedLight light;

	private final ShaderPack pack;
	private final ProgramSet programSet;

	public ClrwlInstancedDrawManager(ShaderPack pack, ProgramSet programSet, ClrwlInstancedPrograms programs)
	{
		this.pack = pack;
		this.programSet = programSet;

		this.programs = programs;
		this.meshPool = new ClrwlMeshPool();
		this.vao = GlVertexArray.create();
		this.instanceTexture = new TextureBuffer();
		this.light = new InstancedLight();

		this.meshPool.bind(vao);
	}

	public static ClrwlInstancedDrawManager build(ShaderSources sources, ShaderPack pack, ProgramSet programSet, boolean isFallback)
	{
		var programs = ClrwlInstancedPrograms.build(sources, pack, programSet, isFallback);
		return new ClrwlInstancedDrawManager(pack, programSet, programs);
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

	public void preparePass(IrisRenderingPipeline pipeline, boolean isShadow)
	{
	}

	public void renderSolid(IrisRenderingPipeline irisPipeline, boolean isShadow)
	{
		if (solidDraws.isEmpty())
		{
			return;
		}

		if (isShadow && ((ProgramSetAccessor) programSet).colorwheel$getClrwlProgramSource(ClrwlProgramId.SHADOW).isEmpty())
		{
			// No base shadow shader, skip
			return;
		}

		var pipelineData = getPipelineData(irisPipeline);
		setPhase(ClrwlRenderingPhase.SOLID, isShadow);

		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();
		light.bind();

		submitDraws(solidDraws, pipelineData, irisPipeline, isShadow);

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	public void renderTranslucent(IrisRenderingPipeline irisPipeline, boolean isShadow)
	{
		if (translucentDraws.isEmpty() && oitDraws.isEmpty())
		{
			return;
		}

		var pipelineData = getPipelineData(irisPipeline);
		var framebuffers = pipelineData.framebuffers();

		setPhase(ClrwlRenderingPhase.TRANSLUCENT, isShadow);

		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();
		light.bind();

		if (!translucentDraws.isEmpty())
		{
			submitDraws(translucentDraws, pipelineData, irisPipeline, isShadow);
		}

		if (!oitDraws.isEmpty())
		{
			var program = !isShadow
					? ClrwlProgramId.GBUFFERS_TRANSLUCENT
					: ClrwlProgramId.SHADOW_TRANSLUCENT;

			var isOitEnabled = GlCompat.SUPPORTS_OIT && ((ShaderPackAccessor) pack).colorwheel$getProperties().isOitEnabled(program.group());

			if (isOitEnabled)
			{
				var maybeSrc = ((ProgramSetAccessor) programSet).colorwheel$getClrwlProgramSource(program);

				if (maybeSrc.isEmpty())
				{
					return;
				}

				var properties = ((ShaderPackAccessor) pack).colorwheel$getProperties();
				var directives = maybeSrc.get().getDirectives();

				var framebuffer = framebuffers.getFramebuffer(program);
				var oitFramebuffer = framebuffers.getOitFramebuffers(program.group(), programs.getOitPrograms(), properties,  programSet.getPackDirectives(), directives);
				var blendOverride = framebuffers.getBlendModeOverride(program).orElse(null);
				var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(program);

				if (framebuffer == null || oitFramebuffer == null)
				{
					return;
				}

				setPhase(ClrwlRenderingPhase.OIT_DEPTH_RANGE, isShadow);

				oitFramebuffer.prepare();

				oitFramebuffer.prepareDepthRange();
				submitOitDraws(pipelineData, irisPipeline, isShadow, ClrwlPipelineCompiler.OitMode.DEPTH_RANGE);

				setPhase(ClrwlRenderingPhase.OIT_COEFFICIENTS, isShadow);

				if (oitFramebuffer.prepareRenderTransmittance())
				{
					submitOitDraws(pipelineData, irisPipeline, isShadow, ClrwlPipelineCompiler.OitMode.GENERATE_COEFFICIENTS);
				}

//				oitFramebuffer.renderDepthFromTransmittance();
//
//				// Need to bind this again because we just drew a full screen quad for OIT.
//				vao.bindForDraw();

				setPhase(ClrwlRenderingPhase.OIT_ACCUMULATE, isShadow);

				oitFramebuffer.prepareAccumulate();
				submitOitDraws(pipelineData, irisPipeline, isShadow, ClrwlPipelineCompiler.OitMode.EVALUATE);

				setPhase(ClrwlRenderingPhase.OIT_COMPOSITE, isShadow);

				oitFramebuffer.composite(framebuffer, blendOverride, bufferBlendOverrides, isShadow);
			}
			else
			{
				setPhase(ClrwlRenderingPhase.TRANSLUCENT, isShadow);
				submitDraws(oitDraws, pipelineData, irisPipeline, isShadow);
			}
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	private final Set<ClrwlShaderKey> brokenShaders = new HashSet<>();

	private void submitDraws(List<ClrwlInstancedDraw> draws, PipelineData pipelineData, IrisRenderingPipeline irisPipeline, boolean isShadow)
	{
		var programs = pipelineData.programs();
		var framebuffers = pipelineData.framebuffers();

		GlFramebuffer prevFramebuffer = null;
		ClrwlProgram prevProgram = null;

		for (var drawCall : draws)
		{
			var material = drawCall.material();
			var groupKey = drawCall.groupKey;
			var environment = groupKey.environment();

			var programId = ClrwlProgramId.fromTransparency(material.transparency(), isShadow);
			var framebuffer = framebuffers.getFramebuffer(programId);

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
				program = programs.get(key, irisPipeline);
			}
			catch (Exception e)
			{
				handleBrokenShader(key, programId, e);
				continue;
			}

			if (prevProgram != program)
			{
				if (prevProgram != null)
				{
					prevProgram.unbind();
				}

				program.bind();
			}

			var blendOverride = framebuffers.getBlendModeOverride(programId).orElse(null);
			var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(programId);

			program.setClrwlCommonUniforms(material, blendOverride, currentRenderPhase);
			program.setInstancingUniforms(drawCall.mesh().baseVertex(), 0, material, drawCall.visual(), drawCall.mesh().meshCenter());
			environment.setupDraw(program.getProgram());
			ClrwlMaterialRenderState.setup(material, blendOverride, bufferBlendOverrides);

			ClrwlSamplers.INSTANCE_BUFFER.makeActive();

			if (prevFramebuffer != framebuffer)
			{
				prevFramebuffer = framebuffer;
				framebuffer.bind();
			}

			drawCall.render(instanceTexture);

			prevProgram = program;
		}

		if (prevProgram != null)
		{
			prevProgram.unbind();
		}
	}

	private void submitOitDraws(PipelineData pipelineData, IrisRenderingPipeline irisPipeline, boolean isShadow, ClrwlPipelineCompiler.OitMode oit)
	{
		var programs = pipelineData.programs();
		var framebuffers = pipelineData.framebuffers();

		var programId = isShadow ? ClrwlProgramId.SHADOW_TRANSLUCENT : ClrwlProgramId.GBUFFERS_TRANSLUCENT;
		var blendOverride = framebuffers.getBlendModeOverride(programId).orElse(null);

		ClrwlProgram prevProgram = null;

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
				program = programs.get(key, irisPipeline);
			}
			catch (Exception e)
			{
				handleBrokenShader(key, programId, e);
				continue;
			}

			if (prevProgram != program)
			{
				if (prevProgram != null)
				{
					prevProgram.unbind();
				}

				program.bind();
			}

			program.setClrwlCommonUniforms(material, blendOverride, currentRenderPhase);
			program.setInstancingUniforms(drawCall.mesh().baseVertex(), 0, material, drawCall.visual(), drawCall.mesh().meshCenter());
			environment.setupDraw(program.getProgram());
			ClrwlMaterialRenderState.setupOit(material);

			Samplers.INSTANCE_BUFFER.makeActive();

			drawCall.render(instanceTexture);

			prevProgram = program;
		}

		if (prevProgram != null)
		{
			prevProgram.unbind();
		}
	}

	public void renderCrumbling(IrisRenderingPipeline irisPipeline, List<Engine.CrumblingBlock> crumblingBlocks)
	{
		var pipelineData = getPipelineData(irisPipeline);
		var programs = pipelineData.programs();
		var framebuffers = pipelineData.framebuffers();

		var isShadow = ShadowRenderingState.areShadowsCurrentlyBeingRendered();
		ClrwlProgram prevProgram = null;

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

		var framebuffer = framebuffers.getFramebuffer(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK);

		if (framebuffer == null)
		{
			return;
		}

		setPhase(ClrwlRenderingPhase.CRUMBLING, false);

		framebuffer.bind();

		ClrwlUniforms.bind(false);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();

		var blendOverride = framebuffers.getBlendModeOverride(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK).orElse(null);
		var bufferBlendOverrides = framebuffers.getBufferBlendModeOverrides(ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK);

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
							program = programs.get(shaderKey, irisPipeline);
						}
						catch (Exception e)
						{
							handleBrokenShader(shaderKey, ClrwlProgramId.GBUFFERS_DAMAGEDBLOCK, e);
							continue;
						}

						if (prevProgram != program)
						{
							if (prevProgram != null)
							{
								prevProgram.unbind();
							}

							program.bind();
						}

						program.setClrwlCommonUniforms(crumblingMaterial, blendOverride, currentRenderPhase);
						program.setInstancingUniforms(0, index, crumblingMaterial, draw.visual(), draw.mesh().meshCenter());
						ClrwlMaterialRenderState.setup(crumblingMaterial, blendOverride, bufferBlendOverrides);

						Samplers.INSTANCE_BUFFER.makeActive();

						draw.renderOne(instanceTexture);

						prevProgram = program;
					}
				}
			}
		}

		if (prevProgram != null)
		{
			prevProgram.unbind();
		}

		ClrwlMaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	private void handleBrokenShader(ClrwlShaderKey key, ClrwlProgramId baseProgramId, Exception e)
	{
		if (brokenShaders.isEmpty() && Colorwheel.CONFIG.shouldAlertBrokenPack())
		{
			Colorwheel.sendWarnMessage(Component.translatable("colorwheel.alert.broken_pack"), true);

			var clickHereComp = Component.translatable("colorwheel.click_here");
			var disableComp = Component.translatable("colorwheel.alert.ask_disable")
					.append(" (").append(clickHereComp).append(")")
					.withStyle(
						Style.EMPTY
							.withUnderlined(true)
							.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/colorwheel alertBrokenPack off"))
							.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("colorwheel.alert.broken_pack.disable")))
					);

			Colorwheel.sendWarnMessage(disableComp, false);
		}

		brokenShaders.add(key);

		ClrwlProgramId realProgramId = ((ProgramSetAccessor) programSet).colorwheel$getRealClrwlProgram(baseProgramId).orElse(baseProgramId);
		String shaderPath = realProgramId.programName() + "/" + key.getPath();

		Colorwheel.LOGGER.error("Could not compile shader: " + shaderPath, e);
	}

	private PipelineData getPipelineData(IrisRenderingPipeline pipeline)
	{
		return pipelineData.computeIfAbsent(pipeline, this::createPipelineData);
	}

	private PipelineData createPipelineData(IrisRenderingPipeline irisPipeline)
	{
		var pipelinePrograms = programs.createPipelineProgramsCache();
		var framebuffers = new ClrwlFramebuffers(irisPipeline, pack, programSet);

		Colorwheel.LOGGER.info("Created pipeline data for {}", irisPipeline);

		return new PipelineData(pipelinePrograms, framebuffers);
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
		vao.delete();

		light.delete();

		deletePipelinesData();
		programs.delete();

		super.delete();
	}

	@Override
	protected <I extends Instance> ClrwlInstancedInstancer<I> create(ClrwlInstancerKey<I> key)
	{
		return new ClrwlInstancedInstancer<>(key, new ClrwlAbstractInstancer.Recreate<>(key, this));
	}

	@Override
	protected <I extends Instance> void initialize(ClrwlInstancerKey<I> key, ClrwlInstancedInstancer<?> instancer)
	{
		instancer.init();

		var meshes = key.model()
				.meshes();
		for (int i = 0; i < meshes.size(); i++)
		{
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
