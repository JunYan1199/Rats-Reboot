package com.github.alexthe666.rats.server.catcatch;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.github.alexthe666.rats.server.items.CatSatchelItem;
import com.github.alexthe666.rats.server.items.CatUnbucklerItem;
import com.github.alexthe666.rats.server.items.CheeseCatBatonItem;
import com.github.alexthe666.rats.server.message.RatsNetworkHandler;
import com.github.alexthe666.rats.server.message.SyncCatSatchelPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = RatsMod.MODID)
public class CatCatchEvents {

	// ---------------- 村庄白天刷新普通老鼠 ----------------
	/** 每个玩家每 10 秒检查一次。 */
	public static final int VILLAGE_SPAWN_INTERVAL = 200;
	/** 每次检查的刷新概率。 */
	public static final float VILLAGE_SPAWN_CHANCE = 0.35F;
	/** 玩家 48 格内野生普通老鼠达到这个数量就不再在村庄刷新。 */
	public static final int VILLAGE_RAT_CAP = 6;

	public static void attachCaps(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof Cat) {
			event.addCapability(CatSatchel.ID, new CatSatchel.Provider());
		}
	}

	public static void sync(Cat cat) {
		CatSatchel s = CatSatchel.get(cat);
		if (s == null || cat.level().isClientSide()) return;
		RatsNetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> cat), new SyncCatSatchelPacket(cat.getId(), s.isEquipped()));
	}

	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof Cat cat && event.getEntity() instanceof ServerPlayer player) {
			CatSatchel s = CatSatchel.get(cat);
			if (s != null && s.isEquipped()) {
				RatsNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCatSatchelPacket(cat.getId(), true));
			}
		}
	}

	@SubscribeEvent
	public static void onJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Cat cat)) return;
		boolean has = cat.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof CatHuntRatGoal);
		if (!has) {
			cat.goalSelector.addGoal(2, new CatDepositGoal(cat));
			cat.goalSelector.addGoal(2, new CatReturnToOwnerGoal(cat));
			cat.goalSelector.addGoal(3, new CatHuntRatGoal(cat));
		}
	}

	/** 猫死亡时：挎包里的鼠片和挎包本身一起掉落，不会凭空消失。 */
	@SubscribeEvent
	public static void onDrops(LivingDropsEvent event) {
		if (!(event.getEntity() instanceof Cat cat)) return;
		CatSatchel s = CatSatchel.get(cat);
		if (s == null || !s.isEquipped()) return;
		for (ItemStack slice : s.takeAll()) {
			event.getDrops().add(new ItemEntity(cat.level(), cat.getX(), cat.getY() + 0.3D, cat.getZ(), slice));
		}
		event.getDrops().add(new ItemEntity(cat.level(), cat.getX(), cat.getY() + 0.3D, cat.getZ(), new ItemStack(RatsItemRegistry.CAT_RAT_SATCHEL.get())));
		s.setEquipped(false);
		s.setDepot(null);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getTarget() instanceof Cat cat)) return;
		CatSatchel satchel = CatSatchel.get(cat);
		if (satchel == null) return;
		Player player = event.getEntity();
		ItemStack held = event.getItemStack();
		InteractionHand hand = event.getHand();
		boolean client = event.getLevel().isClientSide();

		if (held.getItem() instanceof CatSatchelItem) {
			if (!client) equip(player, cat, satchel, held);
		} else if (held.getItem() instanceof CheeseCatBatonItem) {
			if (!client) baton(player, cat, satchel, held);
		} else if (held.getItem() instanceof CatUnbucklerItem) {
			if (!client) unbuckle(player, cat, satchel);
		} else if (satchel.isEquipped() && player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
			if (!client) open(player, cat, satchel);
		} else {
			return;
		}
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(client));
	}

	private static boolean checkOwner(Player player, Cat cat) {
		if (!cat.isTame()) {
			player.displayClientMessage(Component.translatable("item.rats.cat_satchel.not_tame").withStyle(ChatFormatting.RED), true);
			return false;
		}
		if (!player.getUUID().equals(cat.getOwnerUUID())) {
			player.displayClientMessage(Component.translatable("item.rats.cat_satchel.not_owner").withStyle(ChatFormatting.RED), true);
			return false;
		}
		return true;
	}

	private static void equip(Player player, Cat cat, CatSatchel satchel, ItemStack held) {
		if (!checkOwner(player, cat)) return;
		if (cat.isBaby()) {
			player.displayClientMessage(Component.translatable("item.rats.cat_satchel.baby").withStyle(ChatFormatting.RED), true);
			return;
		}
		if (satchel.isEquipped()) {
			player.displayClientMessage(Component.translatable("item.rats.cat_satchel.already").withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		satchel.setEquipped(true);
		satchel.depotBlocked = false;
		if (!player.getAbilities().instabuild) held.shrink(1);
		cat.level().playSound(null, cat.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.NEUTRAL, 1.0F, 1.2F);
		player.displayClientMessage(Component.translatable("item.rats.cat_satchel.equipped", cat.getName()).withStyle(ChatFormatting.GREEN), true);
		sync(cat);
	}

	private static void baton(Player player, Cat cat, CatSatchel satchel, ItemStack held) {
		if (!checkOwner(player, cat)) return;
		if (player.isShiftKeyDown()) {
			satchel.setDepot(null);
			player.displayClientMessage(Component.translatable("item.rats.cheese_cat_baton.unbound", cat.getName()).withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		CheeseCatBatonItem.select(held, cat);
		cat.level().playSound(null, cat.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.PLAYERS, 0.6F, 1.6F);
		player.displayClientMessage(Component.translatable("item.rats.cheese_cat_baton.selected_msg", cat.getName()).withStyle(ChatFormatting.AQUA), true);
	}

	private static void unbuckle(Player player, Cat cat, CatSatchel satchel) {
		if (!checkOwner(player, cat)) return;
		if (!satchel.isEquipped()) {
			player.displayClientMessage(Component.translatable("item.rats.cat_unbuckler.none").withStyle(ChatFormatting.YELLOW), true);
			return;
		}
		int n = 0;
		for (ItemStack slice : satchel.takeAll()) {
			cat.spawnAtLocation(slice, 0.3F);
			n++;
		}
		cat.spawnAtLocation(new ItemStack(RatsItemRegistry.CAT_RAT_SATCHEL.get()), 0.3F);
		satchel.setEquipped(false);
		satchel.setDepot(null);
		cat.getNavigation().stop();
		cat.level().playSound(null, cat.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.NEUTRAL, 1.0F, 0.8F);
		player.displayClientMessage(Component.translatable("item.rats.cat_unbuckler.done", cat.getName(), n).withStyle(ChatFormatting.GREEN), true);
		sync(cat);
	}

	private static void open(Player player, Cat cat, CatSatchel satchel) {
		if (!checkOwner(player, cat) || !(player instanceof ServerPlayer sp)) return;
		Component title = Component.translatable("container.rats.cat_satchel", cat.getName());
		NetworkHooks.openScreen(sp, new SimpleMenuProvider((id, inv, p) -> new CatSatchelMenu(id, inv, new CatSatchelMenu.SatchelContainer(cat, satchel)), title));
		cat.level().playSound(null, cat.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.NEUTRAL, 0.8F, 1.0F);
	}

	// ---------------- 村庄刷新 ----------------

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
		if (level.getGameTime() % VILLAGE_SPAWN_INTERVAL != 0) return;
		if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) || !level.getGameRules().getBoolean(RatsMod.SPAWN_RATS)) return;
		if (!level.dimensionType().natural()) return;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			tryVillageSpawn(level, player.blockPosition(), level.getRandom().nextFloat() < VILLAGE_SPAWN_CHANCE);
		}
	}

	/**
	 * 在玩家附近的村庄里尝试刷新 1~2 只普通野生老鼠（不受白天光照限制）。
	 *
	 * @return 实际刷新的数量
	 */
	public static int tryVillageSpawn(ServerLevel level, BlockPos center, boolean roll) {
		if (!roll || !level.isCloseToVillage(center, 3)) return 0;
		int nearby = level.getEntitiesOfClass(Rat.class, new net.minecraft.world.phys.AABB(center).inflate(48.0D),
				r -> r.getType() == RatsEntityRegistry.RAT.get() && !r.isTame()).size();
		if (nearby >= VILLAGE_RAT_CAP) return 0;
		int want = Math.min(VILLAGE_RAT_CAP - nearby, 1 + level.getRandom().nextInt(2));
		int spawned = 0;
		for (int attempt = 0; attempt < 12 && spawned < want; attempt++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
			double dist = 10.0D + level.getRandom().nextDouble() * 22.0D;
			int x = center.getX() + (int) Math.round(Math.cos(angle) * dist);
			int z = center.getZ() + (int) Math.round(Math.sin(angle) * dist);
			if (!level.hasChunkAt(new BlockPos(x, center.getY(), z))) continue;
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);
			if (!level.isCloseToVillage(pos, 2)) continue;
			if (!NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, level, pos, RatsEntityRegistry.RAT.get())) continue;
			if (!level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), RatsEntityRegistry.RAT.get())) continue;
			if (level.getNearestPlayer(x + 0.5D, y, z + 0.5D, 8.0D, false) != null) continue;
			Rat rat = RatsEntityRegistry.RAT.get().create(level);
			if (rat == null) continue;
			rat.moveTo(x + 0.5D, y, z + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
			if (!level.noCollision(rat)) continue;
			rat.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
			rat.setPlagued(false);
			rat.setToga(false);
			if (level.addFreshEntity(rat)) spawned++;
		}
		return spawned;
	}
}
