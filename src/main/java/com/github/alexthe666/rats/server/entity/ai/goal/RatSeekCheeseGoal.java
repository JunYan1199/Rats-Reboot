package com.github.alexthe666.rats.server.entity.ai.goal;

import com.github.alexthe666.rats.RatConfig;
import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.compat.create.CreateCompat;
import com.github.alexthe666.rats.data.tags.RatsBlockTags;
import com.github.alexthe666.rats.server.block.entity.RatCheeseFeederBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatDaySchedule;
import com.github.alexthe666.rats.server.misc.RatUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

public class RatSeekCheeseGoal
extends Goal {
    private static final int MAX_PATH_CHECKS_PER_SEARCH = 16;
    private final TamedRat rat;
    private BlockPos targetBlock;
    private int nextStartTick;

    public RatSeekCheeseGoal(TamedRat rat) {
        try {
            this.rat = rat;
            this.nextStartTick = 40 + rat.getRandom().nextInt(120);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u0446\u0435\u043b\u044c \u043f\u043e\u0438\u0441\u043a\u0430 \u0441\u044b\u0440\u0430 \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", rat.getUUID(), exception);
            throw exception;
        }
    }

    public boolean canUse() {
        try {
            if (this.nextStartTick > 0) {
                --this.nextStartTick;
                return false;
            }
            this.nextStartTick = RatSeekCheeseGoal.reducedTickDelay((int)(120 + this.rat.getRandom().nextInt(80)));
            if (!this.rat.canMove() && !CreateCompat.isMountedOnCreateSeat(this.rat) || this.rat.isInCage() || this.rat.getTarget() != null) {
                return false;
            }
            if (RatDaySchedule.shouldSleep(this.rat.level())) {
                return false;
            }
            if (this.rat.consumeCheeseFromReserve("\u043b\u0438\u0447\u043d\u044b\u0439 \u0437\u0430\u043f\u0430\u0441") && !this.rat.needsCheeseNow() && !this.rat.shouldRefillCheeseReserve()) {
                return false;
            }
            if (!this.rat.needsCheeseNow() && !this.rat.shouldRefillCheeseReserve()) {
                return false;
            }
            this.targetBlock = this.findTargetBlock();
            if (this.targetBlock == null) {
                this.nextStartTick = RatSeekCheeseGoal.reducedTickDelay((int)(240 + this.rat.getRandom().nextInt(160)));
                this.rat.setLastPathFailure("\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d \u043f\u0443\u0442\u044c \u043a \u0438\u0441\u0442\u043e\u0447\u043d\u0438\u043a\u0443 \u0441\u044b\u0440\u0430 \u0432 \u0440\u0430\u0434\u0438\u0443\u0441\u0435 " + RatConfig.pathfindFeederDistance);
                return false;
            }
            this.rat.clearLastPathFailure();
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c \u043f\u043e\u0438\u0441\u043a \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public boolean canContinueToUse() {
        try {
            return !(this.targetBlock == null || !this.rat.canMove() && !CreateCompat.isMountedOnCreateSeat(this.rat) || !this.rat.needsCheeseNow() && !this.rat.shouldRefillCheeseReserve());
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0434\u043e\u043b\u0436\u0438\u0442\u044c \u043f\u043e\u0438\u0441\u043a \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public void stop() {
        try {
            this.targetBlock = null;
            this.rat.getNavigation().stop();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u043f\u043e\u0438\u0441\u043a \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    public void tick() {
        try {
            if (this.targetBlock == null) {
                return;
            }
            if (CreateCompat.isMountedOnCreateSeat(this.rat)) {
                this.rat.stopRiding();
                this.rat.setOrderedToSit(false);
                RatsMod.LOGGER.debug("????? {} ?????? ? Create-???????, ????? ????? ?? ???? {}", this.rat.getUUID(), this.targetBlock);
            }
            Vec3 target = Vec3.atCenterOf((Vec3i)this.targetBlock);
            this.rat.moveSmoothlyTo(target.x(), target.y(), target.z(), 1.25);
            if (this.rat.distanceToSqr(target) <= this.rat.getRatHarvestDistance(0.0)) {
                if (!this.collectCheeseFromTarget()) {
                    RatsMod.LOGGER.warn("\u041a\u0440\u044b\u0441\u0430 {} \u0434\u043e\u0448\u043b\u0430 \u0434\u043e \u0438\u0441\u0442\u043e\u0447\u043d\u0438\u043a\u0430 \u0441\u044b\u0440\u0430 {}, \u043d\u043e \u043d\u0435 \u0441\u043c\u043e\u0433\u043b\u0430 \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0441\u044b\u0440", this.rat.getUUID(), this.targetBlock);
                }
                this.stop();
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u043f\u043e\u043b\u043d\u0438\u0442\u044c \u043f\u043e\u0438\u0441\u043a \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private BlockPos findTargetBlock() {
        try {
            BlockPos feeder = this.findFeederTarget();
            if (feeder != null) {
                return feeder;
            }
            if (this.rat.needsCheeseNow() && this.rat.getCheeseEatenToday() <= 0 && ForgeEventFactory.getMobGriefingEvent((Level)this.rat.level(), (Entity)this.rat)) {
                return this.findRaidTarget();
            }
            return null;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0439\u0442\u0438 \u0438\u0441\u0442\u043e\u0447\u043d\u0438\u043a \u0441\u044b\u0440\u0430 \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private BlockPos findFeederTarget() {
        try {
            BlockPos center = this.rat.getSearchCenter();
            int radius = RatConfig.pathfindFeederDistance;
            ArrayList<CheeseSearchCandidate> candidates = new ArrayList<CheeseSearchCandidate>();
            for (BlockEntity entity : this.collectNearbyBlockEntities(center, radius)) {
                RatCheeseFeederBlockEntity feeder;
                BlockPos pos = entity.getBlockPos();
                if (!pos.closerThan((Vec3i)center, (double)radius + 1.0) || !(entity instanceof RatCheeseFeederBlockEntity) || !this.isValidFeeder(feeder = (RatCheeseFeederBlockEntity)entity)) continue;
                candidates.add(new CheeseSearchCandidate(pos, this.rat.distanceToSqr(Vec3.atCenterOf((Vec3i)pos))));
            }
            return this.firstReachableCandidate(candidates);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0439\u0442\u0438 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 \u0441 \u0441\u044b\u0440\u043e\u043c \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private BlockPos findRaidTarget() {
        try {
            BlockPos center = this.rat.getSearchCenter();
            int radius = RatConfig.pathfindFeederDistance;
            ArrayList<CheeseSearchCandidate> candidates = new ArrayList<CheeseSearchCandidate>();
            for (BlockEntity entity : this.collectNearbyBlockEntities(center, radius)) {
                BlockPos pos = entity.getBlockPos();
                if (!pos.closerThan((Vec3i)center, (double)radius + 1.0) || !this.isValidRaidContainer(entity)) continue;
                candidates.add(new CheeseSearchCandidate(pos, this.rat.distanceToSqr(Vec3.atCenterOf((Vec3i)pos))));
            }
            return this.firstReachableCandidate(candidates);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0439\u0442\u0438 \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440 \u0441 \u0441\u044b\u0440\u043e\u043c \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private BlockPos firstReachableCandidate(List<CheeseSearchCandidate> candidates) {
        try {
            candidates.sort(Comparator.comparingDouble(CheeseSearchCandidate::distance));
            int checkedPaths = 0;
            for (CheeseSearchCandidate candidate : candidates) {
                if (checkedPaths++ >= 16) {
                    return null;
                }
                if (!this.canReach(candidate.pos())) continue;
                return candidate.pos();
            }
            return null;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u0431\u0440\u0430\u0442\u044c \u0434\u043e\u0441\u0442\u0438\u0436\u0438\u043c\u044b\u0439 \u0438\u0441\u0442\u043e\u0447\u043d\u0438\u043a \u0441\u044b\u0440\u0430 \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private Iterable<BlockEntity> collectNearbyBlockEntities(BlockPos center, int radius) {
        try {
            ArrayList<BlockEntity> entities = new ArrayList<BlockEntity>();
            int minChunkX = center.getX() - radius >> 4;
            int maxChunkX = center.getX() + radius >> 4;
            int minChunkZ = center.getZ() - radius >> 4;
            int maxChunkZ = center.getZ() + radius >> 4;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
                    if (!this.rat.level().hasChunk(chunkX, chunkZ)) continue;
                    LevelChunk chunk = this.rat.level().getChunk(chunkX, chunkZ);
                    entities.addAll(chunk.getBlockEntities().values());
                }
            }
            return entities;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0431\u0440\u0430\u0442\u044c block entity \u0432\u043e\u043a\u0440\u0443\u0433 {} \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", center, this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private boolean canReach(BlockPos pos) {
        try {
            boolean reachable;
            Path path = this.rat.getNavigation().createPath(pos, 1);
            boolean bl = reachable = path != null && path.getNodeCount() > 0;
            if (!reachable) {
                this.rat.setLastPathFailure("\u043d\u0435\u0442 \u043f\u0443\u0442\u0438 \u043a \u0441\u044b\u0440\u0443 " + String.valueOf(pos));
            }
            return reachable;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043f\u0443\u0442\u044c \u043a\u0440\u044b\u0441\u044b {} \u043a \u0441\u044b\u0440\u0443 {}", this.rat.getUUID(), pos, exception);
            throw exception;
        }
    }

    private boolean isValidFeeder(LevelReader reader, BlockPos pos) {
        try {
            RatCheeseFeederBlockEntity feeder;
            if (RatUtils.isBlockProtected(this.rat.level(), pos, this.rat)) {
                return false;
            }
            BlockEntity entity = reader.getBlockEntity(pos);
            return entity instanceof RatCheeseFeederBlockEntity && this.isValidFeeder(feeder = (RatCheeseFeederBlockEntity)entity);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 {} \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", pos, this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private boolean isValidFeeder(RatCheeseFeederBlockEntity feeder) {
        try {
            return !RatUtils.isBlockProtected(this.rat.level(), feeder.getBlockPos(), this.rat) && feeder.hasCheese();
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043d\u0430\u0439\u0434\u0435\u043d\u043d\u0443\u044e \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 {} \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", feeder.getBlockPos(), this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private boolean isValidRaidContainer(LevelReader reader, BlockPos pos) {
        try {
            Container inventory;
            RandomizableContainerBlockEntity container;
            if (reader.getBlockState(pos).is(RatsBlockTags.UNRAIDABLE_CONTAINERS)) {
                return false;
            }
            if (!(reader.getBlockState(pos).getBlock() instanceof EntityBlock)) {
                return false;
            }
            if (RatUtils.isBlockProtected(this.rat.level(), pos, this.rat)) {
                return false;
            }
            BlockEntity entity = reader.getBlockEntity(pos);
            if (entity instanceof RandomizableContainerBlockEntity && (container = (RandomizableContainerBlockEntity)entity).saveWithFullMetadata().contains("LootTable")) {
                return false;
            }
            return entity instanceof Container && this.containerHasCheese(inventory = (Container)entity);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440 {} \u0434\u043b\u044f \u0433\u043e\u043b\u043e\u0434\u043d\u043e\u0439 \u043a\u0440\u044b\u0441\u044b {}", pos, this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private boolean isValidRaidContainer(BlockEntity entity) {
        try {
            Container inventory;
            RandomizableContainerBlockEntity container;
            BlockPos pos = entity.getBlockPos();
            if (this.rat.level().getBlockState(pos).is(RatsBlockTags.UNRAIDABLE_CONTAINERS)) {
                return false;
            }
            if (RatUtils.isBlockProtected(this.rat.level(), pos, this.rat)) {
                return false;
            }
            if (entity instanceof RandomizableContainerBlockEntity && (container = (RandomizableContainerBlockEntity)entity).saveWithFullMetadata().contains("LootTable")) {
                return false;
            }
            return entity instanceof Container && this.containerHasCheese(inventory = (Container)entity);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043d\u0430\u0439\u0434\u0435\u043d\u043d\u044b\u0439 \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440 {} \u0434\u043b\u044f \u0433\u043e\u043b\u043e\u0434\u043d\u043e\u0439 \u043a\u0440\u044b\u0441\u044b {}", entity.getBlockPos(), this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private boolean containerHasCheese(Container inventory) {
        try {
            for (int slot = 0; slot < inventory.getContainerSize(); ++slot) {
                if (!TamedRat.isCheeseReserveItem(inventory.getItem(slot))) continue;
                return true;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0441\u044b\u0440 \u0432 \u043a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440\u0435 \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private boolean collectCheeseFromTarget() {
        try {
            if (this.targetBlock == null) {
                return false;
            }
            BlockEntity entity = this.rat.level().getBlockEntity(this.targetBlock);
            if (entity instanceof RatCheeseFeederBlockEntity) {
                RatCheeseFeederBlockEntity feeder = (RatCheeseFeederBlockEntity)entity;
                return this.collectFromFeeder(feeder);
            }
            if (entity instanceof Container) {
                Container inventory = (Container)entity;
                return this.eatFromContainer(inventory);
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0441\u044b\u0440 \u043a\u0440\u044b\u0441\u044b {} \u0438\u0437 \u0446\u0435\u043b\u0438 {}", this.rat.getUUID(), this.targetBlock, exception);
            throw exception;
        }
    }

    private boolean collectFromFeeder(RatCheeseFeederBlockEntity feeder) {
        try {
            ItemStack cheese;
            boolean changed = false;
            if (this.rat.needsCheeseNow() && !(cheese = feeder.takeCheeseForRat(1)).isEmpty()) {
                boolean eaten = this.rat.eatCheeseFromStack(cheese, "\u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0430");
                if (!eaten) {
                    feeder.addCheeseFrom(cheese);
                    throw new IllegalStateException("\u041a\u0440\u044b\u0441\u0430 \u043f\u043e\u043b\u0443\u0447\u0438\u043b\u0430 \u0441\u044b\u0440 \u0438\u0437 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438, \u043d\u043e \u043d\u0435 \u0441\u044a\u0435\u043b\u0430 \u0435\u0433\u043e");
                }
                changed = true;
            }
            while (this.rat.hasCheeseReserveSpace() && feeder.hasCheese() && !(cheese = feeder.takeCheeseForRat(this.rat.getMaxCheeseReserve())).isEmpty()) {
                int inserted = this.rat.addCheeseToReserve(cheese);
                if (!cheese.isEmpty()) {
                    feeder.addCheeseFrom(cheese);
                }
                if (inserted <= 0) break;
                changed = true;
            }
            return changed;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0441\u044b\u0440 \u043a\u0440\u044b\u0441\u0435 {} \u0438\u0437 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 {}", this.rat.getUUID(), feeder.getBlockPos(), exception);
            throw exception;
        }
    }

    private boolean eatFromContainer(Container inventory) {
        try {
            if (this.targetBlock != null) {
                this.toggleChest(inventory, false);
            }
            for (int slot = 0; slot < inventory.getContainerSize(); ++slot) {
                ItemStack stack = inventory.getItem(slot);
                if (!TamedRat.isCheeseReserveItem(stack) || !this.rat.eatCheeseFromStack(stack, "\u0433\u043e\u043b\u043e\u0434\u043d\u044b\u0439 \u0433\u0440\u0430\u0431\u0451\u0436")) continue;
                if (stack.isEmpty()) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                } else {
                    inventory.setChanged();
                }
                return true;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0443\u043a\u0440\u0430\u0441\u0442\u044c \u0441\u044b\u0440 \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private void toggleChest(Container inventory, boolean open) {
        try {
            if (this.targetBlock != null && inventory instanceof ChestBlockEntity) {
                ChestBlockEntity chest = (ChestBlockEntity)inventory;
                this.rat.level().blockEvent(this.targetBlock, chest.getBlockState().getBlock(), 1, open ? 1 : 0);
                this.rat.level().playSound(null, this.targetBlock, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u0441\u0443\u043d\u0434\u0443\u043a \u043f\u0440\u0438 \u043f\u043e\u0438\u0441\u043a\u0435 \u0441\u044b\u0440\u0430 \u043a\u0440\u044b\u0441\u044b {}", this.rat.getUUID(), exception);
            throw exception;
        }
    }

    private record CheeseSearchCandidate(BlockPos pos, double distance) {
    }
}

