package com.worldremembers.deardiary.network.payload;

import com.worldremembers.deardiary.DearDiaryMod;
import com.worldremembers.deardiary.data.DiaryEntry;
import com.worldremembers.deardiary.data.DiaryJson;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Notifica al cliente que una investigación de Aestria acaba de desbloquearse. */
public record NewResearchEntryPayload(String entryJson) implements CustomPayload {
    public static final Id<NewResearchEntryPayload> ID = new Id<>(Identifier.of(DearDiaryMod.MOD_ID, "new_research_entry"));
    public static final PacketCodec<RegistryByteBuf, NewResearchEntryPayload> CODEC = PacketCodec.tuple(
            DiaryPayloadCodecs.string(DiaryPayloadCodecs.SHORT_TEXT_LIMIT),
            NewResearchEntryPayload::entryJson,
            NewResearchEntryPayload::new
    );

    public static NewResearchEntryPayload fromEntry(DiaryEntry entry) {
        return new NewResearchEntryPayload(DiaryJson.GSON.toJson(entry));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
