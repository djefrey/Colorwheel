package dev.djefrey.colorwheel.mixin.mc;

import dev.djefrey.colorwheel.engine.uniform.ClrwlOptionsUniforms;
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
        ClrwlOptionsUniforms.update((Options) (Object) this);
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void colorwheel$onSave(CallbackInfo ci)
    {
        ClrwlOptionsUniforms.update((Options) (Object) this);
    }
}
