package com.github.alexthe666.rats.server.items;

import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.registry.RatsSoundRegistry;
import com.github.alexthe666.rats.server.catcatch.CatCatch;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.github.alexthe666.rats.server.entity.rat.DiggingRat;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 鼠片：被猫活捉的野生老鼠。完整保存原鼠的实体数据（毛色、性别、名字、生命、持有物、UUID 等），
 * 释放出来的就是原来那只老鼠。不可堆叠，只用于搬运和释放。
 */
public class RatSliceItem extends Item {
	public static final String DATA_TAG = "RatData";

	public RatSliceItem(Item.Properties properties) {
		super(properties);
	}

	public static ItemStack fromRat(Rat rat) {
		return packData(rat);
	}

	public static ItemStack pressed(DiggingRat rat) {
		ItemStack stack = packData(rat);
		stack.getOrCreateTag().putString("PressedRatType", rat instanceof TamedRat ? "tamed" : "wild");
		return stack;
	}

	private static ItemStack packData(DiggingRat rat) {
		ItemStack stack = new ItemStack(RatsItemRegistry.RAT_SLICE.get());
		CompoundTag data = new CompoundTag();
		rat.saveWithoutId(data);
		// 位置/速度/拴绳/乘骑这些属于“在世界里的状态”，不随鼠片携带
		data.remove("Pos");
		data.remove("Motion");
		data.remove("Rotation");
		data.remove("FallDistance");
		data.remove("Leash");
		data.remove("Passengers");
		data.remove("OnGround");
		stack.getOrCreateTag().put(DATA_TAG, data);
		return stack;
	}

	@Nullable
	public static CompoundTag getData(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(DATA_TAG, Tag.TAG_COMPOUND) ? tag.getCompound(DATA_TAG) : null;
	}

	/** 毛色 id（客户端渲染用）。没有数据时返回 null。 */
	@Nullable
	public static String getVariantId(ItemStack stack) {
		CompoundTag data = getData(stack);
		return data != null && data.contains("ColorVariant", Tag.TAG_STRING) ? data.getString("ColorVariant") : null;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		ServerLevel server = (ServerLevel) level;

		BlockPos clicked = context.getClickedPos();
		Direction face = context.getClickedFace();
		BlockState state = level.getBlockState(clicked);
		BlockPos pos = state.getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);

		boolean pressed = isPressed(stack);
		DiggingRat rat = pressed ? restorePressed(stack, server) : createRat(stack, server);
		if (rat == null) {
			if (player != null) player.displayClientMessage(Component.translatable("item.rats.rat_slice.release_failed").withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}
		rat.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
		rat.yHeadRot = rat.getYRot();
		rat.yBodyRot = rat.getYRot();
		if (!level.noCollision(rat)) {
			if (player != null) player.displayClientMessage(Component.translatable("item.rats.rat_slice.no_space").withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}
		if (!pressed && rat instanceof Rat wild) {
			wild.setOwnerUUID(null);
			wild.setTame(false);
			wild.setOrderedToSit(false);
			wild.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CatCatch.RELEASE_PROTECT_TICKS, 9));
			CatCatch.protect(wild);
		}
		rat.setPersistenceRequired();
		if (!server.addFreshEntity(rat)) {
			if (player != null) player.displayClientMessage(Component.translatable("item.rats.rat_slice.release_failed").withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}
		level.playSound(null, pos, RatsSoundRegistry.RAT_IDLE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
		stack.shrink(1);
		if (player != null) player.displayClientMessage(Component.translatable(pressed ? "item.rats.rat_press.released" : "item.rats.rat_slice.released"), true);
		return InteractionResult.CONSUME;
	}

	private static boolean isPressed(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains("PressedRatType");
	}

	@Nullable
	private static DiggingRat restorePressed(ItemStack stack, ServerLevel level) {
		CompoundTag data = getData(stack);
		if (data == null || !data.hasUUID("UUID")) return null;
		UUID id = data.getUUID("UUID");
		for (ServerLevel world : level.getServer().getAllLevels()) {
			if (world.getEntity(id) != null) return null;
		}
		DiggingRat rat = switch (stack.getTag().getString("PressedRatType")) {
			case "tamed" -> RatsEntityRegistry.TAMED_RAT.get().create(level);
			case "wild" -> RatsEntityRegistry.RAT.get().create(level);
			default -> null;
		};
		if (rat != null) rat.load(data.copy());
		return rat;
	}

	/** 按鼠片数据还原老鼠实体（未加入世界）。没有数据的鼠片（创造模式/JEI 取出）生成一只新的野生普通老鼠。 */
	@Nullable
	public static Rat createRat(ItemStack stack, ServerLevel level) {
		Rat rat = RatsEntityRegistry.RAT.get().create(level);
		if (rat == null) return null;
		CompoundTag data = getData(stack);
		if (data != null) {
			rat.load(data.copy());
			UUID id = rat.getUUID();
			if (level.getEntity(id) != null) {
				// 极端情况（同一只老鼠的数据已在世界中）：换一个 UUID 避免冲突，绝不吞物品
				rat.setUUID(UUID.randomUUID());
			}
			rat.setHealth(Math.max(1.0F, rat.getHealth()));
			rat.deathTime = 0;
			rat.hurtTime = 0;
		} else {
			rat.finalizeSpawn(level, level.getCurrentDifficultyAt(rat.blockPosition()), MobSpawnType.SPAWN_EGG, null, null);
			rat.setPlagued(false);
			rat.setToga(false);
		}
		return rat;
	}

	@Override
	public Component getName(ItemStack stack) {
		CompoundTag data = getData(stack);
		if (data != null && data.contains("CustomName", Tag.TAG_STRING)) {
			Component custom = Component.Serializer.fromJson(data.getString("CustomName"));
			if (custom != null) return Component.translatable(isPressed(stack) ? "item.rats.rat_press.named" : "item.rats.rat_slice.named", custom);
		}
		return isPressed(stack) ? Component.translatable("item.rats.rat_press.patty") : super.getName(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		CompoundTag data = getData(stack);
		if (data == null) {
			tooltip.add(Component.translatable("item.rats.rat_slice.empty").withStyle(ChatFormatting.GRAY));
		} else {
			String variant = data.getString("ColorVariant");
			String path = variant.contains(":") ? variant.substring(variant.indexOf(':') + 1) : variant;
			if (!path.isEmpty()) tooltip.add(Component.translatable("item.rats.rat_slice.variant", Component.translatable("rats.rat_variant." + path)).withStyle(ChatFormatting.GRAY));
			tooltip.add(Component.translatable(data.getBoolean("IsMale") ? "item.rats.rat_slice.male" : "item.rats.rat_slice.female").withStyle(ChatFormatting.GRAY));
			tooltip.add(Component.translatable("item.rats.rat_slice.health", String.format("%.1f", data.getFloat("Health"))).withStyle(ChatFormatting.GRAY));
		}
		if (isPressed(stack)) {
			tooltip.add(Component.translatable("item.rats.rat_press.restore").withStyle(ChatFormatting.YELLOW));
			return;
		}
		tooltip.add(Component.translatable("item.rats.rat_slice.desc0").withStyle(ChatFormatting.YELLOW));
		tooltip.add(Component.translatable("item.rats.rat_slice.desc1").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			@Override
			public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
				return com.github.alexthe666.rats.client.render.RatSliceItemRenderer.get();
			}
		});
	}

	public static RatVariant variantOrNull(ItemStack stack) {
		String id = getVariantId(stack);
		return id == null ? null : RatVariant.getVariant(id);
	}
}
