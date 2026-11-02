package dev.umf.autoae2;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** A placement result displayed by the client without hiding it behind the controls. */
public record PlacementFeedback(String message) implements CustomPacketPayload {
    public static final Type<PlacementFeedback> TYPE=new Type<>(Identifier.fromNamespaceAndPath(AutoAE2.MOD_ID,"placement_feedback"));
    public static final StreamCodec<ByteBuf,PlacementFeedback> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,PlacementFeedback::message,PlacementFeedback::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void send(ServerPlayer player,String message) { PacketDistributor.sendToPlayer(player,new PlacementFeedback(message)); }
}
