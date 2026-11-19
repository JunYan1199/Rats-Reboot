package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.server.catcatch.CatSatchel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 奶酪指挥棒：先右键自己的猫选中它，再右键箱子/木桶/任何带物品存储能力的容器，为这只猫绑定存货目标。
 * 潜行右键猫 = 解除这只猫的存货绑定。选猫逻辑在 CatCatchEvents（需要先于原版猫的坐下交互）。
 */
public class CheeseCatBatonItem extends Item {
	public static final String SELECTED = "SelectedCat";
	public static final String SELECTED_NAME = "SelectedCatName";

	public CheeseCatBatonItem(Item.Properties properties) {
		super(properties);
	}

	public static void select(ItemStack stack, Cat cat) {
		CompoundTag tag = stack.getOrCreateTag();
		tag.putUUID(SELECTED, cat.getUUID());
		tag.putString(SELECTED_NAME, Component.Serializer.toJson(cat.getName()));
	}

	@Nullable
	public static UUID selected(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.hasUUID(SELECTED) ? tag.getUUID(SELECTED) : null;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		BlockPos pos = context.getClickedPos();
		if (player == null) return InteractionResult.PASS;
		BlockEntity be = level.getBlockEntity(pos);
		boolean storage = be != null && (be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).isPresent() || be.getCapability(ForgeCapabilities.ITEM_HANDLER, context.getClickedFace()).isPresent());
		if (level.isClientSide()) return storage ? InteractionResult.SUCCESS : InteractionResult.PASS;
		UUID id = selected(stack);
		if (id == null) {
			player.displayClientMessage(Component.translatable("item.rats.cheese_cat_baton.no_cat").withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}
		if (!storage) {
			player.displayClientMessage(Component.translatable("item.rats.cheese_cat_baton.not_storage").withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}
		Entity entity = ((ServerLevel) level).getEntity(id);
		if (!(entity instanceof Cat cat) || !cat.isAlive()) {
			player.displayClientMessage(Component.translatable("item.rats.cheese_cat_baton.cat_missing").withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}
		if (!player.getUUID().equals(cat.getOwnerUUID())) {
			player.displayClientMessage(Component.translatable("item.rats.cat_satchel.not_owner").withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}
		CatSatchel satchel = CatSatchel.get(cat);
		if (satchel == null) return InteractionResult.FAIL;
		satchel.setDepot(GlobalPos.of(level.dimension(), pos.immutable()));
		level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
		player.displayClientMessage(Component.translatable("item.rats.cheese_cat_baton.bound", cat.getName(), pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), false);
		if (!satchel.isEquipped()) {
			player.displayClientMessage(Component.translatable("item.rats.cheese_cat_baton.bound_no_satchel").withStyle(ChatFormatting.GRAY), false);
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains(SELECTED_NAME)) {
			Component name = Component.Serializer.fromJson(tag.getString(SELECTED_NAME));
			if (name != null) tooltip.add(Component.translatable("item.rats.cheese_cat_baton.selected", name).withStyle(ChatFormatting.AQUA));
		}
		tooltip.add(Component.translatable("item.rats.cheese_cat_baton.desc0").withStyle(ChatFormatting.YELLOW));
		tooltip.add(Component.translatable("item.rats.cheese_cat_baton.desc1").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.rats.cheese_cat_baton.desc2").withStyle(ChatFormatting.GRAY));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return selected(stack) != null;
	}
}
