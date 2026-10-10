package com.naverene.stevespantry.item;

import com.naverene.stevespantry.Spice;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SpiceItem extends Item {
    private final Spice spice;

    public SpiceItem(Spice spice, Properties properties) {
        super(properties);
        this.spice = spice;
    }

    public Spice spice() {
        return spice;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent(spice.latinName()).withStyle(TextFormatting.GRAY, TextFormatting.ITALIC));
        tooltip.add(DishItem.describeSpiceBonus(spice).withStyle(TextFormatting.BLUE));
    }
}
