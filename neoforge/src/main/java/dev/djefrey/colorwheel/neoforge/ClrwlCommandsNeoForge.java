package dev.djefrey.colorwheel.neoforge;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ClrwlCommandsNeoForge
{
    public static void registerClientCommands(RegisterClientCommandsEvent event)
    {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("colorwheel");

        ModConfigSpec.BooleanValue alertIncompatiblePack = ClrwlConfigNeoForge.INSTANCE.client.alertIncompatiblePack;
        ModConfigSpec.BooleanValue alertBrokenPack = ClrwlConfigNeoForge.INSTANCE.client.alertBrokenPack;

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
                            sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_incompatible_pack.set.on"));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("off")
                        .executes(ctx ->
                        {
                            alertIncompatiblePack.set(false);
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
                            sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_broken_pack.set.on"));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("off")
                        .executes(ctx ->
                        {
                            alertBrokenPack.set(false);
                            sendMessage(ctx.getSource(), Component.translatable("command.colorwheel.alert_broken_pack.set.off"));

                            return Command.SINGLE_SUCCESS;
                        })));

        event.getDispatcher().register(command);
    }

    private static void sendMessage(CommandSourceStack source, Component message)
    {
        source.sendSuccess(() -> message, true);
    }
}
