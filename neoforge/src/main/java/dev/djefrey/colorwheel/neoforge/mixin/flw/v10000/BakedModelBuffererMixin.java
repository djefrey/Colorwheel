package dev.djefrey.colorwheel.neoforge.mixin.flw.v10000;

import dev.djefrey.colorwheel.accessors.flw10000.MeshEmitterAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.BakedModelBufferer")
@Pseudo
public class BakedModelBuffererMixin
{
    @Redirect(method = "bufferBlocks",
            at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/baked/MeshEmitter;prepare(Ldev/engine_room/flywheel/lib/model/baked/BakedModelBufferer$ResultConsumer;)V"),
            require = 0,
            remap = false)
    private static void injectTerrainFlag(@Coerce Object instance, @Coerce Object resultConsumer)
    {
        ((MeshEmitterAccessor) instance).colorwheel$prepareTerrain(resultConsumer);
    }
}
