package com.worldremembers.deardiary.api;

import com.worldremembers.deardiary.DearDiaryMod;
import com.worldremembers.deardiary.DearDiaryServices;
import com.worldremembers.deardiary.data.DiaryCategory;
import com.worldremembers.deardiary.data.DiaryEntry;
import com.worldremembers.deardiary.data.DiaryEntryKind;
import com.worldremembers.deardiary.data.DiaryEntryMarkers;
import com.worldremembers.deardiary.data.DiaryImportance;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class AestriaJournalApi {
    private static final String EVENT_PREFIX = "aestria:investigacion/";
    private static final String LEGACY_CHAPTER_PREFIX = "aestria:capitulo/";

    private AestriaJournalApi() {}

    public static void reloadResearches(ResourceManager resourceManager) { AestriaResearchLoader.reload(resourceManager); }

    public static int reloadResearches(MinecraftServer server) {
        AestriaResearchLoader.reload(server.getResourceManager());
        int updated = 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerDiary diary = DearDiaryApi.getDiary(player);
            boolean changed = false;
            Set<String> usedChapters = new HashSet<>();
            for (DiaryEntry entry : diary.entriesView()) {
                if (!isResearchEntry(entry)) continue;
                AestriaResearch research = AestriaResearchRegistry.get(entry.getEventType().substring(EVENT_PREFIX.length())).orElse(null);
                if (research == null) continue;
                entry.setChapter(research.chapterId(), research.chapterTitle(), research.chapterOrder());
                if (entry.isEditable()) {
                    entry.updateResolvedText(research.title(), research.text());
                    updated++;
                }
                usedChapters.add(research.chapterId());
                changed = true;
            }
            for (String chapterId : usedChapters) {
                AestriaResearch chapter = AestriaResearchRegistry.all().stream()
                        .filter(research -> research.chapterId().equals(chapterId)).findFirst().orElse(null);
                if (chapter != null && ensureChapterMarker(player, chapter)) changed = true;
            }
            diary.resort();
            if (changed) {
                DearDiaryServices.storage().save(player.getUuid());
                DearDiaryNetworking.sendDiarySnapshot(player);
            }
        }
        return updated;
    }

    public static int listResearches(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("=== Investigaciones de Aestria ==="), false);
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

    public static int unlockResearch(ServerCommandSource source, String researchId, ServerPlayerEntity target) {
        return unlockResearch(target, researchId) ? 1 : 0;
    }

    public static boolean unlockResearch(ServerPlayerEntity player, String researchId) {
        AestriaResearch research = AestriaResearchRegistry.get(researchId).orElse(null);
        if (research == null) {
            player.sendMessage(Text.literal("Investigación no encontrada: " + researchId), false);
            return false;
        }

        PlayerDiary diary = DearDiaryApi.getDiary(player);
        String eventType = EVENT_PREFIX + research.id();
        DiaryEntry entry = findResearchEntry(diary, research);
        boolean newlyUnlocked = entry == null;

        ensureChapterMarker(player, research);

        if (newlyUnlocked) {
            DiaryEntry.Builder builder = DiaryEntry.builder(DiaryEntryKind.AUTOMATIC, eventType, DearDiaryMod.MOD_ID)
                    .category(research.category())
                    .importance(research.importance())
                    .resolvedTitle(research.title())
                    .resolvedText(research.text())
                    .icon(research.icon())
                    .editable(true)
                    .shareable(research.shareable())
                    .chapter(research.chapterId(), research.chapterTitle(), research.chapterOrder());
            if (research.subtitle() != null && !research.subtitle().isBlank()) {
                builder.customData("subtitle", new com.google.gson.JsonPrimitive(research.subtitle()));
            }
            entry = DearDiaryApi.addEntry(player, builder.build());
        } else {
            entry.setChapter(research.chapterId(), research.chapterTitle(), research.chapterOrder());
            if (entry.isEditable()) entry.updateResolvedText(research.title(), research.text());
        }

        diary.resort();
        DearDiaryServices.storage().save(player.getUuid());
        DearDiaryNetworking.sendDiarySnapshot(player);
        if (newlyUnlocked) {
            DearDiaryNetworking.sendResearchEntryNotice(player, entry);
            player.sendMessage(Text.literal("§aNueva investigación desbloqueada: §f" + research.title()), false);
        } else {
            player.sendMessage(Text.literal("§eInvestigación ya desbloqueada; contenido actualizado: §f" + research.title()), false);
        }
        return true;
    }

    private static DiaryEntry findResearchEntry(PlayerDiary diary, AestriaResearch research) {
        String eventType = EVENT_PREFIX + research.id();
        return diary.entriesView().stream().filter(entry -> eventType.equals(entry.getEventType())).findFirst().orElse(null);
    }

    private static boolean matchesResearch(DiaryEntry entry, AestriaResearch research) {
        return (EVENT_PREFIX + research.id()).equals(entry.getEventType());
    }

    private static boolean ensureChapterMarker(ServerPlayerEntity player, AestriaResearch research) {
        PlayerDiary diary = DearDiaryApi.getDiary(player);
        DiaryEntry existing = diary.entriesView().stream()
                .filter(DearDiaryApi::isChapterEntry)
                .filter(entry -> research.chapterId().equals(entry.getChapterId()) || research.chapterTitle().equals(entry.getResolvedTitle()))
                .findFirst().orElse(null);
        if (existing == null) {
            DiaryEntry marker = DiaryEntry.builder(DiaryEntryKind.MANUAL, DiaryEntryMarkers.CHAPTER_EVENT_TYPE, DearDiaryMod.MOD_ID)
                    .category(DiaryCategory.OTHER)
                    .importance(DiaryImportance.NORMAL)
                    .resolvedTitle(research.chapterTitle())
                    .resolvedText("")
                    .icon("minecraft:writable_book")
                    .editable(false)
                    .shareable(false)
                    .chapter(research.chapterId(), research.chapterTitle(), research.chapterOrder())
                    .build();
            DearDiaryApi.addEntry(player, marker);
            return true;
        }
        existing.setChapter(research.chapterId(), research.chapterTitle(), research.chapterOrder());
        return false;
    }

    private static boolean isResearchEntry(DiaryEntry entry) {
        return entry.getEventType() != null && entry.getEventType().startsWith(EVENT_PREFIX);
    }

    private static int deleteChapterInternal(ServerPlayerEntity target, String chapterTitle) {
        PlayerDiary diary = DearDiaryApi.getDiary(target);
        int removed = 0;
        for (DiaryEntry entry : new ArrayList<>(diary.entriesView())) {
            boolean match = (DearDiaryApi.isChapterEntry(entry) && chapterTitle.equals(entry.getResolvedTitle()))
                    || (entry.getEventType() != null && entry.getEventType().equals(LEGACY_CHAPTER_PREFIX + chapterTitle));
            if (match) {
                boolean deleted = DearDiaryApi.deleteEntry(target, entry.getId());
                if (!deleted) deleted = diary.removeEntry(entry.getId());
                if (deleted) removed++;
            }
        }
        return removed;
    }

    public static int deleteResearch(ServerCommandSource source, String researchId) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) return 0;
        return deleteResearch(source, researchId, player);
    }

    public static int deleteResearch(ServerCommandSource source, String researchId, ServerPlayerEntity target) {
        AestriaResearch research = AestriaResearchRegistry.get(researchId).orElse(null);
        if (research == null) return 0;
        PlayerDiary diary = DearDiaryApi.getDiary(target);
        int removed = 0;
        for (DiaryEntry entry : new ArrayList<>(diary.entriesView())) {
            if (matchesResearch(entry, research)) {
                boolean deleted = DearDiaryApi.deleteEntry(target, entry.getId());
                if (!deleted) deleted = diary.removeEntry(entry.getId());
                if (deleted) removed++;
            }
        }
        boolean chapterStillUsed = diary.entriesView().stream().anyMatch(entry -> isResearchEntry(entry)
                && AestriaResearchRegistry.get(entry.getEventType().substring(EVENT_PREFIX.length()))
                .map(other -> other.chapterId().equals(research.chapterId())).orElse(false));
        if (!chapterStillUsed) removed += deleteChapterInternal(target, research.chapterTitle());
        diary.resort();
        DearDiaryServices.storage().save(target.getUuid());
        DearDiaryNetworking.sendDiarySnapshot(target);
        final int result = removed;
        source.sendFeedback(() -> Text.literal("§a" + research.title() + " eliminado. Elementos eliminados: §f" + result), false);
        return removed;
    }

    public static int deleteChapter(ServerCommandSource source, String chapterTitle, ServerPlayerEntity target) {
        int removed = deleteChapterInternal(target, chapterTitle);
        PlayerDiary diary = DearDiaryApi.getDiary(target);
        diary.resort();
        DearDiaryServices.storage().save(target.getUuid());
        DearDiaryNetworking.sendDiarySnapshot(target);
        final int result = removed;
        source.sendFeedback(() -> Text.literal("§aCapítulo " + chapterTitle + " eliminado. Elementos eliminados: §f" + result), false);
        return removed;
    }

    public static int resetAestriaDiary(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) return 0;
        PlayerDiary diary = DearDiaryApi.getDiary(player);
        int removed = 0;
        for (DiaryEntry entry : new ArrayList<>(diary.entriesView())) {
            boolean research = isResearchEntry(entry);
            boolean chapter = DearDiaryApi.isChapterEntry(entry);
            boolean legacy = entry.getEventType() != null && entry.getEventType().startsWith(LEGACY_CHAPTER_PREFIX);
            if (research || chapter || legacy) {
                boolean deleted = DearDiaryApi.deleteEntry(player, entry.getId());
                if (!deleted) deleted = diary.removeEntry(entry.getId());
                if (deleted) removed++;
            }
        }
        DearDiaryServices.storage().save(player.getUuid());
        DearDiaryNetworking.sendDiarySnapshot(player);
        player.sendMessage(Text.literal("§aDiario de Investigador reiniciado: §f" + removed + " elementos eliminados."), false);
        return removed;
    }

    /** Metadata reconciliation only. It never moves an entry to another chapter. */
    public static int reorganizeResearches(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) return 0;
        PlayerDiary diary = DearDiaryApi.getDiary(player);
        int reconciled = 0;
        Set<String> chapters = new HashSet<>();
        for (DiaryEntry entry : diary.entriesView()) {
            if (!isResearchEntry(entry)) continue;
            AestriaResearch research = AestriaResearchRegistry.get(entry.getEventType().substring(EVENT_PREFIX.length())).orElse(null);
            if (research == null) continue;
            entry.setChapter(research.chapterId(), research.chapterTitle(), research.chapterOrder());
            chapters.add(research.chapterId());
            reconciled++;
        }
        for (String chapterId : chapters) {
            AestriaResearch chapter = AestriaResearchRegistry.all().stream()
                    .filter(research -> research.chapterId().equals(chapterId)).findFirst().orElse(null);
            if (chapter != null) ensureChapterMarker(player, chapter);
        }
        diary.resort();
        DearDiaryServices.storage().save(player.getUuid());
        DearDiaryNetworking.sendDiarySnapshot(player);
        final int result = reconciled;
        source.sendFeedback(() -> Text.literal("§aDiario de Investigador reorganizado: §f" + result + " investigaciones verificadas. Los capítulos no fueron reconstruidos."), false);
        return result;
    }
}
