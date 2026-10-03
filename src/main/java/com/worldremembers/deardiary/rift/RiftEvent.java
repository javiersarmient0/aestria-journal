package com.worldremembers.deardiary.rift;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public final class RiftEvent {
    private static RiftSyncPayload state = RiftSyncPayload.NONE;

    private RiftEvent() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> state = RiftSyncPayload.NONE);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("brecha")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("iniciar").executes(context -> start(context.getSource(), false)))
                .then(CommandManager.literal("reiniciar").executes(context -> start(context.getSource(), true)))
                .then(CommandManager.literal("detener").executes(context -> stop(context.getSource()))));
    }

    public static void syncTo(net.minecraft.server.network.ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player, RiftSyncPayload.ID)) ServerPlayNetworking.send(player, state);
    }

    private static int start(ServerCommandSource source, boolean restart) {
        long now = source.getServer().getOverworld().getTime();
        if (!restart && RiftTimeline.isRunning(now, state.startTick(), state.stopTick())) {
            source.sendError(Text.literal("La brecha ya está activa."));
            return 0;
        }
        state = new RiftSyncPayload(now, -1L, source.getWorld().getRandom().nextInt());
        PlayerLookup.all(source.getServer()).forEach(player -> {
            if (ServerPlayNetworking.canSend(player, RiftSyncPayload.ID)) ServerPlayNetworking.send(player, state);
        });
        source.sendFeedback(() -> Text.literal("La brecha dimensional comenzó a abrirse."), false);
        return 1;
    }

    private static int stop(ServerCommandSource source) {
        long now = source.getServer().getOverworld().getTime();
        if (!RiftTimeline.isRunning(now, state.startTick(), state.stopTick())) {
            source.sendError(Text.literal("No hay una brecha activa para detener."));
            return 0;
        }
        state = new RiftSyncPayload(state.startTick(), now, state.seed());
        PlayerLookup.all(source.getServer()).forEach(player -> {
            if (ServerPlayNetworking.canSend(player, RiftSyncPayload.ID)) ServerPlayNetworking.send(player, state);
        });
        source.sendFeedback(() -> Text.literal("La brecha dimensional comenzó a cerrarse."), false);
        return 1;
    }
}
