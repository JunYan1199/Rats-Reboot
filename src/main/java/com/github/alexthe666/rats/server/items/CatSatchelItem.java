package com.github.alexthe666.rats.server.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 猫咪捕鼠挎包：手持右键自己驯服的成年猫即可佩戴。佩戴逻辑在 CatCatchEvents 里（要先于原版猫的坐下交互）。 */
public class CatSatchelItem extends Item {
	public CatSatchelItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.rats.cat_rat_satchel.desc0").withStyle(ChatFormatting.YELLOW));
		tooltip.add(Component.translatable("item.rats.cat_rat_satchel.desc1").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.rats.cat_rat_satchel.desc2").withStyle(ChatFormatting.GRAY));
	}
}
