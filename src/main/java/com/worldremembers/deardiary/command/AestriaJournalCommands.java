package com.worldremembers.deardiary.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.worldremembers.deardiary.api.AestriaJournalApi;
import com.worldremembers.deardiary.api.DearDiaryApi;
import com.worldremembers.deardiary.network.DearDiaryNetworking;
import com.worldremembers.deardiary.research.AestriaResearchRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Comandos públicos y de administración para el Diario de Investigador. */
public final class AestriaJournalCommands {
    private static final SuggestionProvider<ServerCommandSource> RESEARCH_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        for (var research : AestriaResearchRegistry.all()) {
            if (research.id().toLowerCase().startsWith(remaining)) builder.suggest(research.id());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> CHAPTER_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        var seen = new java.util.HashSet<String>();
        for (var research : AestriaResearchRegistry.all()) {
            String title = research.chapterTitle();
            if (seen.add(title) && title.toLowerCase().startsWith(remaining)) builder.suggest(title);
        }
        return builder.buildFuture();
    };

    private AestriaJournalCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("diario")
                .executes(context -> open(context.getSource(), false))
                .then(CommandManager.literal("abrir").executes(context -> open(context.getSource(), false)))
                .then(CommandManager.literal("nuevo").executes(context -> open(context.getSource(), true)))
                .then(CommandManager.literal("investigaciones").executes(context -> AestriaJournalApi.listResearches(context.getSource())))
                .then(CommandManager.literal("desbloquear")
                        .then(CommandManager.argument("id", StringArgumentType.word()).suggests(RESEARCH_SUGGESTIONS)
                                .executes(context -> AestriaJournalApi.unlockResearch(context.getSource(), StringArgumentType.getString(context, "id")))
                                .then(CommandManager.argument("jugador", EntityArgumentType.player())
                                        .executes(context -> AestriaJournalApi.unlockResearch(context.getSource(), StringArgumentType.getString(context, "id"), EntityArgumentType.getPlayer(context, "jugador"))))))
                .then(CommandManager.literal("borrar_investigacion").requires(AestriaJournalCommands::isOperator)
                        .then(CommandManager.argument("id", StringArgumentType.word()).suggests(RESEARCH_SUGGESTIONS)
                                .executes(context -> AestriaJournalApi.deleteResearch(context.getSource(), StringArgumentType.getString(context, "id")))
                                .then(CommandManager.argument("jugador", EntityArgumentType.player())
                                        .executes(context -> AestriaJournalApi.deleteResearch(context.getSource(), StringArgumentType.getString(context, "id"), EntityArgumentType.getPlayer(context, "jugador"))))))
                .then(CommandManager.literal("borrar_capitulo").requires(AestriaJournalCommands::isOperator)
                        .then(CommandManager.argument("titulo", StringArgumentType.greedyString()).suggests(CHAPTER_SUGGESTIONS)
                                .executes(context -> {
                                    String title = StringArgumentType.getString(context, "titulo");
                                    if (!(context.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                                        context.getSource().sendError(Text.literal("Este comando solo puede usarse dentro del juego."));
                                        return 0;
                                    }
                                    return AestriaJournalApi.deleteChapter(context.getSource(), title, player);
                                })))
                .then(CommandManager.literal("reorganizar").requires(AestriaJournalCommands::isOperator)
                        .executes(context -> AestriaJournalApi.reorganizeResearches(context.getSource())))
                .then(CommandManager.literal("reset_aestria").requires(AestriaJournalCommands::isOperator)
                        .executes(context -> AestriaJournalApi.resetAestriaDiary(context.getSource())))
                .then(CommandManager.literal("recargar_investigaciones").requires(AestriaJournalCommands::isOperator)
                        .executes(context -> {
                            var server = context.getSource().getServer();
                            if (server == null) return 0;
                            int updated = AestriaJournalApi.reloadResearches(server);
                            context.getSource().sendFeedback(
                                    () -> Text.literal("§aDiario de Investigador recargado. §f" + AestriaResearchRegistry.size()
                                            + " investigaciones cargadas, " + updated + " entradas actualizadas. §7JSON: config/aestria_journal/investigations/"), false);
                            return 1;
                        }))
                .then(CommandManager.literal("limpiar").requires(AestriaJournalCommands::isOperator)
                        .executes(context -> clearSelf(context.getSource()))));

        dispatcher.register(CommandManager.literal("credencial")
                .executes(context -> openCredential(context.getSource())));
    }

    private static boolean isOperator(ServerCommandSource source) { return source.hasPermissionLevel(2); }

    private static int open(ServerCommandSource source, boolean newEntry) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este comando solo puede usarse dentro del juego."));
            return 0;
        }
        return DearDiaryNetworking.openDiaryScreen(player, newEntry) ? 1 : 0;
    }

    private static int openCredential(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este comando solo puede usarse dentro del juego."));
            return 0;
        }
        if (!DearDiaryNetworking.openResearcherCredential(player)) {
            source.sendError(Text.literal("La credencial no está disponible para este cliente."));
            return 0;
        }
        return 1;
    }

    private static int clearSelf(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este comando solo puede usarse dentro del juego."));
            return 0;
        }
        DearDiaryApi.clearDiary(player);
        DearDiaryNetworking.sendDiarySnapshot(player);
        source.sendFeedback(() -> Text.literal("§aDiario limpiado."), false);
        return 1;
    }
}
