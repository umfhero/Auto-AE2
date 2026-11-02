package dev.umf.autoae2;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PlacementWrench extends Item {
    private static final String LEGACY = "autoae2_oak_log", DIM = "autoae2_dimension", POS = "autoae2_planner", ID = "autoae2_id";
    public PlacementWrench(Properties properties) { super(properties); }
    private static CompoundTag data(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static boolean legacyPaid(ItemStack stack) { return data(stack).getBooleanOr(LEGACY,false); }
    public static boolean linked(ItemStack stack) { CompoundTag t=data(stack); return t.contains(DIM) && t.contains(POS) && t.contains(ID); }
    public static String blueprintId(ItemStack stack) { return data(stack).getStringOr("autoae2_blueprint", "oak_log"); }
    public static boolean isFarm(ItemStack stack) { return linked(stack) && blueprintId(stack).equals("simple_certus_farm"); }
    public static int blueprintLevel(ItemStack stack) { return data(stack).getIntOr("autoae2_level",BlueprintCatalog.maxLevel(blueprintId(stack))); }
    public static int[] pressCounts(ItemStack stack) { var tag=data(stack);return new int[]{tag.getIntOr("autoae2_press_0",1),tag.getIntOr("autoae2_press_1",1),tag.getIntOr("autoae2_press_2",1),tag.getIntOr("autoae2_press_3",1)}; }
    public static int mainCable(ItemStack stack) { return data(stack).getIntOr("autoae2_main_cable",12); }
    public static int isolatedCable(ItemStack stack) { return data(stack).getIntOr("autoae2_isolated_cable",blueprintId(stack).equals("charger")?14:2); }
    public static BlueprintCatalog.Blueprint blueprint(ItemStack stack) { if(!linked(stack))return null;var bp=blueprintId(stack).equals("processor")?BlueprintCatalog.processorVariant(pressCounts(stack)):BlueprintCatalog.variant(blueprintId(stack),blueprintLevel(stack));return bp==null?null:CableChoices.apply(bp,mainCable(stack),isolatedCable(stack)); }
    public static boolean loaded(ItemStack stack) { return linked(stack) || legacyPaid(stack); }
    public static int offset(ItemStack stack,String name) { return Math.clamp(data(stack).getIntOr("autoae2_"+name,0),-2,2); }
    public static int rotation(ItemStack stack) { return Math.floorMod(data(stack).getIntOr("autoae2_rotation",0),4); }
    public static void link(ItemStack stack, Level level, BlockPos pos, PlannerBlockEntity planner,String blueprintId) {
        planner.setChanged();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(DIM,level.dimension().identifier().toString()); tag.putLong(POS,pos.asLong()); tag.putString(ID,planner.linkId());
            tag.putString("autoae2_blueprint",blueprintId);
            tag.putInt("autoae2_level",planner.level(blueprintId));
            tag.putInt("autoae2_main_cable",planner.mainCable());tag.putInt("autoae2_isolated_cable",planner.isolatedCable());
            for(int i=0;i<4;i++)tag.putInt("autoae2_press_"+i,planner.pressCount(i));
            tag.putInt("autoae2_side",0);tag.putInt("autoae2_height",0);tag.putInt("autoae2_forward",0);tag.putInt("autoae2_rotation",0);
        });
    }
    public static boolean linkedTo(ItemStack stack,Level level,BlockPos pos,PlannerBlockEntity planner) {
        if(!linked(stack))return false;var tag=data(stack);
        return tag.getStringOr(DIM,"").equals(level.dimension().identifier().toString()) && BlockPos.of(tag.getLongOr(POS,0)).equals(pos) && tag.getStringOr(ID,"").equals(planner.linkId());
    }
    private static void syncSelection(ItemStack stack,PlannerBlockEntity planner) {
        String id=planner.selectedBlueprint();int level=planner.level(id);
        if(!id.equals(blueprintId(stack)) || blueprintLevel(stack)!=level || mainCable(stack)!=planner.mainCable() || isolatedCable(stack)!=planner.isolatedCable() || id.equals("processor") && !java.util.Arrays.equals(pressCounts(stack),planner.pressCounts()))
            CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->{tag.putString("autoae2_blueprint",id);tag.putInt("autoae2_level",level);tag.putInt("autoae2_main_cable",planner.mainCable());tag.putInt("autoae2_isolated_cable",planner.isolatedCable());for(int i=0;i<4;i++)tag.putInt("autoae2_press_"+i,planner.pressCount(i));});
    }
    public static void syncFromPlanner(ItemStack stack,ServerLevel level) {
        if(!linked(stack))return;
        var tag=data(stack);var pos=BlockPos.of(tag.getLongOr(POS,0));
        if(tag.getStringOr(DIM,"").equals(level.dimension().identifier().toString()) && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof PlannerBlockEntity planner && linkedTo(stack,level,pos,planner))syncSelection(stack,planner);
    }
    @Override public void inventoryTick(ItemStack stack,ServerLevel level,net.minecraft.world.entity.Entity owner,net.minecraft.world.entity.EquipmentSlot slot) {
        syncFromPlanner(stack,level);
        super.inventoryTick(stack,level,owner,slot);
    }
    public static void adjust(ItemStack stack,int action) {
        if(!linked(stack)) return;
        CustomData.update(DataComponents.CUSTOM_DATA,stack,tag -> {
            switch(action) {
                case 0 -> tag.putInt("autoae2_side",Math.clamp(tag.getIntOr("autoae2_side",0)-1,-2,2));
                case 1 -> tag.putInt("autoae2_side",Math.clamp(tag.getIntOr("autoae2_side",0)+1,-2,2));
                case 2 -> tag.putInt("autoae2_height",Math.clamp(tag.getIntOr("autoae2_height",0)+1,-2,2));
                case 3 -> tag.putInt("autoae2_height",Math.clamp(tag.getIntOr("autoae2_height",0)-1,-2,2));
                case 4 -> tag.putInt("autoae2_forward",Math.clamp(tag.getIntOr("autoae2_forward",0)+1,-2,2));
                case 5 -> tag.putInt("autoae2_forward",Math.clamp(tag.getIntOr("autoae2_forward",0)-1,-2,2));
                case 6 -> tag.putInt("autoae2_rotation",Math.floorMod(tag.getIntOr("autoae2_rotation",0)+1,4));
                case 7 -> { tag.putInt("autoae2_side",0);tag.putInt("autoae2_height",0);tag.putInt("autoae2_forward",0);tag.putInt("autoae2_rotation",0); }
            }
        });
    }
    @Nullable public static BlockPos target(Player player,ItemStack stack) {
        HitResult hit = player.pick(player.blockInteractionRange(),1.0f,false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType()!=HitResult.Type.BLOCK) return null;
        Direction forward=player.getDirection(), right=forward.getClockWise();
        return (isFarm(stack) ? blockHit.getBlockPos() : blockHit.getBlockPos().relative(blockHit.getDirection()))
                .relative(right,offset(stack,"side")).relative(forward,offset(stack,"forward")).above(offset(stack,"height"));
    }
    @Override public Component getName(ItemStack stack) {
        return loaded(stack) ? super.getName(stack).copy().append(blueprint(stack)!=null ? " ["+blueprint(stack).title()+"]" : " [Oak Log test]") : super.getName(stack);
    }
    @Override public boolean isFoil(ItemStack stack) { return linked(stack) || legacyPaid(stack) || super.isFoil(stack); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack,context,display,out,flag);
        if(linked(stack)) {
            CompoundTag t=data(stack); BlockPos p=BlockPos.of(t.getLongOr(POS,0));
            out.accept(Component.literal(blueprint(stack)!=null?"Linked: "+blueprint(stack).title():"Linked: Oak Log test").withStyle(ChatFormatting.AQUA));
            if(blueprint(stack)!=null && blueprintId(stack).equals("processor"))out.accept(Component.literal(java.util.Arrays.stream(pressCounts(stack)).sum()+" presses + assembler").withStyle(ChatFormatting.GRAY));
            else if(blueprint(stack)!=null && BlueprintCatalog.maxLevel(blueprintId(stack))>1)
                out.accept(Component.literal(BlueprintCatalog.levelLabel(blueprintId(stack),blueprintLevel(stack))).withStyle(ChatFormatting.GRAY));
            out.accept(Component.literal("Planner: "+p.getX()+", "+p.getY()+", "+p.getZ()).withStyle(ChatFormatting.GRAY));
            out.accept(Component.literal("Dimension: "+t.getStringOr(DIM,"")).withStyle(ChatFormatting.DARK_GRAY));
            out.accept(Component.literal("Placement controls: ").append(Component.keybind("key.autoae2.placement_controls_v2")).withStyle(ChatFormatting.GRAY));
        } else if(legacyPaid(stack)) out.accept(Component.literal("Contains one previously paid Oak Log").withStyle(ChatFormatting.GOLD));
        else out.accept(Component.literal("Unlinked - assign in an Automation Planner").withStyle(ChatFormatting.GRAY));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer(); var level=context.getLevel(); var stack=context.getItemInHand();
        if(player==null || !loaded(stack)) return InteractionResult.PASS;
        if(level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel server)) return InteractionResult.FAIL;
        syncFromPlanner(stack,server);BlockPos target=target(player,stack);
        if(target==null)return InteractionResult.FAIL;
        PendingPlacements.enqueue(serverPlayer,server,context.getHand(),stack,target);
        return InteractionResult.SUCCESS;
    }
    public static void place(ServerPlayer player, ServerLevel level, InteractionHand hand, ItemStack stack, BlockPos target) {
        if (blueprint(stack)!=null) {
            CompoundTag tag=data(stack);BlockPos plannerPos=BlockPos.of(tag.getLongOr(POS,0));
            if(!level.dimension().identifier().toString().equals(tag.getStringOr(DIM,"")) || !level.hasChunkAt(plannerPos)
                    || !(level.getBlockEntity(plannerPos) instanceof PlannerBlockEntity planner) || !planner.linkId().equals(tag.getStringOr(ID,""))) {
                PlacementFeedback.send(player,"Linked planner unavailable or unloaded");return;
            }
            if(!level.mayInteract(player,plannerPos) || !player.mayInteract(level,plannerPos)) { PlacementFeedback.send(player,"Cannot access linked planner");return; }
            String before=blueprintId(stack);int beforeLevel=blueprintLevel(stack),beforeMain=mainCable(stack),beforeIsolated=isolatedCable(stack);int[] beforePress=pressCounts(stack);syncSelection(stack,planner);
            if(!before.equals(blueprintId(stack)) || beforeLevel!=blueprintLevel(stack) || beforeMain!=mainCable(stack) || beforeIsolated!=isolatedCable(stack) || before.equals("processor") && !java.util.Arrays.equals(beforePress,pressCounts(stack))) { PlacementFeedback.send(player,"Blueprint changed; try placing again");return; }
            if((isFarm(stack) || blueprintId(stack).equals("charger")) && (mainCable(stack)==0 || isolatedCable(stack)==0 || mainCable(stack)==isolatedCable(stack))) { PlacementFeedback.send(player,"Open planner to update isolated cable colours");return; }
            if(isFarm(stack))FarmBuilder.place(player,level,target,rotation(stack),planner,blueprint(stack));
            else ModuleBuilder.place(player,level,target,rotation(stack),planner,blueprint(stack));
            return;
        }
        if(!level.hasChunkAt(target) || !level.getBlockState(target).canBeReplaced() || !level.mayInteract(player,target) || !player.mayInteract(level,target)) {
            PlacementFeedback.send(player,"Cannot place here"); return;
        }
        PlannerResources.Withdrawal withdrawal=null;
        if(!legacyPaid(stack)) {
            CompoundTag tag=data(stack); BlockPos pos=BlockPos.of(tag.getLongOr(POS,0));
            if(!level.dimension().identifier().toString().equals(tag.getStringOr(DIM,"")) || !level.hasChunkAt(pos)
                    || !(level.getBlockEntity(pos) instanceof PlannerBlockEntity planner) || !planner.linkId().equals(tag.getStringOr(ID,""))) {
                PlacementFeedback.send(player,"Linked planner unavailable or unloaded"); return;
            }
            if(!level.mayInteract(player,pos) || !player.mayInteract(level,pos)) { PlacementFeedback.send(player,"Cannot access linked planner"); return; }
            withdrawal=PlannerResources.take(level,pos,planner,player);
            if(withdrawal==null) { PlacementFeedback.send(player,"Item requirements not met in ME storage"); return; }
        }
        ItemStack material=new ItemStack(Items.OAK_LOG);
        BlockHitResult synthetic=new BlockHitResult(Vec3.atCenterOf(target),Direction.UP,target,false);
        InteractionResult result=material.useOn(new UseOnContext(level,player,hand,material,synthetic));
        if(!result.consumesAction()) { if(withdrawal!=null) withdrawal.refund(player); return; }
        if(legacyPaid(stack)) CustomData.update(DataComponents.CUSTOM_DATA,stack,tag -> tag.remove(LEGACY));
    }
}
