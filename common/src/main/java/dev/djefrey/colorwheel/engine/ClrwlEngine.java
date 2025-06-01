package dev.djefrey.colorwheel.engine;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.ShadowRenderContext;
import dev.djefrey.colorwheel.compile.ClrwlPrograms;
import dev.djefrey.colorwheel.engine.embed.EmbeddedEnvironment;
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
import dev.engine_room.flywheel.backend.engine.*;
import dev.engine_room.flywheel.backend.engine.embed.Environment;
import dev.engine_room.flywheel.backend.gl.GlStateTracker;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClrwlEngine implements Engine
{
	public static Map<IrisRenderingPipeline, ClrwlEngine> ENGINES = new HashMap<>();

	private final ClrwlInstancedDrawManager drawManager;
	private final int sqrMaxOriginDistance;
	private final EnvironmentStorage environmentStorage;
	private final LightStorage lightStorage;
	private BlockPos renderOrigin = BlockPos.ZERO;

	private final LevelAccessor level;
	private final NamespacedId dimension;
	private final IrisRenderingPipeline irisPipeline;
	private final ShaderPack pack;

	public ClrwlEngine(LevelAccessor level, int maxOriginDistance)
	{
		ClientLevel clientLevel = (ClientLevel) level;
		this.level = level;
		this.dimension = new NamespacedId(clientLevel.dimension().location().getNamespace(),
				clientLevel.dimension().location().getPath());

		WorldRenderingPipeline worldPipeline = Iris.getPipelineManager().preparePipeline(dimension);
		this.irisPipeline = (IrisRenderingPipeline) worldPipeline;
		this.pack = Iris.getCurrentPack().orElseThrow();

		ClrwlPrograms programs = ClrwlPrograms.build(FlwPrograms.SOURCES);

		this.drawManager = new ClrwlInstancedDrawManager(dimension, irisPipeline, pack, programs);
		this.sqrMaxOriginDistance = maxOriginDistance * maxOriginDistance;
		this.environmentStorage = new EnvironmentStorage();
		this.lightStorage = new LightStorage(level);

		ENGINES.put(irisPipeline, this);
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

	@Override
	public void render(RenderContext context)
	{
		try (var state = GlStateTracker.getRestoreState())
		{
			RenderSystem.replayQueue();

			if (context instanceof ShadowRenderContext shadowContext)
			{
				if (shadowContext.phase() == ShadowRenderContext.RenderPhase.SOLID)
				{
					ClrwlUniforms.update(context);
					environmentStorage.flush();
					drawManager.prepareFrame(lightStorage, environmentStorage);

					drawManager.renderSolid();
				}
				else
				{
					drawManager.renderTranslucent();
				}
			}
			else
			{
				ClrwlUniforms.update(context);
				environmentStorage.flush();
				drawManager.prepareFrame(lightStorage, environmentStorage);

				drawManager.renderAll();
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

	@Override
	public void delete()
	{
		ENGINES.remove(irisPipeline);

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

	public LevelAccessor level() { return level; };

	public <I extends Instance>Instancer<I> instancer(ClrwlInstanceVisual visual, Environment environment, InstanceType<I> type, Model model, int bias)
	{
		return drawManager.getInstancer(visual, environment, type, model, bias);
	}

	public class ClrwlMainVisualizationContext implements VisualizationContext
	{
		private final ClrwlInstancerProvider instancerProvider;
		private final Map<Integer, ClrwlBlockEntityVisualizationContext> blockEntityCtxs = new HashMap<>();
		private final Map<Integer, ClrwlEntityVisualizationContext> entityCtxs = new HashMap<>();

		public ClrwlMainVisualizationContext()
		{
			instancerProvider = new ClrwlInstancerProvider(ClrwlEngine.this, ClrwlInstanceVisual.undefined());
		}

		public VisualizationContext getBlockEntityVisualCtx(int irisId)
		{
			return blockEntityCtxs.computeIfAbsent(irisId, ClrwlBlockEntityVisualizationContext::new);
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
			var out = new EmbeddedEnvironment(ClrwlEngine.this, ClrwlInstanceVisual.undefined(), renderOrigin);
			environmentStorage.track(out);
			return out;
		}
	}

	private class ClrwlBlockEntityVisualizationContext implements VisualizationContext
	{
		private final int irisId;
		private final ClrwlInstancerProvider instancerProvider;

		public ClrwlBlockEntityVisualizationContext(int irisId)
		{
			this.irisId = irisId;
			instancerProvider = new ClrwlInstancerProvider(ClrwlEngine.this, ClrwlInstanceVisual.blockEntity(irisId));
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
			var out = new EmbeddedEnvironment(ClrwlEngine.this, ClrwlInstanceVisual.blockEntity(irisId), renderOrigin);
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
			var out = new EmbeddedEnvironment(ClrwlEngine.this, ClrwlInstanceVisual.entity(irisId), renderOrigin);
			environmentStorage.track(out);
			return out;
		}
	}
}
