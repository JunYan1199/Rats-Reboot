package com.github.alexthe666.rats.server.compat.rsi;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;

/**
 * 老鼠共振快递：接管 rs_integration 对诡厄暗黑祭坛的「瞬移放料 + 立即开仪式」。
 * <p>
 * RSI 照常从存储扣料、放上基座并校验仪式（同一 tick 内），在它调用 startAltarRitual 的瞬间，
 * 若主人附近有装了已绑定控制器的共振快递升级的老鼠，就把基座上的材料悄悄收回交给老鼠（同 tick，客户端看不到闪烁），
 * 由老鼠跑到控制器取料 → 逐个基座放下 → 把激活物放上祭坛，再调用原始 startAltarRitual 开仪式（灵魂照旧扣玩家的）。
 * 任何意外（老鼠死亡/卸载/卡路/超时 60 秒/停服）都把剩余材料瞬移补齐并开仪式，相当于退回原模组行为。
 */
@Mod.EventBusSubscriber(modid = RatsMod.MODID)
public final class RsiCourierJobs {
	public enum Status {NONE, PENDING, STARTED, FAILED}

	private enum Stage {TO_CONTROLLER, LOADING, TO_PEDESTAL, PLACING, TO_ALTAR, ACTIVATING}

	public static final String GHOST_TAG = "RatsRsiCourierGhost";
	private static final String SAVED_HAND = "RatsRsiCourierHand";
	private static final int TOTAL_TIMEOUT = 20 * 60;
	private static final int LEG_TIMEOUT = 20 * 25;
	private static final int NO_PATH_TIMEOUT = 20 * 4;
	private static final double RAT_RANGE = 48.0D;
	private static final double CONTROLLER_RANGE = 64.0D;
	private static final double SPEED = 1.35D;

	private static final Map<RsiCourierDelegate, Job> BY_DELEGATE = new IdentityHashMap<>();
	private static final Map<UUID, Job> BY_RAT = new HashMap<>();
	private static final ThreadLocal<Boolean> DIRECT = ThreadLocal.withInitial(() -> false);
	private static final Set<TamedRat> GOAL_ADDED = Collections.newSetFromMap(new WeakHashMap<>());
	/** 冒烟测试里没有 RS，可替换成别的方块。 */
	public static java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> controllerCheck = RsiCourierUpgradeItem::isRsController;

	private RsiCourierJobs() {
	}

	private static final class Drop {
		final BlockEntity pedestal;
		final ItemStack stack;

		Drop(BlockEntity pedestal, ItemStack stack) {
			this.pedestal = pedestal;
			this.stack = stack;
		}
	}

	private static final class Job {
		final RsiCourierDelegate delegate;
		final ServerLevel level;
		final TamedRat rat;
		final BlockEntity altar;
		final ServerPlayer player;
		final ItemStack activation;
		final Object recipe;
		final BlockPos controller;
		final List<Drop> drops;
		ItemStack savedHand = ItemStack.EMPTY;
		boolean wasSitting;
		int index;
		Stage stage = Stage.TO_CONTROLLER;
		int stageTicks;
		int noPathTicks;
		int age;
		boolean finished;
		Status status = Status.PENDING;

		Job(RsiCourierDelegate delegate, ServerLevel level, TamedRat rat, BlockEntity altar, ServerPlayer player, ItemStack activation, Object recipe, BlockPos controller, List<Drop> drops) {
			this.delegate = delegate;
			this.level = level;
			this.rat = rat;
			this.altar = altar;
			this.player = player;
			this.activation = activation;
			this.recipe = recipe;
			this.controller = controller;
			this.drops = drops;
		}
	}

	// ---------------------------------------------------------------- mixin entry points

