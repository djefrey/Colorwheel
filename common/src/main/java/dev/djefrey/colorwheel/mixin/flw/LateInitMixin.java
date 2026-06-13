package dev.djefrey.colorwheel.mixin.flw;

import dev.djefrey.colorwheel.accessors.flw.LateInitAccessor;
import dev.engine_room.flywheel.api.backend.Engine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl$LateInit")
public class LateInitMixin implements LateInitAccessor
{
    @Shadow
    @Final
    private Engine engine;

    @Override
    public Engine colorwheel$getEngine()
    {
        return engine;
    }
}
