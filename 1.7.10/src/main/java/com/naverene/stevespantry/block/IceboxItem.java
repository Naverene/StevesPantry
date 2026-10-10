package com.naverene.stevespantry.block;

import com.naverene.stevespantry.reference.Reference;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

/** The icebox's item form. 1.7.10 blocks have no tooltip hook, so the tooltip lives here. */
public class IceboxItem extends ItemBlock {
    public IceboxItem(Block block) {
        super(block);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted(
                "tooltip." + Reference.MODID + ".icebox", IceboxBlockEntity.SLOWDOWN));
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("tooltip." + Reference.MODID + ".icebox.ice"));
    }
}
