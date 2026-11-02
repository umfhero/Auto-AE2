package dev.umf.autoae2;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import appeng.api.AECapabilities;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

@Mod(AutoAE2.MOD_ID)
public class AutoAE2 {
    public static final String MOD_ID = "autoae2";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredBlock<PlannerBlock> PLANNER = BLOCKS.registerBlock("automation_planner", PlannerBlock::new,
            p -> p.strength(2.5f));
    public static final DeferredItem<BlockItem> PLANNER_ITEM = ITEMS.registerSimpleBlockItem("automation_planner", PLANNER);
    public static final DeferredItem<PlacementWrench> WRENCH = ITEMS.registerItem("placement_wrench", PlacementWrench::new,
            p -> p.stacksTo(1));
    public static final Supplier<BlockEntityType<PlannerBlockEntity>> PLANNER_ENTITY = ENTITIES.register("automation_planner",
            () -> new BlockEntityType<>(PlannerBlockEntity::new, false, PLANNER.get()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AUTO_AE2_TAB = TABS.register("auto_ae2", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.autoae2"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> PLANNER_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(PLANNER_ITEM.get());
                        output.accept(WRENCH.get());
                    }).build());
    public static final Supplier<MenuType<PlannerMenu>> PLANNER_MENU = MENUS.register("planner",
            () -> new MenuType<>(PlannerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public AutoAE2(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(this::capabilities);
        bus.addListener(this::payloads);

    }

    private void payloads(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar("1");
        registrar.playToServer(AdjustPlacement.TYPE, AdjustPlacement.CODEC, AdjustPlacement::handle);
        registrar.playToClient(PlacementFeedback.TYPE, PlacementFeedback.CODEC);
    }

    private void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, PLANNER_ENTITY.get(), (entity, side) -> entity);
    }

}
