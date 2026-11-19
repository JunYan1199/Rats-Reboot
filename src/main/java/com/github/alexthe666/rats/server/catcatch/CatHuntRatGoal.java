package com.github.alexthe666.rats.server.catcatch;

import com.github.alexthe666.rats.server.entity.rat.Rat;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * 佩戴挎包的猫：搜索 16 格内可捕获的普通成年野生老鼠，追上后扑咬活捉。
 * 没有冷却，挎包有空位就继续；挎包满了立即停止（不会再咬，鼠片不会丢）。
 */
public class CatHuntRatGoal extends Goal {
	private final Cat cat;
	private Rat target;
	private int searchCooldown;
	private int repath;
	private int chaseTicks;
	private int pounceCooldown;

	public CatHuntRatGoal(Cat cat) {
		this.cat = cat;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	private boolean ready() {
		CatSatchel s = CatSatchel.equipped(this.cat);
		return s != null && !s.isFull() && this.cat.isTame() && !this.cat.isOrderedToSit()
				&& !this.cat.isPassenger() && !this.cat.isLeashed() && !this.cat.isBaby();
	}

	@Override
	public boolean canUse() {
		if (!this.ready()) return false;
		if (this.searchCooldown-- > 0) return false;
		this.searchCooldown = 10;
		this.target = this.findTarget();
		return this.target != null;
	}

	private Rat findTarget() {
		double r = CatCatch.SEARCH_RADIUS;
		List<Rat> rats = this.cat.level().getEntitiesOfClass(Rat.class, this.cat.getBoundingBox().inflate(r, 6.0D, r),
				rat -> CatCatch.isCatchable(rat) && rat.distanceToSqr(this.cat) <= r * r);
		rats.sort(Comparator.comparingDouble(this.cat::distanceToSqr));
		for (Rat rat : rats) {
			if (this.cat.getNavigation().createPath(rat, 0) != null || this.cat.distanceToSqr(rat) < 4.0D) return rat;
		}
		return null;
	}

	@Override
	public boolean canContinueToUse() {
		return this.ready() && this.target != null && CatCatch.isCatchable(this.target)
				&& this.target.distanceToSqr(this.cat) <= (CatCatch.SEARCH_RADIUS + 8) * (CatCatch.SEARCH_RADIUS + 8)
				&& this.chaseTicks < 600;
	}

	@Override
	public void start() {
		this.repath = 0;
		this.chaseTicks = 0;
		this.cat.setInSittingPose(false);
		this.cat.setSprinting(true);
		this.cat.getNavigation().moveTo(this.target, 1.35D);
	}

	@Override
	public void stop() {
		this.target = null;
		this.cat.setSprinting(false);
		this.cat.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (this.target == null) return;
		this.chaseTicks++;
		this.cat.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
		double dist = this.cat.distanceToSqr(this.target);
		if (--this.repath <= 0) {
			this.repath = 4 + this.cat.getRandom().nextInt(4);
			this.cat.getNavigation().moveTo(this.target, 1.35D);
		}
		if (this.pounceCooldown > 0) this.pounceCooldown--;
		// 扑：靠近到 3 格内时向老鼠跃起
		if (dist < 9.0D && dist > 1.2D && this.cat.onGround() && this.pounceCooldown <= 0) {
			Vec3 d = new Vec3(this.target.getX() - this.cat.getX(), 0.0D, this.target.getZ() - this.cat.getZ());
			if (d.lengthSqr() > 1.0E-7D) d = d.normalize().scale(0.45D);
			this.cat.setDeltaMovement(d.x, 0.32D, d.z);
			this.pounceCooldown = 15;
		}
		// 咬：进入攻击距离后扑咬，造成伤害即活捉
		double reach = this.cat.getBbWidth() * 2.0F * this.cat.getBbWidth() * 2.0F + this.target.getBbWidth() + 0.6D;
		if (dist <= reach) {
			CatSatchel s = CatSatchel.equipped(this.cat);
			if (s != null && CatCatch.biteAndCapture(this.cat, this.target, s)) {
				this.target = null;
				this.searchCooldown = 0;
			}
		}
	}
}
