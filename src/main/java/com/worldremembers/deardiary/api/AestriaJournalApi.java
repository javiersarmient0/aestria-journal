package com.worldremembers.deardiary.api;

import com.worldremembers.deardiary.DearDiaryMod;
import com.worldremembers.deardiary.DearDiaryServices;
import com.worldremembers.deardiary.data.DiaryEntry;
import com.worldremembers.deardiary.data.DiaryEntryKind;
import com.worldremembers.deardiary.data.PlayerDiary;
import com.worldremembers.deardiary.research.AestriaResearch;
import com.worldremembers.deardiary.research.AestriaResearchLoader;
import com.worldremembers.deardiary.research.AestriaResearchRegistry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** API específica del contenido de historia/investigación de Aestria. */
public final class AestriaJournalApi {
    private static final String EVENT_PREFIX = "aestria:investigacion/";

    private AestriaJournalApi() {
    }

    public static void reloadResearches(ResourceManager resourceManager) {
        AestriaResearchLoader.reload(resourceManager);
    }

    public static int listResearches(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("=== Investigaciones de Aestria ==="), false);
        if (AestriaResearchRegistry.all().isEmpty()) {
            source.sendFeedback(() -> Text.literal("No hay investigaciones cargadas."), false);
            return 0;
        }

        for (AestriaResearch research : AestriaResearchRegistry.all()) {
            source.sendFeedback(() -> Text.literal(research.id() + " - " + research.title()), false);
        }
        return AestriaResearchRegistry.size();
    }

    public static int unlockResearch(ServerCommandSource source, String researchId) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este comando solo puede usarse dentro del juego."));
            return 0;
        }

        return unlockResearch(player, researchId) ? 1 : 0;
    }

    /**
     * Desbloquea una investigación para un jugador. Es idempotente: completar
     * la misma misión dos veces no crea entradas duplicadas.
     */
    public static boolean unlockResearch(ServerPlayerEntity player, String researchId) {
        if (researchId == null || researchId.isBlank()) {
            return false;
        }

        AestriaResearch research = AestriaResearchRegistry.get(researchId).orElse(null);
        if (research == null) {
            player.sendMessage(Text.literal("Investigación no encontrada: " + researchId), false);
            return false;
        }

        PlayerDiary diary = DearDiaryApi.getDiary(player);
        String eventType = EVENT_PREFIX + research.id();
        if (diary.hasEntryWithEventType(eventType)) {
            player.sendMessage(Text.literal("Esta investigación ya está desbloqueada: " + research.title()), false);
            return true;
        }

        DiaryEntry entry = DiaryEntry.builder(DiaryEntryKind.AUTOMATIC, eventType, DearDiaryMod.MOD_ID)
                .category(research.category())
                .importance(research.importance())
                .resolvedTitle(research.title())
                .resolvedText(research.text())
                .icon(research.icon())
                .editable(false)
                .shareable(research.shareable())
                .build();

        DearDiaryApi.addEntry(player, entry);
        DearDiaryServices.storage().save(player.getUuid());
        player.sendMessage(Text.literal("§aNueva investigación desbloqueada: §f" + research.title()), false);
        return true;
    }
}
