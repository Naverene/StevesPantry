package com.naverene.stevespantry.block;

import com.naverene.stevespantry.reference.Reference;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Unpowered cold storage: 27 food slots and an ice slot. Dishes inside spoil three times slower while there's ice. */
public class IceboxBlock extends ContainerBlock {

    public IceboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockRenderType getRenderShape(BlockState state) {
        return BlockRenderType.MODEL;
    }

    // Ticking comes from the tile entity itself (ITickableTileEntity) on 1.16.5, not from a block ticker.
    @Override
    public TileEntity newBlockEntity(IBlockReader level) {
        return new IceboxBlockEntity();
    }

    @Override
    public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand,
            BlockRayTraceResult hit) {
        if (!level.isClientSide) {
            TileEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IceboxBlockEntity) {
                player.openMenu((IceboxBlockEntity) blockEntity);
            }
        }
        return ActionResultType.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, World level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            TileEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IceboxBlockEntity) {
                InventoryHelper.dropContents(level, pos, (IceboxBlockEntity) blockEntity);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, World level, BlockPos pos) {
        return Container.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable IBlockReader level, List<ITextComponent> tooltip,
            ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("tooltip." + Reference.MODID + ".icebox", IceboxBlockEntity.SLOWDOWN)
                .withStyle(TextFormatting.AQUA));
        tooltip.add(new TranslationTextComponent("tooltip." + Reference.MODID + ".icebox.ice")
                .withStyle(TextFormatting.GRAY));
    }
}
