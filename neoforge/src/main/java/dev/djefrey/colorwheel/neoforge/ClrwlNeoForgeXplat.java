package dev.djefrey.colorwheel.neoforge;

import dev.djefrey.colorwheel.ClrwlXplat;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.Version;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClrwlNeoForgeXplat implements ClrwlXplat
{
    private final Pattern VERSION_REGEX = Pattern.compile("(\\d+).(\\d+).(\\d+).*");

    @Override
    public String getFormattedVersion()
    {
        var version = LoadingModList.get().getModFileById(Colorwheel.MOD_ID).versionString();
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
            hasFlywheel = ClrwlNeoForge.hasFlywheel();
        }

        return hasFlywheel;
    }

    private Version flwBackendVersion;
    private Version flwLibVersion;

    @Override
    public Version getFlywheelBackendVersion()
    {
        if (flwBackendVersion != null)
        {
            return flwBackendVersion;
        }

        var modInfo = LoadingModList.get().getModFileById("flywheel");

        if (modInfo == null)
        {
            return null;
        }

        Matcher matcher = VERSION_REGEX.matcher(modInfo.versionString());

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

        ModFileInfo apiModInfo = null;

        for (var mod : LoadingModList.get().getMods())
        {
            if (mod.getModId().startsWith("dev_engine-room_flywheel_flywheel-neoforge-api"))
            {
                apiModInfo = mod.getOwningFile();
                break;
            }
        }

        if (apiModInfo == null)
        {
            flwLibVersion = flwBackendVersion;
            return flwLibVersion;
        }

        Matcher matcher = VERSION_REGEX.matcher(apiModInfo.versionString());

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
        return LoadingModList.get().getModFileById("create") != null;
    }

    @Override
    public boolean doesHavePonder()
    {
        return LoadingModList.get().getModFileById("ponder") != null;
    }

    @Override
    @Nullable
    public String getCustomModCompatClasspath()
    {
        if (LoadingModList.get().getModFileById("sable") != null)
        {
            return "dev.djefrey.colorwheel.neoforge.mod_compat.sable.ClrwlSableModCompat";
        }

        return null;
    }
}
