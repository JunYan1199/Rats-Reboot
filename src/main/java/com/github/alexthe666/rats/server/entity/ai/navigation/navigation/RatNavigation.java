package com.github.alexthe666.rats.server.entity.ai.navigation.navigation;

import com.github.alexthe666.rats.server.entity.ai.navigation.evaluator.RatNodeEvaluator;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatTubeTransit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

public class RatNavigation
extends GroundPathNavigation {
    private boolean stoppingPathForTubeTransit;
    private boolean movingWithoutTubePlanning;

    public RatNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    protected PathFinder createPathFinder(int nodes) {
        this.nodeEvaluator = new RatNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanFloat(true);
        return new PathFinder(this.nodeEvaluator, nodes);
    }

    public boolean moveTo(double x, double y, double z, double speed) {
        if (this.movingWithoutTubePlanning) {
            return super.moveTo(x, y, z, speed);
        }
        Mob mob = this.mob;
        if (mob instanceof TamedRat) {
            TamedRat rat = (TamedRat)mob;
            Vec3 target = new Vec3(x, y, z);
            if (rat.isTubeTransitActive() && rat.getTubeTransit() != null && rat.getTubeTransit().targetsSame(target, 1.5)) {
                if (rat.getTubeTransit().needsVanillaApproach()) {
                    BlockPos entry = rat.getTubeTransit().entryStandPos();
                    return this.moveToWithoutTubePlanning((double)entry.getX() + 0.5, entry.getY(), (double)entry.getZ() + 0.5, rat.getTubeTransit().speed());
                }
                return true;
            }
            rat.clearTubeTransit();
            if (RatTubeTransit.tryStart(rat, target, speed) && rat.getTubeTransit() != null) {
                BlockPos entry = rat.getTubeTransit().entryStandPos();
                return this.moveToWithoutTubePlanning((double)entry.getX() + 0.5, entry.getY(), (double)entry.getZ() + 0.5, speed);
            }
        }
        // Work goals request the same destination repeatedly. Re-submitting the active path
        // resets vanilla's stuck timer even though none of its waypoints have changed.
        if (!this.isDone() && this.path.canReach() && this.path.getTarget().equals(BlockPos.containing(x, y, z))) {
            this.setSpeedModifier(speed);
            return true;
        }
        return this.moveToWithoutTubePlanning(x, y, z, speed);
    }

    public boolean moveTo(Entity entity, double speed) {
        if (this.movingWithoutTubePlanning) {
            return super.moveTo(entity, speed);
        }
        return this.moveTo(entity.getX(), entity.getY(), entity.getZ(), speed);
    }

    public boolean moveTo(Path path, double speed) {
        Mob mob;
        if (this.movingWithoutTubePlanning) {
            return super.moveTo(path, speed);
        }
        if (path != null && (mob = this.mob) instanceof TamedRat) {
            TamedRat rat = (TamedRat)mob;
            Vec3 target = path.getTarget().getCenter();
            if (rat.isTubeTransitActive() && rat.getTubeTransit() != null && rat.getTubeTransit().targetsSame(target, 1.5)) {
                return true;
            }
            rat.clearTubeTransit();
            if (RatTubeTransit.tryStart(rat, target, speed) && rat.getTubeTransit() != null) {
                BlockPos entry = rat.getTubeTransit().entryStandPos();
                return this.moveToWithoutTubePlanning((double)entry.getX() + 0.5, entry.getY(), (double)entry.getZ() + 0.5, speed);
            }
        }
        return super.moveTo(path, speed);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean moveToWithoutTubePlanning(double x, double y, double z, double speed) {
        this.movingWithoutTubePlanning = true;
        try {
            boolean bl = super.moveTo(x, y, z, speed);
            return bl;
        }
        finally {
            this.movingWithoutTubePlanning = false;
        }
    }

    public void stopPathOnly() {
        this.stoppingPathForTubeTransit = true;
        try {
            super.stop();
        }
        finally {
            this.stoppingPathForTubeTransit = false;
        }
    }

    public void stop() {
        Mob mob;
        if (!this.stoppingPathForTubeTransit && (mob = this.mob) instanceof TamedRat) {
            TamedRat rat = (TamedRat)mob;
            rat.clearTubeTransit();
        }
        super.stop();
    }
}

