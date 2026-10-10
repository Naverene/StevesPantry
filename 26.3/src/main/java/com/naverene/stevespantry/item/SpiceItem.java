package com.naverene.stevespantry.item;

import com.naverene.stevespantry.Spice;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal(spice.latinName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.accept(DishItem.describeSpiceBonus(spice).withStyle(ChatFormatting.BLUE));
    }
}
