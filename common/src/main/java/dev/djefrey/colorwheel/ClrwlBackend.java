package dev.djefrey.colorwheel;

import dev.djefrey.colorwheel.accessors.iris.ProgramSetAccessor;
import dev.djefrey.colorwheel.engine.ClrwlEngine;
import dev.djefrey.colorwheel.instancing.ClrwlInstancedDrawManager;
import dev.djefrey.colorwheel.util.AccumulateTimer;
import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.lib.backend.SimpleBackend;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;

public class ClrwlBackend
{
    public static final Backend IRIS_INSTANCING = SimpleBackend.builder()
            .engineFactory(level -> new ClrwlEngine(level, 256, ClrwlInstancedDrawManager::build))
            .priority(500)
            .supported(() -> GlCompat.SUPPORTS_INSTANCING && isUsingCompatibleShaderPack())
            .register(Colorwheel.rl("instancing"));


    public static void init()
    {

    }
    private static final AccumulateTimer ACCUMULATE_INCOMPATIBLE = new AccumulateTimer(0.5f);

    public static boolean isUsingCompatibleShaderPack()
    {
        Optional<ShaderPack> pack = Iris.getCurrentPack();

        if (pack.isEmpty())
        {
            return false;
        }

        if (!Colorwheel.CONFIG.isFallbackModeEnabled())
        {
            String name = Iris.getCurrentPackName();
            ProgramSet programSet = pack.get().getProgramSet(Iris.getCurrentDimension());
            var isFallback = ((ProgramSetAccessor) programSet).colorwheel$isFallbackMode();

            if (isFallback)
            {
                if (Colorwheel.CONFIG.shouldAlertIncompatiblePack())
                {
                    ACCUMULATE_INCOMPATIBLE.request(() ->
                    {
                        var patch = findPatchedShaderpack(name);

                        Colorwheel.sendEmptyMessage();

                        var packComp = Component.literal(name).setStyle(Style.EMPTY.withItalic(true));
                        Colorwheel.sendErrorMessage(Component.translatable("colorwheel.alert.incompatible_pack", packComp), true);

                        var clickHereComp = Component.translatable("colorwheel.click_here");

                        if (patch.isPresent())
                        {
                            var patchComp = Component.literal(patch.get()).setStyle(Style.EMPTY.withItalic(true));
                            Colorwheel.sendErrorMessage(Component.translatable("colorwheel.alert.incompatible_pack.patch_available", patchComp), false );
                        }
                        else
                        {
                            var fallbackComp = Component.translatable("colorwheel.fallback_mode")
                                    .append(" (").append(clickHereComp).append(")")
                                    .withStyle(
                                        Style.EMPTY
                                            .withUnderlined(true)
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/colorwheel enableFallbackMode on"))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("colorwheel.fallback_mode.enable")))
                                    );

                            Colorwheel.sendErrorMessage(Component.translatable("colorwheel.alert.incompatible_pack.ask_fallback_mode", fallbackComp), false);
                        }

                        var disableComp = Component.translatable("colorwheel.alert.ask_disable")
                                .append(" (").append(clickHereComp).append(")")
                                .withStyle(
                                    Style.EMPTY
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/colorwheel alertIncompatiblePack off"))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("colorwheel.alert.incompatible_pack.disable")))
                                );

                        Colorwheel.sendEmptyMessage();

                        Colorwheel.sendErrorMessage(disableComp, false);

                        Colorwheel.sendEmptyMessage();
                    });
                }

                return false;
            }
        }

        return true;
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
}
