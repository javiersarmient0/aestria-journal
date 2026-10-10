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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Procedural aurora curtain rendered with a custom Minecraft core shader.
 * Geometry is camera-relative: the curtain forms a broad ring around the viewer,
 * while the fragment shader supplies the soft veil and animated filaments.
 */
public final class AuroraSkyRenderer {
    private static final Identifier SHADER_ID = Identifier.of("dear_diary", "aurora");
    private static final int SEGMENTS = 256;
    private static final double RADIUS = 180.0;

    private static ShaderProgram auroraShader;
    private static net.minecraft.client.world.ClientWorld anchoredWorld;
    private static double anchorX;
    private static double anchorZ;

    private AuroraSkyRenderer() {
    }

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context ->
                context.register(SHADER_ID, VertexFormats.POSITION_TEXTURE_COLOR,
                        program -> auroraShader = program));

        // END lets the effect render after the ordinary sky/cloud pass.
        WorldRenderEvents.END.register(AuroraSkyRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (context.world() == null || context.matrixStack() == null || auroraShader == null) {
            return;
        }

        // This is a temporary visual test: Overworld only, at night.
        if (context.world().getDimension().hasFixedTime()
                || context.world().getTimeOfDay() % 24000L < 12500L) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Keep the aurora anchored to a fixed point in world space for this world session.
        // WorldRenderContext's matrix is camera-oriented, so submitted vertices must be
        // expressed relative to the camera position rather than centered at (0, 0, 0).
        var cameraPos = context.camera().getPos();
        if (anchoredWorld != context.world()) {
            anchoredWorld = context.world();
            anchorX = Math.floor(cameraPos.x / 256.0 + 0.5) * 256.0;
            anchorZ = Math.floor(cameraPos.z / 256.0 + 0.5) * 256.0;
        }

        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        float time = (context.world().getTime()
                + context.tickCounter().getTickDelta(true)) * 0.01F;

        var gameTime = auroraShader.getUniform("GameTime");
        if (gameTime != null) {
            gameTime.set(time);
        }

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        // Render as a distant sky effect, not as a world-space wall occluded by terrain.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        try {
            RenderSystem.setShader(() -> auroraShader);
            BufferBuilder buffer = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            // One continuous 360-degree curtain, subdivided for a smooth silhouette.
            for (int i = 0; i < SEGMENTS; i++) {
                float u0 = (float) i / SEGMENTS;
                float u1 = (float) (i + 1) / SEGMENTS;
                double a0 = u0 * Math.PI * 2.0;
                double a1 = u1 * Math.PI * 2.0;

                double x0 = anchorX + Math.cos(a0) * RADIUS - cameraX;
                double z0 = anchorZ + Math.sin(a0) * RADIUS - cameraZ;
                double x1 = anchorX + Math.cos(a1) * RADIUS - cameraX;
                double z1 = anchorZ + Math.sin(a1) * RADIUS - cameraZ;

                double top0 = 78.0
                        + 8.0 * Math.sin(a0 * 2.0 + time * 0.22)
                        + 4.0 * Math.sin(a0 * 5.0 - time * 0.13);
                double top1 = 78.0
                        + 8.0 * Math.sin(a1 * 2.0 + time * 0.22)
                        + 4.0 * Math.sin(a1 * 5.0 - time * 0.13);
                double bottom0 = 28.0
                        + 5.0 * Math.sin(a0 * 3.0 - time * 0.17)
                        + 3.0 * Math.sin(a0 * 7.0 + time * 0.11);
                double bottom1 = 28.0
                        + 5.0 * Math.sin(a1 * 3.0 - time * 0.17)
                        + 3.0 * Math.sin(a1 * 7.0 + time * 0.11);

                // UV.y = 0 at the base and 1 at the top.
                vertex(buffer, matrix, x0, bottom0 - cameraY, z0, u0, 0.0F);
                vertex(buffer, matrix, x0, top0 - cameraY, z0, u0, 1.0F);
                vertex(buffer, matrix, x1, top1 - cameraY, z1, u1, 1.0F);
                vertex(buffer, matrix, x1, bottom1 - cameraY, z1, u1, 0.0F);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix,
                               double x, double y, double z, float u, float v) {
        buffer.vertex(matrix, (float) x, (float) y, (float) z)
                .texture(u, v)
                .color(255, 255, 255, 255);
    }
}