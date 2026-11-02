package dev.umf.autoae2;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AdjustPlacement(int action) implements CustomPacketPayload {
    public static final Type<AdjustPlacement> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AutoAE2.MOD_ID,"adjust_placement"));
    public static final StreamCodec<ByteBuf,AdjustPlacement> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,AdjustPlacement::action,AdjustPlacement::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(AdjustPlacement msg, IPayloadContext context) {
        if(msg.action<0 || msg.action>8) return;
        ItemStack held=context.player().getMainHandItem();
        if (!held.is(AutoAE2.WRENCH.get()) || !PlacementWrench.linked(held)) return;
        if (msg.action==8 && context.player() instanceof net.minecraft.server.level.ServerPlayer player
                && player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            PlacementWrench.syncFromPlanner(held,level);
            var target=PlacementWrench.target(player,held);
            if(target!=null) PendingPlacements.enqueue(player,level,net.minecraft.world.InteractionHand.MAIN_HAND,held,target);
            else PlacementFeedback.send(player,PlacementWrench.isFarm(held)?"Aim at a budding Certus Quartz block":"Aim at a block to place the blueprint");
        } else PlacementWrench.adjust(held,msg.action);
    }
}
