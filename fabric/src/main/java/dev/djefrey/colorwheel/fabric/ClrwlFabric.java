package dev.djefrey.colorwheel.fabric;

import net.fabricmc.api.ModInitializer;

import dev.djefrey.colorwheel.Colorwheel;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClrwlFabric implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        Colorwheel.init();

        ClrwlConfigFabric.INSTANCE.load();
    }
}
