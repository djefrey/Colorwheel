package dev.djefrey.colorwheel.neoforge;

import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.mod_compat.PonderCompat;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
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

        if (hasPonder())
        {
            gameEventBus.addListener(EventPriority.LOWEST, ClrwlNeoForge::onRenderWorld);
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
