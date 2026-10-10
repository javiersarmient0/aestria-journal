package com.worldremembers.deardiary.client.aurora;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * First visual prototype: animated green/cyan aurora ribbons rendered in the sky.
 * This is intentionally client-only and temporary; mission-controlled visibility will
 * be added after the look and rendering behavior are validated in-game.
 */
public final class AuroraSkyRenderer {
    private static final int RIBBONS = 7;
    private static final int SEGMENTS = 72;
    private static final double SKY_RADIUS = 190.0;

    private AuroraSkyRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_SKY.register(AuroraSkyRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (context.world() == null || context.matrixStack() == null || context.camera() == null) {
            return;
        }

        // Prototype is visible at night only, so it can be evaluated without a command.
        float skyBrightness = 1.0F - context.world().getSkyAngle(context.tickCounter().getTickDelta(true));
        if (context.world().getDimension().hasFixedTime() || context.world().getTimeOfDay() % 24000L < 12500L) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        Vec3d camera = context.camera().getPos();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        double time = (context.world().getTime() + context.tickCounter().getTickDelta(true)) * 0.018;
        float pulse = 0.78F + 0.22F * MathHelper.sin((float) (time * 0.7));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int ribbon = 0; ribbon < RIBBONS; ribbon++) {
            emitRibbon(buffer, matrix, camera, time, ribbon, pulse);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void emitRibbon(BufferBuilder buffer, Matrix4f matrix, Vec3d camera,
                                   double time, int ribbon, float pulse) {
        double center = -1.05 + ribbon * 0.31;
        double width = 0.16 + 0.045 * Math.sin(ribbon * 1.7);
        double phase = ribbon * 1.13;
        int segments = SEGMENTS;

        for (int i = 0; i < segments; i++) {
            double a0 = (double) i / segments;
            double a1 = (double) (i + 1) / segments;
            double x0 = -1.0 + 2.0 * a0;
            double x1 = -1.0 + 2.0 * a1;
            double wave0 = Math.sin(x0 * 4.2 + time + phase) * 0.12
                    + Math.sin(x0 * 8.5 - time * 0.55 + phase) * 0.045;
            double wave1 = Math.sin(x1 * 4.2 + time + phase) * 0.12
                    + Math.sin(x1 * 8.5 - time * 0.55 + phase) * 0.045;
            double y0 = center + wave0;
            double y1 = center + wave1;

            Vec3d p00 = skyPoint(camera, x0, y0 - width, phase);
            Vec3d p01 = skyPoint(camera, x0, y0 + width, phase);
            Vec3d p10 = skyPoint(camera, x1, y1 + width, phase);
            Vec3d p11 = skyPoint(camera, x1, y1 - width, phase);

            float blend = (float) (0.5 + 0.5 * Math.sin((a0 * 3.0 + ribbon * 0.19) * Math.PI));
            int red = Math.round(12 + 12 * blend);
            int green = Math.round((145 + 75 * (1.0F - blend)) * pulse);
            int blue = Math.round((165 + 80 * blend) * pulse);
            int alpha = Math.round(16 + 42 * (float) Math.sin(Math.PI * a0));

            vertex(buffer, matrix, p00, red, green, blue, alpha);
            vertex(buffer, matrix, p01, red, green, blue, alpha);
            vertex(buffer, matrix, p10, red, green, blue, alpha);
            vertex(buffer, matrix, p11, red, green, blue, alpha);
        }
    }

    private static Vec3d skyPoint(Vec3d camera, double horizontal, double vertical, double phase) {
        double angle = horizontal * 1.42 + phase * 0.12;
        double y = vertical * SKY_RADIUS * 0.52 + 38.0;
        double radius = SKY_RADIUS * Math.sqrt(Math.max(0.15, 1.0 - vertical * vertical * 0.13));
        return new Vec3d(
                camera.x + Math.sin(angle) * radius,
                camera.y + y,
                camera.z + Math.cos(angle) * radius
        );
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3d point,
                               int red, int green, int blue, int alpha) {
        buffer.vertex(matrix, (float) (point.x), (float) (point.y), (float) (point.z))
                .color(red, green, blue, alpha);
    }
}
