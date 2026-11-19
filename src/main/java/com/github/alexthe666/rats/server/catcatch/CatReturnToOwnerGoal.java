package com.github.alexthe666.rats.server.catcatch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.EnumSet;

/**
 * 挎包装满、但没有绑定容器（或容器无法接收）时：回到主人脚边停下，等主人潜行右键取鼠片。
 * 取出鼠片腾出空位后本目标自动结束，捕猎目标接手。
 */
public class CatReturnToOwnerGoal extends Goal {
	private final Cat cat;
	private LivingEntity owner;
	private int repath;

	public CatReturnToOwnerGoal(Cat cat) {
		this.cat = cat;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	private boolean shouldWait() {
		CatSatchel s = CatSatchel.equipped(this.cat);
		if (s == null || !s.isFull() || !this.cat.isTame() || this.cat.isOrderedToSit() || this.cat.isPassenger() || this.cat.isLeashed()) return false;
		boolean canDeposit = s.getDepot() != null && s.getDepot().dimension().equals(this.cat.level().dimension())
				&& (!s.depotBlocked || this.cat.level().getGameTime() >= s.depotRetryAt);
		if (canDeposit) return false;
		LivingEntity o = this.cat.getOwner();
		return o != null && o.isAlive() && !o.isSpectator() && o.level() == this.cat.level();
	}

	@Override
	public boolean canUse() {
		if (!this.shouldWait()) return false;
		this.owner = this.cat.getOwner();
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.shouldWait() && this.cat.getOwner() == this.owner;
	}

	@Override
	public void start() {
		this.repath = 0;
		this.cat.setInSittingPose(false);
	}

	@Override
	public void stop() {
		this.owner = null;
		this.cat.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.owner == null) return;
		this.cat.getLookControl().setLookAt(this.owner, 10.0F, this.cat.getMaxHeadXRot());
		double d = this.cat.distanceToSqr(this.owner);
		if (d <= 2.2D * 2.2D) {
			// 已在主人脚边：停下等待
			this.cat.getNavigation().stop();
			return;
		}
		if (d >= 20.0D * 20.0D) {
			this.teleportNearOwner();
			return;
		}
		if (--this.repath <= 0) {
			this.repath = 10;
			this.cat.getNavigation().moveTo(this.owner, 1.2D);
		}
	}

	private void teleportNearOwner() {
		BlockPos base = this.owner.blockPosition();
		for (int i = 0; i < 10; i++) {
			int x = base.getX() + this.cat.getRandom().nextInt(5) - 2;
			int z = base.getZ() + this.cat.getRandom().nextInt(5) - 2;
			int y = base.getY() + this.cat.getRandom().nextInt(3) - 1;
			if (Math.abs(x - base.getX()) < 1 && Math.abs(z - base.getZ()) < 1) continue;
			BlockPos p = new BlockPos(x, y, z);
			if (WalkNodeEvaluator.getBlockPathTypeStatic(this.cat.level(), p.mutable()) != BlockPathTypes.WALKABLE) continue;
			if (!this.cat.level().noCollision(this.cat, this.cat.getBoundingBox().move(p.getCenter().subtract(this.cat.position()).add(0, -0.5D, 0)))) continue;
			this.cat.moveTo(x + 0.5D, y, z + 0.5D, this.cat.getYRot(), this.cat.getXRot());
			this.cat.getNavigation().stop();
			return;
		}
	}
}
