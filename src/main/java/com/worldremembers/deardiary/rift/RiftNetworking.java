package com.worldremembers.deardiary.rift;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class RiftNetworking {
    private RiftNetworking() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(RiftSyncPayload.ID, RiftSyncPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, RiftSyncPayload.ID)) {
                ServerPlayNetworking.send(handler.player, RiftSyncPayload.NONE);
            }
        });
    }
}
