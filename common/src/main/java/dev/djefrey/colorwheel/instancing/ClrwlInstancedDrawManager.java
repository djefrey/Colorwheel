package dev.djefrey.colorwheel.instancing;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.djefrey.colorwheel.ClrwlMeshPool;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.accessors.IrisRenderingPipelineAccessor;
import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.compile.ClrwlProgram;
import dev.djefrey.colorwheel.compile.ClrwlPrograms;
import dev.djefrey.colorwheel.compile.ClrwlShaderKey;
import dev.djefrey.colorwheel.engine.ClrwlAbstractInstancer;
import dev.djefrey.colorwheel.engine.ClrwlDrawManager;
import dev.djefrey.colorwheel.engine.embed.EnvironmentStorage;
import dev.djefrey.colorwheel.engine.uniform.ClrwlUniforms;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.backend.Samplers;
import dev.engine_room.flywheel.backend.engine.*;
import dev.engine_room.flywheel.backend.engine.instancing.InstancedLight;
import dev.engine_room.flywheel.backend.gl.TextureBuffer;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClrwlInstancingDrawManager extends ClrwlDrawManager<ClrwlInstancedInstancer<?>>
{
	private static final Comparator<ClrwlInstancedDraw> DRAW_COMPARATOR = Comparator.comparing(ClrwlInstancedDraw::bias)
			.thenComparing(ClrwlInstancedDraw::indexOfMeshInModel)
			.thenComparing(ClrwlInstancedDraw::material, MaterialRenderState.COMPARATOR);

	private final List<ClrwlInstancedDraw> draws = new ArrayList<>();
	private boolean needSort = false;

	private final ClrwlPrograms programs;
	/**
	 * A map of vertex types to their mesh pools.
	 */
	private final ClrwlMeshPool meshPool;
	private final GlVertexArray vao;
	private final TextureBuffer instanceTexture;
	private final InstancedLight light;

	@Nullable
	private GlFramebuffer framebuffer;

	@Nullable
	private GlFramebuffer shadowFramebuffer;

	public ClrwlInstancingDrawManager(ClrwlPrograms programs) {
		programs.acquire();
		this.programs = programs;

		meshPool = new ClrwlMeshPool();
		vao = GlVertexArray.create();
		instanceTexture = new TextureBuffer();
		light = new InstancedLight();

		meshPool.bind(vao);
	}

	@Override
	public void render(LightStorage lightStorage, EnvironmentStorage environmentStorage)
	{
		var pack = Iris.getCurrentPack().orElseThrow();
		var dimension = Iris.getCurrentDimension();
		var isShadow = ShadowRenderingState.areShadowsCurrentlyBeingRendered();

		if (isShadow && ((ProgramSetAccessor) pack.getProgramSet(dimension)).colorwheel$getFlwShadow().isEmpty())
		{
			// No shadow shader, skip
			return;
		}

		super.render(lightStorage, environmentStorage);

		this.instancers.values()
				.removeIf(instancer -> {
			if (instancer.instanceCount() == 0) {
				instancer.delete();
				return true;
			} else {
				instancer.updateBuffer();
				return false;
			}
		});

		// Remove the draw calls for any instancers we deleted.
		needSort |= draws.removeIf(ClrwlInstancedDraw::deleted);

		if (needSort) {
			draws.sort(DRAW_COMPARATOR);
			needSort = false;
		}

		meshPool.flush();

		light.flush(lightStorage);

		if (draws.isEmpty()) {
			return;
		}

		var framebuffer = getCurrentFramebuffer(pack.getProgramSet(dimension), isShadow);

		if (framebuffer == null)
		{
			return;
		}

		ClrwlUniforms.bind(isShadow);
		vao.bindForDraw();
		TextureBinder.bindLightAndOverlay();
		light.bind();

		framebuffer.bind();

		submitDraws(pack, dimension, isShadow);

		// NOTE: oit cannot work currently as OitFramework DOES NOT target Iris Framebuffers
//		if (!oitDraws.isEmpty()) {
//			oitFramebuffer.prepare();
//
//			oitFramebuffer.depthRange();
//
//			submitOitDraws(PipelineCompiler.OitMode.DEPTH_RANGE);
//
//			oitFramebuffer.renderTransmittance();
//
//			submitOitDraws(PipelineCompiler.OitMode.GENERATE_COEFFICIENTS);
//
//			oitFramebuffer.renderDepthFromTransmittance();
//
//			// Need to bind this again because we just drew a full screen quad for OIT.
//			vao.bindForDraw();
//
//			oitFramebuffer.accumulate();
//
//			submitOitDraws(PipelineCompiler.OitMode.EVALUATE);
//
//			oitFramebuffer.composite();
//		}

		MaterialRenderState.reset();
		TextureBinder.resetLightAndOverlay();
	}

	private GlFramebuffer getCurrentFramebuffer(ProgramSet programSet, boolean isShadow)
	{
		WorldRenderingPipeline worldPipeline = Iris.getPipelineManager().getPipelineNullable();

		if (worldPipeline instanceof IrisRenderingPipeline irisPipeline)
		{
			if (!isShadow)
			{
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

		return null;
	}

	private final Set<ClrwlShaderKey> brokenShaders = new HashSet<>();

	private void submitDraws(ShaderPack pack, NamespacedId dimensionId, boolean isShadow)
	{
		for (var drawCall : draws) {
			var material = drawCall.material();
			var groupKey = drawCall.groupKey;
			var environment = groupKey.environment();

			var key = new ClrwlShaderKey(groupKey.instanceType(), material, environment.contextShader(), pack, dimensionId, isShadow);

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

			program.bind(drawCall.mesh().baseVertex(), material);
			environment.setupDraw(program.getProgram());
			MaterialRenderState.setup(material);

			// TODO: custom MaterialRenderState
			if (material.transparency() == Transparency.ORDER_INDEPENDENT)
			{
				RenderSystem.enableBlend();
				RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			}

			Samplers.INSTANCE_BUFFER.makeActive();

			drawCall.render(instanceTexture);

			program.unbind();
		}
	}

	@Override
	public void delete()
	{
		brokenShaders.clear();

		instancers.values()
				.forEach(ClrwlInstancedInstancer::delete);

		draws.forEach(ClrwlInstancedDraw::delete);
		draws.clear();

		meshPool.delete();
		instanceTexture.delete();
		programs.release();
		vao.delete();

		light.delete();

		if (framebuffer != null)
		{
			framebuffer.destroy();
			framebuffer = null;
		}

		if (shadowFramebuffer != null)
		{
			shadowFramebuffer.destroy();
			shadowFramebuffer = null;
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

			draws.add(instancedDraw);
			needSort = true;
			instancer.addDrawCall(instancedDraw);
		}
	}

	@Override
	public void renderCrumbling(List<Engine.CrumblingBlock> crumblingBlocks) {
//		// Sort draw calls into buckets, so we don't have to do as many shader binds.
//		var byType = doCrumblingSort(crumblingBlocks, handle -> {
//			// AbstractInstancer directly implement HandleState, so this check is valid.
//			if (handle instanceof InstancedInstancer<?> instancer) {
//				return instancer;
//			}
//			// This rejects instances that were created by a different engine,
//			// and also instances that are hidden or deleted.
//			return null;
//		});
//
//		if (byType.isEmpty()) {
//			return;
//		}
//
//		var crumblingMaterial = SimpleMaterial.builder();
//
//		Uniforms.bindAll();
//		vao.bindForDraw();
//		TextureBinder.bindLightAndOverlay();
//
//		for (var groupEntry : byType.entrySet()) {
//			var byProgress = groupEntry.getValue();
//
//			GroupKey<?> key = groupEntry.getKey();
//
//			for (var progressEntry : byProgress.int2ObjectEntrySet()) {
//				Samplers.CRUMBLING.makeActive();
//				TextureBinder.bind(ModelBakery.BREAKING_LOCATIONS.get(progressEntry.getIntKey()));
//
//				for (var instanceHandlePair : progressEntry.getValue()) {
//					InstancedInstancer<?> instancer = instanceHandlePair.getFirst();
//					var index = instanceHandlePair.getSecond().index;
//
//					for (InstancedDraw draw : instancer.draws()) {
//						CommonCrumbling.applyCrumblingProperties(crumblingMaterial, draw.material());
//
//						var shader = programs.get(key.instanceType(), ContextShader.CRUMBLING, crumblingMaterial, PipelineCompiler.OitMode.OFF);
//						var program = ((ShaderInstanceAccessor) shader).flwcompat$getProgram();
//						shader.apply();
//						program.setInt("_flw_baseInstance", index);
//						uploadMaterialUniform(program, crumblingMaterial);
//
//						MaterialRenderState.setup(crumblingMaterial);
//
//						Samplers.INSTANCE_BUFFER.makeActive();
//
//						draw.renderOne(instanceTexture);
//					}
//				}
//			}
//		}
//
//		MaterialRenderState.reset();
//		TextureBinder.resetLightAndOverlay();
	}

	@Override
	public void triggerFallback() {
		ClrwlPrograms.kill();
		Minecraft.getInstance().levelRenderer.allChanged();
	}
}
