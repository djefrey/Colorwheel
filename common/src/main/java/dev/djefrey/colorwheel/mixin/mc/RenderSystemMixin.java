package dev.djefrey.colorwheel.mixin.mc;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.djefrey.colorwheel.util.GlCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin
{
    @Inject(method = "initRenderer", at = @At("RETURN"), remap = false)
    private static void colorwheel$init(int i, boolean bl, CallbackInfo ci)
    {
        GlCompat.init();
    }
}
