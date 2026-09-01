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

    private final String playerName;

    public ResearcherCredentialScreen(String playerName) {
        super(Text.translatable("screen.dear_diary.researcher_credential.title"));
        this.playerName = playerName;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // Primera prueba: dibujar la PNG a su tamaño nativo para evitar que
        // Minecraft repita la textura o la deforme por un escalado incorrecto.
        int x = (this.width - TEXTURE_WIDTH) / 2;
        int y = (this.height - TEXTURE_HEIGHT) / 2;

        context.drawTexture(
                TEXTURE,
                x, y,
                0, 0,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
