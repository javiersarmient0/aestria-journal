package com.worldremembers.deardiary;

import com.worldremembers.deardiary.command.AestriaJournalCommands;
import com.worldremembers.deardiary.compat.fabric.FabricCompatBootstrap;
import com.worldremembers.deardiary.config.DearDiaryConfigManager;
import com.worldremembers.deardiary.api.AestriaJournalApi;
import com.worldremembers.deardiary.network.DearDiaryNetworking;
import com.worldremembers.deardiary.storage.DiaryBackupManager;
import com.worldremembers.deardiary.storage.JsonDiaryStorage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DearDiaryMod implements ModInitializer {
    public static final String MOD_ID = "dear_diary";
    public static final String MOD_NAME = "Diario de Investigador";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve("world_remembers").resolve("dear_diary").resolve(MOD_ID + ".json");
        migrateLegacyConfig(configDir.resolve(MOD_ID + ".json"), configPath);

        DearDiaryConfigManager configManager = new DearDiaryConfigManager(configPath);
        configManager.load();
        DearDiaryServices.setConfigManager(configManager);

        DearDiaryNetworking.register();
        FabricCompatBootstrap.register();
        configManager.writeSupportFiles();
        // El comando original /deardiary queda deshabilitado para que la interfaz pública
        // del mod sea únicamente /diario y no exponga comandos internos en inglés.
        AestriaJournalCommands.register();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Path worldRoot = server.getSavePath(WorldSavePath.ROOT);
            JsonDiaryStorage storage = new JsonDiaryStorage(worldRoot.resolve("data").resolve(MOD_ID).resolve("players"));
            storage.initialize();
            DearDiaryServices.setStorage(storage);
            AestriaJournalApi.reloadResearches(server.getResourceManager());
            LOGGER.info("Diario de Investigador storage initialized at {}", worldRoot.resolve("data").resolve(MOD_ID));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                DearDiaryNetworking.sendDiarySnapshot(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                DiaryBackupManager.backupPlayerDiaryOnLogout(handler.player.getUuid()));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> DearDiaryServices.saveAll());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DearDiaryServices.clearStorage());
    }

    private static void migrateLegacyConfig(Path legacyPath, Path configPath) {
        if (Files.exists(configPath) || Files.notExists(legacyPath)) {
            return;
        }

        try {
            Files.createDirectories(configPath.getParent());
            Files.copy(legacyPath, configPath);
            LOGGER.info("Migrated Dear Diary config from {} to {}", legacyPath, configPath);
        } catch (IOException exception) {
            LOGGER.warn("Failed to migrate Dear Diary config from {} to {}", legacyPath, configPath, exception);
        }
    }
}
