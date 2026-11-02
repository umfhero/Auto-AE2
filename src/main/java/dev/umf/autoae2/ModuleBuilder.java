package dev.umf.autoae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import java.util.ArrayList;

/** Preflights every cell and pays atomically from the linked ME network. */
public final class ModuleBuilder {
    private record Taken(AEItemKey key,long count) {}
    private ModuleBuilder() {}
    private static void addPart(ServerLevel level,BlockPos pos,Direction side,String id,ServerPlayer player) {
        var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        var host=appeng.api.parts.PartHelper.getOrPlacePartHost(level,pos,false,player);
        if(host==null || !(item instanceof appeng.api.parts.IPartItem<?> part) || !host.canAddPart(item.getDefaultInstance(),side) || host.addPart(part,side,player)==null)
            throw new IllegalStateException("Could not place part "+id);
        host.markForSave();host.markForUpdate();
    }
    public static void place(ServerPlayer player,ServerLevel level,BlockPos origin,int rotation,PlannerBlockEntity planner,BlueprintCatalog.Blueprint blueprint) {
        var node=planner.getMainNode();var grid=node.isActive()?node.getGrid():null;
        if(grid==null){PlacementFeedback.send(player,"Planner must be connected to an ME network");return;}
        var storage=grid.getStorageService().getInventory();var source=IActionSource.ofPlayer(player,planner);
        var snapshots=new ArrayList<BlockSnapshot>();
        for(var cell:blueprint.cells()) {
            var p=cell.at(origin,rotation);
            if(!level.hasChunkAt(p)||!level.isInWorldBounds(p)||!level.getBlockState(p).isAir()||!level.mayInteract(player,p)||!player.mayInteract(level,p)||!level.isUnobstructed(Blocks.STONE.defaultBlockState(),p,CollisionContext.placementContext(player))) {
                PlacementFeedback.send(player,"Blueprint needs clear, accessible space");return;
            }
            snapshots.add(BlockSnapshot.create(level.dimension(),level,p));
        }
        if(blueprint.id().equals("controller") || blueprint.id().equals("controller_cube")) {
            var footprint=blueprint.cells().stream().map(cell->cell.at(origin,rotation)).collect(java.util.stream.Collectors.toSet());
            for(var cell:blueprint.cells())for(var side:Direction.values()) {
                var adjacent=cell.at(origin,rotation).relative(side);
                if(footprint.contains(adjacent))continue;
                if(!level.hasChunkAt(adjacent) || appeng.api.networking.GridHelper.getNodeHost(level,adjacent)!=null) {
                    PlacementFeedback.send(player,"Place controller separately, then connect it safely");return;
                }
            }
        }
        if(blueprint.id().equals("crafting_cpu")) {
            for(var cell:blueprint.cells())for(var side:Direction.values()) {
                var adjacent=cell.at(origin,rotation).relative(side);
                if(!level.hasChunkAt(adjacent)) { PlacementFeedback.send(player,"CPU neighbours must be loaded");return; }
                String id=BuiltInRegistries.BLOCK.getKey(level.getBlockState(adjacent).getBlock()).toString();
                if(id.startsWith("ae2:") && (id.contains("crafting_storage")||id.equals("ae2:crafting_unit")||id.equals("ae2:crafting_accelerator")||id.equals("ae2:crafting_monitor"))) {
                    PlacementFeedback.send(player,"Leave space between this CPU and other CPUs");return;
                }
            }
        }
        var patterns=blueprint.id().equals("processor")?ProcessorPatterns.plan(blueprint.cells()):java.util.List.<ProcessorPatterns.Spec>of();
        if(patterns.stream().anyMatch(p->!ProcessorPatterns.matchesRecipe(level,p))) { PlacementFeedback.send(player,"Processor recipes changed; automatic patterns unavailable");return; }
        for(var req:blueprint.requirements()) {
            var key=AEItemKey.of(req.item());
            if(key==null || storage.extract(key,req.count(),Actionable.SIMULATE,source)!=req.count()) { PlacementFeedback.send(player,"Item requirements not met in ME storage");return; }
        }
        var taken=new ArrayList<Taken>();boolean success=false;
        try {
            for(var req:blueprint.requirements()) {
                var key=AEItemKey.of(req.item());long count=storage.extract(key,req.count(),Actionable.MODULATE,source);
                if(count>0)taken.add(new Taken(key,count));
                if(count!=req.count())throw new IllegalStateException("Materials changed during placement");
            }
            for(var cell:blueprint.cells()) {
                var pos=cell.at(origin,rotation);
                if(cell.cable()) { addPart(level,pos,null,cell.block(),player);continue; }
                var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(cell.block()));
                var state=block.defaultBlockState();
                if(blueprint.id().equals("processor") && cell.block().equals("ae2:inscriber") && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
                    var neighbour=blueprint.cells().stream().filter(other->other.block().equals("ae2:pattern_provider") && Math.abs(other.x()-cell.x())+Math.abs(other.y()-cell.y())+Math.abs(other.z()-cell.z())==1).findFirst().orElseThrow();
                    var away=Direction.getNearest(cell.x()-neighbour.x(),cell.y()-neighbour.y(),cell.z()-neighbour.z(),Direction.NORTH);
                    state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,away);
                }
                var turn=switch(Math.floorMod(rotation,4)) { case 1 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;case 2 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;case 3 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;default -> net.minecraft.world.level.block.Rotation.NONE; };
                if(block==Blocks.AIR || !level.setBlockAndUpdate(pos,state.rotate(turn)))throw new IllegalStateException("Block placement failed: "+cell.block());
                if(level.getBlockEntity(pos) instanceof appeng.block.IOwnerAwareBlockEntity owner)owner.setOwner(player);
            }
            for(var cell:blueprint.cells())for(var part:cell.parts())
                addPart(level,cell.at(origin,rotation),FarmLayout.rotate(part.side(),rotation),part.id(),player);
            if(blueprint.id().equals("furnace"))for(var cell:blueprint.cells())for(var part:cell.parts())if(part.id().equals("ae2:export_bus")) {
                var bus=appeng.api.parts.PartHelper.getPart(level,cell.at(origin,rotation),FarmLayout.rotate(part.side(),rotation));
                if(!(bus instanceof appeng.parts.automation.ExportBusPart export))throw new IllegalStateException("Expected fuel export bus");
                export.getConfig().setStack(0,new appeng.api.stacks.GenericStack(AEItemKey.of(net.minecraft.world.item.Items.COAL),1));
            }
            for(var cell:blueprint.cells())if(cell.inserted()!=null) {
                var pos=cell.at(origin,rotation);
                if(!(level.getBlockEntity(pos) instanceof appeng.blockentity.misc.InscriberBlockEntity inscriber))throw new IllegalStateException("Expected inscriber");
                var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(cell.inserted()));
                if(item==net.minecraft.world.item.Items.AIR)throw new IllegalStateException("Missing press "+cell.inserted());
                inscriber.getInternalInventory().setItemDirect(0,item.getDefaultInstance());inscriber.setChanged();
            }
            if(blueprint.id().equals("processor"))for(var cell:blueprint.cells())if(cell.block().equals("ae2:inscriber")) {
                if(!(level.getBlockEntity(cell.at(origin,rotation)) instanceof appeng.blockentity.misc.InscriberBlockEntity inscriber))throw new IllegalStateException("Expected inscriber");
                inscriber.getConfigManager().putSetting(appeng.api.config.Settings.AUTO_EXPORT,appeng.api.config.YesNo.YES);
                inscriber.getConfigManager().putSetting(appeng.api.config.Settings.INSCRIBER_INPUT_CAPACITY,appeng.api.config.InscriberInputCapacity.FOUR);
            }
            var patternSlots=new java.util.HashMap<net.minecraft.core.BlockPos,Integer>();
            for(var spec:patterns) {
                var relative=spec.provider();var pos=origin.offset(FarmLayout.rotate(relative.getX(),relative.getY(),relative.getZ(),rotation));
                if(!(level.getBlockEntity(pos) instanceof appeng.blockentity.crafting.PatternProviderBlockEntity provider))throw new IllegalStateException("Expected pattern provider");
                var inv=provider.getLogic().getPatternInv();int slot=patternSlots.getOrDefault(pos,0);
                if(slot>=inv.size())throw new IllegalStateException("Not enough pattern slots");
                inv.setItemDirect(slot,ProcessorPatterns.encode(spec));patternSlots.put(pos,slot+1);
            }
            if(EventHooks.onMultiBlockPlace(player,snapshots,Direction.UP))throw new IllegalStateException("Placement denied");
            success=true;PlacementFeedback.send(player,"Placed "+blueprint.title()+". "+blueprint.hint());
        } catch(RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().warn("Blueprint placement failed",ex);
            PlacementFeedback.send(player,"Placement failed; materials returned");
        } finally {
            if(!success) {
                for(int i=snapshots.size()-1;i>=0;i--)snapshots.get(i).restore();
                for(var item:taken) { long left=item.count()-storage.insert(item.key(),item.count(),Actionable.MODULATE,source);while(left>0){int n=(int)Math.min(left,64);player.drop(item.key().toStack(n),false);left-=n;} }
            }
        }
    }
}
