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
        boolean hasFlywheel = hasFlywheel();

        Colorwheel.init(hasFlywheel);

        if (hasFlywheel)
        {
            ClrwlConfigFabric.INSTANCE.load();
        }
    }

    public static boolean hasFlywheel()
    {
        return FabricLoader.getInstance().isModLoaded("flywheel");
    }
}
