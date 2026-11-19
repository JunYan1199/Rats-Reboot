package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.compat.farmersdelight.FarmersDelightCompat;
import com.github.alexthe666.rats.registry.RatsItemRegistry;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUpgradeUtils;
import com.github.alexthe666.rats.server.misc.RatUtils;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class FarmersDelightChefGoal
extends Goal {
    private static final double WORK_REACH_EXPANSION = 1.0;
    private static final double SEAT_WORK_REACH = 8.0;
    private final TamedRat rat;
    private BlockPos workPos;
    private BlockPos pickupPos;
    private BlockPos depositPos;
    private int nextStartTick;

    public FarmersDelightChefGoal(TamedRat rat) {
        try {
            this.rat = rat;
            this.nextStartTick = rat.getRandom().nextInt(30);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u0446\u0435\u043b\u044c \u0448\u0435\u0444\u0430 Farmer's Delight \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", rat.getUUID(), exception);
            throw exception;
        }
    }

    public boolean canUse() {
        try {
            if (this.nextStartTick > 0) {
                --this.nextStartTick;
                return false;
            }
            this.nextStartTick = this.adjustedTickDelay(20 + this.rat.getRandom().nextInt(20));
            if (!RatsMod.FARMERS_DELIGHT_LOADED || !this.canRatWorkAsChef()) {
                return false;
            }
            Optional<GlobalPos> work = this.rat.getCreateWorkPos();
            Optional<GlobalPos> pickup = this.rat.getPickupPos();
            Optional<GlobalPos> deposit = this.rat.getDepositPos();
            if (work.isEmpty() || pickup.isEmpty() || deposit.isEmpty()) {
                return false;
            }
            if (!(work.get().dimension().equals(this.rat.level().dimension()) && pickup.get().dimension().equals(this.rat.level().dimension()) && deposit.get().dimension().equals(this.rat.level().dimension()))) {
                return false;
            }
            this.workPos = work.get().pos();
            this.pickupPos = pickup.get().pos();
            this.depositPos = deposit.get().pos();
            if (RatUtils.isBlockProtected(this.rat.level(), this.workPos, this.rat) || RatUtils.isBlockProtected(this.rat.level(), this.pickupPos, this.rat) || RatUtils.isBlockProtected(this.rat.level(), this.depositPos, this.rat)) {
                return false;
            }
            BlockEntity blockEntity = this.rat.level().getBlockEntity(this.workPos);
            return blockEntity != null && FarmersDelightCompat.isSupportedWorkBlock(blockEntity);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0437\u0430\u043f\u0443\u0441\u043a \u0446\u0435\u043b\u0438 \u0448\u0435\u0444\u0430 Farmer's Delight", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public boolean canContinueToUse() {
        try {
            if (this.workPos == null || !this.canRatWorkAsChef()) {
                return false;
            }
            BlockEntity blockEntity = this.rat.level().getBlockEntity(this.workPos);
            return blockEntity != null && FarmersDelightCompat.isSupportedWorkBlock(blockEntity);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043f\u0440\u043e\u0434\u043e\u043b\u0436\u0435\u043d\u0438\u0435 \u0446\u0435\u043b\u0438 \u0448\u0435\u0444\u0430 Farmer's Delight", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public void start() {
        try {
            this.rat.isCurrentlyWorking = true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043d\u0430\u0447\u0430\u0442\u044c \u0446\u0435\u043b\u044c \u0448\u0435\u0444\u0430 Farmer's Delight", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public void stop() {
        try {
            this.rat.isCurrentlyWorking = false;
            this.workPos = null;
            this.pickupPos = null;
            this.depositPos = null;
            this.nextStartTick = 10;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u0446\u0435\u043b\u044c \u0448\u0435\u0444\u0430 Farmer's Delight", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public void tick() {
        try {
            if (this.workPos == null || this.pickupPos == null || this.depositPos == null) {
                this.stop();
                return;
            }
            if (this.canWorkFromCurrentSeat()) {
                this.performChefCycle();
                return;
            }
            if (this.rat.isPassenger()) {
                this.rat.stopRiding();
            }
            Vec3 movePos = Vec3.atBottomCenterOf((Vec3i)this.workPos.relative(this.rat.createWorkFacing));
            this.rat.moveSmoothlyTo(movePos.x(), movePos.y(), movePos.z(), this.rat.getHungerAdjustedWorkSpeed(1.25));
            double distance = this.rat.distanceToSqr(movePos.x(), movePos.y(), movePos.z());
            double reach = this.rat.getRatHarvestDistance(1.0);
            if (distance <= reach * reach) {
                this.performChefCycle();
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0432\u044b\u043f\u043e\u043b\u043d\u0438\u0442\u044c \u0448\u0430\u0433 \u0446\u0435\u043b\u0438 \u0448\u0435\u0444\u0430 Farmer's Delight", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private boolean canRatWorkAsChef() {
        try {
            return this.rat.canMove() && this.rat.canWorkBySchedule() && this.rat.canWorkFromHunger() && !this.rat.isInCage() && this.rat.getTarget() == null && this.rat.getCommand() == RatCommand.HARVEST && RatUpgradeUtils.hasUpgrade(this.rat, (Item)RatsItemRegistry.RAT_UPGRADE_CHEF.get());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0443\u0441\u043b\u043e\u0432\u0438\u044f \u0440\u0430\u0431\u043e\u0442\u044b \u0448\u0435\u0444\u0430 Farmer's Delight", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private void performChefCycle() {
        try {
            boolean worked = FarmersDelightCompat.tryRunChefCycle(this.rat, this.workPos, this.pickupPos, this.depositPos);
            if (!worked) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0432\u044b\u043f\u043e\u043b\u043d\u0438\u043b\u0430 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435 Farmer's Delight \u0443 {}", this.rat.getUUID(), this.workPos);
            }
            this.stop();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u0432\u044b\u043f\u043e\u043b\u043d\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0447\u0438\u0439 \u0446\u0438\u043a\u043b Farmer's Delight \u0443 {}", this.rat.getUUID(), this.workPos, exception);
            throw exception;
        }
    }

    private boolean canWorkFromCurrentSeat() {
        try {
            return this.isMountedOnSeat() && this.workPos != null && this.rat.distanceToSqr(Vec3.atCenterOf((Vec3i)this.workPos)) <= 64.0;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u0443 \u0441 \u0441\u0438\u0434\u0435\u043d\u044c\u044f Farmer's Delight", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private boolean isMountedOnSeat() {
        try {
            Entity vehicle = this.rat.getVehicle();
            if (vehicle == null) {
                return false;
            }
            String name = vehicle.getClass().getName().toLowerCase();
            return name.contains("seat");
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041a\u0440\u044b\u0441\u0430 {} \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0441\u0438\u0434\u0435\u043d\u044c\u0435 Farmer's Delight", this.rat.getUUID(), exception);
            throw exception;
        }
    }
}

