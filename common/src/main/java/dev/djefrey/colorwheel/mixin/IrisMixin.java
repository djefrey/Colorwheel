package dev.djefrey.colorwheel.mixin;

import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Iris.class)
public class IrisMixin
{
    @Inject(method = "reload()V",
            at = @At("RETURN"),
            remap = false)
    private static void onReload(CallbackInfo ci)
    {
        Minecraft.getInstance().levelRenderer.allChanged();
    }
}
