package com.naverene.stevespantry.item;

import com.naverene.stevespantry.block.FreezerControllerBlockEntity;
import com.naverene.stevespantry.reference.Reference;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/**
 * The freezer's wear part. Each point of durability is {@link #TICKS_PER_DAMAGE} ticks of chilling,
 * so a new one lasts about ten in-game days of running. A worn-out condenser isn't destroyed: the
 * freezer just falls back to Icebox speed until it's swapped or repaired with copper in an anvil.
 */
public class CondenserItem extends Item {
    public static final int DURABILITY = 240;
    public static final int TICKS_PER_DAMAGE = 1000;

    public CondenserItem(Properties properties) {
        super(properties);
    }

    public static boolean isWorn(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.COPPER_INGOT);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "tooltip." + Reference.MODID + ".condenser";
        if (isWorn(stack)) {
            tooltip.add(Component.translatable(key + ".worn", FreezerControllerBlockEntity.WORN_SLOWDOWN)
                    .withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}
