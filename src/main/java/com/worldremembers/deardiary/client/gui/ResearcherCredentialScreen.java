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

        // Draw the 103x123 source texture at an exact 2x scale, centered.
        // The source UV size remains 103x123 so the texture is never repeated.
        int x = (this.width - DISPLAY_WIDTH) / 2;
        int y = (this.height - DISPLAY_HEIGHT) / 2;

        context.drawTexture(
                TEXTURE,
                x, y,
                0, 0,
                DISPLAY_WIDTH, DISPLAY_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
