package dev.djefrey.colorwheel.fabric;

import net.fabricmc.api.ModInitializer;

import dev.djefrey.colorwheel.Colorwheel;
import net.fabricmc.loader.api.FabricLoader;

public final class ClrwlFabric implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        boolean hasFlywheel = hasFlywheel();

        if (hasFlywheel && !isFlywheelVersionSupported())
        {
            throw new RuntimeException("Flywheel version is not compatible with Colorwheel");
        }

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

    public static boolean isFlywheelVersionSupported()
    {
        var dependencies = FabricLoader.getInstance().getModContainer(Colorwheel.MOD_ID).get().getMetadata().getDependencies();
        var flwVersion = FabricLoader.getInstance().getModContainer("flywheel").get().getMetadata().getVersion();

        for (var dep : dependencies)
        {
            if (dep.getModId().equals("flywheel"))
            {
                return dep.matches(flwVersion);
            }
        }

        // Should never happen
        throw new RuntimeException("Flywheel is not a dependency");
    }
}
