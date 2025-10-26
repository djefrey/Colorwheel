package dev.djefrey.colorwheel.neoforge;

import dev.djefrey.colorwheel.Colorwheel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Colorwheel.MOD_ID)
public final class ClrwlNeoForge
{
    public ClrwlNeoForge(IEventBus modEventBus, ModContainer modContainer)
    {
        boolean hasFlywheel = hasFlywheel();

        Colorwheel.init(hasFlywheel);

        if (hasFlywheel)
        {
            IEventBus gameEventBus = NeoForge.EVENT_BUS;

            ClrwlConfigNeoForge.INSTANCE.registerSpecs(modContainer);

            clientInit(gameEventBus, modEventBus);
        }
    }

    private static void clientInit(IEventBus gameEventBus, IEventBus modEventBus)
    {
        gameEventBus.addListener(ClrwlCommandsNeoForge::registerClientCommands);
    }

    public static boolean hasFlywheel()
    {
        return LoadingModList.get().getModFileById("flywheel") != null;
    }
}
