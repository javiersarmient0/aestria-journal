package com.worldremembers.deardiary.research;

import com.worldremembers.deardiary.data.DiaryCategory;
import com.worldremembers.deardiary.data.DiaryImportance;

/** Datos de una investigación persistente del Diario de Aestria. */
public record AestriaResearch(
        String id,
        String title,
        String text,
        DiaryCategory category,
        DiaryImportance importance,
        String icon,
        boolean shareable
) {
}
