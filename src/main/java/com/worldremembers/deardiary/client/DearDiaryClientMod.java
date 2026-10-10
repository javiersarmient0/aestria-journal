package com.worldremembers.deardiary.client;

import net.fabricmc.api.ClientModInitializer;
import com.worldremembers.deardiary.client.aurora.AuroraSkyRenderer;

public final class DearDiaryClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DiaryNotificationManager.register();
        DearDiaryClientNetworking.register();
        AuroraSkyRenderer.register();
        DearDiaryKeybindings.register();
        DearDiaryInventoryButton.register();
    }
}
