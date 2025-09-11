package dev.djefrey.colorwheel.fabric.mixin.flw;

import dev.djefrey.colorwheel.accessors.MeshEmitterAccessor;
import dev.djefrey.colorwheel.accessors.UniversalMeshEmitterAccessor;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.engine_room.flywheel.lib.model.baked.BakedModelBufferer")
public class BakedModelBuffererMixin
{
    @Redirect(method = "bufferBlocks",
            at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/baked/MeshEmitter;prepare(Ldev/engine_room/flywheel/lib/model/baked/BakedModelBufferer$ResultConsumer;)V"),
            remap = false)
    private static void injectTerrainFlag(@Coerce Object instance, @Coerce Object resultConsumer)
    {
        ((MeshEmitterAccessor) instance).colorwheel$prepareTerrain(resultConsumer);
    }

    @Redirect(method = "bufferBlocks",
            at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/lib/model/baked/UniversalMeshEmitter;prepare(Lnet/minecraft/client/renderer/RenderType;)V"))
    private static void injectUniversalTerrainFlag(@Coerce Object instance, RenderType renderType)
    {
        ((UniversalMeshEmitterAccessor) instance).colorwheel$prepareTerrain(renderType);
    }
}
