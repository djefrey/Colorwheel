package dev.djefrey.colorwheel.forge;

import dev.djefrey.colorwheel.ClrwlXplat;
import dev.djefrey.colorwheel.Colorwheel;
import dev.djefrey.colorwheel.Version;
import net.minecraftforge.fml.loading.LoadingModList;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClrwlForgeXplat implements ClrwlXplat
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
            hasFlywheel = ClrwlForge.hasFlywheel();
        }

        return hasFlywheel;
    }

    @Override
    public Version getFlywheelVersion()
    {
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

        return new Version(major, minor, patch);
    }
}
