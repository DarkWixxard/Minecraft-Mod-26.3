package com.darkwixxard.rubymod.network;

import com.darkwixxard.rubymod.RubyMod;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetworking {
    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(RubyMod.MODID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();

    private ModNetworking() {
    }

    /** Call once from the mod constructor. */
    public static void register() {
        CHANNEL.play()
                .serverbound()
                .addMain(MistStepPacket.class, MistStepPacket.STREAM_CODEC, MistStepPacket::handle);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }
}
