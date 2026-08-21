package com.worldremembers.deardiary.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.worldremembers.deardiary.api.AestriaJournalApi;
import com.worldremembers.deardiary.research.AestriaResearchRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Comandos públicos y de administración para el Diario de Aestria. */
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
                .executes(context -> execute(context.getSource(), "deardiary open"))
                .then(CommandManager.literal("abrir").executes(context -> execute(context.getSource(), "deardiary open")))
                .then(CommandManager.literal("nuevo").executes(context -> execute(context.getSource(), "deardiary open new")))
                .then(CommandManager.literal("lista").executes(context -> execute(context.getSource(), "deardiary list")))
                .then(CommandManager.literal("capitulo")
                        .then(CommandManager.argument("titulo", StringArgumentType.greedyString())
                                .executes(context -> execute(context.getSource(), "deardiary chapter " + StringArgumentType.getString(context, "titulo")))))
                .then(CommandManager.literal("exportar").executes(context -> execute(context.getSource(), "deardiary export markdown")))
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
                            if (context.getSource().getServer() == null) return 0;
                            AestriaJournalApi.reloadResearches(context.getSource().getServer().getResourceManager());
                            context.getSource().sendFeedback(() -> Text.literal("Investigaciones de Aestria recargadas."), false);
                            return 1;
                        }))
                .then(CommandManager.literal("limpiar").requires(AestriaJournalCommands::isOperator).executes(context -> execute(context.getSource(), "deardiary clear_self")))
                .then(CommandManager.literal("prueba").requires(AestriaJournalCommands::isOperator).executes(context -> execute(context.getSource(), "deardiary add_test")))
                .then(CommandManager.literal("prueba_manual").requires(AestriaJournalCommands::isOperator).executes(context -> execute(context.getSource(), "deardiary add_manual_test")))
                .then(CommandManager.literal("configuracion").requires(AestriaJournalCommands::isOperator).executes(context -> execute(context.getSource(), "deardiary config_status")))
                .then(CommandManager.literal("ayuda_configuracion").requires(AestriaJournalCommands::isOperator).executes(context -> execute(context.getSource(), "deardiary config_help")))
                .then(CommandManager.literal("eventos").requires(AestriaJournalCommands::isOperator)
                        .executes(context -> execute(context.getSource(), "deardiary events_list"))
                        .then(CommandManager.argument("categoria", StringArgumentType.word())
                                .executes(context -> execute(context.getSource(), "deardiary events_list " + StringArgumentType.getString(context, "categoria")))))
                .then(CommandManager.literal("validar_eventos").requires(AestriaJournalCommands::isOperator).executes(context -> execute(context.getSource(), "deardiary validate_events")))
                .then(CommandManager.literal("probar_evento").requires(AestriaJournalCommands::isOperator)
                        .then(CommandManager.argument("evento", StringArgumentType.word())
                                .executes(context -> execute(context.getSource(), "deardiary trigger_test " + StringArgumentType.getString(context, "evento")))
                                .then(CommandManager.literal("forzar").executes(context -> execute(context.getSource(), "deardiary trigger_test " + StringArgumentType.getString(context, "evento") + " force")))))
                .then(CommandManager.literal("estado_eventos").requires(AestriaJournalCommands::isOperator).executes(context -> execute(context.getSource(), "deardiary event_state")))
                .then(CommandManager.literal("contador").requires(AestriaJournalCommands::isOperator)
                        .then(CommandManager.argument("tipo", StringArgumentType.word())
                                .then(CommandManager.argument("cantidad", IntegerArgumentType.integer(1))
                                        .executes(context -> execute(context.getSource(), "deardiary add_counter " + StringArgumentType.getString(context, "tipo") + " " + IntegerArgumentType.getInteger(context, "cantidad"))))))
                .then(CommandManager.literal("relocalizar").requires(AestriaJournalCommands::isOperator)
                        .then(CommandManager.argument("idioma", StringArgumentType.word())
                                .executes(context -> execute(context.getSource(), "deardiary relocalize_self " + StringArgumentType.getString(context, "idioma")))))
                .then(CommandManager.literal("editar_ultima").requires(AestriaJournalCommands::isOperator)
                        .then(CommandManager.argument("titulo", StringArgumentType.string())
                                .then(CommandManager.argument("texto", StringArgumentType.greedyString())
                                        .executes(context -> execute(context.getSource(), "deardiary edit_last " + StringArgumentType.escapeIfRequired(StringArgumentType.getString(context, "titulo")) + " " + StringArgumentType.getString(context, "texto"))))))
                .then(CommandManager.literal("borrar_ultima").requires(AestriaJournalCommands::isOperator).executes(context -> execute(context.getSource(), "deardiary delete_last")))
                .then(CommandManager.literal("favorito_ultima").requires(AestriaJournalCommands::isOperator).executes(context -> execute(context.getSource(), "deardiary favorite_last")))
                .then(CommandManager.literal("compartir_ultima").requires(AestriaJournalCommands::isOperator).executes(context -> execute(context.getSource(), "deardiary share_last"))));
    }

    private static boolean isOperator(ServerCommandSource source) { return source.hasPermissionLevel(2); }

    private static int execute(ServerCommandSource source, String command) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este comando solo puede usarse dentro del juego.")); return 0;
        }
        player.getServer().getCommandManager().executeWithPrefix(source, command);
        return 1;
    }
}
