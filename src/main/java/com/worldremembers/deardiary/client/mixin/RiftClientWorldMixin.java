package com.worldremembers.deardiary.client.mixin;

import com.worldremembers.deardiary.rift.RiftSkyRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
abstract class RiftClientWorldMixin {
    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
    private void dearDiary$tintClouds(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        cir.setReturnValue(RiftSkyRenderer.tintClouds(cir.getReturnValue()));
    }
}
