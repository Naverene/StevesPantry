package com.naverene.stevespantry.item;

import com.naverene.stevespantry.Spice;
import com.naverene.stevespantry.reference.Reference;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class SpiceItem extends Item {
    private final Spice spice;

    public SpiceItem(Spice spice) {
        this.spice = spice;
        setUnlocalizedName(spice.unlocalizedName());
        setTextureName(Reference.MODID + ":" + spice.id());
    }

    public Spice spice() {
        return spice;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(EnumChatFormatting.GRAY + "" + EnumChatFormatting.ITALIC + spice.latinName());
        tooltip.add(EnumChatFormatting.BLUE + DishItem.describeSpiceBonus(spice));
    }
}
