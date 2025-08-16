package dev.djefrey.colorwheel.util;

import com.mojang.blaze3d.platform.GlUtil;
import dev.djefrey.colorwheel.Colorwheel;

import java.util.Locale;

public class GlCompat
{
    public static final boolean SUPPORTS_OIT = isOitSupported();

    public static void init()
    {

    }

    private static boolean isOitSupported()
    {
        var isSupported = !GlUtil.getRenderer().toLowerCase(Locale.ROOT).startsWith("apple");

        Colorwheel.LOGGER.info("Is OIT supported: {}", isSupported);
        return isSupported;
    }
}
