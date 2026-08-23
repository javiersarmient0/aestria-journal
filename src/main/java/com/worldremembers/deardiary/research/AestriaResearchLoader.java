package com.worldremembers.deardiary.research;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.worldremembers.deardiary.DearDiaryMod;
import com.worldremembers.deardiary.data.DiaryCategory;
import com.worldremembers.deardiary.data.DiaryImportance;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
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

/** Carga investigaciones desde JSON externos, organizados por capítulo/pueblo. */
public final class AestriaResearchLoader {
    private static final String NAMESPACE = "aestria_journal";
    private static final String ROOT = "investigations";
    private static final Path EXTERNAL_ROOT = FabricLoader.getInstance().getConfigDir().resolve("aestria_journal").resolve(ROOT);

    private AestriaResearchLoader() {}
    public static Path getExternalRoot() { return EXTERNAL_ROOT; }

    public static void reload(ResourceManager resourceManager) {
        List<AestriaResearch> loaded = new ArrayList<>();
        Map<Identifier, Resource> bundled = resourceManager.findResources(ROOT,
                identifier -> identifier.getNamespace().equals(NAMESPACE) && identifier.getPath().endsWith(".json"));

        initializeExternalFilesIfNeeded(bundled);
        boolean externalMode = Files.isDirectory(EXTERNAL_ROOT);
        boolean externalError = false;

        if (externalMode) {
            try (var paths = Files.walk(EXTERNAL_ROOT)) {
                var jsonPaths = paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted(Comparator.comparing(Path::toString)).toList();
                for (Path path : jsonPaths) if (!loadFile(path, loaded)) externalError = true;
            } catch (IOException exception) {
                externalError = true;
                DearDiaryMod.LOGGER.error("No se pudieron leer las investigaciones externas de Diario de Investigador", exception);
            }
        }

        if (externalError) {
            DearDiaryMod.LOGGER.error("Diario de Investigador: recarga cancelada por JSON inválido; se conserva la configuración anterior.");
            return;
        }

        // Once the external directory exists, it is authoritative even when it is empty.
        // This makes deleting the last JSON a real deletion instead of restoring bundled defaults.
        if (!externalMode) {
            for (Map.Entry<Identifier, Resource> resourceEntry : bundled.entrySet()) {
                try (Reader reader = new InputStreamReader(resourceEntry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
                    loadJson(JsonParser.parseReader(reader).getAsJsonObject(), resourceEntry.getKey().toString(), loaded);
                } catch (Exception exception) {
                    DearDiaryMod.LOGGER.error("No se pudo cargar la investigación {}", resourceEntry.getKey(), exception);
                }
            }
        }

        loaded.sort(Comparator.comparingInt(AestriaResearch::chapterOrder)
                .thenComparing(AestriaResearch::chapterId)
                .thenComparing(AestriaResearch::id));
        AestriaResearchRegistry.replaceAll(loaded);
        DearDiaryMod.LOGGER.info("Diario de Investigador: {} investigaciones cargadas desde {}", loaded.size(), EXTERNAL_ROOT);
    }

    private static void initializeExternalFilesIfNeeded(Map<Identifier, Resource> bundled) {
        try {
            if (Files.exists(EXTERNAL_ROOT)) return;
            Files.createDirectories(EXTERNAL_ROOT);
            for (Map.Entry<Identifier, Resource> resourceEntry : bundled.entrySet()) {
                String relative = resourceEntry.getKey().getPath().substring(ROOT.length() + 1);
                Path target = EXTERNAL_ROOT.resolve(relative);
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                try (var input = resourceEntry.getValue().getInputStream(); OutputStream output = Files.newOutputStream(target)) {
                    input.transferTo(output);
                }
            }
            DearDiaryMod.LOGGER.info("Diario de Investigador: se creó la carpeta inicial de investigaciones en {}", EXTERNAL_ROOT);
        } catch (IOException exception) {
            DearDiaryMod.LOGGER.error("No se pudieron preparar los JSON iniciales de Diario de Investigador", exception);
        }
    }

    private static boolean loadFile(Path path, List<AestriaResearch> loaded) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return loadJson(JsonParser.parseReader(reader).getAsJsonObject(), path.toString(), loaded);
        } catch (Exception exception) {
            DearDiaryMod.LOGGER.error("No se pudo cargar {}", path, exception);
            return false;
        }
    }

    private static boolean loadJson(JsonObject json, String source, List<AestriaResearch> loaded) {
        try {
            String id = json.get("id").getAsString();
            if ("astronomo".equals(id)) return true;
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
            DiaryCategory category = json.has("category") ? DiaryCategory.valueOf(json.get("category").getAsString().toUpperCase()) : DiaryCategory.DISCOVERY;
            DiaryImportance importance = json.has("importance") ? DiaryImportance.valueOf(json.get("importance").getAsString().toUpperCase()) : DiaryImportance.NORMAL;
            String icon = json.has("icon") ? json.get("icon").getAsString() : "minecraft:writable_book";
            boolean shareable = json.has("shareable") && json.get("shareable").getAsBoolean();
            String chapterId = json.has("chapter_id") ? json.get("chapter_id").getAsString() : defaultChapterId(id);
            String chapterTitle = json.has("chapter_title") ? json.get("chapter_title").getAsString() : defaultChapterTitle(chapterId);
            int chapterOrder = json.has("chapter_order") ? json.get("chapter_order").getAsInt() : defaultChapterOrder(chapterId);
            String subtitle = json.has("subtitle") ? json.get("subtitle").getAsString() : "";
            loaded.add(new AestriaResearch(id, title, text, category, importance, icon, chapterId, chapterTitle, chapterOrder, subtitle, shareable));
            return true;
        } catch (Exception exception) {
            DearDiaryMod.LOGGER.error("Investigación inválida en {}", source, exception);
            return false;
        }
    }

    private static String defaultChapterId(String researchId) {
        return switch (researchId) {
            case "capitan_jones", "damian", "marinero_elias" -> "puerto_cerezo";
            case "tomas", "astronomo", "profesor_oak", "akira", "ernesto" -> "pueblo_albor";
            case "cultivos_auroras", "primera_investigacion" -> "anotaciones_personales";
            default -> "general";
        };
    }

    private static String defaultChapterTitle(String chapterId) {
        return switch (chapterId) {
            case "puerto_cerezo" -> "Capítulo 1 · Puerto Cerezo";
            case "pueblo_albor" -> "Capítulo 2 · Pueblo Albor";
            case "anotaciones_personales" -> "Capítulo 3 · Anotaciones personales";
            default -> "Investigaciones";
        };
    }

    private static int defaultChapterOrder(String chapterId) {
        return switch (chapterId) {
            case "puerto_cerezo" -> 1;
            case "pueblo_albor" -> 2;
            case "anotaciones_personales" -> 3;
            default -> 1000;
        };
    }
}
