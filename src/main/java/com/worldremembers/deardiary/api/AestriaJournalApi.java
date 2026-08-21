package com.worldremembers.deardiary.api;

import com.worldremembers.deardiary.DearDiaryMod;
import com.worldremembers.deardiary.DearDiaryServices;
import com.worldremembers.deardiary.data.DiaryEntry;
import com.worldremembers.deardiary.data.DiaryEntryKind;
import com.worldremembers.deardiary.data.PlayerDiary;
import com.worldremembers.deardiary.network.DearDiaryNetworking;
import com.worldremembers.deardiary.research.AestriaResearch;
import com.worldremembers.deardiary.research.AestriaResearchLoader;
import com.worldremembers.deardiary.research.AestriaResearchRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** API específica del contenido de historia/investigación de Aestria. */
public final class AestriaJournalApi {
    private static final String EVENT_PREFIX = "aestria:investigacion/";
    private static final String LEGACY_CHAPTER_PREFIX = "aestria:capitulo/";

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

        String currentChapter = null;
        for (AestriaResearch research : AestriaResearchRegistry.all()) {
            if (!research.chapterId().equals(currentChapter)) {
                currentChapter = research.chapterId();
                source.sendFeedback(() -> Text.literal("-- " + research.chapterTitle() + " --"), false);
            }
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

    /** Desbloquea una investigación para un jugador de forma idempotente. */
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

        // Dear Diary renders a chapter as a timeline separator. It must be
        // inserted BEFORE the first research entry belonging to that chapter.
        ensureChapter(player, research);

        DiaryEntry entry = DiaryEntry.builder(DiaryEntryKind.AUTOMATIC, eventType, DearDiaryMod.MOD_ID)
                .category(research.category())
                .importance(research.importance())
                .resolvedTitle(research.title())
                .resolvedText(research.text())
                .icon(research.icon())
                // Enabled temporarily during development so entries can be
                // removed/retested repeatedly. Final release will disable it.
                .editable(true)
                .shareable(false)
                .build();

        DearDiaryApi.addEntry(player, entry);
        DearDiaryServices.storage().save(player.getUuid());
        DearDiaryNetworking.sendDiarySnapshot(player);
        DearDiaryNetworking.sendResearchEntryNotice(player, entry);
        player.sendMessage(Text.literal("§aNueva investigación desbloqueada: §f" + research.title()), false);
        return true;
    }

    /** Creates a real Dear Diary chapter before its first investigation. */
    private static void ensureChapter(ServerPlayerEntity player, AestriaResearch research) {
        PlayerDiary diary = DearDiaryApi.getDiary(player);
        String chapterTitle = research.chapterTitle();

        boolean chapterExists = diary.entriesView().stream().anyMatch(entry ->
                DearDiaryApi.isChapterEntry(entry) && chapterTitle.equals(entry.getResolvedTitle())
        );
        if (chapterExists) {
            return;
        }

        DearDiaryApi.createChapterEntry(player, chapterTitle, "", false);
    }

    /**
     * Development reset: removes only Aestria investigations and Aestria
     * chapter markers. Personal notes and unrelated Dear Diary entries remain.
     */
    public static int resetAestriaDiary(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este comando solo puede usarse dentro del juego."));
            return 0;
        }

        Set<String> chapterTitles = new HashSet<>();
        for (AestriaResearch research : AestriaResearchRegistry.all()) {
            chapterTitles.add(research.chapterTitle());
        }

        PlayerDiary diary = DearDiaryApi.getDiary(player);
        int removed = 0;
        for (DiaryEntry entry : new ArrayList<>(diary.entriesView())) {
            boolean aestriaResearch = entry.getEventType().startsWith(EVENT_PREFIX);
            boolean legacyAestriaChapter = entry.getEventType().startsWith(LEGACY_CHAPTER_PREFIX);
            boolean aestriaChapter = DearDiaryApi.isChapterEntry(entry)
                    && chapterTitles.contains(entry.getResolvedTitle());

            if ((aestriaResearch || legacyAestriaChapter || aestriaChapter)
                    && DearDiaryApi.deleteEntry(player, entry.getId())) {
                removed++;
            }
        }

        DearDiaryServices.storage().save(player.getUuid());
        DearDiaryNetworking.sendDiarySnapshot(player);
        player.sendMessage(Text.literal("§aDiario de Aestria reiniciado: §f" + removed + " elementos eliminados."), false);
        return removed;
    }

    /** Reordena las investigaciones ya desbloqueadas y reconstruye sus capítulos. */
    public static int reorganizeResearches(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este comando solo puede usarse dentro del juego."));
            return 0;
        }

        PlayerDiary diary = DearDiaryApi.getDiary(player);
        List<String> unlockedIds = new ArrayList<>();
        for (DiaryEntry entry : diary.entriesView()) {
            if (entry.getEventType().startsWith(EVENT_PREFIX)) {
                unlockedIds.add(entry.getEventType().substring(EVENT_PREFIX.length()));
            }
        }

        resetAestriaDiary(source);

        int rebuilt = 0;
        for (AestriaResearch research : AestriaResearchRegistry.all()) {
            if (unlockedIds.contains(research.id()) && unlockResearch(player, research.id())) {
                rebuilt++;
            }
        }

        DearDiaryServices.storage().save(player.getUuid());
        DearDiaryNetworking.sendDiarySnapshot(player);
        player.sendMessage(Text.literal("§aDiario de Aestria reorganizado: §f" + rebuilt + " investigaciones."), false);
        return rebuilt;
    }
}
