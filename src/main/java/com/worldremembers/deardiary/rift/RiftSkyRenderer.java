package com.worldremembers.deardiary.rift;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public final class RiftSkyRenderer {
    static ShaderProgram shader;

    private static final float FOG_R = 0.32F;
    private static final float FOG_G = 0.02F;
    private static final float FOG_B = 0.04F;

    private static VertexBuffer cube;

    private RiftSkyRenderer() {}

    public static void register() {
        net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback.EVENT.register(context ->
                context.register(Identifier.of("dear_diary", "rift_sky"), VertexFormats.POSITION, program -> shader = program));
    }

    public static void renderAfterOpaqueBlocks(Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        if (shader == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (skyHidden(client)) return;

        if (!ClientRiftState.update(client.getRenderTickCounter().getTickDelta(false))) return;

        shader.getUniform("ColorModulator").set(
                ClientRiftState.seconds,
                ClientRiftState.spread,
                ClientRiftState.line,
                ClientRiftState.open);
        shader.getUniform("ModelOffset").set(
                ClientRiftState.fade,
                ClientRiftState.seed,
                ClientRiftState.sweep);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        VertexBuffer buffer = cube();
        buffer.bind();
        buffer.draw(modelViewMatrix, projectionMatrix, shader);
        VertexBuffer.unbind();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static boolean skyHidden(MinecraftClient client) {
        CameraSubmersionType fluid = client.gameRenderer.getCamera().getSubmersionType();
        if (fluid == CameraSubmersionType.LAVA || fluid == CameraSubmersionType.POWDER_SNOW) return true;

        return client.getCameraEntity() instanceof LivingEntity living
                && (living.hasStatusEffect(StatusEffects.BLINDNESS)
                || living.hasStatusEffect(StatusEffects.DARKNESS));
    }

    public static void tintFog(Camera camera, float partialTick, Vector3f color) {
        if (camera.getSubmersionType() != CameraSubmersionType.NONE) return;

        float amount = ClientRiftState.coverage(partialTick);
        if (amount <= 0.0F) return;

        color.set(
                lerp(amount, color.x, FOG_R),
                lerp(amount, color.y, FOG_G),
                lerp(amount, color.z, FOG_B));
    }

    public static net.minecraft.util.math.Vec3d tintClouds(net.minecraft.util.math.Vec3d color) {
        float amount = cloudAmount();
        if (amount <= 0.0F) return color;

        return new net.minecraft.util.math.Vec3d(
                lerp(amount, color.x, 120 / 255.0),
                lerp(amount, color.y, 8 / 255.0),
                lerp(amount, color.z, 16 / 255.0));
    }

    public static float cloudAlpha() {
        return 1.0F - cloudAmount();
    }

    private static float cloudAmount() {
        return ClientRiftState.coverage(
                MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false));
    }

    private static float lerp(float amount, double from, double to) {
        return (float) (from + (to - from) * amount);
    }

    private static VertexBuffer cube() {
        if (cube != null) return cube;

        BufferBuilder builder =
                Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        float s = 10.0F;
        float[][] faces = {
            {-s, s, -s, s, s, -s, s, s, s, -s, s, s},
            {-s, -s, -s, -s, -s, s, s, -s, s, s, -s, -s},
            {s, -s, -s, s, -s, s, s, s, s, s, s, -s},
            {-s, -s, -s, -s, s, -s, -s, s, s, -s, -s, s},
            {-s, -s, s, -s, s, s, s, s, s, s, -s, s},
            {-s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s}
        };

        for (float[] face : faces) {
            for (int i = 0; i < face.length; i += 3) {
                builder.vertex(face[i], face[i + 1], face[i + 2]);
            }
        }

        cube = new VertexBuffer(VertexBuffer.Usage.STATIC);
        cube.bind();
        cube.upload(builder.end());
        VertexBuffer.unbind();

        return cube;
    }
}
