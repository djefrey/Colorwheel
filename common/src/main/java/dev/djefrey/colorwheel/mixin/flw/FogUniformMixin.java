package dev.djefrey.colorwheel.mixin.flw;

import dev.djefrey.colorwheel.engine.uniform.ClrwlFogUniforms;
import dev.engine_room.flywheel.backend.engine.uniform.FogUniforms;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogUniforms.class)
public class FogUniformMixin
{
    @Inject(method = "update",
            at = @At("TAIL"),
            remap = false)
    private static void update(Vector4fc color, float environmentalStart, float environmentalEnd, float renderDistanceStart, float renderDistanceEnd, float skyEnd, float cloudEnd, CallbackInfo ci)
    {
        ClrwlFogUniforms.update(color, environmentalStart, environmentalEnd, renderDistanceStart, renderDistanceEnd, skyEnd, cloudEnd);
    }
}
