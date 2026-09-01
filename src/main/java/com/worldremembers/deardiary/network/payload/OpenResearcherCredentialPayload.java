package com.worldremembers.deardiary.network.payload;

import com.worldremembers.deardiary.DearDiaryMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenResearcherCredentialPayload(String playerName) implements CustomPayload {
    public static final Id<OpenResearcherCredentialPayload> ID = new Id<>(Identifier.of(DearDiaryMod.MOD_ID, "open_researcher_credential"));
    public static final PacketCodec<RegistryByteBuf, OpenResearcherCredentialPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.cast(),
            OpenResearcherCredentialPayload::playerName,
            OpenResearcherCredentialPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
