package dev.djefrey.colorwheel.fabric.client;

import dev.djefrey.colorwheel.fabric.ClrwlProgramsReloader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public final class ClrwlFabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(ClrwlProgramsReloader.INSTANCE);
    }
}
