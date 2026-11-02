package dev.umf.autoae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PlannerResources {
    private PlannerResources() {}
    public interface Withdrawal { void refund(Player player); }
    public static Withdrawal take(ServerLevel level,BlockPos pos,PlannerBlockEntity planner,Player player) {
        var grid=planner.getMainNode().isActive()?planner.getMainNode().getGrid():null;
        if(grid==null)return null;
        var storage=grid.getStorageService().getInventory(); var source=IActionSource.ofPlayer(player,planner);var key=AEItemKey.of(Items.OAK_LOG);
        if(storage.extract(key,1,Actionable.MODULATE,source)!=1)return null;
        return p -> { if(storage.insert(key,1,Actionable.MODULATE,source)!=1)p.drop(new ItemStack(Items.OAK_LOG),false); };
    }
}
