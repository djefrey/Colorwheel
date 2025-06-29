package dev.djefrey.colorwheel;

import dev.djefrey.colorwheel.accessors.ProgramSetAccessor;
import dev.djefrey.colorwheel.accessors.ShaderPackAccessor;
import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.lib.backend.SimpleBackend;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;

public final class Colorwheel {
    public static final String MOD_ID = "colorwheel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Backend IRIS_INSTANCING = SimpleBackend.builder()
            .engineFactory(level -> new ClrwlEngine(level, 256))
            .priority(450)
            .supported(() -> GlCompat.SUPPORTS_INSTANCING && isUsingCompatibleShaderPack())
            .register(rl("instancing"));

    // Not ideal but good enough for now
    public static ClrwlConfig CONFIG = null;

    public static void init()
    {

    }

    public static ResourceLocation rl(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static final AccumulateTimer ACCUMULATE_INCOMPATIBLE = new AccumulateTimer(0.3f);

    public static boolean isUsingCompatibleShaderPack()
    {
        Optional<ShaderPack> pack = Iris.getCurrentPack();

        if (pack.isEmpty())
        {
            return false;
        }

        String name = ((ShaderPackAccessor) pack.get()).colorwheel$getPackName();
        ProgramSet programSet = pack.get().getProgramSet(Iris.getCurrentDimension());
        var isCompatible = ((ProgramSetAccessor) programSet).colorwheel$getClrwlGbuffers().isPresent();

        if (!isCompatible)
        {
            if (Colorwheel.CONFIG.shouldAlertIncompatiblePack())
            {
                ACCUMULATE_INCOMPATIBLE.request(() ->
                {
                    var patch = findPatchedShaderpack(name);

                    sendErrorMessage(Component.translatable("colorwheel.alert.incompatible_pack", name));
                    patch.ifPresent(s ->
                            sendErrorMessage(Component.translatable("colorwheel.alert.incompatible_pack.patch_available", s)));
                });
            }

            return false;
        }

        WorldRenderingPipeline worldPipeline = Iris.getPipelineManager().getPipelineNullable();

        return worldPipeline instanceof IrisRenderingPipeline;
    }

    public static Optional<String> findPatchedShaderpack(String shaderpack)
    {
        Path shaderpackFolder = Iris.getShaderpacksDirectory();

        try
        {
            Class<?> clazz = Class.forName("dev.djefrey.colorwheel_patcher.ClrwlPatcher");
            Method method = clazz.getMethod("findPatchedShaderpackInFolder", String.class, Path.class);
            Object res = method.invoke(null, shaderpack, shaderpackFolder);

            if (res instanceof Optional<?> maybePatch)
            {
                if (maybePatch.isPresent() && maybePatch.get() instanceof String)
                {
                    return Optional.of((String) maybePatch.get());
                }
            }
        }
        catch (Exception e)
        {
            // Patcher is not installed, do nothing
        }

        return Optional.empty();
    }

    public static void sendWarnMessage(MutableComponent component)
    {
        var player =  Minecraft.getInstance().player;

        if (player == null)
        {
            return;
        }

        var prefixed = Component.literal("[Colorwheel] ");
        prefixed.append(component);

        prefixed.setStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW)));
        player.sendSystemMessage(prefixed);
    }

    public static void sendErrorMessage(Component component)
    {
        var player =  Minecraft.getInstance().player;

        if (player == null)
        {
            return;
        }

        var prefixed = Component.literal("[Colorwheel] ");
        prefixed.append(component);

        prefixed.setStyle(Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED)));
        player.sendSystemMessage(prefixed);
    }
}
