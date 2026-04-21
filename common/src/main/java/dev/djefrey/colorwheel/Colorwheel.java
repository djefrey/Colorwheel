package dev.djefrey.colorwheel;

import dev.djefrey.colorwheel.mod_compat.ClrwlDefaultModCompat;
import dev.djefrey.colorwheel.mod_compat.ClrwlModCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public final class Colorwheel
{
    public static final String MOD_ID = "colorwheel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Not ideal but good enough for now
    public static ClrwlConfig CONFIG = null;
    public static String FORMATTED_VERSION = ClrwlXplat.INSTANCE.getFormattedVersion();

    private static ClrwlSafeFlw SAFE_FLW_INSTANCE = null;
    private static ClrwlModCompat MOD_COMPAT_INSTANCE = null;

    public static void init(boolean hasFlywheel)
    {
        if (hasFlywheel)
        {
            ClrwlBackend.init();
        }
        else
        {
            LOGGER.warn("Flywheel is not installed, Colorwheel will be disabled");
        }
    }

    public static ClrwlSafeFlw getSafeFlw()
    {
        if (SAFE_FLW_INSTANCE == null)
        {
            try
            {
                if (ClrwlXplat.INSTANCE.doesHaveFlywheel())
                {
                    SAFE_FLW_INSTANCE = (ClrwlSafeFlw) Class.forName("dev.djefrey.colorwheel.ClrwlSafeFlwImpl").getConstructor().newInstance();
                }
                else
                {
                    SAFE_FLW_INSTANCE = new ClrwlSafeFlwNoop();
                }
            }
            catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
            {
                LOGGER.error("Could not load ClrwlSafeFlwImpl", e);
                throw new RuntimeException("Error while loading Colorwheel", e);
            }
        }

        return SAFE_FLW_INSTANCE;
    }

    public static ClrwlModCompat getModCompat()
    {
        if (MOD_COMPAT_INSTANCE == null)
        {
            var modCompatClasspath = ClrwlXplat.INSTANCE.getCustomModCompatClasspath();

            try
            {
                if (modCompatClasspath != null)
                {
                    MOD_COMPAT_INSTANCE = (ClrwlModCompat) Class.forName(modCompatClasspath).getConstructor().newInstance();
                }
                else
                {
                    MOD_COMPAT_INSTANCE = new ClrwlDefaultModCompat();
                }
            }
            catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
            {
                LOGGER.error("Could not load mod compat class: " + modCompatClasspath, e);
                throw new RuntimeException("Error while loading Colorwheel", e);
            }
        }

        return MOD_COMPAT_INSTANCE;
    }

    public static ResourceLocation rl(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void sendWarnMessage(MutableComponent component, boolean prefix)
    {
        var player =  Minecraft.getInstance().player;

        if (player == null)
        {
            return;
        }

        var comp = prefix
                ? Component.literal("[Colorwheel] ")
                : Component.empty();

        comp.append(component);

        comp.setStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW)));
        player.sendSystemMessage(comp);
    }

    public static void sendErrorMessage(Component component, boolean prefix)
    {
        var player =  Minecraft.getInstance().player;

        if (player == null)
        {
            return;
        }

        var comp = prefix
                ? Component.literal("[Colorwheel] ")
                : Component.empty();

        comp.append(component);

        comp.setStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED)));
        player.sendSystemMessage(comp);
    }

    public static void sendEmptyMessage()
    {
        var player =  Minecraft.getInstance().player;

        if (player == null)
        {
            return;
        }

        player.sendSystemMessage(Component.empty());
    }
}
