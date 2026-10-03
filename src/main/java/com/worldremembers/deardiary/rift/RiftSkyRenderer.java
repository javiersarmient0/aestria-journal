package com.worldremembers.deardiary.rift;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.entity.LivingEntity;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class RiftSkyRenderer {
    private static ShaderProgram shader;
    private static VertexBuffer cube;

    private RiftSkyRenderer() {}

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(
                Identifier.of("dear_diary", "rift_sky"), VertexFormats.POSITION, program -> shader = program));
        WorldRenderEvents.AFTER_ENTITIES.register(RiftSkyRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (shader == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (skyHidden(client)) return;
        if (!ClientRiftState.update(client.getRenderTickCounter().getTickDelta(false))) return;

        var colorModulator = shader.getUniform("ColorModulator");
        if (colorModulator != null) {
            colorModulator.set(ClientRiftState.seconds, ClientRiftState.spread, ClientRiftState.line, ClientRiftState.open);
        }
        var modelOffset = shader.getUniform("ModelOffset");
        if (modelOffset != null) {
            modelOffset.set(ClientRiftState.fade, ClientRiftState.seed, ClientRiftState.sweep);
        }

        Matrix4f modelView = new Matrix4f(context.matrixStack().peek().getPositionMatrix());
        Matrix4f projection = new Matrix4f(context.projectionMatrix());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        VertexBuffer buffer = cube();
        buffer.bind();
        buffer.draw(modelView, projection, shader);
        VertexBuffer.unbind();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static boolean skyHidden(MinecraftClient client) {
        CameraSubmersionType fluid = client.gameRenderer.getCamera().getSubmersionType();
        if (fluid == CameraSubmersionType.LAVA || fluid == CameraSubmersionType.POWDER_SNOW) return true;
        return client.getCameraEntity() instanceof LivingEntity living
                && (living.hasStatusEffect(StatusEffects.BLINDNESS) || living.hasStatusEffect(StatusEffects.DARKNESS));
    }

    private static VertexBuffer cube() {
        if (cube != null) return cube;
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
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
            for (int i = 0; i < face.length; i += 3) builder.vertex(face[i], face[i + 1], face[i + 2]);
        }
        cube = new VertexBuffer(VertexBuffer.Usage.STATIC);
        cube.bind();
        cube.upload(builder.end());
        VertexBuffer.unbind();
        return cube;
    }
}
