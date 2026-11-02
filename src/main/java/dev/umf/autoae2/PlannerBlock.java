package dev.umf.autoae2;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.BlockHitResult;

public class PlannerBlock extends BaseEntityBlock {
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    public static final MapCodec<PlannerBlock> CODEC = simpleCodec(PlannerBlock::new);
    public PlannerBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(CONNECTED, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CONNECTED); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof PlannerBlockEntity planner) planner.setOwner(player);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PlannerBlockEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
                    new PlannerMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                    Component.literal("Automation Planner")));
        }
        return InteractionResult.SUCCESS;
    }
}
