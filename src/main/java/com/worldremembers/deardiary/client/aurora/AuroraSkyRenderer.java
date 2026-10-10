package com.worldremembers.deardiary.client.aurora;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Draws an aurora volume by reconstructing world-space camera rays and
 * sampling a high-altitude atmospheric layer. The effect is not attached to
 * screen UVs and does not use a camera-facing world plane.
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

        WorldRenderEvents.LAST.register(AuroraSkyRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (context.world() == null || context.camera() == null || auroraShader == null) {
            return;
        }

        // Overworld at night only while this effect is being tested.
        if (context.world().getDimension().hasFixedTime()
                || context.world().getTimeOfDay() % 24000L < 12500L) {
            return;
        }

        float time = (context.world().getTime()
                + context.tickCounter().getTickDelta(true)) * 0.035F;

        Matrix4f inverseProjection = new Matrix4f(RenderSystem.getProjectionMatrix()).invert();
        Matrix4f inverseModelView = new Matrix4f(RenderSystem.getModelViewMatrix()).invert();
        Vec3d cameraPos = context.camera().getPos();

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
        var cameraPosition = auroraShader.getUniform("CameraPos");
        if (cameraPosition != null) {
            cameraPosition.set((float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z);
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        try {
            RenderSystem.setShader(() -> auroraShader);
            BufferBuilder buffer = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

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