	/** 在 RSI 的 startAltarRitual 开头调用；返回 true 表示已交给老鼠，RSI 应视为「已请求开仪式」。 */
	public static boolean tryDefer(RsiCourierDelegate delegate, Object altarObj, ServerPlayer player, ItemStack activation, Object recipe) {
		if (DIRECT.get() || player == null || BY_DELEGATE.containsKey(delegate)) return false;
		if (!(altarObj instanceof BlockEntity altar) || !(altar.getLevel() instanceof ServerLevel level)) return false;
		List<Drop> drops = null;
		try {
			TamedRat rat = null;
			BlockPos controller = null;
			double best = Double.MAX_VALUE;
			BlockPos altarPos = altar.getBlockPos();
			for (TamedRat candidate : level.getEntitiesOfClass(TamedRat.class, new AABB(altarPos).inflate(RAT_RANGE), r -> r.isAlive() && !r.isBaby() && player.getUUID().equals(r.getOwnerUUID()) && !BY_RAT.containsKey(r.getUUID()) && r.getRespawnCountdown() <= 0)) {
				BlockPos bound = boundController(candidate, level, altarPos);
				if (bound == null) continue;
				if (candidate.isPassenger() && !(candidate.getVehicle() instanceof Player)) continue;
				double d = candidate.distanceToSqr(altarPos.getX() + 0.5D, altarPos.getY(), altarPos.getZ() + 0.5D);
				if (d < best) {
					best = d;
					rat = candidate;
					controller = bound;
				}
			}
			if (rat == null) return false;
			drops = pullFromPedestals(delegate);
			Job job = new Job(delegate, level, rat, altar, player, activation, recipe, controller, drops);
			begin(job);
			BY_DELEGATE.put(delegate, job);
			BY_RAT.put(rat.getUUID(), job);
			RatsMod.LOGGER.debug("[Rats-RSI] rat {} takes over ritual at {} ({} pedestal items)", rat.getUUID(), altarPos, drops.size());
			return true;
		} catch (Throwable t) {
			RatsMod.LOGGER.warn("[Rats-RSI] courier hand-off failed, using instant placement", t);
			if (drops != null) {
				for (Drop drop : drops) {
					ItemStack rest = insert(drop.pedestal, drop.stack);
					if (!rest.isEmpty()) delegate.rats$unplacedInputs().add(rest);
				}
			}
			return false;
		}
	}

	public static Status status(RsiCourierDelegate delegate) {
		Job job = BY_DELEGATE.get(delegate);
		return job == null ? Status.NONE : job.status;
	}

	public static void forget(RsiCourierDelegate delegate) {
		Job job = BY_DELEGATE.remove(delegate);
		if (job != null) BY_RAT.remove(job.rat.getUUID(), job);
	}

	/** RSI 取消/清理本次仪式时调用：把老鼠还没送到的材料直接放回基座，交给 RSI 自己的退款逻辑。 */
	public static void abort(RsiCourierDelegate delegate) {
		Job job = BY_DELEGATE.remove(delegate);
		if (job == null) return;
		BY_RAT.remove(job.rat.getUUID(), job);
		if (job.finished) return;
		job.finished = true;
		for (int i = job.index; i < job.drops.size(); i++) {
			Drop drop = job.drops.get(i);
			ItemStack rest = insert(drop.pedestal, drop.stack);
			if (!rest.isEmpty()) ItemHandlerHelper.giveItemToPlayer(job.player, rest);
		}
		job.index = job.drops.size();
		restoreRat(job);
	}

	/** 老鼠正在给这个祭坛送料时，别的订单不能抢用它。 */
	public static boolean isAltarBusy(RsiCourierDelegate asker, @Nullable net.minecraft.resources.ResourceLocation dim, BlockPos pos) {
		if (BY_DELEGATE.isEmpty() || pos == null) return false;
		for (Job job : BY_DELEGATE.values()) {
			if (job.finished) continue;
			if (job.altar.getBlockPos().equals(pos) && (dim == null || job.level.dimension().location().equals(dim))) return true;
		}
		return false;
	}

	public static boolean isWorking(TamedRat rat) {
		Job job = BY_RAT.get(rat.getUUID());
		return job != null && !job.finished && job.rat == rat;
	}

	// ---------------------------------------------------------------- selection helpers

	@Nullable
	private static BlockPos boundController(TamedRat rat, ServerLevel level, BlockPos altarPos) {
		ItemStack upgrade = RatUpgradeUtils.getUpgrade(rat, RatsItemRegistry.RAT_UPGRADE_RSI_COURIER.get());
		if (upgrade.isEmpty()) return null;
		GlobalPos bound = RsiCourierUpgradeItem.getBinding(upgrade);
		if (bound == null || !bound.dimension().equals(level.dimension())) return null;
		BlockPos pos = bound.pos();
		if (pos.distSqr(altarPos) > CONTROLLER_RANGE * CONTROLLER_RANGE || !level.isLoaded(pos)) return null;
		return controllerCheck.test(level.getBlockState(pos)) ? pos : null;
	}

