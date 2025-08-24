package dev.djefrey.colorwheel.fabric;

import net.fabricmc.api.ModInitializer;

import dev.djefrey.colorwheel.Colorwheel;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClrwlFabric implements ModInitializer
{
    private final Pattern VERSION_REGEX = Pattern.compile("(\\d+).(\\d+).(\\d+).*");

    @Override
    public void onInitialize()
    {
        ModContainer clrwl = FabricLoader.getInstance().getModContainer(Colorwheel.MOD_ID)
                        .orElseThrow(() -> new IllegalStateException("Could not get Colorwheel mod container"));

        String version = clrwl.getMetadata().getVersion().getFriendlyString();
        Matcher matcher = VERSION_REGEX.matcher(version);

        if (!matcher.matches())
        {
            throw new IllegalStateException("Could not parse Colorwheel mod version");
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int incremental = Integer.parseInt(matcher.group(3));

        Colorwheel.init(major, minor, incremental);

        ClrwlConfigFabric.INSTANCE.load();
    }
}
