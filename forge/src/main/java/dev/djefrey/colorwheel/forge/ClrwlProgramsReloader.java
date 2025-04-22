package dev.djefrey.colorwheel.forge;

import dev.djefrey.colorwheel.Colorwheel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class ClrwlProgramsReloader implements ResourceManagerReloadListener
{
    public static final ClrwlProgramsReloader INSTANCE = new ClrwlProgramsReloader();

    private ClrwlProgramsReloader() {}

    @Override
    public void onResourceManagerReload(ResourceManager manager)
    {
        Colorwheel.reload(manager);
    }
}