	private static List<Drop> pullFromPedestals(RsiCourierDelegate delegate) throws ReflectiveOperationException {
		List<Drop> drops = new ArrayList<>();
		List<?> placed = delegate.rats$filledPedestals();
		if (placed == null) return drops;
		for (Object record : placed) {
			if (!(component(record, "pedestal") instanceof BlockEntity pedestal)) continue;
			if (!(component(record, "template") instanceof ItemStack template) || template.isEmpty()) continue;
			IItemHandler handler = handler(pedestal);
			if (handler == null || handler.getSlots() <= 0) continue;
			ItemStack current = handler.getStackInSlot(0);
			if (!ItemStack.isSameItemSameTags(current, template)) continue;
			ItemStack taken = handler.extractItem(0, Math.min(current.getCount(), template.getCount()), false);
			if (taken.isEmpty()) continue;
			markChanged(pedestal);
			drops.add(new Drop(pedestal, taken));
		}
		return drops;
	}

	private static Object component(Object record, String name) throws ReflectiveOperationException {
		RecordComponent[] components = record.getClass().getRecordComponents();
		if (components != null) {
			for (RecordComponent c : components) {
				if (c.getName().equals(name)) {
					Method m = c.getAccessor();
					m.setAccessible(true);
					return m.invoke(record);
				}
			}
		}
		return null;
	}

