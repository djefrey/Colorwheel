package dev.djefrey.colorwheel.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.djefrey.colorwheel.engine.uniform.ClrwlFrameUniforms;
import dev.djefrey.colorwheel.engine.uniform.ClrwlShadowFrameUniforms;
import dev.djefrey.colorwheel.engine.uniform.DebugMode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

public class ClrwlCommandsFabric
{
    public static void registerClientCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext)
    {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommands.literal("colorwheel");

        var debug = ClientCommands.literal("debug");

        debug.then(ClientCommands.literal("shader")
                .then(ClientCommands.argument("mode", DebugMode.CommandArgument.INSTANCE)
                        .executes(ctx ->
                        {
                            DebugMode mode = ctx.getArgument("mode", DebugMode.class);
                            ClrwlFrameUniforms.debugMode(mode);
                            return Command.SINGLE_SUCCESS;
                        })));

        debug.then(ClientCommands.literal("frustum")
                .then(ClientCommands.literal("capture")
                        .executes(ctx ->
                        {
                            ClrwlFrameUniforms.captureFrustum();
                            ClrwlShadowFrameUniforms.captureFrustum();
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(ClientCommands.literal("unpause"))
                .executes(ctx ->
                {
                    ClrwlFrameUniforms.unpauseFrustum();
                    ClrwlShadowFrameUniforms.unpauseFrustum();
                    return Command.SINGLE_SUCCESS;
                }));

        command.then(debug);

        command.then(ClientCommands.literal("alertIncompatiblePack")
                .executes(ctx ->
                {
                    if (ClrwlConfigFabric.INSTANCE.alertIncompatiblePack)
                    {
                        ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_incompatible_pack.get.on"));
                    }
                    else
                    {
                        ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_incompatible_pack.get.off"));
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .then(ClientCommands.literal("on")
                        .executes(ctx ->
                        {
                            ClrwlConfigFabric.INSTANCE.alertIncompatiblePack = true;
                            ClrwlConfigFabric.INSTANCE.save();
                            ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_incompatible_pack.set.on"));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(ClientCommands.literal("off")
                        .executes(ctx ->
                        {
                            ClrwlConfigFabric.INSTANCE.alertIncompatiblePack = false;
                            ClrwlConfigFabric.INSTANCE.save();
                            ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_incompatible_pack.set.off"));

                            return Command.SINGLE_SUCCESS;
                        })));

        command.then(ClientCommands.literal("alertBrokenPack")
                .executes(ctx ->
                {
                    if (ClrwlConfigFabric.INSTANCE.alertBrokenPack)
                    {
                        ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_broken_pack.get.on"));
                    }
                    else
                    {
                        ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_broken_pack.get.off"));
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .then(ClientCommands.literal("on")
                        .executes(ctx ->
                        {
                            ClrwlConfigFabric.INSTANCE.alertBrokenPack = true;
                            ClrwlConfigFabric.INSTANCE.save();
                            ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_broken_pack.set.on"));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(ClientCommands.literal("off")
                        .executes(ctx ->
                        {
                            ClrwlConfigFabric.INSTANCE.alertBrokenPack = false;
                            ClrwlConfigFabric.INSTANCE.save();
                            ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_broken_pack.set.off"));

                            return Command.SINGLE_SUCCESS;
                        })));

        command.then(ClientCommands.literal("enableFallbackMode")
                .executes(ctx ->
                {
                    if (ClrwlConfigFabric.INSTANCE.fallbackModeEnabled)
                    {
                        ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.fallback_mode.get.on"));
                    }
                    else
                    {
                        ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.fallback_mode.get.off"));
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .then(ClientCommands.literal("on")
                        .executes(ctx ->
                        {
                            ClrwlConfigFabric.INSTANCE.fallbackModeEnabled = true;
                            ClrwlConfigFabric.INSTANCE.save();
                            ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.fallback_mode.set.on"));
                            Minecraft.getInstance().levelRenderer.allChanged();

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(ClientCommands.literal("off")
                        .executes(ctx ->
                        {
                            ClrwlConfigFabric.INSTANCE.fallbackModeEnabled = false;
                            ClrwlConfigFabric.INSTANCE.save();
                            ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.fallback_mode.set.off"));
                            Minecraft.getInstance().levelRenderer.allChanged();

                            return Command.SINGLE_SUCCESS;
                        })));

        dispatcher.register(command);
    }
}
