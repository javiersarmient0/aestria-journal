package com.worldremembers.deardiary.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.worldremembers.deardiary.DearDiaryMod;
import com.worldremembers.deardiary.data.DiaryCategory;
import com.worldremembers.deardiary.data.DiaryImportance;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;

/** Carga investigaciones desde data/aestria_journal/investigations/*.json. */
public final class AestriaResearchLoader {
    private static final String ROOT = "aestria_journal/investigations";

    private AestriaResearchLoader() {
    }

    public static void reload(ResourceManager resourceManager) {
        List<AestriaResearch> loaded = new ArrayList<>();
        Map<net.minecraft.util.Identifier, Resource> resources = resourceManager.findResources(
                ROOT,
                identifier -> identifier.getPath().endsWith(".json")
        );

        for (Map.Entry<net.minecraft.util.Identifier, Resource> resourceEntry : resources.entrySet()) {
            try (Reader reader = resourceEntry.getValue().getReader()) {
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

                loaded.add(new AestriaResearch(id, title, text, category, importance, icon, shareable));
            } catch (Exception exception) {
                DearDiaryMod.LOGGER.error("No se pudo cargar la investigación {}", resourceEntry.getKey(), exception);
            }
        }

        AestriaResearchRegistry.replaceAll(loaded);
        DearDiaryMod.LOGGER.info("Diario de Aestria: {} investigaciones cargadas", loaded.size());
    }
}
