package com.worldremembers.deardiary.research;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registro de investigaciones disponibles para el Diario de Aestria. */
public final class AestriaResearchRegistry {
    private static final Map<String, AestriaResearch> RESEARCHES = new LinkedHashMap<>();

    private AestriaResearchRegistry() {
    }

    public static void replaceAll(Collection<AestriaResearch> researches) {
        RESEARCHES.clear();
        for (AestriaResearch research : researches) {
            if (research != null && research.id() != null && !research.id().isBlank()) {
                RESEARCHES.put(research.id(), research);
            }
        }
    }

    public static Optional<AestriaResearch> get(String id) {
        return Optional.ofNullable(RESEARCHES.get(id));
    }

    public static List<AestriaResearch> all() {
        return List.copyOf(RESEARCHES.values());
    }

    public static int size() {
        return RESEARCHES.size();
    }
}
