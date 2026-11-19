package com.github.alexthe666.rats.server.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 解扣器：右键戴着挎包的猫，卸下挎包；挎包内鼠片全部掉落，空挎包返还。逻辑在 CatCatchEvents。 */
public class CatUnbucklerItem extends Item {
	public CatUnbucklerItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.rats.cat_unbuckler.desc0").withStyle(ChatFormatting.YELLOW));
		tooltip.add(Component.translatable("item.rats.cat_unbuckler.desc1").withStyle(ChatFormatting.GRAY));
	}
}
