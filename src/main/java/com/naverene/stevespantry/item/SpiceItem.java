package com.naverene.stevespantry.item;

import com.naverene.stevespantry.Spice;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(spice.latinName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(DishItem.describeSpiceBonus(spice).withStyle(ChatFormatting.BLUE));
    }
}
