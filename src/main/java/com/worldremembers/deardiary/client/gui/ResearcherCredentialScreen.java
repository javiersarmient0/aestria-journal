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
    private static final int DISPLAY_WIDTH = 206;
    private static final int DISPLAY_HEIGHT = 246;

    private final String playerName;

    public ResearcherCredentialScreen(String playerName) {
        super(Text.translatable("screen.dear_diary.researcher_credential.title"));
        this.playerName = playerName;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Keep Minecraft's normal blurred world background.
        this.renderBackground(context, mouseX, mouseY, delta);

        // Render the native 103x123 texture and scale the matrix exactly 2x.
        // This avoids asking the texture draw call to resample the image.
        int x = (this.width - DISPLAY_WIDTH) / 2;
        int y = (this.height - DISPLAY_HEIGHT) / 2;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(2.0f, 2.0f, 1.0f);

        context.drawTexture(
                TEXTURE,
                0, 0,
                0, 0,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT
        );

        context.getMatrices().pop();

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
