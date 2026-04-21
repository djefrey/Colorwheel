package dev.djefrey.colorwheel.fabric;

import dev.djefrey.colorwheel.ClrwlXplat;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.Version;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Nullable;

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
        int patch = Integer.parseInt(matcher.group(3));

        return "%d%02d%02d".formatted(major, minor, patch);
    }

    private Boolean hasFlywheel = null;

    @Override
    public boolean doesHaveFlywheel()
    {
        if (hasFlywheel == null)
        {
            hasFlywheel = ClrwlFabric.hasFlywheel();
        }

        return hasFlywheel;
    }

    private Version flwVersion = null;

    @Override
    public Version getFlywheelVersion()
    {
        if (flwVersion != null)
        {
            return flwVersion;
        }

        var modContainer = FabricLoader.getInstance().getModContainer("flywheel");

        if (modContainer.isEmpty())
        {
            return null;
        }

        String version = modContainer.get().getMetadata().getVersion().getFriendlyString();
        Matcher matcher = VERSION_REGEX.matcher(version);

        if (!matcher.matches())
        {
            throw new IllegalStateException("Could not parse Flywheel mod version");
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));

        flwVersion = new Version(major, minor, patch);
        return flwVersion;
    }

    @Override
    public boolean doesHaveCreate()
    {
        return FabricLoader.getInstance().isModLoaded("create");
    }

    @Override
    public boolean doesHavePonder()
    {
        return FabricLoader.getInstance().isModLoaded("ponder");
    }

    @Override
    public @Nullable String getCustomModCompatClasspath()
    {
        return null;
    }
}
