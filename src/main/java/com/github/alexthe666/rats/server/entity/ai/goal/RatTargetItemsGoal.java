package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.server.entity.rat.RatCommand;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class RatTargetItemsGoal extends Goal {
	private final TamedRat rat;
	@Nullable
	private ItemEntity targetItem = null;
	private net.minecraft.core.BlockPos lastTargetPos;

	public RatTargetItemsGoal(TamedRat rat) {
		this.rat = rat;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		//we only want the rat to pick up items if it can actually move around
		if (!this.rat.canMove() || this.rat.isInCage() || this.rat.isCurrentlyWorking)
			return false;

		if (!canCollect()) return false;
		List<ItemEntity> items = this.rat.level().getEntitiesOfClass(ItemEntity.class, this.getTargetableArea(),
				item -> (item.onGround() || item.isInWater()) && canHold(item.getItem()));
		items.sort(Comparator.comparingDouble(this.rat::distanceToSqr));
		for (ItemEntity item : items) {
			Path path = this.rat.getNavigation().createPath(item, 1);
			if (path != null && path.canReach()) {
				this.targetItem = item;
				return true;
			}
		}
		return false;
	}

	private boolean canCollect() {
        if(rat.getCommand()==RatCommand.SPELL_COMBAT)return false;
		if (!rat.canMove() || rat.isBaby() || rat.getTarget() != null || rat.isCurrentlyWorking) return false;
		return rat.isTargetCommand() || (rat.isTame() && (rat.getDepositPos().isEmpty()
				|| rat.getCommand() == RatCommand.WANDER || rat.getCommand() == RatCommand.FOLLOW));
	}

	private boolean canHold(ItemStack stack) {
		return this.rat.shouldCollectItem(stack) && this.rat.canRatPickupItem(stack)
				&& this.rat.cargoInventory().collect(stack, 1, true) > 0;
	}

	protected AABB getTargetableArea() {
		if (this.rat.getRadiusCenter().isPresent()) {
			Vec3 center = new Vec3(this.rat.getSearchCenter().getX() + 0.5D, this.rat.getSearchCenter().getY() + 0.5D, this.rat.getSearchCenter().getZ() + 0.5D);
			double radius = this.rat.getRadius();
			AABB aabb = new AABB(-radius, -radius, -radius, radius, radius, radius);
			return aabb.move(center);
		}
		return this.rat.getBoundingBox().inflate(RatConfig.defaultRatRadius);
	}

	@Override
	public boolean canContinueToUse() {
		if (this.targetItem == null || this.targetItem.isRemoved() || !this.rat.isAlive() || !canCollect() || !canHold(this.targetItem.getItem())) return false;
		if (withinPickupRange() || this.rat.isTubeTransitActive()) return true;
		return !this.rat.getNavigation().isDone() && !this.rat.getNavigation().isStuck();
	}

	private boolean withinPickupRange() {
		return this.rat.distanceToSqr(this.targetItem) < this.rat.getRatHarvestDistance(1.0D);
	}

	@Override
	public void start() {
		if (this.targetItem != null) {
			this.lastTargetPos = this.targetItem.blockPosition();
			this.rat.getNavigation().moveTo(this.targetItem, 1.25D);
		}
	}

	@Override
	public void stop() {
		if (this.targetItem != null) this.rat.getNavigation().stop();
		this.targetItem = null;
		this.lastTargetPos = null;
	}

	@Override
	public void tick() {
		if (this.targetItem != null && !this.targetItem.isRemoved()) {
			if (withinPickupRange()) {
				ItemStack source = this.targetItem.getItem();
				if (canCollect() && canHold(source)) {
					int limit = RatUpgradeUtils.hasUpgrade(this.rat, RatsItemRegistry.RAT_UPGRADE_PLATTER.get()) ? source.getCount() : 1;
					int moved = this.rat.cargoInventory().collect(source, limit, false);
					ItemStack remainder = source.copyWithCount(source.getCount() - moved);
					if (remainder.isEmpty()) this.targetItem.discard();
					else this.targetItem.setItem(remainder);
				}
				this.stop();
			} else if (!this.targetItem.blockPosition().equals(this.lastTargetPos)) {
				this.lastTargetPos = this.targetItem.blockPosition();
				this.rat.getNavigation().moveTo(this.targetItem, 1.25D);
			}
		}
	}
}