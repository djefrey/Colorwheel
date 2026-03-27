package dev.djefrey.colorwheel.forge.mixin.create;

import com.simibubi.create.foundation.events.ClientEvents;
import dev.djefrey.colorwheel.Colorwheel;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientEvents.class)
public class ClientEventsMixin
{
    // This mixin is used to fix Create's schematic rendering with shaders

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
