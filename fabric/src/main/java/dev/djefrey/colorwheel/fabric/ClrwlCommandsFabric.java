package dev.djefrey.colorwheel.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.djefrey.colorwheel.engine.uniform.ClrwlFrameUniforms;
import dev.djefrey.colorwheel.engine.uniform.ClrwlShadowFrameUniforms;
import dev.djefrey.colorwheel.engine.uniform.DebugMode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

public class ClrwlCommandsFabric
{
    public static void registerClientCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext)
    {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommandManager.literal("colorwheel");

        var debug = ClientCommandManager.literal("debug");

        debug.then(ClientCommandManager.literal("shader")
                .then(ClientCommandManager.argument("mode", DebugMode.CommandArgument.INSTANCE)
                        .executes(ctx ->
                        {
                            DebugMode mode = ctx.getArgument("mode", DebugMode.class);
                            ClrwlFrameUniforms.debugMode(mode);
                            return Command.SINGLE_SUCCESS;
                        })));

        debug.then(ClientCommandManager.literal("frustum")
                .then(ClientCommandManager.literal("capture")
                        .executes(ctx ->
                        {
                            ClrwlFrameUniforms.captureFrustum();
                            ClrwlShadowFrameUniforms.captureFrustum();
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(ClientCommandManager.literal("unpause"))
                .executes(ctx ->
                {
                    ClrwlFrameUniforms.unpauseFrustum();
                    ClrwlShadowFrameUniforms.unpauseFrustum();
                    return Command.SINGLE_SUCCESS;
                }));

        command.then(debug);

        command.then(ClientCommandManager.literal("alertIncompatiblePack")
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
                .then(ClientCommandManager.literal("on")
                        .executes(ctx ->
                        {
                            ClrwlConfigFabric.INSTANCE.alertIncompatiblePack = true;
                            ClrwlConfigFabric.INSTANCE.save();
                            ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_incompatible_pack.set.on"));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(ClientCommandManager.literal("off")
                        .executes(ctx ->
                        {
                            ClrwlConfigFabric.INSTANCE.alertIncompatiblePack = false;
                            ClrwlConfigFabric.INSTANCE.save();
                            ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_incompatible_pack.set.off"));

                            return Command.SINGLE_SUCCESS;
                        })));

        command.then(ClientCommandManager.literal("alertBrokenPack")
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
                .then(ClientCommandManager.literal("on")
                        .executes(ctx ->
                        {
                            ClrwlConfigFabric.INSTANCE.alertBrokenPack = true;
                            ClrwlConfigFabric.INSTANCE.save();
                            ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_broken_pack.set.on"));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(ClientCommandManager.literal("off")
                        .executes(ctx ->
                        {
                            ClrwlConfigFabric.INSTANCE.alertBrokenPack = false;
                            ClrwlConfigFabric.INSTANCE.save();
                            ctx.getSource().sendFeedback(Component.translatable("command.colorwheel.alert_broken_pack.set.off"));

                            return Command.SINGLE_SUCCESS;
                        })));

        dispatcher.register(command);
    }
}
