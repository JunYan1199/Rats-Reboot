package com.github.alexthe666.rats.server.catcatch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.EnumSet;

/**
 * 挎包装满 9 只且绑定了存货容器：猫实际走到容器旁边，把鼠片一张张存进去。
 * 存完继续捕鼠；容器放不下（或不可达/被拆/不在同一维度）就保留剩余鼠片，回主人身边等待，30 秒后再试。
 */
public class CatDepositGoal extends Goal {
	public static final int RETRY_TICKS = 600;
	private static final int GIVE_UP_TICKS = 400;
	private final Cat cat;
	private BlockPos target;
	private int ticks;
	private int repath;

	public CatDepositGoal(Cat cat) {
		this.cat = cat;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	private CatSatchel satchelIfDue() {
		CatSatchel s = CatSatchel.equipped(this.cat);
		if (s == null || !s.isFull() || s.getDepot() == null) return null;
		if (!this.cat.isTame() || this.cat.isOrderedToSit() || this.cat.isPassenger() || this.cat.isLeashed()) return null;
		if (s.depotBlocked && this.cat.level().getGameTime() < s.depotRetryAt) return null;
		return s;
	}

	@Override
	public boolean canUse() {
		CatSatchel s = this.satchelIfDue();
		if (s == null) return false;
		GlobalPos depot = s.getDepot();
		if (!depot.dimension().equals(this.cat.level().dimension())) {
			this.block(s, false);
			return false;
		}
		this.target = depot.pos();
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.target != null && this.satchelIfDue() != null;
	}

	@Override
	public void start() {
		this.ticks = 0;
		this.repath = 0;
		this.cat.setInSittingPose(false);
	}

	@Override
	public void stop() {
		this.target = null;
		this.cat.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		CatSatchel s = CatSatchel.equipped(this.cat);
		if (s == null || this.target == null) return;
		this.ticks++;
		double cx = this.target.getX() + 0.5D, cy = this.target.getY() + 0.5D, cz = this.target.getZ() + 0.5D;
		this.cat.getLookControl().setLookAt(cx, cy, cz);
		double dx = this.cat.getX() - cx, dz = this.cat.getZ() - cz, dy = this.cat.getY() - cy;
		boolean near = dx * dx + dz * dz <= 2.6D * 2.6D && Math.abs(dy) <= 2.5D;
		if (near) {
			this.deposit(s);
			this.target = null;
			return;
		}
		if (!this.cat.level().isLoaded(this.target) || this.ticks > GIVE_UP_TICKS) {
			this.block(s, true);
			this.target = null;
			return;
		}
		if (--this.repath <= 0) {
			this.repath = 10;
			if (!this.cat.getNavigation().moveTo(cx, this.target.getY(), cz, 1.2D) && this.ticks > 100) {
				this.block(s, true);
				this.target = null;
			}
		}
	}

	private void deposit(CatSatchel s) {
		BlockEntity be = this.cat.level().getBlockEntity(this.target);
		IItemHandler handler = null;
		if (be != null) {
			handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
			if (handler == null) handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
		}
		if (handler == null) {
			this.block(s, true);
			return;
		}
		int moved = 0;
		for (int i = 0; i < CatSatchel.SIZE; i++) {
			ItemStack slice = s.items().get(i);
			if (slice.isEmpty()) continue;
			ItemStack rest = ItemHandlerHelper.insertItemStacked(handler, slice.copy(), false);
			if (rest.isEmpty()) {
				s.items().set(i, ItemStack.EMPTY);
				moved++;
			}
		}
		if (moved > 0) {
			be.setChanged();
			this.cat.level().playSound(null, this.target, SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.NEUTRAL, 0.8F, 1.0F);
			this.cat.level().playSound(null, this.cat.blockPosition(), SoundEvents.CAT_AMBIENT, SoundSource.NEUTRAL, 0.6F, 1.2F);
		}
		if (s.isEmpty()) {
			s.depotBlocked = false;
			s.depotRetryAt = 0;
		} else {
			// 容器满了，剩余鼠片留在挎包里
			this.block(s, true);
		}
	}

	private void block(CatSatchel s, boolean notify) {
		boolean first = !s.depotBlocked;
		s.depotBlocked = true;
		s.depotRetryAt = this.cat.level().getGameTime() + RETRY_TICKS;
		if (notify && first) {
			LivingEntity owner = this.cat.getOwner();
			if (owner instanceof Player p) {
				p.displayClientMessage(Component.translatable("rats.cat_satchel.depot_blocked", this.cat.getName()).withStyle(ChatFormatting.GOLD), true);
			}
		}
	}
}
