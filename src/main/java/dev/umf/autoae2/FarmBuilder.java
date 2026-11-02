package dev.umf.autoae2;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.parts.storagebus.StorageBusPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import java.util.ArrayList;
import java.util.List;

/** Builds only into empty cells around an existing budding block; never replaces the budding block. */
public final class FarmBuilder {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private FarmBuilder() {}
    public static boolean isBudding(BlockState state) {
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return id.equals("ae2:flawless_budding_quartz")||id.equals("ae2:flawed_budding_quartz")||id.equals("ae2:chipped_budding_quartz")||id.equals("ae2:damaged_budding_quartz");
    }
    private static Item item(String id) { return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)); }
    private static BlockPos pos(BlockPos anchor,int x,int y,int z,int rotation) { return anchor.offset(FarmLayout.rotate(x,y,z,rotation)); }
    private static boolean addPart(ServerLevel level,BlockPos pos,Direction side,String id,ServerPlayer player) {
        var host=PartHelper.getOrPlacePartHost(level,pos,false,player);
        if(host==null || !(item(id) instanceof IPartItem<?> part)) return false;
        if(!host.canAddPart(item(id).getDefaultInstance(),side)) return false;
        if(host.addPart(part,side,player)==null)return false;
        host.markForSave();host.markForUpdate();return true;
    }
    private static boolean configure(ServerLevel level,BlockPos pos,Direction side,int priority) {
        if(!(PartHelper.getPart(level,pos,side) instanceof StorageBusPart bus))return false;
        bus.getConfig().setStack(0,new GenericStack(AEItemKey.of(item("ae2:certus_quartz_crystal")),1));
        bus.getConfigManager().putSetting(Settings.FILTER_ON_EXTRACT,YesNo.YES);
        bus.setPriority(priority);return true;
    }
    private record Taken(AEItemKey key,long count) {}
    public static void place(ServerPlayer player,ServerLevel level,BlockPos anchor,int rotation,PlannerBlockEntity planner,BlueprintCatalog.Blueprint blueprint) {
        if(!level.hasChunkAt(anchor)||!isBudding(level.getBlockState(anchor))) { PlacementFeedback.send(player,"Aim at a budding Certus Quartz block");return; }
        var node=planner.getMainNode();var grid=node.isActive()?node.getGrid():null;
        if(grid==null){PlacementFeedback.send(player,"Planner must be connected to an ME network");return;}
        var storage=grid.getStorageService().getInventory();var source=IActionSource.ofPlayer(player,planner);
        for(var req:blueprint.requirements()) if(storage.extract(AEItemKey.of(req.item()),req.count(),Actionable.SIMULATE,source)!=req.count()) {
            PlacementFeedback.send(player,"Item requirements not met in ME storage");return;
        }
        List<BlockSnapshot> before=new ArrayList<>();
        for(var part:FarmLayout.PARTS) {
            BlockPos p=part.at(anchor,rotation);
            if(!level.hasChunkAt(p)||!level.isInWorldBounds(p)||!level.getBlockState(p).isAir()||!level.mayInteract(player,p)||!player.mayInteract(level,p)
                    ||!level.isUnobstructed(Blocks.STONE.defaultBlockState(),p,CollisionContext.placementContext(player))) {
                PlacementFeedback.send(player,"Farm needs clear, accessible space around the bud");return;
            }
            before.add(BlockSnapshot.create(level.dimension(),level,p));
        }
        List<Taken> taken=new ArrayList<>();boolean success=false;
        try {
            for(var req:blueprint.requirements()) {
                AEItemKey key=AEItemKey.of(req.item());long n=storage.extract(key,req.count(),Actionable.MODULATE,source);
                if(n>0)taken.add(new Taken(key,n));
                if(n!=req.count())throw new IllegalStateException("Materials changed while extracting");
            }
            for(var part:FarmLayout.PARTS) {
                BlockPos p=part.at(anchor,rotation);
                switch(part.kind()) {
                    case BLUE_CABLE -> { if(!addPart(level,p,null,blueprint.requirements()[0].id(),player))throw new IllegalStateException("Blue cable"); }
                    case ORANGE_CABLE -> { if(!addPart(level,p,null,blueprint.requirements()[1].id(),player))throw new IllegalStateException("Orange cable"); }
                    case BARREL -> { if(!level.setBlockAndUpdate(p,Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP)))throw new IllegalStateException("Barrel"); }
                    case ACCELERATOR -> {
                        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("ae2:growth_accelerator"));
                        var state=block.defaultBlockState().setValue(BlockStateProperties.FACING,FarmLayout.rotate(part.facing(),rotation));
                        if(!level.setBlockAndUpdate(p,state))throw new IllegalStateException("Accelerator");
                        if(level.getBlockEntity(p) instanceof appeng.block.IOwnerAwareBlockEntity own)own.setOwner(player);
                    }
                }
            }
            BlockPos outer=pos(anchor,-2,1,-1,rotation),inner=pos(anchor,-1,1,0,rotation),fiber=pos(anchor,-1,1,-1,rotation);
            Direction outerSide=FarmLayout.rotate(Direction.SOUTH,rotation),innerSide=FarmLayout.rotate(Direction.WEST,rotation),planeSide=FarmLayout.rotate(Direction.EAST,rotation);
            if(!addPart(level,outer,outerSide,"ae2:storage_bus",player)||!addPart(level,inner,innerSide,"ae2:storage_bus",player)
                    ||!addPart(level,inner,planeSide,"ae2:annihilation_plane",player)||!addPart(level,fiber,Direction.DOWN,"ae2:quartz_fiber",player))throw new IllegalStateException("Cable parts");
            if(!configure(level,outer,outerSide,1000)||!configure(level,inner,innerSide,0))throw new IllegalStateException("Bus configuration");
            if(EventHooks.onMultiBlockPlace(player,before,Direction.UP))throw new IllegalStateException("Placement denied");
            success=true;
            player.sendSystemMessage(Component.literal("Certus farm placed. Connect the main cable to your ME network. Degrading budding quartz needs replacement."));
        } catch(RuntimeException ex) {
            LOGGER.warn("Certus farm placement failed; restoring blocks and materials",ex);
            PlacementFeedback.send(player,"Farm placement failed; materials returned.");
        } finally {
            if(!success) {
                for(int i=before.size()-1;i>=0;i--)before.get(i).restore();
                for(var t:taken) {
                    long returned=storage.insert(t.key,t.count,Actionable.MODULATE,source);
                    long left=t.count-returned;
                    while(left>0) { int amount=(int)Math.min(left,64);player.drop(t.key.toStack(amount),false);left-=amount; }
                }
            }
        }
    }
}
