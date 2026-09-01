package com.worldremembers.deardiary.client.gui;

import com.worldremembers.deardiary.DearDiaryMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ResearcherCredentialScreen extends Screen {
    private static final Identifier TEXTURE = Identifier.of(DearDiaryMod.MOD_ID, "textures/gui/diary_book.png");
    private final String playerName;

    public ResearcherCredentialScreen(String playerName) {
        super(Text.translatable("screen.dear_diary.researcher_credential.title"));
        this.playerName = playerName;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int width = 320;
        int height = 220;
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;

        // Prototipo: reutilizamos la textura del libro para validar el flujo completo.
        context.drawTexture(TEXTURE, x, y, 0, 0, width, height, width, height);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.dear_diary.researcher_credential.title"),
                this.width / 2, y + 14, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.dear_diary.researcher_credential.name", playerName),
                this.width / 2, y + height - 30, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
