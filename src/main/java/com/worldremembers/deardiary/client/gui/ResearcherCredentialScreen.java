package com.worldremembers.deardiary.client.gui;

import com.worldremembers.deardiary.DearDiaryMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ResearcherCredentialScreen extends Screen {
    private static final Identifier TEXTURE = Identifier.of(DearDiaryMod.MOD_ID, "textures/gui/credencial.png");
    private static final int TEXTURE_WIDTH = 103;
    private static final int TEXTURE_HEIGHT = 123;
    private static final int DISPLAY_HEIGHT = 360;
    private static final int DISPLAY_WIDTH = Math.round((float) TEXTURE_WIDTH * DISPLAY_HEIGHT / TEXTURE_HEIGHT);

    private final String playerName;

    public ResearcherCredentialScreen(String playerName) {
        super(Text.translatable("screen.dear_diary.researcher_credential.title"));
        this.playerName = playerName;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int x = (this.width - DISPLAY_WIDTH) / 2;
        int y = (this.height - DISPLAY_HEIGHT) / 2;

        // Prototipo: mostramos la credencial respetando su proporción original de 103x123.
        context.drawTexture(TEXTURE, x, y, 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.dear_diary.researcher_credential.title"),
                this.width / 2, y + 12, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.dear_diary.researcher_credential.name", playerName),
                this.width / 2, y + DISPLAY_HEIGHT - 24, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
