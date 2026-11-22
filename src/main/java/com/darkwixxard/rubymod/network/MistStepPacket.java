package com.darkwixxard.rubymod.network;

import com.darkwixxard.rubymod.ability.MistStep;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Sent from the client to the server when the player presses the Mist Step key.
 * It carries no data: the server already knows who sent it and where they are looking.
 */
public record MistStepPacket() {

    public static final MistStepPacket INSTANCE = new MistStepPacket();

    // The packet has no content, so encoding/decoding always gives back the same instance
    public static final StreamCodec<RegistryFriendlyByteBuf, MistStepPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    // Runs on the server's main thread
    public static void handle(MistStepPacket packet, CustomPayloadEvent.Context context) {
        var player = context.getSender();
        if (player != null) {
            MistStep.perform(player);
        }
        context.setPacketHandled(true);
    }
}
