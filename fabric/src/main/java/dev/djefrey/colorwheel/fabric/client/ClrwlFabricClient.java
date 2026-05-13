package dev.djefrey.colorwheel.fabric.client;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.fabric.ClrwlCommandsFabric;
import dev.djefrey.colorwheel.fabric.ClrwlFabric;
import dev.djefrey.colorwheel.mod_compat.PonderCompat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

public final class ClrwlFabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        if (ClrwlFabric.hasFlywheel())
        {
            ClientCommandRegistrationCallback.EVENT.register(ClrwlCommandsFabric::registerClientCommands);
        }

        ResourceLocation latePhase = Colorwheel.rl("late");

        if (ClrwlFabric.hasPonder())
        {
            WorldRenderEvents.AFTER_ENTITIES.addPhaseOrdering(Event.DEFAULT_PHASE, latePhase);
            WorldRenderEvents.AFTER_ENTITIES.register(latePhase, context -> PonderCompat.getBufferInstance().drawSolid());
            WorldRenderEvents.AFTER_TRANSLUCENT.addPhaseOrdering(Event.DEFAULT_PHASE, latePhase);
            WorldRenderEvents.AFTER_TRANSLUCENT.register(latePhase, context -> PonderCompat.getBufferInstance().drawTranslucent());
        }
    }
}
