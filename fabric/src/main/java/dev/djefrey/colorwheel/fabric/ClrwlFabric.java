package dev.djefrey.colorwheel.fabric;

import net.fabricmc.api.ModInitializer;

import dev.djefrey.colorwheel.Colorwheel;

public final class ClrwlFabric implements ModInitializer {
    @Override
    public void onInitialize()
    {
        Colorwheel.init();
    }
}
