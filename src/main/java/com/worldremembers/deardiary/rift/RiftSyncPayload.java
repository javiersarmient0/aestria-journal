package com.worldremembers.deardiary.rift;

import com.worldremembers.deardiary.DearDiaryMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RiftSyncPayload(long startTick, long stopTick, int seed) implements CustomPayload {
    public static final RiftSyncPayload NONE = new RiftSyncPayload(-1L, -1L, 0);
    public static final Id<RiftSyncPayload> ID = new Id<>(Identifier.of(DearDiaryMod.MOD_ID, "rift_sync"));
    public static final PacketCodec<RegistryByteBuf, RiftSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, RiftSyncPayload::startTick,
            PacketCodecs.VAR_LONG, RiftSyncPayload::stopTick,
            PacketCodecs.INTEGER, RiftSyncPayload::seed,
            RiftSyncPayload::new);

    public RiftSyncPayload {
        if (startTick < -1 || stopTick < -1 || (stopTick >= 0 && (startTick < 0 || stopTick < startTick))) {
            throw new IllegalArgumentException("Estado de sincronización de brecha inválido");
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
