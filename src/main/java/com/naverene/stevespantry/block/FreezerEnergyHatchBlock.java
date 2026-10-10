package com.naverene.stevespantry.block;

import com.mojang.serialization.MapCodec;
import com.naverene.stevespantry.reference.Reference;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Takes Forge Energy (FE) from cables on any side and buffers it for the freezer controller. */
public class FreezerEnergyHatchBlock extends BaseEntityBlock {
    public static final MapCodec<FreezerEnergyHatchBlock> CODEC = simpleCodec(FreezerEnergyHatchBlock::new);

    public FreezerEnergyHatchBlock(Properties properties) {
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
        return new FreezerEnergyHatchBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FreezerEnergyHatchBlockEntity hatch) {
            player.displayClientMessage(Component.translatable("message." + Reference.MODID + ".energy_hatch",
                    hatch.energy().getEnergyStored(), hatch.energy().getMaxEnergyStored()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip." + Reference.MODID + ".freezer_energy_hatch",
                FreezerEnergyHatchBlockEntity.CAPACITY, FreezerEnergyHatchBlockEntity.MAX_RECEIVE)
                .withStyle(ChatFormatting.GRAY));
    }
}
