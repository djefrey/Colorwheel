package dev.djefrey.colorwheel.neoforge;

import dev.djefrey.colorwheel.Colorwheel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Colorwheel.MOD_ID)
public final class ClrwlNeoForge
{
    public ClrwlNeoForge(IEventBus modEventBus, ModContainer modContainer)
    {
        Colorwheel.init();

        IEventBus gameEventBus = NeoForge.EVENT_BUS;

        ClrwlConfigNeoForge.INSTANCE.registerSpecs(modContainer);

        clientInit(gameEventBus, modEventBus);
    }

    private static void clientInit(IEventBus gameEventBus, IEventBus modEventBus)
    {
        gameEventBus.addListener(ClrwlCommandsNeoForge::registerClientCommands);
    }
}
