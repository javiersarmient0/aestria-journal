package com.worldremembers.deardiary.command;

import com.worldremembers.deardiary.network.DearDiaryNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Spanish-facing command aliases for Aestria Journal.
 * The original /deardiary command remains available for compatibility.
 */
public final class AestriaJournalCommands {
    private AestriaJournalCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("diario")
                .executes(context -> open(context.getSource(), false))
                .then(CommandManager.literal("nuevo")
                        .executes(context -> open(context.getSource(), true)))
                .then(CommandManager.literal("abrir")
                        .executes(context -> open(context.getSource(), false)))
                .then(CommandManager.literal("lista")
                        .executes(context -> {
                            context.getSource().sendFeedback(
                                    () -> Text.translatable("commands.aestria_journal.list_hint"),
                                    false
                            );
                            return 1;
                        }))
                .then(CommandManager.literal("capitulo")
                        .then(CommandManager.argument("titulo", StringArgumentType.greedyString())
                                .executes(context -> {
                                    context.getSource().sendFeedback(
                                            () -> Text.translatable("commands.aestria_journal.chapter_hint"),
                                            false
                                    );
                                    return 1;
                                }))));
    }

    private static int open(ServerCommandSource source, boolean newEntry) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.translatable("commands.dear_diary.client.not_in_world"));
            return 0;
        }

        if (!DearDiaryNetworking.openDiaryScreen(player, newEntry)) {
            source.sendError(Text.translatable("commands.dear_diary.open.unavailable"));
            return 0;
        }

        return 1;
    }
}
