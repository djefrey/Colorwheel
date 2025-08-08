package dev.djefrey.colorwheel.mixin.iris;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.gl.shader.StandardMacros;
import net.irisshaders.iris.helpers.StringPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@Mixin(StandardMacros.class)
public abstract class StandardMacrosMixin
{
    @Shadow
    private static void define(List<StringPair> defines, String key)
    {
        throw new RuntimeException();
    }

    @Inject(method = "createStandardEnvironmentDefines()Lcom/google/common/collect/ImmutableList;",
            at = @At(value = "CONSTANT", args = "stringValue=IS_IRIS"),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            remap = false
    )
    private static void colorwheel$injectClrwlStandardDefines(CallbackInfoReturnable<ImmutableList<StringPair>> cir, ArrayList<StringPair> standardDefines)
    {
        define(standardDefines, "IS_COLORWHEEL");
    }
}
