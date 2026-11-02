package dev.umf.autoae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PlannerMenu extends AbstractContainerMenu {
    private final SimpleContainer wrench=new SimpleContainer(1);
    private final ContainerLevelAccess access;
    private final DataSlot source=DataSlot.standalone();
    private final DataSlot selected=DataSlot.standalone();
    private final DataSlot level=DataSlot.standalone();
    private final DataSlot linked=DataSlot.standalone();
    private final DataSlot craftStatus=DataSlot.standalone();
    private final DataSlot mainCable=DataSlot.standalone(),isolatedCable=DataSlot.standalone();
    private int colorCheck;
    private final DataSlot[] quantities=new DataSlot[12];
    private final DataSlot[] craftable=new DataSlot[12];
    private final DataSlot[] pressCounts=new DataSlot[4], pressAvailable=new DataSlot[4];
    private final Player owner;
    public PlannerMenu(int id,Inventory inventory) { this(id,inventory,ContainerLevelAccess.NULL); }
    public PlannerMenu(int id,Inventory inventory,ContainerLevelAccess access) {
        super(AutoAE2.PLANNER_MENU.get(),id); this.access=access; owner=inventory.player;
        addSlot(new Slot(wrench,0,260,118) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(AutoAE2.WRENCH.get()); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addStandardInventorySlots(inventory,57,160);
        addDataSlot(source); addDataSlot(selected);addDataSlot(level);addDataSlot(linked);addDataSlot(craftStatus);addDataSlot(mainCable);addDataSlot(isolatedCable);
        for(int i=0;i<quantities.length;i++) { quantities[i]=DataSlot.standalone(); addDataSlot(quantities[i]); }
        for(int i=0;i<craftable.length;i++) { craftable[i]=DataSlot.standalone(); addDataSlot(craftable[i]); }
        for(int i=0;i<4;i++) { pressCounts[i]=DataSlot.standalone();addDataSlot(pressCounts[i]);pressAvailable[i]=DataSlot.standalone();addDataSlot(pressAvailable[i]); }
    }
    public int pressCount(int i) { return pressCounts[i].get(); }
    public int pressAvailable(int i) { return pressAvailable[i].get(); }
    public int[] pressCounts() { return new int[]{pressCount(0),pressCount(1),pressCount(2),pressCount(3)}; }
    public int pressTotal() { return java.util.Arrays.stream(pressCounts()).sum(); }
    public BlueprintCatalog.Blueprint baseBlueprint() { var id=BlueprintCatalog.ALL.get(Math.clamp(selected.get(),0,BlueprintCatalog.ALL.size()-1)).id();return id.equals("processor")?BlueprintCatalog.processorVariant(pressCounts()):BlueprintCatalog.variant(id,level()); }
    public BlueprintCatalog.Blueprint blueprint() { return CableChoices.apply(baseBlueprint(),mainCable.get(),isolatedCable.get()); }
    public int cableHash() { return mainCable.get()*31+isolatedCable.get(); }
    public int level() { return Math.max(1,level.get()); }
    public boolean isLinked() { return linked.get()!=0; }
    public boolean isCraftable(int index) { return craftable[index].get()!=0; }
    public int craftStatus() { return craftStatus.get(); }
    public int selected() { return selected.get(); }
    public int source() { return source.get(); }
    public int quantity(int index) { return quantities[index].get(); }
    public int totalAvailable() { int n=0;for(var q:quantities)n+=q.get();return n; }
    public boolean available() { return totalAvailable()==blueprint().total(); }
    public boolean hasWrench() { return getSlot(0).hasItem(); }
    @Override public boolean stillValid(Player player) { return stillValid(access,player,AutoAE2.PLANNER.get()); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(index==0) { if(!moveItemStackTo(stack,1,slots.size(),true))return ItemStack.EMPTY; }
        else if(!stack.is(AutoAE2.WRENCH.get()) || !moveItemStackTo(stack,0,1,false)) return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
    @Override public void removed(Player player) { super.removed(player);if(!player.level().isClientSide())clearContainer(player,wrench); }
    private void refresh(Level level,BlockPos pos) {
        var planner=level.getBlockEntity(pos) instanceof PlannerBlockEntity p?p:null;
        if(planner!=null) {
            int i=0;for(;i<BlueprintCatalog.ALL.size();i++)if(BlueprintCatalog.ALL.get(i).id().equals(planner.selectedBlueprint()))break;
            selected.set(Math.min(i,BlueprintCatalog.ALL.size()-1));this.level.set(planner.level(planner.selectedBlueprint()));
            for(int n=0;n<4;n++)pressCounts[n].set(planner.pressCount(n));
            boolean found=PlacementWrench.linkedTo(wrench.getItem(0),level,pos,planner);
            for(int slot=0;slot<owner.getInventory().getContainerSize() && !found;slot++)found=PlacementWrench.linkedTo(owner.getInventory().getItem(slot),level,pos,planner);
            linked.set(found?1:0);
        }
        var grid=planner!=null && planner.getMainNode().isActive()?planner.getMainNode().getGrid():null;
        source.set(grid==null?0:1);
        if(planner!=null) { if(colorCheck++%20==0){var colors=CableChoices.select(baseBlueprint(),grid,owner,planner);planner.setCables(colors[0],colors[1]);} mainCable.set(planner.mainCable());isolatedCable.set(planner.isolatedCable()); }
        for(int n=0;n<4;n++) {
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(BlueprintCatalog.PRESS_IDS[n]));
            var key=AEItemKey.of(item);long found=grid==null||key==null?0:grid.getStorageService().getInventory().extract(key,64,Actionable.SIMULATE,IActionSource.ofPlayer(owner,planner));
            pressAvailable[n].set((int)found);
        }
        for(int i=0;i<quantities.length;i++) {
            if(i>=blueprint().requirements().length) { quantities[i].set(0);craftable[i].set(0);continue; }
            var req=blueprint().requirements()[i];
            var key=AEItemKey.of(req.item());
            long found=grid==null||key==null?0:grid.getStorageService().getInventory().extract(key,req.count(),Actionable.SIMULATE,IActionSource.ofPlayer(owner,planner));
            quantities[i].set((int)found);
            craftable[i].set(grid!=null && key!=null && grid.getCraftingService().isCraftable(key)?1:0);
        }
    }
    @Override public void broadcastChanges() { if(!owner.level().isClientSide())access.execute(this::refresh);super.broadcastChanges(); }
    @Override public boolean clickMenuButton(Player player,int buttonId) {
        if(player.level().isClientSide() || !stillValid(player)) return false;
        if(buttonId>=100 && buttonId<100+BlueprintCatalog.ALL.size()) {
            craftStatus.set(0);colorCheck=0;access.execute((world,pos)->{if(world.getBlockEntity(pos) instanceof PlannerBlockEntity planner)planner.select(BlueprintCatalog.ALL.get(buttonId-100).id());refresh(world,pos);});return true;
        }
        if(buttonId>=200 && buttonId<=456) {
            craftStatus.set(0);colorCheck=0;access.execute((world,pos)->{if(world.getBlockEntity(pos) instanceof PlannerBlockEntity planner)planner.setLevel(planner.selectedBlueprint(),buttonId-199);refresh(world,pos);});return true;
        }
        if(buttonId>=500 && buttonId<508) {
            craftStatus.set(0);colorCheck=0;access.execute((world,pos)->{if(world.getBlockEntity(pos) instanceof PlannerBlockEntity planner && planner.selectedBlueprint().equals("processor"))planner.adjustPress((buttonId-500)/2,(buttonId%2==0)?-1:1);refresh(world,pos);});return true;
        }
        if(buttonId==1 && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            return access.evaluate((world,pos)->{
                refresh(world,pos);
                if(!(world.getBlockEntity(pos) instanceof PlannerBlockEntity planner) || source.get()==0){craftStatus.set(1);return false;}
                var entries=new java.util.ArrayList<appeng.helpers.ICraftingGridMenu.AutoCraftEntry>();
                for(int i=0;i<blueprint().requirements().length;i++) {
                    var req=blueprint().requirements()[i];int missing=req.count()-quantity(i);
                    if(missing>0 && isCraftable(i))entries.add(new appeng.helpers.ICraftingGridMenu.AutoCraftEntry(AEItemKey.of(req.item()),java.util.Collections.nCopies(missing,0)));
                }
                if(entries.isEmpty()){craftStatus.set(available()?2:3);return false;}
                appeng.menu.me.crafting.CraftConfirmMenu.openWithCraftingList(planner,serverPlayer,appeng.menu.locator.MenuLocators.forBlockEntity(planner),entries);
                return true;
            },false);
        }
        if(buttonId!=0)return false;
        ItemStack tool=wrench.getItem(0);
        if(!tool.is(AutoAE2.WRENCH.get()) || PlacementWrench.legacyPaid(tool))return false;
        return access.evaluate((level,pos) -> {
            refresh(level,pos);
            if(!(level.getBlockEntity(pos) instanceof PlannerBlockEntity planner))return false;
            PlacementWrench.link(tool,level,pos,planner,blueprint().id());wrench.setChanged();return true;
        },false);
    }
}
