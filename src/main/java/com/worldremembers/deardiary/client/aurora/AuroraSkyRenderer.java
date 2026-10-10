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
 * Client-side procedural aurora curtains. This is a geometry-based prototype,
 * designed to create layered translucent veils and fine vertical light rays.
 */
public final class AuroraSkyRenderer {
    private static final int CURTAIN_LAYERS = 4;
    private static final int SEGMENTS = 128;
    private static final int RAYS = 115;
    private static final double SKY_RADIUS = 150.0;

    private AuroraSkyRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(AuroraSkyRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (context.world() == null || context.matrixStack() == null || context.camera() == null) {
            return;
        }

        // Temporary test behavior: visible only at night in dimensions without fixed time.
        if (context.world().getDimension().hasFixedTime()
                || context.world().getTimeOfDay() % 24000L < 12500L) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        double time = (context.world().getTime()
                + context.tickCounter().getTickDelta(true)) * 0.012;
        float pulse = 0.82F + 0.18F * MathHelper.sin((float) (time * 0.42));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Broad, soft veils behind the sharper filaments.
        for (int layer = 0; layer < CURTAIN_LAYERS; layer++) {
            emitCurtain(buffer, matrix, time, layer, pulse);
        }

        // Fine, uneven vertical rays give the curtains their aurora-like texture.
        emitRays(buffer, matrix, time, pulse);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void emitCurtain(BufferBuilder buffer, Matrix4f matrix,
                                    double time, int layer, float pulse) {
        double phase = layer * 1.47;
        double radius = SKY_RADIUS + layer * 13.0;
        double arcCenter = -0.12 + layer * 0.10;
        double arcWidth = 2.35 - layer * 0.13;

        for (int i = 0; i < SEGMENTS; i++) {
            double u0 = (double) i / SEGMENTS;
            double u1 = (double) (i + 1) / SEGMENTS;
            double a0 = arcCenter + (u0 - 0.5) * arcWidth;
            double a1 = arcCenter + (u1 - 0.5) * arcWidth;

            double top0 = 64.0 + 9.0 * Math.sin(a0 * 2.2 + time * 0.35 + phase)
                    + 5.0 * Math.sin(a0 * 5.4 - time * 0.21 + phase);
            double top1 = 64.0 + 9.0 * Math.sin(a1 * 2.2 + time * 0.35 + phase)
                    + 5.0 * Math.sin(a1 * 5.4 - time * 0.21 + phase);
            double bottom0 = 24.0 + 8.0 * Math.sin(a0 * 2.8 - time * 0.24 + phase)
                    + 4.0 * Math.sin(a0 * 6.1 + time * 0.18);
            double bottom1 = 24.0 + 8.0 * Math.sin(a1 * 2.8 - time * 0.24 + phase)
                    + 4.0 * Math.sin(a1 * 6.1 + time * 0.18);

            Vec3d p0 = arcPoint(a0, top0, radius);
            Vec3d p1 = arcPoint(a0, bottom0, radius);
            Vec3d p2 = arcPoint(a1, bottom1, radius);
            Vec3d p3 = arcPoint(a1, top1, radius);

            float wave = (float) (0.5 + 0.5 * Math.sin(u0 * 8.0 + time * 0.7 + phase));
            int red = 28 + layer * 3;
            int green = Math.round((95 + 70 * wave) * pulse);
            int blue = Math.round((115 + 85 * (1.0F - wave)) * pulse);
            int alpha = Math.round((5 + 7 * wave) * (1.0F - layer * 0.13F));

            vertex(buffer, matrix, p0, red, green, blue, alpha);
            vertex(buffer, matrix, p1, red, green, blue, alpha / 2);
            vertex(buffer, matrix, p2, red, green, blue, alpha / 2);
            vertex(buffer, matrix, p3, red, green, blue, alpha);
        }
    }

    private static void emitRays(BufferBuilder buffer, Matrix4f matrix,
                                 double time, float pulse) {
        for (int i = 0; i < RAYS; i++) {
            double u = (i + 0.5) / RAYS;
            double angle = -1.24 + u * 2.42;
            double drift = Math.sin(i * 12.9898) * 0.012;
            double moving = Math.sin(time * 0.55 + i * 0.31);
            double top = 54.0 + 13.0 * Math.sin(angle * 2.0 + time * 0.28)
                    + 7.0 * Math.sin(i * 0.19 + time * 0.17);
            double bottom = 17.0 + 10.0 * Math.sin(angle * 3.1 - time * 0.20 + i * 0.08);
            double lean = drift + moving * 0.018;
            double halfWidth = 0.12 + 0.20 * (0.5 + 0.5 * Math.sin(i * 0.73));

            Vec3d p0 = arcPoint(angle - halfWidth / SKY_RADIUS, top, SKY_RADIUS + 2.0);
            Vec3d p1 = arcPoint(angle + halfWidth / SKY_RADIUS, top + lean * 18.0, SKY_RADIUS + 2.0);
            Vec3d p2 = arcPoint(angle + halfWidth / SKY_RADIUS, bottom + lean * 8.0, SKY_RADIUS + 2.0);
            Vec3d p3 = arcPoint(angle - halfWidth / SKY_RADIUS, bottom, SKY_RADIUS + 2.0);

            float strength = (float) (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(i * 0.41 + time * 0.65)));
            int red = Math.round(48 * strength);
            int green = Math.round((165 + 65 * strength) * pulse);
            int blue = Math.round((150 + 80 * (1.0F - strength)) * pulse);
            int alpha = Math.round(12 + 35 * strength);

            vertex(buffer, matrix, p0, red, green, blue, alpha / 3);
            vertex(buffer, matrix, p1, red, green, blue, alpha);
            vertex(buffer, matrix, p2, red, green, blue, alpha / 2);
            vertex(buffer, matrix, p3, red, green, blue, alpha / 4);
        }
    }

    private static Vec3d arcPoint(double angle, double y, double radius) {
        return new Vec3d(Math.sin(angle) * radius, y, Math.cos(angle) * radius);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3d point,
                               int red, int green, int blue, int alpha) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha);
    }
}
