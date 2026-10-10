package com.naverene.stevespantry.block;

import com.mojang.serialization.MapCodec;
import com.naverene.stevespantry.ModRegistries;
import com.naverene.stevespantry.reference.Reference;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Unpowered cold storage: 27 food slots and an ice slot. Dishes inside spoil three times slower while there's ice. */
public class IceboxBlock extends BaseEntityBlock {
    public static final MapCodec<IceboxBlock> CODEC = simpleCodec(IceboxBlock::new);

    public IceboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IceboxBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModRegistries.ICEBOX_BLOCK_ENTITY.get(), IceboxBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IceboxBlockEntity icebox) {
            player.openMenu(icebox);
        }
        return InteractionResult.SUCCESS;
    }

    // The block entity drops its contents itself (BlockEntity#preRemoveSideEffects); this just tells comparators.
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    /** Blocks no longer have tooltips of their own, so {@link IceboxBlockItem} calls this. */
    public static void appendHoverText(Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("tooltip." + Reference.MODID + ".icebox", IceboxBlockEntity.SLOWDOWN)
                .withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip." + Reference.MODID + ".icebox.ice").withStyle(ChatFormatting.GRAY));
    }
}
