package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.compat.create.CreateCompat;
import com.github.alexthe666.rats.server.entity.ai.goal.RatWorkGoal;
import com.github.alexthe666.rats.server.entity.rat.RatCommand;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import com.github.alexthe666.rats.server.misc.RebornCapabilities;
import net.minecraftforge.items.IItemHandler;

public class CreateWorkerGoal
extends Goal
implements RatWorkGoal {
    private static final double REMOTE_BELT_WORK_DISTANCE = 4.5;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Direction[][] ORDERED_SIDES = CreateWorkerGoal.createOrderedSides();
    private final TamedRat rat;
    private BlockPos targetBlock;
    private int nextStartTick;
    private int nextApplicationPickupTick;
    private BlockPos lastReachablePos;
    private int lastReachableTick = -1000;
    private boolean lastReachableResult;
    private boolean isCurrentlyWorking;

    public CreateWorkerGoal(TamedRat rat) {
        try {
            this.rat = rat;
            this.nextStartTick = rat.getRandom().nextInt(Math.max(1, 20));
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ??????? ???? ???????? Create ??? ????? {}", rat.getUUID(), exception);
            throw exception;
        }
    }

    public boolean canUse() {
        try {
            if (this.nextStartTick > 0) {
                --this.nextStartTick;
                return false;
            }
            this.nextStartTick = this.adjustedTickDelay(20);
            if (!this.canWorkAsCreateWorker()) {
                return false;
            }
            this.targetBlock = this.findTargetBlock().orElse(null);
            return this.targetBlock != null;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ?????? Create ??? ????? {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public boolean canContinueToUse() {
        try {
            return this.targetBlock != null && this.canWorkAsCreateWorker() && this.isCreateTargetStillValid(this.targetBlock);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ?????????? ?????? Create ??? ????? {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public void start() {
        try {
            this.isCurrentlyWorking = true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ?????? ?????? Create ??? ????? {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public void stop() {
        try {
            this.isCurrentlyWorking = false;
            this.rat.getNavigation().stop();
            this.targetBlock = null;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ?????????? ?????? Create ??? ????? {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public void tick() {
        try {
            if (this.targetBlock == null) {
                return;
            }
            if (this.canWorkFromCurrentPosition(this.targetBlock)) {
                this.performCreateWork();
                return;
            }
            if (this.rat.isPassenger() && CreateCompat.isMountedOnCreateSeat(this.rat)) {
                this.rat.stopRiding();
                this.rat.setOrderedToSit(false);
                RatsMod.LOGGER.debug("????? {} ?????? ? Create-???????, ????? ????? ?? ??????? ????? {}", this.rat.getUUID(), this.targetBlock);
            }
            Vec3 target = Vec3.atCenterOf((Vec3i)this.targetBlock);
            this.rat.moveSmoothlyTo(target.x(), target.y(), target.z(), this.rat.getHungerAdjustedWorkSpeed(1.15));
            if (this.rat.distanceToSqr(target) <= this.rat.getRatHarvestDistance(0.0)) {
                this.performCreateWork();
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ??? ?????? Create ??? ????? {} ? {}", this.rat.getUUID(), this.targetBlock, exception);
            throw exception;
        }
    }

    private boolean isCreateTargetStillValid(BlockPos pos) {
        try {
            BlockState state = this.rat.level().getBlockState(pos);
            if (!CreateCompat.canWorkerUse(state)) {
                return false;
            }
            if (this.canWorkFromCurrentPosition(pos)) {
                return true;
            }
            if (this.rat.isPassenger() && CreateCompat.isMountedOnCreateSeat(this.rat)) {
                return false;
            }
            return this.rat.distanceToSqr(Vec3.atCenterOf((Vec3i)pos)) <= (double)RatConfig.pathfindHomeWorkDistance * (double)RatConfig.pathfindHomeWorkDistance;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ??????? ????????? ????? Create ??? ????? {} ? {}", this.rat.getUUID(), pos, exception);
            throw exception;
        }
    }

    @Override
    public RatWorkGoal.TaskType getRatTaskType() {
        try {
            return RatWorkGoal.TaskType.HARVEST;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ???????? ??? ?????? Create ??? ????? {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public boolean isCurrentlyWorking() {
        try {
            return this.isCurrentlyWorking;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ?????????? Create-???????? {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private boolean canWorkAsCreateWorker() {
        try {
            boolean canAct = this.rat.canMove() || CreateCompat.isMountedOnCreateSeat(this.rat);
            return RatsMod.CREATE_LOADED && canAct && this.rat.canWorkBySchedule() && this.rat.canWorkFromHunger() && this.rat.getCommand() == RatCommand.HARVEST && !this.rat.isInCage() && this.rat.getTarget() == null && this.rat.hasWorkerUpgrade();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ??????? ???????? Create ??? ????? {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private Optional<BlockPos> findTargetBlock() {
        try {
            Optional<GlobalPos> marked = this.rat.getCreateWorkPos();
            if (marked.isPresent() && marked.get().dimension().equals(this.rat.level().dimension()) && this.isReachableCreateTarget(marked.get().pos())) {
                return Optional.of(marked.get().pos());
            }
            Optional<GlobalPos> workAnchor = this.rat.getWorkAnchorPoint();
            if (workAnchor.isPresent() && workAnchor.get().dimension().equals(this.rat.level().dimension()) && this.isReachableCreateTarget(workAnchor.get().pos())) {
                return Optional.of(workAnchor.get().pos());
            }
            return Optional.empty();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????? Create-????? ??? ????? {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private boolean isReachableCreateTarget(BlockPos pos) {
        try {
            Path path;
            BlockState state = this.rat.level().getBlockState(pos);
            if (!CreateCompat.canWorkerUse(state)) {
                return false;
            }
            if (this.canWorkFromCurrentPosition(pos)) {
                return true;
            }
            if (this.rat.isPassenger() && CreateCompat.isMountedOnCreateSeat(this.rat)) {
                return false;
            }
            if (this.rat.distanceToSqr(Vec3.atCenterOf((Vec3i)pos)) > (double)RatConfig.pathfindHomeWorkDistance * (double)RatConfig.pathfindHomeWorkDistance) {
                this.rat.setLastPathFailure("\u0440\u0430\u0431\u043e\u0447\u0430\u044f \u0442\u043e\u0447\u043a\u0430 Create " + String.valueOf(pos) + " \u0434\u0430\u043b\u044c\u0448\u0435 \u043b\u0438\u043c\u0438\u0442\u0430 " + RatConfig.pathfindHomeWorkDistance);
                return false;
            }
            if (pos.equals(this.lastReachablePos)) {
                int cacheTicks;
                int n = cacheTicks = this.lastReachableResult ? 60 : 20;
                if (this.rat.tickCount - this.lastReachableTick <= cacheTicks) {
                    return this.lastReachableResult;
                }
            }
            boolean reachable = (path = this.rat.getNavigation().createPath(pos, 1)) != null && path.getNodeCount() > 0;
            this.lastReachablePos = pos;
            this.lastReachableTick = this.rat.tickCount;
            this.lastReachableResult = reachable;
            if (!reachable) {
                this.rat.setLastPathFailure("\u043d\u0435\u0442 \u043f\u0443\u0442\u0438 \u043a \u0440\u0430\u0431\u043e\u0447\u0435\u0439 \u0442\u043e\u0447\u043a\u0435 Create " + String.valueOf(pos));
            }
            return reachable;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ???? ????? {} ? Create-????? {}", this.rat.getUUID(), pos, exception);
            throw exception;
        }
    }

    private boolean canWorkFromCurrentPosition(BlockPos pos) {
        try {
            BlockState state = this.rat.level().getBlockState(pos);
            double distance = this.rat.distanceToSqr(Vec3.atCenterOf((Vec3i)pos));
            double harvestDistance = this.rat.getRatHarvestDistance(0.0);
            if (distance <= harvestDistance * harvestDistance) {
                return true;
            }
            if (CreateCompat.canWorkerReachRemotely(state)) {
                double remoteDistance = Math.max(4.5, this.rat.getRatHarvestDistance(1.0));
                return distance <= remoteDistance * remoteDistance;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ????????? ?????? Create ??? ????? {} ? {}", this.rat.getUUID(), pos, exception);
            throw exception;
        }
    }

    private void performCreateWork() {
        try {
            this.collectApplicationItemIfNeeded();
            boolean worked = CreateCompat.workBlock(this.rat, this.targetBlock);
            if (!worked && CreateCompat.isCreateBeltBlock(this.rat.level().getBlockState(this.targetBlock))) {
                if (this.rat.tickCount % 100 == 0) {
                    RatsMod.LOGGER.debug("????? {} ???? ?????????? ??????? ?? ????? Create {}", this.rat.getUUID(), this.targetBlock);
                }
                this.nextStartTick = this.adjustedTickDelay(4 + this.rat.getRandom().nextInt(4));
                this.stop();
                return;
            }
            if (!worked) {
                RatsMod.LOGGER.debug("????? {} ????? ?? Create-????? {}, ?? ???? ?? ?????? ????????", this.rat.getUUID(), this.targetBlock);
            }
            CreateCompat.returnRatToCreateSeatIfConfigured(this.rat);
            this.nextStartTick = worked && CreateCompat.isCreateBeltBlock(this.rat.level().getBlockState(this.targetBlock)) ? this.adjustedTickDelay(4) : this.adjustedTickDelay(20);
            this.stop();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ????????? ???????? Create ??? ????? {} ? {}", this.rat.getUUID(), this.targetBlock, exception);
            throw exception;
        }
    }

    private void collectApplicationItemIfNeeded() {
        try {
            if (!this.rat.getMainHandItem().isEmpty()) {
                return;
            }
            if (this.rat.tickCount < this.nextApplicationPickupTick) {
                return;
            }
            Optional<GlobalPos> pickup = this.rat.getPickupPos();
            if (pickup.isEmpty() || !pickup.get().dimension().equals(this.rat.level().dimension())) {
                return;
            }
            BlockPos pickupPos = pickup.get().pos();
            for (Direction side : this.orderedSides(this.rat.pickupFacing)) {
                IItemHandler handler = RebornCapabilities.items(this.rat.level(), pickupPos, side);
                if (handler == null) continue;
                for (int slot = 0; slot < handler.getSlots(); ++slot) {
                    ItemStack simulated = handler.extractItem(slot, 1, true);
                    if (simulated.isEmpty() || !CreateCompat.isKnownApplicationItem(this.rat.level(), simulated)) continue;
                    ItemStack extracted = handler.extractItem(slot, 1, false);
                    this.rat.setItemInHand(InteractionHand.MAIN_HAND, extracted);
                    this.nextApplicationPickupTick = 0;
                    RatsMod.LOGGER.debug("????? {} ????? {} ?? {} ??? ?????????? ?? Create-?????", this.rat.getUUID(), extracted, pickupPos);
                    return;
                }
            }
            this.nextApplicationPickupTick = this.rat.tickCount + 20 + this.rat.getRandom().nextInt(20);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("????? {} ?? ?????? ??????? ???????? ??? ?????????? Create", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private Direction[] orderedSides(Direction preferredSide) {
        try {
            return ORDERED_SIDES[preferredSide.ordinal()];
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("?? ??????? ??????? ??????? ?????????? ??? ????? {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private static Direction[][] createOrderedSides() {
        try {
            Direction[][] ordered = new Direction[DIRECTIONS.length][DIRECTIONS.length];
            for (Direction preferredSide : DIRECTIONS) {
                Direction[] sides = ordered[preferredSide.ordinal()];
                sides[0] = preferredSide;
                int index = 1;
                for (Direction direction : DIRECTIONS) {
                    if (direction == preferredSide) continue;
                    sides[index++] = direction;
                }
            }
            return ordered;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u043a\u0435\u0448 \u043f\u043e\u0440\u044f\u0434\u043a\u0430 \u0441\u0442\u043e\u0440\u043e\u043d Create-\u0440\u0430\u0431\u043e\u0442\u044f\u0433\u0438", (Throwable)exception);
            throw exception;
        }
    }
}

