package com.worldremembers.deardiary.research;

import com.worldremembers.deardiary.data.DiaryCategory;
import com.worldremembers.deardiary.data.DiaryImportance;

/** Datos de una investigación persistente del Diario de Investigador. */
public record AestriaResearch(
        String id,
        String title,
        String text,
        DiaryCategory category,
        DiaryImportance importance,
        String icon,
        String chapterId,
        String chapterTitle,
        int chapterOrder,
        String subtitle,
        boolean shareable
) {
}
