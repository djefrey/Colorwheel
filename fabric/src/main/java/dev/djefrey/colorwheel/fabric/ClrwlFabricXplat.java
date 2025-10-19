package dev.djefrey.colorwheel.fabric;

import dev.djefrey.colorwheel.ClrwlXplat;
import dev.djefrey.colorwheel.Colorwheel;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClrwlFabricXplat implements ClrwlXplat
{
    private final Pattern VERSION_REGEX = Pattern.compile("(\\d+).(\\d+).(\\d+).*");

    @Override
    public String getFormattedVersion()
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

        return "%d%02d%02d".formatted(major, minor, incremental);
    }

    private Boolean hasFlywheel = false;

    @Override
    public boolean doesHaveFlywheel()
    {
        if (hasFlywheel == null)
        {
            hasFlywheel = ClrwlFabric.hasFlywheel();
        }

        return hasFlywheel;
    }
}
