package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatHouseBlock;
import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RatReturnToHouseGoal
extends Goal {
    private static final double ENTRY_ACCEPT_DISTANCE_SQR = 9.0;
    private static final double HOUSE_ACCEPT_DISTANCE_SQR = 6.25;
    private final TamedRat rat;
    private final double speedModifier;
    private BlockPos housePos;
    private Vec3 entryCenter;
    private int nextStartTick;

    public RatReturnToHouseGoal(TamedRat rat, double speedModifier) {
        this.rat = rat;
        this.speedModifier = speedModifier;
        this.nextStartTick = rat.getRandom().nextInt(20);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        try {
            RatHouseBlockEntity houseEntity;
            if (this.nextStartTick > 0) {
                --this.nextStartTick;
                return false;
            }
            this.nextStartTick = this.adjustedTickDelay(10 + this.rat.getRandom().nextInt(11));
            if (!this.rat.canMove() || this.rat.isInCage() || this.rat.isPassenger()) {
                return false;
            }
            if (!RatDaySchedule.shouldSleep(this.rat.level())) {
                return false;
            }
            Optional<GlobalPos> house = this.rat.getBoundHousePos();
            if (house.isEmpty() || !house.get().dimension().equals(this.rat.level().dimension())) {
                return false;
            }
            BlockPos pos = house.get().pos();
            if (this.rat.distanceToSqr(Vec3.atCenterOf((Vec3i)pos)) > (double)RatConfig.pathfindHomeWorkDistance * (double)RatConfig.pathfindHomeWorkDistance) {
                this.rat.setLastPathFailure("\u0434\u043e\u043c\u0438\u043a " + String.valueOf(pos) + " \u0434\u0430\u043b\u044c\u0448\u0435 \u043b\u0438\u043c\u0438\u0442\u0430 " + RatConfig.pathfindHomeWorkDistance);
                return false;
            }
            BlockState state = this.rat.level().getBlockState(pos);
            if (!(state.getBlock() instanceof RatHouseBlock)) {
                RatsMod.LOGGER.warn("\u041a\u0440\u044b\u0441\u0430 {} \u043f\u043e\u0442\u0435\u0440\u044f\u043b\u0430 \u043f\u0440\u0438\u0432\u044f\u0437\u0430\u043d\u043d\u044b\u0439 \u0434\u043e\u043c\u0438\u043a {}: \u0431\u043b\u043e\u043a \u043d\u0435 rat_house", this.rat.getUUID(), pos);
                this.rat.setBoundHousePos(null);
                return false;
            }
            BlockEntity blockEntity = this.rat.level().getBlockEntity(pos);
            if (!(blockEntity instanceof RatHouseBlockEntity) || (houseEntity = (RatHouseBlockEntity)blockEntity).hasStoredRat() || !houseEntity.canAcceptRat(this.rat)) {
                return false;
            }
            this.housePos = pos;
            this.entryCenter = RatHouseBlock.getEntryCenter(state, pos);
            this.rat.clearLastPathFailure();
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c \u043f\u0443\u0442\u044c \u043a\u0440\u044b\u0441\u044b {} \u043a \u0434\u043e\u043c\u0438\u043a\u0443: {}", this.rat.getUUID(), exception.getMessage(), exception);
            throw exception;
        }
    }

    public boolean canContinueToUse() {
        return this.rat.isAlive() && this.housePos != null && this.entryCenter != null && RatDaySchedule.shouldSleep(this.rat.level()) && (!this.rat.getNavigation().isDone() || this.isCloseEnoughToEnter());
    }

    public void start() {
        try {
            if (this.entryCenter == null) {
                throw new IllegalStateException("\u041d\u0435 \u0437\u0430\u0434\u0430\u043d \u0432\u0445\u043e\u0434 \u0434\u043e\u043c\u0438\u043a\u0430");
            }
            boolean pathStarted = this.rat.moveSmoothlyTo(this.entryCenter.x, this.entryCenter.y, this.entryCenter.z, this.speedModifier);
            if (!pathStarted) {
                this.rat.setLastPathFailure("\u043d\u0435\u0442 \u043f\u0443\u0442\u0438 \u043a \u0434\u043e\u043c\u0438\u043a\u0443 " + String.valueOf(this.housePos));
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0447\u0430\u0442\u044c \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u0435 \u043a\u0440\u044b\u0441\u044b {} \u043a \u0434\u043e\u043c\u0438\u043a\u0443 {}: {}", this.rat.getUUID(), this.housePos, exception.getMessage(), exception);
            throw exception;
        }
    }

    public void tick() {
        try {
            RatHouseBlockEntity houseEntity;
            if (this.housePos == null || this.entryCenter == null) {
                return;
            }
            if (!this.isCloseEnoughToEnter()) {
                return;
            }
            BlockEntity blockEntity = this.rat.level().getBlockEntity(this.housePos);
            if (blockEntity instanceof RatHouseBlockEntity && (houseEntity = (RatHouseBlockEntity)blockEntity).storeRat(this.rat)) {
                this.rat.clearLastPathFailure();
                this.rat.getNavigation().stop();
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043c\u0435\u0441\u0442\u0438\u0442\u044c \u043a\u0440\u044b\u0441\u0443 {} \u0432 \u0434\u043e\u043c\u0438\u043a {}: {}", this.rat.getUUID(), this.housePos, exception.getMessage(), exception);
            throw exception;
        }
    }

    private boolean isCloseEnoughToEnter() {
        try {
            if (this.housePos == null || this.entryCenter == null) {
                return false;
            }
            if (this.rat.distanceToSqr(this.entryCenter) <= 9.0) {
                return true;
            }
            if (this.rat.distanceToSqr(Vec3.atCenterOf((Vec3i)this.housePos)) <= 6.25) {
                return true;
            }
            BlockPos entryBlock = BlockPos.containing((Position)this.entryCenter);
            BlockPos ratPos = this.rat.blockPosition();
            return ratPos.distManhattan((Vec3i)entryBlock) <= 2 && Math.abs(this.rat.getY() - this.entryCenter.y) <= 2.0;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0431\u043b\u0438\u0437\u043e\u0441\u0442\u044c \u043a\u0440\u044b\u0441\u044b {} \u043a\u043e \u0432\u0445\u043e\u0434\u0443 \u0434\u043e\u043c\u0438\u043a\u0430 {}", this.rat.getUUID(), this.housePos, exception);
            throw exception;
        }
    }

    public void stop() {
        this.housePos = null;
        this.entryCenter = null;
    }
}

