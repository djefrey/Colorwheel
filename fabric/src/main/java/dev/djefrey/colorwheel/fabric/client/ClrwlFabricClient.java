package dev.djefrey.colorwheel.fabric.client;

import dev.djefrey.colorwheel.fabric.ClrwlCommandsFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public final class ClrwlFabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ClientCommandRegistrationCallback.EVENT.register(ClrwlCommandsFabric::registerClientCommands);
    }
}
