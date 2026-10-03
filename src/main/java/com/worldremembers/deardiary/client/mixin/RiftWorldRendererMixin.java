package com.worldremembers.deardiary.client.mixin;

import com.worldremembers.deardiary.rift.RiftSkyRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
abstract class RiftWorldRendererMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
                    ordinal = 2,
                    shift = At.Shift.AFTER))
    private void dearDiary$renderRift(
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f matrix4f,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        RiftSkyRenderer.renderAfterOpaqueBlocks(matrix4f, projectionMatrix);
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void dearDiary$hideClouds(
            net.minecraft.client.util.math.MatrixStack matrices,
            Matrix4f matrix4f,
            Matrix4f matrix4f2,
            float tickDelta,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfo ci) {
        if (RiftSkyRenderer.cloudAlpha() <= 0.0F) ci.cancel();
    }
}
