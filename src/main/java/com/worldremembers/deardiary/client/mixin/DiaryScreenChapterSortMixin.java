package com.worldremembers.deardiary.client.mixin;

import com.worldremembers.deardiary.client.gui.DiaryScreen;
import com.worldremembers.deardiary.data.DiaryEntry;
import com.worldremembers.deardiary.data.DiaryEntryMarkers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the diary UI grouped by the chapter metadata stored on each entry.
 * The normal date/importance sorting must never be allowed to move an entry
 * across chapter boundaries.
 */
@Mixin(DiaryScreen.class)
public abstract class DiaryScreenChapterSortMixin {
    @Inject(method = "filteredEntries", at = @At("RETURN"), cancellable = true)
    private void dearDiary$groupEntriesByChapter(CallbackInfoReturnable<List<DiaryEntry>> cir) {
        List<DiaryEntry> entries = new ArrayList<>(cir.getReturnValue());

        entries.sort(Comparator
                .comparingInt(DiaryEntry::getChapterOrder)
                .thenComparingInt(entry -> DiaryEntryMarkers.isChapterEntry(entry) ? 0 : 1)
                .thenComparing(DiaryEntry::getCreatedAt)
                .thenComparing(DiaryEntry::getResolvedTitle, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(DiaryEntry::getId));

        cir.setReturnValue(entries);
    }
}
