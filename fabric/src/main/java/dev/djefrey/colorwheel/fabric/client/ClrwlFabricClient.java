package dev.djefrey.colorwheel.fabric.client;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.fabric.ClrwlCommandsFabric;
import dev.djefrey.colorwheel.fabric.ClrwlFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public final class ClrwlFabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        if (ClrwlFabric.hasFlywheel())
        {
            ClientCommandRegistrationCallback.EVENT.register(ClrwlCommandsFabric::registerClientCommands);
        }
    }
}
