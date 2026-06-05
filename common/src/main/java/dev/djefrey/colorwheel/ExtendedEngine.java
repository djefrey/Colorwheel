package dev.djefrey.colorwheel;

import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.backend.RenderContext;

public interface ExtendedEngine extends Engine
{
    void beginFrame(RenderContext ctx);
}
