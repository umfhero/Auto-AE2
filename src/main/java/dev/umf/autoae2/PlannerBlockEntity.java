package dev.umf.autoae2;

import appeng.api.networking.IGridNodeListener;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.UUID;

public class PlannerBlockEntity extends AENetworkedBlockEntity implements appeng.api.storage.ISubMenuHost {
    private String linkId = UUID.randomUUID().toString();
    private String selectedBlueprint = "simple_certus_farm";
    private int controllerLevel=127, cubeLevel=8, assemblerLevel=10, cpuLevel=5, processorLevel=5, furnaceLevel=8;
    private final int[] pressCounts={1,1,1,1};
    private int mainCable=12, isolatedCable=2;
    public int mainCable() { return mainCable; }
    public int isolatedCable() { return isolatedCable; }
    public void setCables(int main,int isolated) { if(mainCable!=main || isolatedCable!=isolated){mainCable=main;isolatedCable=isolated;setChanged();} }
    public int pressCount(int index) { return Math.clamp(pressCounts[index],0,8); }
    public int[] pressCounts() { return new int[]{pressCount(0),pressCount(1),pressCount(2),pressCount(3)}; }
    public void adjustPress(int index,int delta) { if(index>=0 && index<4){pressCounts[index]=Math.clamp(pressCounts[index]+delta,0,8);setChanged();} }
    public String linkId() { return linkId; }
    @Override public net.minecraft.world.item.ItemStack getMainMenuIcon() { return AutoAE2.PLANNER_ITEM.get().getDefaultInstance(); }
    @Override public void returnToMainMenu(net.minecraft.world.entity.player.Player player,appeng.menu.ISubMenu subMenu) {
        var world=getLevel();if(world==null || world.isClientSide() || isRemoved())return;
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->new PlannerMenu(id,inventory,net.minecraft.world.inventory.ContainerLevelAccess.create(world,getBlockPos())),net.minecraft.network.chat.Component.literal("Automation Planner")));
    }
    public String selectedBlueprint() { return selectedBlueprint; }
    public int level(String id) { return BlueprintCatalog.clampLevel(id,switch(id) { case "controller" -> controllerLevel; case "controller_cube" -> cubeLevel; case "assembler_module" -> assemblerLevel; case "crafting_cpu" -> cpuLevel; case "processor" -> processorLevel; case "furnace" -> furnaceLevel; default -> 1; }); }
    public void select(String id) { if(BlueprintCatalog.byId(id)!=null) { selectedBlueprint=id;setChanged(); } }
    public void setLevel(String id,int level) {
        level=BlueprintCatalog.clampLevel(id,level);
        switch(id) { case "controller" -> controllerLevel=level; case "controller_cube" -> cubeLevel=level; case "assembler_module" -> assemblerLevel=level; case "crafting_cpu" -> cpuLevel=level; case "processor" -> processorLevel=level; case "furnace" -> furnaceLevel=level; default -> { return; } }
        setChanged();
    }
    @Override public void loadTag(ValueInput input) {
        super.loadTag(input);mainCable=input.getIntOr("main_cable",12);isolatedCable=input.getIntOr("isolated_cable",2);linkId=input.getStringOr("autoae2_link_id",linkId);
        var id=input.getStringOr("selected_blueprint","simple_certus_farm");selectedBlueprint=BlueprintCatalog.byId(id)!=null?id:"simple_certus_farm";
        controllerLevel=input.getIntOr("controller_level",127);cubeLevel=input.getIntOr("cube_level",8);assemblerLevel=input.getIntOr("assembler_level",10);cpuLevel=input.getIntOr("cpu_level",5);processorLevel=input.getIntOr("processor_level",5);furnaceLevel=input.getIntOr("furnace_level",8);
        for(int i=0;i<4;i++)pressCounts[i]=Math.clamp(input.getIntOr("processor_press_"+i,processorLevel>i?1:0),0,8);
    }
    @Override public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);output.putInt("main_cable",mainCable);output.putInt("isolated_cable",isolatedCable);output.putString("autoae2_link_id",linkId);output.putString("selected_blueprint",selectedBlueprint);
        output.putInt("controller_level",controllerLevel);output.putInt("cube_level",cubeLevel);output.putInt("assembler_level",assemblerLevel);output.putInt("cpu_level",cpuLevel);output.putInt("processor_level",processorLevel);output.putInt("furnace_level",furnaceLevel);
        for(int i=0;i<4;i++)output.putInt("processor_press_"+i,pressCounts[i]);
    }
    public PlannerBlockEntity(BlockPos pos, BlockState state) {
        super(AutoAE2.PLANNER_ENTITY.get(), pos, state);
        getMainNode().setIdlePowerUsage(1.0);
    }
    private void updateConnectionIndicator() {
        var world = getLevel();
        if (world == null || world.isClientSide()) return;
        var state = getBlockState();
        if (state.hasProperty(PlannerBlock.CONNECTED)) {
            boolean connected = getMainNode().isActive();
            if (state.getValue(PlannerBlock.CONNECTED) != connected)
                world.setBlockAndUpdate(getBlockPos(), state.setValue(PlannerBlock.CONNECTED, connected));
        }
    }
    @Override public void onMainNodeStateChanged(IGridNodeListener.State reason) { updateConnectionIndicator(); }
    @Override public void onReady() { super.onReady(); updateConnectionIndicator(); }
}
