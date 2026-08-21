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
                String chapterTitle = research.chapterTitle();
                source.sendFeedback(() -> Text.literal("-- " + chapterTitle + " --"), false);
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

    public static int unlockResearch(ServerCommandSource source, String researchId, ServerPlayerEntity target) {
        return unlockResearch(target, researchId) ? 1 : 0;
    }

    /** Desbloquea una investigación para el jugador indicado. */
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
        DiaryEntry existing = findResearchEntry(diary, research);
        boolean newlyUnlocked = existing == null;
        DiaryEntry entry = existing;

        if (newlyUnlocked) {
            entry = DiaryEntry.builder(DiaryEntryKind.AUTOMATIC, eventType, DearDiaryMod.MOD_ID)
                    .category(research.category())
                    .importance(research.importance())
                    .resolvedTitle(research.title())
                    .resolvedText(research.text())
                    .icon(research.icon())
                    .editable(true)
                    .shareable(false)
                    .build();
            entry = DearDiaryApi.addEntry(player, entry);
        }

        refreshChapterMarker(player, research.chapterTitle());
        DearDiaryServices.storage().save(player.getUuid());
        DearDiaryNetworking.sendDiarySnapshot(player);

        if (newlyUnlocked) {
            DearDiaryNetworking.sendResearchEntryNotice(player, entry);
            player.sendMessage(Text.literal("§aNueva investigación desbloqueada: §f" + research.title()), false);
        } else {
            player.sendMessage(Text.literal("§eInvestigación ya desbloqueada; capítulo reorganizado: §f" + research.title()), false);
        }
        return true;
    }

    private static DiaryEntry findResearchEntry(PlayerDiary diary, AestriaResearch research) {
        String eventType = EVENT_PREFIX + research.id();
        return diary.entriesView().stream()
                .filter(entry -> eventType.equals(entry.getEventType())
                        || (research.title().equals(entry.getResolvedTitle())
                        && research.text().equals(entry.getResolvedText())))
                .findFirst()
                .orElse(null);
    }

    private static boolean matchesResearch(DiaryEntry entry, AestriaResearch research) {
        String eventType = EVENT_PREFIX + research.id();
        return eventType.equals(entry.getEventType())
                || (research.title().equals(entry.getResolvedTitle())
                && research.text().equals(entry.getResolvedText()));
    }

    private static void refreshChapterMarker(ServerPlayerEntity player, String chapterTitle) {
        PlayerDiary diary = DearDiaryApi.getDiary(player);
        for (DiaryEntry entry : new ArrayList<>(diary.entriesView())) {
            if (DearDiaryApi.isChapterEntry(entry) && chapterTitle.equals(entry.getResolvedTitle())) {
                if (!DearDiaryApi.deleteEntry(player, entry.getId())) {
                    diary.removeEntry(entry.getId());
                }
            }
        }
        DearDiaryApi.createChapterEntry(player, chapterTitle, "", false);
    }

    /** Borra una investigación concreta, incluyendo registros de versiones antiguas. */
    public static int deleteResearch(ServerCommandSource source, String researchId) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este comando solo puede usarse dentro del juego."));
            return 0;
        }
        return deleteResearch(source, researchId, player);
    }

    /** Borra una investigación concreta del jugador indicado. Solo se usa durante desarrollo. */
    public static int deleteResearch(ServerCommandSource source, String researchId, ServerPlayerEntity target) {
        AestriaResearch research = AestriaResearchRegistry.get(researchId).orElse(null);
        if (research == null) {
            source.sendError(Text.literal("Investigación no encontrada: " + researchId));
            return 0;
        }

        PlayerDiary diary = DearDiaryApi.getDiary(target);
        int removed = 0;
        for (DiaryEntry entry : new ArrayList<>(diary.entriesView())) {
            if (matchesResearch(entry, research)) {
                boolean deleted = DearDiaryApi.deleteEntry(target, entry.getId());
                if (!deleted) {
                    deleted = diary.removeEntry(entry.getId());
                }
                if (deleted) {
                    removed++;
                }
            }
        }

        // También elimina el marcador del capítulo si ya no quedan investigaciones
        // de Aestria pertenecientes a ese capítulo.
        boolean chapterStillUsed = diary.entriesView().stream().anyMatch(entry ->
                AestriaResearchRegistry.all().stream().anyMatch(other ->
                        other.chapterTitle().equals(research.chapterTitle()) && matchesResearch(entry, other)));
        if (!chapterStillUsed) {
            for (DiaryEntry entry : new ArrayList<>(diary.entriesView())) {
                if (DearDiaryApi.isChapterEntry(entry) && research.chapterTitle().equals(entry.getResolvedTitle())) {
                    if (DearDiaryApi.deleteEntry(target, entry.getId()) || diary.removeEntry(entry.getId())) {
                        removed++;
                    }
                }
            }
        }

        DearDiaryServices.storage().save(target.getUuid());
        DearDiaryNetworking.sendDiarySnapshot(target);
        source.sendFeedback(() -> Text.literal("§a" + research.title() + " eliminado de " + target.getName().getString() + ". Elementos eliminados: §f" + removed), false);
        return removed;
    }

    /** Development reset. No toca notas personales. */
    public static int resetAestriaDiary(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este comando solo puede usarse dentro del juego."));
            return 0;
        }

        Set<String> researchTitles = new HashSet<>();
        Set<String> researchTexts = new HashSet<>();
        Set<String> chapterTitles = new HashSet<>();
        for (AestriaResearch research : AestriaResearchRegistry.all()) {
            researchTitles.add(research.title());
            researchTexts.add(research.text());
            chapterTitles.add(research.chapterTitle());
        }

        PlayerDiary diary = DearDiaryApi.getDiary(player);
        int removed = 0;
        for (DiaryEntry entry : new ArrayList<>(diary.entriesView())) {
            boolean aestriaResearch = entry.getEventType().startsWith(EVENT_PREFIX)
                    || (researchTitles.contains(entry.getResolvedTitle()) && researchTexts.contains(entry.getResolvedText()));
            boolean legacyAestriaChapter = entry.getEventType().startsWith(LEGACY_CHAPTER_PREFIX);
            boolean aestriaChapter = DearDiaryApi.isChapterEntry(entry) && chapterTitles.contains(entry.getResolvedTitle());

            if (aestriaResearch || legacyAestriaChapter || aestriaChapter) {
                boolean deleted = DearDiaryApi.deleteEntry(player, entry.getId());
                if (!deleted) {
                    deleted = diary.removeEntry(entry.getId());
                }
                if (deleted) removed++;
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
            String eventType = entry.getEventType();
            if (eventType.startsWith(EVENT_PREFIX)) {
                unlockedIds.add(eventType.substring(EVENT_PREFIX.length()));
                continue;
            }
            for (AestriaResearch research : AestriaResearchRegistry.all()) {
                if (research.title().equals(entry.getResolvedTitle()) && research.text().equals(entry.getResolvedText())) {
                    unlockedIds.add(research.id());
                    break;
                }
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
