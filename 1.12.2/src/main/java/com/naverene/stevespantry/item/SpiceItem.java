package com.naverene.stevespantry.item;

import com.naverene.stevespantry.Spice;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class SpiceItem extends Item {
    private final Spice spice;

    public SpiceItem(Spice spice) {
        this.spice = spice;
    }

    public Spice spice() {
        return spice;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + "" + TextFormatting.ITALIC + spice.latinName());
        tooltip.add(TextFormatting.BLUE + DishItem.describeSpiceBonus(spice));
    }
}
