package dev.djefrey.colorwheel.fabric.mixin.create;

import com.simibubi.create.foundation.events.ClientEvents;
import dev.djefrey.colorwheel.Colorwheel;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientEvents.class)
public abstract class ClientEventsMixin
{
    // This mixin is used to fix Create's schematic rendering with shaders

    @Unique
    private static boolean isColorwheelCall = false;

    @Inject(method = "register",
            at = @At("TAIL"),
            require = 0,
            remap = false)
    private static void colorwheel$injectAfterEntityRenderEvent(CallbackInfo ci)
    {
        WorldRenderEvents.AFTER_ENTITIES.register(context ->
        {
            if (!Colorwheel.getSafeFlw().isColorwheelCurrentBackend())
            {
                return;
            }

            isColorwheelCall = true;
            ClientEvents.onRenderWorld(context);
            isColorwheelCall = false;
        });
    }

    @Inject(method = "onRenderWorld",
            at = @At("HEAD"),
            require = 0,
            cancellable = true,
            remap = false)
    private static void colorwheel$cancelIncorrectCalls(WorldRenderContext event, CallbackInfo ci)
    {
        if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend() && !isColorwheelCall)
        {
            ci.cancel();
        }
    }
}
