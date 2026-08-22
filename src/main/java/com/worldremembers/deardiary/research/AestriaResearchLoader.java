package com.worldremembers.deardiary.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.worldremembers.deardiary.DearDiaryMod;
import com.worldremembers.deardiary.data.DiaryCategory;
import com.worldremembers.deardiary.data.DiaryImportance;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

/** Carga las investigaciones desde JSON externos editables por el administrador. */
public final class AestriaResearchLoader {
    private static final String NAMESPACE = "aestria_journal";
    private static final String ROOT = "investigations";
    private static final Path EXTERNAL_ROOT = FabricLoader.getInstance().getConfigDir()
            .resolve("aestria_journal")
            .resolve(ROOT);

    private AestriaResearchLoader() {
    }

    public static Path getExternalRoot() {
        return EXTERNAL_ROOT;
    }

    public static void reload(ResourceManager resourceManager) {
        List<AestriaResearch> loaded = new ArrayList<>();
        Map<Identifier, Resource> bundled = resourceManager.findResources(
                ROOT,
                identifier -> identifier.getNamespace().equals(NAMESPACE)
                        && identifier.getPath().endsWith(".json")
        );

        ensureExternalFiles(bundled);
        boolean externalFilesFound = false;
        boolean externalError = false;

        if (Files.isDirectory(EXTERNAL_ROOT)) {
            try (var paths = Files.list(EXTERNAL_ROOT)) {
                var jsonPaths = paths
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
                externalFilesFound = !jsonPaths.isEmpty();
                for (Path path : jsonPaths) {
                    if (!loadFile(path, loaded)) {
                        externalError = true;
                    }
                }
            } catch (IOException exception) {
                externalError = true;
                DearDiaryMod.LOGGER.error("No se pudieron leer las investigaciones externas de Diario de Investigador", exception);
            }
        }

        if (externalError) {
            DearDiaryMod.LOGGER.error("Diario de Investigador: la recarga fue cancelada porque al menos un JSON es inválido. Se conserva la configuración anterior.");
            return;
        }

        // Los JSON incluidos en el mod solo sirven como plantillas iniciales.
        // Una vez copiados a config/, los archivos externos pasan a ser la única fuente de contenido.
        if (!externalFilesFound) {
            for (Map.Entry<Identifier, Resource> resourceEntry : bundled.entrySet()) {
                try (Reader reader = new InputStreamReader(
                        resourceEntry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
                    if (!loadJson(JsonParser.parseReader(reader).getAsJsonObject(), resourceEntry.getKey().toString(), loaded)) {
                        DearDiaryMod.LOGGER.error("No se pudo cargar la investigación {}", resourceEntry.getKey());
                    }
                } catch (Exception exception) {
                    DearDiaryMod.LOGGER.error("No se pudo cargar la investigación {}", resourceEntry.getKey(), exception);
                }
            }
        }

        AestriaResearchRegistry.replaceAll(loaded);
        DearDiaryMod.LOGGER.info("Diario de Investigador: {} investigaciones cargadas desde {}", loaded.size(), EXTERNAL_ROOT);
    }

    private static void ensureExternalFiles(Map<Identifier, Resource> bundled) {
        try {
            Files.createDirectories(EXTERNAL_ROOT);
            for (Map.Entry<Identifier, Resource> resourceEntry : bundled.entrySet()) {
                String fileName = resourceEntry.getKey().getPath().substring(ROOT.length() + 1);
                Path target = EXTERNAL_ROOT.resolve(fileName);
                if (Files.exists(target)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                try (var input = resourceEntry.getValue().getInputStream();
                     OutputStream output = Files.newOutputStream(target)) {
                    input.transferTo(output);
                }
                DearDiaryMod.LOGGER.info("Diario de Investigador: creado JSON editable {}", target);
            }
        } catch (IOException exception) {
            DearDiaryMod.LOGGER.error("No se pudieron preparar los JSON editables de Diario de Investigador", exception);
        }
    }

    private static boolean loadFile(Path path, List<AestriaResearch> loaded) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return loadJson(JsonParser.parseReader(reader).getAsJsonObject(), path.toString(), loaded);
        } catch (Exception exception) {
            DearDiaryMod.LOGGER.error("No se pudo cargar {}. La recarga será cancelada.", path, exception);
            return false;
        }
    }

    private static boolean loadJson(JsonObject json, String source, List<AestriaResearch> loaded) {
        try {
            String id = json.get("id").getAsString();
            String title = json.get("title").getAsString();
            String text = json.has("text") ? json.get("text").getAsString() : "";

            if (json.has("content") && json.get("content").isJsonArray()) {
                StringBuilder builder = new StringBuilder();
                for (var element : json.getAsJsonArray("content")) {
                    if (element.isJsonPrimitive()) {
                        if (builder.length() > 0) builder.append("\n\n");
                        builder.append(element.getAsString());
                    } else if (element.isJsonObject() && element.getAsJsonObject().has("text")) {
                        if (builder.length() > 0) builder.append("\n\n");
                        builder.append(element.getAsJsonObject().get("text").getAsString());
                    }
                }
                if (builder.length() > 0) text = builder.toString();
            }

            DiaryCategory category = json.has("category")
                    ? DiaryCategory.valueOf(json.get("category").getAsString().toUpperCase())
                    : DiaryCategory.DISCOVERY;
            DiaryImportance importance = json.has("importance")
                    ? DiaryImportance.valueOf(json.get("importance").getAsString().toUpperCase())
                    : DiaryImportance.NORMAL;
            String icon = json.has("icon") ? json.get("icon").getAsString() : "minecraft:writable_book";
            boolean shareable = json.has("shareable") && json.get("shareable").getAsBoolean();
            String chapterId = json.has("chapter_id") ? json.get("chapter_id").getAsString() : defaultChapterId(id);
            String chapterTitle = json.has("chapter_title") ? json.get("chapter_title").getAsString() : defaultChapterTitle(chapterId);

            loaded.add(new AestriaResearch(id, title, text, category, importance, icon, chapterId, chapterTitle, shareable));
            return true;
        } catch (Exception exception) {
            DearDiaryMod.LOGGER.error("Investigación inválida en {}. El archivo fue ignorado.", source, exception);
            return false;
        }
    }

    private static String defaultChapterId(String researchId) {
        return switch (researchId) {
            case "capitan_jones", "damian", "marinero_elias" -> "puerto_cerezo";
            case "tomas", "astronomo", "profesor_oak", "cultivos_auroras", "primera_investigacion", "akira", "ernesto" -> "pueblo_albor";
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
