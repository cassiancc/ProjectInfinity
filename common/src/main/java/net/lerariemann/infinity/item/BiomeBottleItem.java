package net.lerariemann.infinity.item;

import net.lerariemann.infinity.block.custom.BiomeBottleBlock;
import net.lerariemann.infinity.registry.core.ModComponentTypes;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.List;

public class BiomeBottleItem extends BlockItem {
    public BiomeBottleItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        Identifier biome = stack.getComponents().get(ModComponentTypes.BIOME_CONTENTS.get());
        if (biome != null) {
            tooltip.add(Text.translatable(Util.createTranslationKey("biome", biome)).formatted(Formatting.GRAY));
        }
        else {
            MutableText mutableText = Text.translatable("caption.infinity.biomebottle.empty");
            tooltip.add(mutableText.formatted(Formatting.GRAY));
        }
        if (type.isAdvanced()) {
            tooltip.add(Text.translatable("caption.infinity.biomebottle.charge", BiomeBottleBlock.getCharge(stack)).formatted(Formatting.GRAY));
        }
    }
}
