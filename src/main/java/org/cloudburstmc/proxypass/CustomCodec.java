package org.cloudburstmc.proxypass;

import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.LevelEventSerializer_v291;
import org.cloudburstmc.protocol.bedrock.codec.v361.serializer.LevelEventGenericSerializer_v361;
import org.cloudburstmc.protocol.bedrock.codec.v575.BedrockCodecHelper_v575;
import org.cloudburstmc.protocol.bedrock.codec.v662.Bedrock_v662;
import org.cloudburstmc.protocol.bedrock.codec.v671.Bedrock_v671;
import org.cloudburstmc.protocol.bedrock.codec.v671.serializer.*;
import org.cloudburstmc.protocol.bedrock.data.EncodingSettings;
import org.cloudburstmc.protocol.bedrock.packet.*;

public class CustomCodec extends Bedrock_v671 {
    public static final BedrockCodec CODEC = Bedrock_v662.CODEC.toBuilder()
            .raknetProtocolVersion(11)
            .protocolVersion(671)
            .minecraftVersion("1.20.80")
            .helper(() -> {
                BedrockCodecHelper_v575 bedrockCodecHelperV575 = new BedrockCodecHelper_v575(ENTITY_DATA, GAME_RULE_TYPES, ITEM_STACK_REQUEST_TYPES, CONTAINER_SLOT_TYPES, PLAYER_ABILITIES, TEXT_PROCESSING_ORIGINS);
                bedrockCodecHelperV575.setEncodingSettings(EncodingSettings.builder()
                        .maxByteArraySize(1024 * 1024 * 1024)
                        .maxListSize(1024 * 1024 * 1024)
                        .maxNetworkNBTSize(1024 * 1024 * 1024)
                        .maxItemNBTSize(1024 * 1024 * 1024)
                        .maxStringLength(1024 * 1024 * 1024)
                        .build());
                return bedrockCodecHelperV575;
            })
            .updateSerializer(LevelEventPacket.class, new LevelEventSerializer_v291(LEVEL_EVENTS))
            .updateSerializer(LevelEventGenericPacket.class, new LevelEventGenericSerializer_v361(LEVEL_EVENTS))
            .updateSerializer(ClientboundDebugRendererPacket.class, ClientboundDebugRendererSerializer_v671.INSTANCE)
            .updateSerializer(CorrectPlayerMovePredictionPacket.class, CorrectPlayerMovePredictionSerializer_v671.INSTANCE)
            .updateSerializer(ResourcePackStackPacket.class, ResourcePackStackSerializer_v671.INSTANCE)
            .updateSerializer(UpdatePlayerGameTypePacket.class, UpdatePlayerGameTypeSerializer_v671.INSTANCE)
            .updateSerializer(StartGamePacket.class, StartGameSerializer_v671.INSTANCE)
            .updateSerializer(CraftingDataPacket.class, CraftingDataSerializer_v671.INSTANCE)
            .deregisterPacket(FilterTextPacket.class) // TODO: check
            // TODO: confirm change in AnimatePacket
            .build();
}