	@Nullable
	private static IItemHandler handler(BlockEntity be) {
		for (Class<?> c = be.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField("itemStackHandler");
				f.setAccessible(true);
				if (f.get(be) instanceof LazyOptional<?> lazy && lazy.resolve().orElse(null) instanceof IItemHandler h) return h;
				return null;
			} catch (NoSuchFieldException ignored) {
			} catch (ReflectiveOperationException | RuntimeException e) {
				return null;
			}
		}
		return null;
	}

	/** 放到基座上；返回放不下的部分。 */
	private static ItemStack insert(BlockEntity pedestal, ItemStack stack) {
		if (stack.isEmpty()) return ItemStack.EMPTY;
		if (pedestal.isRemoved()) return stack.copy();
		IItemHandler h = handler(pedestal);
		if (h == null || h.getSlots() <= 0 || !h.getStackInSlot(0).isEmpty()) return stack.copy();
		ItemStack rest = h.insertItem(0, stack.copy(), false);
		markChanged(pedestal);
		return rest;
	}

	private static void markChanged(BlockEntity be) {
		be.setChanged();
		if (be.getLevel() != null && !be.getLevel().isClientSide()) {
			be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
		}
	}

	// ---------------------------------------------------------------- job flow

	private static void begin(Job job) {
		TamedRat rat = job.rat;
		if (rat.getVehicle() instanceof Player) rat.stopRiding();
		job.wasSitting = rat.isOrderedToSit();
		if (job.wasSitting) rat.setOrderedToSit(false);
		ItemStack hand = rat.getMainHandItem();
		if (!isGhost(hand)) {
			job.savedHand = hand.copy();
			rat.getPersistentData().put(SAVED_HAND, hand.save(new CompoundTag()));
		}
		rat.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		rat.setTarget(null);
		rat.getNavigation().stop();
		job.player.displayClientMessage(Component.translatable("rats.rsi_courier.start", rat.getName()).withStyle(ChatFormatting.AQUA), true);
	}

	private static void restoreRat(Job job) {
		TamedRat rat = job.rat;
		ItemStack hand = rat.getMainHandItem();
		if (hand.isEmpty() || isGhost(hand)) rat.setItemInHand(InteractionHand.MAIN_HAND, job.savedHand);
		rat.getPersistentData().remove(SAVED_HAND);
		if (job.wasSitting && rat.isAlive()) {
			rat.getNavigation().stop();
			rat.setOrderedToSit(true);
		}
	}

	private static boolean isGhost(ItemStack stack) {
		return !stack.isEmpty() && stack.getTag() != null && stack.getTag().getBoolean(GHOST_TAG);
	}

	private static void hold(Job job, ItemStack stack) {
		if (stack.isEmpty()) {
			job.rat.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return;
		}
		ItemStack ghost = stack.copy();
		ghost.getOrCreateTag().putBoolean(GHOST_TAG, true);
		job.rat.setItemInHand(InteractionHand.MAIN_HAND, ghost);
	}

	private static void holdNext(Job job) {
		hold(job, job.index < job.drops.size() ? job.drops.get(job.index).stack : job.activation);
	}

	private static void setStage(Job job, Stage stage) {
		job.stage = stage;
		job.stageTicks = 0;
		job.noPathTicks = 0;
	}

	private static void tick(Job job) {
		job.age++;
		job.stageTicks++;
		TamedRat rat = job.rat;
		if (!rat.isAlive() || rat.isRemoved() || rat.level() != job.level) {
			completeInstantly(job, "rat unavailable");
			return;
		}
		if (job.age > TOTAL_TIMEOUT) {
			completeInstantly(job, "timeout");
			return;
		}
		switch (job.stage) {
			case TO_CONTROLLER -> {
				if (moveTo(job, job.controller)) setStage(job, Stage.LOADING);
			}
			case LOADING -> {
				lookAt(rat, job.controller);
				if (job.stageTicks == 1) {
					job.level.playSound(null, job.controller, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 0.9F, 1.5F);
					job.level.sendParticles(ParticleTypes.REVERSE_PORTAL, job.controller.getX() + 0.5D, job.controller.getY() + 1.0D, job.controller.getZ() + 0.5D, 24, 0.35D, 0.3D, 0.35D, 0.02D);
				}
				if (job.stageTicks >= 18) {
					job.level.playSound(null, rat.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6F, 1.3F);
					holdNext(job);
					setStage(job, job.drops.isEmpty() ? Stage.TO_ALTAR : Stage.TO_PEDESTAL);
				}
			}
			case TO_PEDESTAL -> {
				Drop drop = job.drops.get(job.index);
				if (drop.pedestal.isRemoved()) {
					job.delegate.rats$unplacedInputs().add(drop.stack.copy());
					advanceAfterPlace(job);
				} else if (moveTo(job, drop.pedestal.getBlockPos())) {
					setStage(job, Stage.PLACING);
				}
			}
			case PLACING -> {
				Drop drop = job.drops.get(job.index);
				lookAt(rat, drop.pedestal.getBlockPos());
				if (job.stageTicks >= 8) {
					place(job, drop, true);
					advanceAfterPlace(job);
				}
			}
			case TO_ALTAR -> {
				if (moveTo(job, job.altar.getBlockPos())) setStage(job, Stage.ACTIVATING);
			}
			case ACTIVATING -> {
				lookAt(rat, job.altar.getBlockPos());
				if (job.stageTicks >= 10) finishStart(job, true);
			}
		}
	}

	private static void advanceAfterPlace(Job job) {
		job.index++;
		holdNext(job);
		setStage(job, job.index < job.drops.size() ? Stage.TO_PEDESTAL : Stage.TO_ALTAR);
	}

	private static void place(Job job, Drop drop, boolean effects) {
		ItemStack rest = insert(drop.pedestal, drop.stack);
		if (!rest.isEmpty()) job.delegate.rats$unplacedInputs().add(rest);
		if (effects && rest.getCount() < drop.stack.getCount()) {
			BlockPos p = drop.pedestal.getBlockPos();
			job.level.playSound(null, p, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.NEUTRAL, 0.8F, 1.2F);
			job.level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, drop.stack), p.getX() + 0.5D, p.getY() + 1.1D, p.getZ() + 0.5D, 6, 0.15D, 0.1D, 0.15D, 0.03D);
			job.level.sendParticles(ParticleTypes.WITCH, p.getX() + 0.5D, p.getY() + 1.2D, p.getZ() + 0.5D, 4, 0.2D, 0.15D, 0.2D, 0.0D);
		}
	}

	/** 返回 true 表示已走到目标旁边。 */
	private static boolean moveTo(Job job, BlockPos target) {
		TamedRat rat = job.rat;
		double dx = rat.getX() - (target.getX() + 0.5D);
		double dz = rat.getZ() - (target.getZ() + 0.5D);
		double dy = rat.getY() - target.getY();
		double horizontal = dx * dx + dz * dz;
		if (horizontal <= 2.25D * 2.25D && dy > -1.5D && dy < 2.5D) {
			rat.getNavigation().stop();
			return true;
		}
		if (job.stageTicks > LEG_TIMEOUT) {
			completeInstantly(job, "stuck on the way to " + target);
			return false;
		}
		if (job.stageTicks == 1 || job.stageTicks % 10 == 0 || rat.getNavigation().isDone()) {
			boolean pathing = rat.getNavigation().moveTo(target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D, SPEED);
			if (!pathing && ++job.noPathTicks > NO_PATH_TIMEOUT) {
				completeInstantly(job, "no path to " + target);
			}
			if (pathing) job.noPathTicks = 0;
		}
		return false;
	}

	private static void lookAt(TamedRat rat, BlockPos pos) {
		rat.getNavigation().stop();
		rat.getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D);
	}

	/** 兜底：剩余材料直接瞬移上基座并开仪式（原模组行为）。 */
	private static void completeInstantly(Job job, String reason) {
		if (job.finished) return;
		RatsMod.LOGGER.debug("[Rats-RSI] courier fallback ({}), placing remaining {} items instantly", reason, job.drops.size() - job.index);
		for (int i = job.index; i < job.drops.size(); i++) place(job, job.drops.get(i), false);
		job.index = job.drops.size();
		finishStart(job, false);
	}

	private static void finishStart(Job job, boolean byRat) {
		if (job.finished) return;
		job.finished = true;
		BY_RAT.remove(job.rat.getUUID(), job);
		restoreRat(job);
		boolean ok;
		DIRECT.set(true);
		try {
			ok = job.delegate.rats$startRitualDirect(job.altar, job.player, job.activation, job.recipe);
		} catch (Throwable t) {
			RatsMod.LOGGER.error("[Rats-RSI] starting ritual after courier delivery failed", t);
			ok = false;
		} finally {
			DIRECT.set(false);
		}
		job.status = ok ? Status.STARTED : Status.FAILED;
		if (ok && byRat) {
			BlockPos p = job.altar.getBlockPos();
			job.level.playSound(null, p, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 0.8F);
			job.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX() + 0.5D, p.getY() + 1.1D, p.getZ() + 0.5D, 16, 0.3D, 0.2D, 0.3D, 0.01D);
			job.player.displayClientMessage(Component.translatable("rats.rsi_courier.done", job.rat.getName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
		}
	}

	// ---------------------------------------------------------------- events

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || BY_DELEGATE.isEmpty()) return;
		for (Job job : List.copyOf(BY_DELEGATE.values())) {
			if (!job.finished) {
				tick(job);
			} else if (++job.age > TOTAL_TIMEOUT * 10) {
				BY_DELEGATE.remove(job.delegate, job);
			}
		}
	}

	public static void completeAll(String reason) {
		for (Job job : List.copyOf(BY_DELEGATE.values())) completeInstantly(job, reason);
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		completeAll("server stopping");
		BY_DELEGATE.clear();
		BY_RAT.clear();
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof TamedRat rat && !rat.level().isClientSide()) {
			Job job = BY_RAT.get(rat.getUUID());
			if (job != null && job.rat == rat) completeInstantly(job, "rat died");
		}
	}

	@SubscribeEvent
	public static void onDrops(LivingDropsEvent event) {
		if (event.getEntity() instanceof TamedRat) event.getDrops().removeIf(e -> isGhost(e.getItem()));
	}

	@SubscribeEvent
	public static void onJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()) return;
		if (event.getEntity() instanceof ItemEntity item && isGhost(item.getItem())) {
			event.setCanceled(true);
			return;
		}
		if (event.getEntity() instanceof TamedRat rat) {
			if (GOAL_ADDED.add(rat)) rat.goalSelector.addGoal(0, new RsiCourierGoal(rat));
			if (!BY_RAT.containsKey(rat.getUUID()) && isGhost(rat.getMainHandItem())) {
				CompoundTag saved = rat.getPersistentData().getCompound(SAVED_HAND);
				rat.setItemInHand(InteractionHand.MAIN_HAND, saved.isEmpty() ? ItemStack.EMPTY : ItemStack.of(saved));
				rat.getPersistentData().remove(SAVED_HAND);
			}
		}
	}
}
