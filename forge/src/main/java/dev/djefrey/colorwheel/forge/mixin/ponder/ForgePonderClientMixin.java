package dev.djefrey.colorwheel.forge.mixin.ponder;

import dev.djefrey.colorwheel.Colorwheel;
import net.createmod.ponder.ForgePonderClient;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ForgePonderClient.ClientEvents.class)
public class ForgePonderClientMixin
{
    // This mixin is used to fix Create's glue rendering with shaders

    @Redirect(method = "onRenderWorld",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraftforge/client/event/RenderLevelStageEvent$Stage;AFTER_PARTICLES:Lnet/minecraftforge/client/event/RenderLevelStageEvent$Stage;",
                    opcode = Opcodes.GETSTATIC),
            require = 0,
            remap = false)
    private static RenderLevelStageEvent.Stage changeRenderStage()
    {
        if (Colorwheel.getSafeFlw().isColorwheelCurrentBackend())
        {
            return RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
        }

        return RenderLevelStageEvent.Stage.AFTER_PARTICLES;
    }
}
