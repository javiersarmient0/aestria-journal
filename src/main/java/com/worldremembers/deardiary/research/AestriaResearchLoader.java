package com.worldremembers.deardiary.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.worldremembers.deardiary.DearDiaryMod;
import com.worldremembers.deardiary.data.DiaryCategory;
import com.worldremembers.deardiary.data.DiaryImportance;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

/** Carga investigaciones desde data/aestria_journal/investigations/*.json. */
public final class AestriaResearchLoader {
    private static final String NAMESPACE = "aestria_journal";
    private static final String ROOT = "investigations";

    private AestriaResearchLoader() {
    }

    public static void reload(ResourceManager resourceManager) {
        List<AestriaResearch> loaded = new ArrayList<>();
        Map<Identifier, Resource> resources = resourceManager.findResources(
                ROOT,
                identifier -> identifier.getNamespace().equals(NAMESPACE)
                        && identifier.getPath().endsWith(".json")
        );

        DearDiaryMod.LOGGER.info(
                "Diario de Aestria: buscando investigaciones en {}:{} ({} archivos encontrados)",
                NAMESPACE,
                ROOT,
                resources.size()
        );

        for (Map.Entry<Identifier, Resource> resourceEntry : resources.entrySet()) {
            try (Reader reader = new InputStreamReader(
                    resourceEntry.getValue().getInputStream(),
                    StandardCharsets.UTF_8
            )) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                String id = json.get("id").getAsString();
                String title = json.get("title").getAsString();
                String text = json.get("text").getAsString();

                DiaryCategory category = json.has("category")
                        ? DiaryCategory.valueOf(json.get("category").getAsString().toUpperCase())
                        : DiaryCategory.DISCOVERY;

                DiaryImportance importance = json.has("importance")
                        ? DiaryImportance.valueOf(json.get("importance").getAsString().toUpperCase())
                        : DiaryImportance.NORMAL;

                String icon = json.has("icon")
                        ? json.get("icon").getAsString()
                        : "minecraft:writable_book";

                boolean shareable = !json.has("shareable") || json.get("shareable").getAsBoolean();

                String chapterId = json.has("chapter_id")
                        ? json.get("chapter_id").getAsString()
                        : defaultChapterId(id);
                String chapterTitle = json.has("chapter_title")
                        ? json.get("chapter_title").getAsString()
                        : defaultChapterTitle(chapterId);

                loaded.add(new AestriaResearch(
                        id, title, text, category, importance, icon,
                        chapterId, chapterTitle, shareable
                ));
                DearDiaryMod.LOGGER.info(
                        "Diario de Aestria: investigación cargada: {} - {} ({})",
                        id, title, chapterTitle
                );
            } catch (Exception exception) {
                DearDiaryMod.LOGGER.error(
                        "No se pudo cargar la investigación {}",
                        resourceEntry.getKey(),
                        exception
                );
            }
        }

        AestriaResearchRegistry.replaceAll(loaded);
        DearDiaryMod.LOGGER.info("Diario de Aestria: {} investigaciones cargadas", loaded.size());
    }

    private static String defaultChapterId(String researchId) {
        return switch (researchId) {
            case "capitan_jones", "damian", "marinero_elias" -> "puerto_cerezo";
            case "tomas", "astronomo", "profesor_oak", "cultivos_auroras", "primera_investigacion" -> "pueblo_albor";
            default -> "general";
        };
    }

    private static String defaultChapterTitle(String chapterId) {
        return switch (chapterId) {
            case "puerto_cerezo" -> "Capítulo 1 · Puerto Cerezo";
            case "pueblo_albor" -> "Capítulo 2 · Pueblo Albor";
            default -> "Investigaciones";
        };
    }
}
