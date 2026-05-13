package dev.djefrey.colorwheel.forge;

import dev.djefrey.colorwheel.mod_compat.PonderCompat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

import dev.djefrey.colorwheel.Colorwheel;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.LoadingModList;

@Mod(Colorwheel.MOD_ID)
public final class ClrwlForge
{
    public ClrwlForge()
    {
        ModLoadingContext modLoadingContext = ModLoadingContext.get();
        boolean hasFlywheel = hasFlywheel();

        Colorwheel.init(hasFlywheel);

        if (hasFlywheel)
        {
            IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
            IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClrwlForge.clientInit(forgeEventBus, modEventBus));

            ClrwlConfigForge.INSTANCE.registerSpecs(modLoadingContext);
        }
    }

    private static void clientInit(IEventBus forgeEventBus, IEventBus modEventBus)
    {
        forgeEventBus.addListener(ClrwlCommandsForge::registerClientCommands);

        if (hasPonder())
        {
            forgeEventBus.addListener(EventPriority.LOWEST, ClrwlForge::onRenderWorld);
        }
    }

    public static void onRenderWorld(RenderLevelStageEvent event)
    {
        if (!Colorwheel.getSafeFlw().isColorwheelCurrentBackend())
        {
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)
        {
            PonderCompat.getBufferInstance().drawSolid();
        }
        else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            PonderCompat.getBufferInstance().drawTranslucent();
        }
    }

    public static boolean hasFlywheel()
    {
        return LoadingModList.get().getModFileById("flywheel") != null;
    }

    public static boolean hasPonder()
    {
        return LoadingModList.get().getModFileById("ponder") != null;
    }
}
