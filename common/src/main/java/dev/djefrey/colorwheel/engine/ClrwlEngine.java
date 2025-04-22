package dev.djefrey.colorwheel.engine;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.djefrey.colorwheel.instancing.ClrwlInstancingDrawManager;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.internal.FlwApiLink;
import dev.engine_room.flywheel.backend.FlwBackend;
import dev.engine_room.flywheel.backend.engine.EngineImpl;
import dev.engine_room.flywheel.backend.engine.uniform.Uniforms;
import dev.engine_room.flywheel.backend.gl.GlStateTracker;
import net.minecraft.world.level.LevelAccessor;

public class ClrwlEngine extends EngineImpl
{
	public ClrwlEngine(LevelAccessor level, ClrwlInstancingDrawManager drawManager, int maxOriginDistance)
	{
        super(level, drawManager, maxOriginDistance);
	}

	@Override
	public void render(RenderContext context)
	{
		ClrwlUniform.update(context);
		super.render(context);
	}
}
