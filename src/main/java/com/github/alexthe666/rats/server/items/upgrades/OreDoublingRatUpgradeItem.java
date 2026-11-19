package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.OreRatNuggetItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

public class OreDoublingRatUpgradeItem extends BaseRatUpgradeItem {

	public OreDoublingRatUpgradeItem(Item.Properties properties) {
		super(properties, 2, 4);
	}

	@Override
	public boolean shouldDepositItem(TamedRat rat, ItemStack stack) {
		return !isProcessable(rat.level(), stack);
	}

	@Override
	public boolean isRatHoldingFood(TamedRat rat) {
		return isProcessable(rat.level(), rat.getMainHandItem());
	}

	public static boolean isProcessable(Level level, ItemStack stack) {
		return !getProcessingResult(level, stack).isEmpty();
	}

	public static ItemStack getProcessingResult(Level level, ItemStack stack) {
		if (stack.isEmpty()) return ItemStack.EMPTY;
		if (stack.is(Tags.Items.ORES) || stack.is(Tags.Items.RAW_MATERIALS) || stack.getTags().anyMatch(tag -> {
			ResourceLocation id = tag.location();
			return (id.getNamespace().equals("forge") || id.getNamespace().equals("c"))
					&& (id.getPath().equals("raw_materials") || id.getPath().startsWith("raw_materials/"));
		})) {
			return OreRatNuggetItem.getIngot(level, stack);
		}

		// Some mods omit ore tags but use raw_<metal> and <metal>_ingot for a smelting pair.
		ResourceLocation input = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (stack.getItem() instanceof BlockItem || !input.getPath().startsWith("raw_")) return ItemStack.EMPTY;
		ItemStack result = OreRatNuggetItem.getIngot(level, stack);
		if (result.isEmpty()) return ItemStack.EMPTY;
		ResourceLocation output = BuiltInRegistries.ITEM.getKey(result.getItem());
		boolean matchingIngot = output.getNamespace().equals(input.getNamespace())
				&& output.getPath().equals(input.getPath().substring(4) + "_ingot");
		return result.is(Tags.Items.INGOTS) || matchingIngot ? result : ItemStack.EMPTY;
	}
}
