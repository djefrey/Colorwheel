package dev.djefrey.colorwheel.forge;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.djefrey.colorwheel.compile.ClrwlIndirectPrograms;
import dev.djefrey.colorwheel.engine.uniform.ClrwlFrameUniforms;
import dev.djefrey.colorwheel.engine.uniform.ClrwlGbuffersPassUniforms;
import dev.djefrey.colorwheel.engine.uniform.ClrwlShadowPassUniforms;
import dev.djefrey.colorwheel.engine.uniform.DebugMode;
import dev.djefrey.colorwheel.indirect.ClrwlIndirectDrawManager;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.ForgeConfigSpec;

public class ClrwlCommandsForge
{
    public static void registerClientCommands(RegisterClientCommandsEvent event)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("colorwheel");

        ForgeConfigSpec.BooleanValue alertIncompatiblePack = ClrwlConfigForge.INSTANCE.client.alertIncompatiblePack;
        ForgeConfigSpec.BooleanValue alertBrokenPack = ClrwlConfigForge.INSTANCE.client.alertBrokenPack;
        ForgeConfigSpec.BooleanValue fallbackModeEnabled = ClrwlConfigForge.INSTANCE.client.fallbackModeEnabled;

        var debug = Commands.literal("debug");

        debug.then(Commands.literal("shader")
                .then(Commands.argument("mode", DebugMode.CommandArgument.INSTANCE)
                    .executes(ctx ->
                    {
                        DebugMode mode = ctx.getArgument("mode", DebugMode.class);
                        ClrwlFrameUniforms.debugMode(mode);
                        return Command.SINGLE_SUCCESS;
                    })));

        debug.then(Commands.literal("frustum")
                .then(Commands.literal("capture")
                    .executes(ctx ->
                    {
                        ClrwlGbuffersPassUniforms.captureFrustum();
                        ClrwlShadowPassUniforms.captureFrustum();
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(Commands.literal("unpause")
                    .executes(ctx ->
                    {
                        ClrwlGbuffersPassUniforms.unpauseFrustum();
                        ClrwlShadowPassUniforms.unpauseFrustum();
                        return Command.SINGLE_SUCCESS;
                    })));

        debug.then(Commands.literal("cull")
                .then(Commands.literal("two_pass")
                        .then(Commands.literal("on")
                                .executes(ctx ->
                                {
                                    ClrwlIndirectDrawManager.toggleTwoPassCull(true);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("off")
                                .executes(ctx ->
                                {
                                    ClrwlIndirectDrawManager.toggleTwoPassCull(false);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("late")
                        .then(Commands.literal("on")
                                .executes(ctx ->
                                {
                                    ClrwlIndirectDrawManager.toggleLateCull(true);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("off")
                                .executes(ctx ->
                                {
                                    ClrwlIndirectDrawManager.toggleLateCull(false);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("shadow_debug")
                        .then(Commands.literal("on")
                                .executes(ctx ->
                                {
                                    ClrwlIndirectDrawManager.toggleShadowCullDebug(true);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("off")
                                .executes(ctx ->
                                {
                                    ClrwlIndirectDrawManager.toggleShadowCullDebug(false);
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("subgroup_ballot")
                        .then(Commands.literal("on")
                                .executes(ctx ->
                                {
                                    ClrwlIndirectPrograms.toggleSubgroupBallot(true);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("off")
                                .executes(ctx ->
                                {
                                    ClrwlIndirectPrograms.toggleSubgroupBallot(false);
                                    return Command.SINGLE_SUCCESS;
                                }))));

        command.then(debug);

        command.then(Commands.literal("alertIncompatiblePack")
                .executes(ctx ->
                {
                    if (alertIncompatiblePack.get())
                    {
                        sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_incompatible_pack.get.on"));
                    }
                    else
                    {
                        sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_incompatible_pack.get.off"));
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("on")
                        .executes(ctx ->
                        {
                            alertIncompatiblePack.set(true);
                            ClrwlConfigForge.INSTANCE.save();
                            sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_incompatible_pack.set.on"));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("off")
                        .executes(ctx ->
                        {
                            alertIncompatiblePack.set(false);
                            ClrwlConfigForge.INSTANCE.save();
                            sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_incompatible_pack.set.off"));

                            return Command.SINGLE_SUCCESS;
                        })));

        command.then(Commands.literal("alertBrokenPack")
                .executes(ctx ->
                {
                    if (alertBrokenPack.get())
                    {
                        sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_broken_pack.get.on"));
                    }
                    else
                    {
                        sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_broken_pack.get.off"));
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("on")
                        .executes(ctx ->
                        {
                            alertBrokenPack.set(true);
                            ClrwlConfigForge.INSTANCE.save();
                            sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_broken_pack.set.on"));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("off")
                        .executes(ctx ->
                        {
                            alertBrokenPack.set(false);
                            ClrwlConfigForge.INSTANCE.save();
                            sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_broken_pack.set.off"));

                            return Command.SINGLE_SUCCESS;
                        })));

        command.then(Commands.literal("enableFallbackMode")
                .executes(ctx ->
                {
                    if (fallbackModeEnabled.get())
                    {
                        sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.fallback_mode.get.on"));
                    }
                    else
                    {
                        sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.fallback_mode.get.off"));
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("on")
                        .executes(ctx ->
                        {
                            fallbackModeEnabled.set(true);
                            ClrwlConfigForge.INSTANCE.save();
                            sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.fallback_mode.set.on"));
                            Minecraft.getInstance().levelRenderer.allChanged();

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("off")
                        .executes(ctx ->
                        {
                            fallbackModeEnabled.set(false);
                            ClrwlConfigForge.INSTANCE.save();
                            sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.fallback_mode.set.off"));
                            Minecraft.getInstance().levelRenderer.allChanged();

                            return Command.SINGLE_SUCCESS;
                        })));

        event.getDispatcher().register(command);
    }

    private static void sendMessage(CommandSourceStack source, Component message)
    {
        source.sendSuccess(() -> message, true);
    }
}
