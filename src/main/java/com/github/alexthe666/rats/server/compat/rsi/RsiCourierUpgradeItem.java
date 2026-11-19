package com.github.alexthe666.rats.server.compat.rsi;

import com.github.alexthe666.rats.server.items.upgrades.BaseRatUpgradeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 共振快递升级：潜行右键 Refined Storage 控制器（含彩色/创造变种）绑定，再装到老鼠身上。
 * 装上后，主人通过共振存储（rs_integration）下单诡厄祭坛仪式时，由老鼠从控制器取料、搬上基座并放上激活物开仪式。
 */
public class RsiCourierUpgradeItem extends BaseRatUpgradeItem {
	private static final String TAG = "RsiController";

	public RsiCourierUpgradeItem(Properties properties) {
		super(properties, 2, 4);
	}

	public static boolean isRsController(BlockState state) {
		ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
		return id != null && "refinedstorage".equals(id.getNamespace()) && id.getPath().endsWith("controller");
	}

	@Nullable
	public static GlobalPos getBinding(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(TAG, 10)) return null;
		CompoundTag b = tag.getCompound(TAG);
		ResourceLocation dim = ResourceLocation.tryParse(b.getString("Dim"));
		if (dim == null) return null;
		return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(b.getLong("Pos")));
	}

	public static void setBinding(ItemStack stack, @Nullable GlobalPos pos) {
		if (pos == null) {
			if (stack.getTag() != null) stack.getTag().remove(TAG);
			return;
		}
		CompoundTag b = new CompoundTag();
		b.putString("Dim", pos.dimension().location().toString());
		b.putLong("Pos", pos.pos().asLong());
		stack.getOrCreateTag().put(TAG, b);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
		Level level = context.getLevel();
		BlockState state = level.getBlockState(context.getClickedPos());
		if (!isRsController(state)) {
			if (!level.isClientSide()) player.displayClientMessage(Component.translatable("item.rats.rat_upgrade_rsi_courier.not_controller").withStyle(ChatFormatting.RED), true);
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		if (!level.isClientSide()) {
			setBinding(context.getItemInHand(), GlobalPos.of(level.dimension(), context.getClickedPos().immutable()));
			BlockPos p = context.getClickedPos();
			player.displayClientMessage(Component.translatable("item.rats.rat_upgrade_rsi_courier.bound", p.getX(), p.getY(), p.getZ()).withStyle(ChatFormatting.AQUA), true);
			level.playSound(null, p, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown() && getBinding(stack) != null) {
			if (!level.isClientSide()) {
				setBinding(stack, null);
				player.displayClientMessage(Component.translatable("item.rats.rat_upgrade_rsi_courier.cleared").withStyle(ChatFormatting.GRAY), true);
			}
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		}
		return super.use(level, player, hand);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return getBinding(stack) != null;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		GlobalPos pos = getBinding(stack);
		if (pos == null) {
			tooltip.add(Component.translatable("item.rats.rat_upgrade_rsi_courier.unbound").withStyle(ChatFormatting.RED));
		} else {
			BlockPos p = pos.pos();
			tooltip.add(Component.translatable("item.rats.rat_upgrade_rsi_courier.binding", p.getX(), p.getY(), p.getZ(), pos.dimension().location().toString()).withStyle(ChatFormatting.AQUA));
		}
	}
}
