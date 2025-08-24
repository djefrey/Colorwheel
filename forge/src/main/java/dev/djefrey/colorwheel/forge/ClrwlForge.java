package dev.djefrey.colorwheel.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

import dev.djefrey.colorwheel.Colorwheel;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Colorwheel.MOD_ID)
public final class ClrwlForge {
    public ClrwlForge()
    {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        var version = modLoadingContext.getActiveContainer().getModInfo().getVersion();

        Colorwheel.init(version.getMajorVersion(),version.getMinorVersion(), version.getIncrementalVersion());

        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClrwlForge.clientInit(forgeEventBus, modEventBus));

        ClrwlConfigForge.INSTANCE.registerSpecs(modLoadingContext);
    }

    private static void clientInit(IEventBus forgeEventBus, IEventBus modEventBus)
    {
        forgeEventBus.addListener(ClrwlCommandsForge::registerClientCommands);
    }
}
