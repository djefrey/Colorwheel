package dev.djefrey.colorwheel.mixin.mc;

import dev.djefrey.colorwheel.Colorwheel;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class OptionsMixin
{
    @Inject(method = "load()V", at = @At("RETURN"))
    private void colorwheel$onLoad(CallbackInfo ci)
    {
        Colorwheel.getSafeFlw().updateOptionsUniform((Options) (Object) this);
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void colorwheel$onSave(CallbackInfo ci)
    {
        Colorwheel.getSafeFlw().updateOptionsUniform((Options) (Object) this);
    }
}
