package com.worldremembers.deardiary.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.worldremembers.deardiary.rift.ClientRiftState;
import com.worldremembers.deardiary.rift.RiftSkyRenderer;
import com.worldremembers.deardiary.rift.RiftSyncPayload;

public final class DearDiaryClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DiaryNotificationManager.register();
        DearDiaryClientNetworking.register();
        RiftSkyRenderer.register();
        ClientPlayNetworking.registerGlobalReceiver(RiftSyncPayload.ID, (payload, context) -> ClientRiftState.accept(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientRiftState.clear());
        DearDiaryKeybindings.register();
        DearDiaryInventoryButton.register();
    }
}
