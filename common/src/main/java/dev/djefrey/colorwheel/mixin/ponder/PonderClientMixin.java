package dev.djefrey.colorwheel.mixin.ponder;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.mod_compat.PonderCompat;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.ponder.PonderClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PonderClient.class)
public class PonderClientMixin
{
    @ModifyVariable(method = "onRenderWorld",
                    at = @At("STORE"), name = "buffer",
                    require = 0,
                    remap = false)
    private static SuperRenderTypeBuffer useClrwBuffer(SuperRenderTypeBuffer buffer)
    {
        if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend())
        {
            return PonderCompat.getBufferInstance();
        }

        return buffer;
    }

    @WrapOperation(method = "onRenderWorld",
            at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/render/SuperRenderTypeBuffer;draw()V"),
            require = 0,
            remap = false)
    private static void cancelDraw(SuperRenderTypeBuffer instance, Operation<Void> original)
    {
        if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend())
        {
            return;
        }

        original.call(instance);
    }
}
