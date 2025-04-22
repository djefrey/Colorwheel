package dev.djefrey.colorwheel.fabric;

import dev.djefrey.colorwheel.Colorwheel;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ClrwlProgramsReloader implements SimpleSynchronousResourceReloadListener
{
    public static final ClrwlProgramsReloader INSTANCE = new ClrwlProgramsReloader();

    private ClrwlProgramsReloader() {}

    @Override
    public ResourceLocation getFabricId()
    {
        return new ResourceLocation(Colorwheel.MOD_ID, "programs");
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager)
    {
        Colorwheel.reload(resourceManager);
    }
}
