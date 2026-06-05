package dev.djefrey.colorwheel.engine;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.ExtendedEngine;
import dev.djefrey.colorwheel.accessors.iris.ProgramSetAccessor;
import dev.djefrey.colorwheel.compile.ClrwlInstancedPrograms;
import dev.djefrey.colorwheel.compile.oit.ClrwlOitPrograms;
import dev.djefrey.colorwheel.engine.embed.EnvironmentStorage;
import dev.djefrey.colorwheel.engine.uniform.ClrwlUniforms;
import dev.djefrey.colorwheel.instancing.ClrwlInstancedDrawManager;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.backend.compile.FlwPrograms;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.engine.embed.Environment;
import dev.engine_room.flywheel.backend.gl.GlStateTracker;
import dev.engine_room.flywheel.backend.glsl.ShaderSources;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClrwlEngine implements ExtendedEngine
{
	public interface DrawManagerFactory
	{
		ClrwlDrawManager<?> build(ShaderSources sources, ShaderPack pack, NamespacedId dimension, boolean isFallback);
	}

	public static List<ClrwlEngine> ENGINES = new ArrayList<>();

	private final ClrwlDrawManager<?> drawManager;
	private final int sqrMaxOriginDistance;
	private final EnvironmentStorage environmentStorage;
	private final LightStorage lightStorage;
	private BlockPos renderOrigin = BlockPos.ZERO;

	private final LevelAccessor level;
	private final NamespacedId dimension;
	private final ShaderPack pack;

	public ClrwlEngine(LevelAccessor level, int maxOriginDistance, DrawManagerFactory drawManagerFactory)
	{
		ClientLevel clientLevel = (ClientLevel) level;
		this.level = level;
		this.dimension = new NamespacedId(clientLevel.dimension().location().getNamespace(),
										  clientLevel.dimension().location().getPath());

		this.pack = Iris.getCurrentPack().orElseThrow();

		var programSet = pack.getProgramSet(dimension);
		var isFallback = (((ProgramSetAccessor) programSet).colorwheel$isFallbackMode());

		this.drawManager = drawManagerFactory.build(FlwPrograms.SOURCES, pack, dimension, isFallback);
		this.sqrMaxOriginDistance = maxOriginDistance * maxOriginDistance;
		this.environmentStorage = new EnvironmentStorage();
		this.lightStorage = Colorwheel.getModCompat().makeLightStorage(level);

		ENGINES.add(this);
	}

	@Override
	public VisualizationContext createVisualizationContext()
	{
		return new ClrwlMainVisualizationContext();
	}

	@Override
	public Plan<RenderContext> createFramePlan()
	{
		return drawManager.createFramePlan().and(lightStorage.createFramePlan());
	}

	@Override
	public Vec3i renderOrigin()
	{
		return renderOrigin;
	}

	@Override
	public boolean updateRenderOrigin(Camera camera)
	{
		Vec3 cameraPos = camera.getPosition();
		double dx = renderOrigin.getX() - cameraPos.x;
		double dy = renderOrigin.getY() - cameraPos.y;
		double dz = renderOrigin.getZ() - cameraPos.z;
		double distanceSqr = dx * dx + dy * dy + dz * dz;

		if (distanceSqr <= sqrMaxOriginDistance) {
			return false;
		}

		renderOrigin = BlockPos.containing(cameraPos);
		drawManager.onRenderOriginChanged();
		return true;
	}

	@Override
	public void lightSections(LongSet longSet)
	{
		lightStorage.sections(longSet);
	}

	@Override
	public void onLightUpdate(SectionPos sectionPos, LightLayer lightLayer)
	{
		lightStorage.onLightUpdate(sectionPos.asLong());
	}

	private boolean shouldPrepareFrame = false;

	@Override
	public void beginFrame(RenderContext ctx)
	{
		shouldPrepareFrame = true;
	}

	private void prepareFrame()
	{
		if (shouldPrepareFrame)
		{
			shouldPrepareFrame = false;

			environmentStorage.flush();
			drawManager.prepareFrame(lightStorage, environmentStorage);
		}
	}

	@Override
	public void render(RenderContext context)
	{
		try (var state = GlStateTracker.getRestoreState())
		{
			prepareFrame();

			if (context instanceof ShadowRenderContext shadowContext)
			{
				if (shadowContext.phase() == ShadowRenderingPhase.SOLID)
				{
					ClrwlUniforms.update(context, pack, dimension);
					drawManager.renderSolid(true);
				}
				else
				{
					drawManager.renderTranslucent(true);
				}
			}
			else if (context instanceof TranslucentRenderContext)
			{
				drawManager.renderTranslucent(false);
			}
			else
			{
				ClrwlUniforms.update(context, pack, dimension);
				drawManager.renderSolid(false);
			}
		}
		catch (Exception e)
		{
			Colorwheel.LOGGER.error("Falling back", e);
			drawManager.triggerFallback();
		}
	}

	@Override
	public void renderCrumbling(RenderContext renderContext, List<CrumblingBlock> crumblingBlocks)
	{
		try (var state = GlStateTracker.getRestoreState())
		{
			drawManager.renderCrumbling(crumblingBlocks);
		}
		catch (Exception e)
		{
			Colorwheel.LOGGER.error("Falling back", e);
			drawManager.triggerFallback();
		}
	}

	public void onIrisPipelineDestroy(IrisRenderingPipeline pipeline)
	{
		drawManager.onIrisPipelineDestroy(pipeline);
	}

	@Override
	public void delete()
	{
		ENGINES.remove(this);

		drawManager.delete();
		lightStorage.delete();
		environmentStorage.delete();
	}

	public EnvironmentStorage environmentStorage() {
		return environmentStorage;
	}

	public LightStorage lightStorage() {
		return lightStorage;
	}

	public LevelAccessor level() { return level; }

    public <I extends Instance>Instancer<I> instancer(ClrwlInstanceVisual visual, Environment environment, InstanceType<I> type, Model model, int bias)
	{
		return drawManager.getInstancer(visual, environment, type, model, bias);
	}

	public class ClrwlMainVisualizationContext implements VisualizationContext
	{
		private final ClrwlInstancerProvider instancerProvider;
		private final Map<ClrwlBlockEntityVisualizationContext.Key, ClrwlBlockEntityVisualizationContext> blockEntityCtxs = new HashMap<>();
		private final Map<Integer, ClrwlEntityVisualizationContext> entityCtxs = new HashMap<>();

		public ClrwlMainVisualizationContext()
		{
			instancerProvider = new ClrwlInstancerProvider(ClrwlEngine.this, ClrwlInstanceVisual.undefined());
		}

		public VisualizationContext getBlockEntityVisualCtx(int irisId, int lightEmission)
		{
			return blockEntityCtxs.computeIfAbsent(new ClrwlBlockEntityVisualizationContext.Key(irisId, lightEmission), ClrwlBlockEntityVisualizationContext::new);
		}

		public VisualizationContext getEntityVisualCtx(int irisId)
		{
			return entityCtxs.computeIfAbsent(irisId, ClrwlEntityVisualizationContext::new);
		}

		@Override
		public InstancerProvider instancerProvider() {
			return instancerProvider;
		}

		@Override
		public Vec3i renderOrigin() {
			return ClrwlEngine.this.renderOrigin();
		}

		@Override
		public VisualEmbedding createEmbedding(Vec3i renderOrigin)
		{
			var out = Colorwheel.getModCompat().makeEmbeddedEnvironment(ClrwlEngine.this, ClrwlInstanceVisual.undefined(), renderOrigin, null);
			environmentStorage.track(out);
			return out;
		}
	}

	private class ClrwlBlockEntityVisualizationContext implements VisualizationContext
	{
		record Key(int irisId, int lightEmission)
		{
		}

		private final int irisId;
		private final int lightEmission;
		private final ClrwlInstancerProvider instancerProvider;

		public ClrwlBlockEntityVisualizationContext(Key key)
		{
			this.irisId = key.irisId;
			this.lightEmission = key.lightEmission;
			instancerProvider = new ClrwlInstancerProvider(ClrwlEngine.this, ClrwlInstanceVisual.blockEntity(irisId, lightEmission));
		}

		@Override
		public InstancerProvider instancerProvider()
		{
			return instancerProvider;
		}

		@Override
		public Vec3i renderOrigin() {
			return ClrwlEngine.this.renderOrigin();
		}

		@Override
		public VisualEmbedding createEmbedding(Vec3i renderOrigin)
		{
			var out = Colorwheel.getModCompat().makeEmbeddedEnvironment(ClrwlEngine.this, ClrwlInstanceVisual.blockEntity(irisId, lightEmission), renderOrigin, null);
			environmentStorage.track(out);
			return out;
		}
	}

	private class ClrwlEntityVisualizationContext implements VisualizationContext
	{
		private final int irisId;
		private final ClrwlInstancerProvider instancerProvider;

		public ClrwlEntityVisualizationContext(int irisId)
		{
			this.irisId = irisId;
			instancerProvider = new ClrwlInstancerProvider(ClrwlEngine.this, ClrwlInstanceVisual.entity(irisId));
		}

		@Override
		public InstancerProvider instancerProvider()
		{
			return instancerProvider;
		}

		@Override
		public Vec3i renderOrigin() {
			return ClrwlEngine.this.renderOrigin();
		}

		@Override
		public VisualEmbedding createEmbedding(Vec3i renderOrigin)
		{
			var out = Colorwheel.getModCompat().makeEmbeddedEnvironment(ClrwlEngine.this, ClrwlInstanceVisual.entity(irisId), renderOrigin, null);
			environmentStorage.track(out);
			return out;
		}
	}
}
