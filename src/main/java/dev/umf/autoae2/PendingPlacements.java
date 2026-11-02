package dev.umf.autoae2;

import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Deferred until after the wrench's own use event finishes, so placement cancellation can be refunded. */
@EventBusSubscriber(modid=AutoAE2.MOD_ID)
public final class PendingPlacements {
    private record Request(ServerPlayer player, ServerLevel level, InteractionHand hand, ItemStack tool, BlockPos target) {}
    private static final Queue<Request> QUEUE=new ArrayDeque<>();
    public static void enqueue(ServerPlayer player, ServerLevel level, InteractionHand hand, ItemStack tool, BlockPos target) {
        QUEUE.add(new Request(player,level,hand,tool,target));
    }
    @SubscribeEvent public static void onTick(ServerTickEvent.Post event) {
        while(!QUEUE.isEmpty()) {
            Request request=QUEUE.remove();
            if(request.level.getServer()==event.getServer() && request.player.isAlive() && request.player.level()==request.level
                    && request.player.getItemInHand(request.hand)==request.tool)
                PlacementWrench.place(request.player,request.level,request.hand,request.tool,request.target);
        }
    }
}
