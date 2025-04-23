package dev.djefrey.colorwheel.engine;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.djefrey.colorwheel.Colorwheel;
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
import dev.engine_room.flywheel.backend.engine.*;
import dev.engine_room.flywheel.backend.engine.embed.Environment;
import dev.engine_room.flywheel.backend.gl.GlStateTracker;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ClrwlEngine implements Engine
{
	private final ClrwlInstancedDrawManager drawManager;
	private final int sqrMaxOriginDistance;
	private final EnvironmentStorage environmentStorage;
	private final LightStorage lightStorage;
	private BlockPos renderOrigin = BlockPos.ZERO;

	public ClrwlEngine(LevelAccessor level, ClrwlInstancedDrawManager drawManager, int maxOriginDistance)
	{
		this.drawManager = drawManager;
		this.sqrMaxOriginDistance = maxOriginDistance * maxOriginDistance;
		this.environmentStorage = new EnvironmentStorage();
		this.lightStorage = new LightStorage(level);
	}

	@Override
	public VisualizationContext createVisualizationContext()
	{
		return new ClrwlVisualizationContext();
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
			ClrwlUniforms.update(context);

			environmentStorage.flush();
			drawManager.render(lightStorage, environmentStorage);
		}
		catch (Exception e)
		{
			Colorwheel.LOGGER.error("Falling back", e);
			drawManager.triggerFallback();
		}
	}

	@Override
	public void renderCrumbling(RenderContext renderContext, List<CrumblingBlock> list)
	{
		// TODO
	}

	@Override
	public void delete()
	{
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

	public <I extends Instance>Instancer<I> instancer(Environment environment, InstanceType<I> type, Model model, int bias)
	{
		return drawManager.getInstancer(environment, type, model, bias);
	}

	private class ClrwlVisualizationContext implements VisualizationContext
	{
		private final ClrwlInstancerProvider instancerProvider;

		public ClrwlVisualizationContext()
		{
			instancerProvider = new ClrwlInstancerProvider(ClrwlEngine.this);
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
			var out = new EmbeddedEnvironment(ClrwlEngine.this, renderOrigin);
			environmentStorage.track(out);
			return out;
		}
	}
}
