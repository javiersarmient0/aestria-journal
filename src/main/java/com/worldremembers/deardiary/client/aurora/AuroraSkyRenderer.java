package com.worldremembers.deardiary.client.aurora;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Screen-space ray-marched-style aurora. Unlike the previous ring mesh, this
 * evaluates the effect from world-space view directions, so it belongs to the
 * sky dome and changes on screen when the camera rotates.
 */
public final class AuroraSkyRenderer {
    private static final Identifier SHADER_ID = Identifier.of("dear_diary", "aurora");

    private static ShaderProgram auroraShader;

    private AuroraSkyRenderer() {
    }

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context ->
                context.register(SHADER_ID, VertexFormats.POSITION_TEXTURE_COLOR,
                        program -> auroraShader = program));

        // LAST retains the world camera matrices and the completed depth buffer.
        WorldRenderEvents.LAST.register(AuroraSkyRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (context.world() == null || auroraShader == null) {
            return;
        }

        // Temporary test conditions: Overworld, at night.
        if (context.world().getDimension().hasFixedTime()
                || context.world().getTimeOfDay() % 24000L < 12500L) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        float time = (context.world().getTime()
                + context.tickCounter().getTickDelta(true)) * 0.012F;

        Matrix4f inverseProjection = new Matrix4f(RenderSystem.getProjectionMatrix()).invert();
        Matrix4f inverseModelView = new Matrix4f(RenderSystem.getModelViewMatrix()).invert();

        var gameTime = auroraShader.getUniform("GameTime");
        if (gameTime != null) {
            gameTime.set(time);
        }
        var invProj = auroraShader.getUniform("InvProjMat");
        if (invProj != null) {
            invProj.set(inverseProjection);
        }
        var invView = auroraShader.getUniform("InvModelViewMat");
        if (invView != null) {
            invView.set(inverseModelView);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        try {
            RenderSystem.setShader(() -> auroraShader);
            BufferBuilder buffer = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            // A single far-plane quad. The fragment shader reconstructs a world
            // ray per pixel; depth testing limits the overlay to visible sky.
            fullscreenVertex(buffer, -1.0F, -1.0F, 1.0F, 0.0F, 0.0F);
            fullscreenVertex(buffer, -1.0F,  1.0F, 1.0F, 0.0F, 1.0F);
            fullscreenVertex(buffer,  1.0F,  1.0F, 1.0F, 1.0F, 1.0F);
            fullscreenVertex(buffer,  1.0F, -1.0F, 1.0F, 1.0F, 0.0F);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void fullscreenVertex(BufferBuilder buffer, float x, float y, float z,
                                         float u, float v) {
        buffer.vertex(x, y, z).texture(u, v).color(255, 255, 255, 255);
    }
}