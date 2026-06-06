package dev.djefrey.colorwheel.fabric.mixin.ponder;

import dev.djefrey.colorwheel.Colorwheel;
import net.createmod.ponder.FabricPonderClient;
import net.createmod.ponder.PonderClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FabricPonderClient.class)
public class FabricPonderClientMixin
{
    // This mixin is used to fix Create's glue rendering with shaders

    @Inject(method = "onInitializeClient",
            at = @At("TAIL"),
            require = 0,
            remap = false)
    private static void colorwheel$injectAfterEntityRenderEvent(CallbackInfo ci)
    {
        WorldRenderEvents.AFTER_ENTITIES.register(context ->
        {
            if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend())
            {
                PonderClient.onRenderWorld(context.matrixStack());
            }
        });
    }

    @Inject(method = "lambda$onInitializeClient$2",
            at = @At("HEAD"),
            require = 0,
            cancellable = true,
            remap = false)
    private static void colorwheel$cancelCall(WorldRenderContext context, CallbackInfo ci)
    {
        if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend())
        {
            ci.cancel();
        }
    }
}
