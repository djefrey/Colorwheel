package dev.djefrey.colorwheel.fabric;

import dev.djefrey.colorwheel.ClrwlXplat;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.Version;
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

    private Version flwBackendVersion = null;
    private Version flwLibVersion = null;

    @Override
    public Version getFlywheelBackendVersion()
    {
        if (flwBackendVersion != null)
        {
            return flwBackendVersion;
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

        flwBackendVersion = new Version(major, minor, patch);
        return flwBackendVersion;
    }
    @Override
    public Version getFlywheelLibVersion()
    {
        if (flwLibVersion != null)
        {
            return flwLibVersion;
        }

        var flwBackendVersion = getFlywheelBackendVersion();

        if (flwBackendVersion == null)
        {
            return null;
        }

        ModContainer apiModContainer = null;

        for (var mod : FabricLoader.getInstance().getAllMods())
        {
            if (mod.getMetadata().getId().startsWith("dev_engine-room_flywheel_flywheel-fabric-api"))
            {
                apiModContainer = mod;
                break;
            }
        }

        if (apiModContainer == null)
        {
            flwLibVersion = flwBackendVersion;
            return flwLibVersion;
        }

        String version = apiModContainer.getMetadata().getVersion().getFriendlyString();
        Matcher matcher = VERSION_REGEX.matcher(version);

        if (!matcher.matches())
        {
            throw new IllegalStateException("Could not parse Flywheel API mod version");
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));

        var apiVersion = new Version(major, minor, patch);

        flwLibVersion = apiVersion.compareTo(flwBackendVersion) >= 0
                    ? apiVersion
                    : flwBackendVersion;
        return flwLibVersion;
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
}
