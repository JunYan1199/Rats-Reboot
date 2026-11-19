package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.ai.navigation.navigation.RatNavigation;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatTubePathHelper;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

public final class RatTubeTransit {
    private static final java.util.Map<TamedRat,Long> LAST_SEARCH = new java.util.WeakHashMap<>();
    private static final int STUCK_TICK_LIMIT = 80;
    private static final int TICKS_PER_SEGMENT = 4;
    private static final double ENTRY_REACHED_DISTANCE_SQ = 1.75;

    private RatTubeTransit() {
    }

    public static boolean tryStart(TamedRat rat, Vec3 target, double speed) {
        try {
            int idx;
            if (rat.level().isClientSide()) {
                return false;
            }
            Session existing = rat.getTubeTransit();
            if (existing != null && existing.isActive() && existing.targetsSame(target, 1.5)) {
                return true;
            }
            long now=rat.level().getGameTime();
            Long last=LAST_SEARCH.get(rat);
            if(last!=null && now-last<40) return false;
            LAST_SEARCH.put(rat,now);
            Optional<RatTubePathHelper.TubeRoute> planned = RatTubePathHelper.planRoute(rat.level(), rat.blockPosition(), target);
            if (planned.isEmpty()) {
                return false;
            }
            Session session = new Session(planned.get(), target, speed);
            if (rat.isInTube() && (idx = planned.get().segments().indexOf(rat.blockPosition())) >= 0) {
                session.segmentIndex = idx + 1;
                session.phase = Phase.TRAVERSE;
            }
            rat.setTubeTransit(session);
            RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0432\u044b\u0431\u0440\u0430\u043b\u0430 \u043c\u0430\u0440\u0448\u0440\u0443\u0442 \u043f\u043e \u0442\u0440\u0443\u0431\u0435: \u0432\u0445\u043e\u0434 {}, \u0432\u044b\u0445\u043e\u0434 {}, \u0441\u0435\u0433\u043c\u0435\u043d\u0442\u043e\u0432 {}", rat.getUUID(), session.route.entry().standPos(), session.route.exit().standPos(), session.route.segments().size());
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0447\u0430\u0442\u044c \u0442\u0440\u0430\u043d\u0437\u0438\u0442 \u043f\u043e \u0442\u0440\u0443\u0431\u0435 \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}: {}", rat.getUUID(), exception.getMessage(), exception);
            throw exception;
        }
    }

    public static boolean tick(TamedRat rat) {
        try {
            Session session = rat.getTubeTransit();
            if (session == null || !session.isActive()) {
                return false;
            }
            if (rat.level().isClientSide()) {
                return true;
            }
            BlockPos feet = rat.blockPosition();
            if (feet.equals(session.lastFeetPos)) {
                ++session.stuckTicks;
            } else {
                session.stuckTicks = 0;
                session.lastFeetPos = feet;
            }
            if (session.stuckTicks >= 80 && session.phase == Phase.APPROACH_ENTRY) {
                RatsMod.LOGGER.debug("\u041a\u0440\u044b\u0441\u0430 {} \u0437\u0430\u0432\u0438\u0441\u043b\u0430 \u043f\u0435\u0440\u0435\u0434 \u0432\u0445\u043e\u0434\u043e\u043c \u0442\u0440\u0443\u0431\u044b \u0432 {}, \u0441\u0431\u0440\u043e\u0441 \u0442\u0440\u0430\u043d\u0437\u0438\u0442\u0430", rat.getUUID(), feet);
                rat.clearTubeTransit();
                return false;
            }
            switch (session.phase.ordinal()) {
                case 0: {
                    RatTubeTransit.tickApproachEntry(rat, session);
                    break;
                }
                case 1: {
                    RatTubeTransit.tickEnterTube(rat, session);
                    break;
                }
                case 2: {
                    RatTubeTransit.tickTraverse(rat, session);
                    break;
                }
                case 3: {
                    RatTubeTransit.tickExitTube(rat, session);
                    break;
                }
                case 4: {
                    RatTubeTransit.tickFinish(rat, session);
                }
            }
            if (!session.isActive()) {
                rat.clearTubeTransit();
                return false;
            }
            return true;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041e\u0448\u0438\u0431\u043a\u0430 \u0442\u0438\u043a\u0430 \u0442\u0440\u0430\u043d\u0437\u0438\u0442\u0430 \u043f\u043e \u0442\u0440\u0443\u0431\u0435 \u0434\u043b\u044f \u043a\u0440\u044b\u0441\u044b {}: {}", rat.getUUID(), exception.getMessage(), exception);
            rat.clearTubeTransit();
            throw exception;
        }
    }

    private static void tickApproachEntry(TamedRat rat, Session session) {
        if (RatTubeTransit.isAtEntrance(rat, session.route.entry())) {
            RatTubeTransit.stopPathOnly(rat);
            session.phase = Phase.ENTER_TUBE;
            session.stuckTicks = 0;
            return;
        }
        RatTubeTransit.driveVanillaApproach(rat, session);
    }

    private static void tickEnterTube(TamedRat rat, Session session) {
        RatTubeTransit.stopPathOnly(rat);
        rat.switchNavigator(2);
        if (RatTubeTransit.advanceBlockStep(rat, session, session.route.entry().standPos(), session.route.entry().tubePos())) {
            session.segmentIndex = Math.max(1, session.route.segments().indexOf(session.route.entry().tubePos()) + 1);
            session.phase = Phase.TRAVERSE;
            RatTubeTransit.clearCurrentStep(session);
        }
    }

    private static void tickTraverse(TamedRat rat, Session session) {
        BlockPos to;
        if (session.segmentIndex >= session.route.segments().size()) {
            session.phase = Phase.EXIT_TUBE;
            RatTubeTransit.clearCurrentStep(session);
            return;
        }
        BlockPos from = session.segmentIndex == 0 ? session.route.entry().tubePos() : session.route.segments().get(session.segmentIndex - 1);
        if (RatTubeTransit.advanceBlockStep(rat, session, from, to = session.route.segments().get(session.segmentIndex))) {
            ++session.segmentIndex;
            RatTubeTransit.clearCurrentStep(session);
        }
    }

    private static void tickExitTube(TamedRat rat, Session session) {
        RatTubeTransit.stopPathOnly(rat);
        BlockPos from = session.route.exit().tubePos();
        BlockPos to = session.route.exit().standPos();
        if (RatTubeTransit.advanceBlockStep(rat, session, from, to)) {
            session.phase = Phase.FINISH;
            RatTubeTransit.clearCurrentStep(session);
        }
    }

    private static void tickFinish(TamedRat rat, Session session) {
        session.phase = null;
        rat.climbingTube = false;
        rat.setTubeClimbDirection(0);
    }

    private static boolean advanceBlockStep(TamedRat rat, Session session, BlockPos from, BlockPos to) {
        try {
            RatTubeTransit.stopPathOnly(rat);
            if (session.currentStepFrom == null || session.currentStepTo == null || !session.currentStepFrom.equals(from) || !session.currentStepTo.equals(to)) {
                session.currentStepFrom = from;
                session.currentStepTo = to;
                session.currentStepTick = 0;
                RatTubeTransit.faceToward(rat, from, to);
            }
            int nextTick = Math.min(4, session.currentStepTick + 1);
            double progress = (double)nextTick / 4.0;
            Vec3 fromCenter = Vec3.atBottomCenterOf((Vec3i)from);
            Vec3 toCenter = Vec3.atBottomCenterOf((Vec3i)to);
            double x = Mth.lerp((double)progress, (double)fromCenter.x(), (double)toCenter.x());
            double y = Mth.lerp((double)progress, (double)fromCenter.y(), (double)toCenter.y());
            double z = Mth.lerp((double)progress, (double)fromCenter.z(), (double)toCenter.z());
            RatTubeTransit.faceToward(rat, from, to);
            rat.setDeltaMovement(Vec3.ZERO);
            rat.fallDistance = 0.0f;
            rat.setPos(x, y, z);
            session.currentStepTick = nextTick;
            return session.currentStepTick >= 4;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u043f\u043e\u043b\u043d\u0438\u0442\u044c \u0448\u0430\u0433 \u043a\u0440\u044b\u0441\u044b {} \u043f\u043e \u0442\u0440\u0443\u0431\u0435 {} -> {}: {}", rat.getUUID(), from, to, exception.getMessage(), exception);
            throw exception;
        }
    }

    private static void clearCurrentStep(Session session) {
        session.currentStepFrom = null;
        session.currentStepTo = null;
        session.currentStepTick = 0;
    }

    private static void driveVanillaApproach(TamedRat rat, Session session) {
        PathNavigation navigation = rat.getNavigation();
        if (navigation instanceof RatNavigation) {
            RatNavigation ratNavigation = (RatNavigation)navigation;
            BlockPos stand = session.entryStandPos();
            if (navigation.isDone() || navigation.isStuck() || rat.tickCount % 20 == 0) {
                ratNavigation.moveToWithoutTubePlanning((double)stand.getX() + 0.5, stand.getY(), (double)stand.getZ() + 0.5, session.speed());
            }
        }
    }

    private static boolean isAtEntrance(TamedRat rat, RatTubePathHelper.TubeEntrance entrance) {
        if (rat.blockPosition().equals(entrance.tubePos())) {
            return true;
        }
        Vec3 standCenter = Vec3.atBottomCenterOf((Vec3i)entrance.standPos());
        Vec3 tubeCenter = Vec3.atBottomCenterOf((Vec3i)entrance.tubePos());
        return rat.position().distanceToSqr(standCenter) <= 1.75 || rat.position().distanceToSqr(tubeCenter) <= 1.75;
    }

    private static void stopPathOnly(TamedRat rat) {
        PathNavigation navigation = rat.getNavigation();
        if (navigation instanceof RatNavigation) {
            RatNavigation ratNavigation = (RatNavigation)navigation;
            ratNavigation.stopPathOnly();
        } else {
            navigation.stop();
        }
    }

    private static void faceToward(TamedRat rat, BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        int dy = to.getY() - from.getY();
        if (dx == 0.0 && dz == 0.0) {
            if (dy != 0) {
                rat.climbingTube = true;
                rat.setTubeClimbDirection(dy > 0 ? 1 : -1);
            }
            return;
        }
        float yaw = (float)(Mth.atan2((double)dz, (double)dx) * 57.2957763671875) - 90.0f;
        rat.setYRot(yaw);
        rat.yBodyRot = yaw;
        rat.yHeadRot = yaw;
        rat.yRotO = yaw;
        boolean bl = rat.climbingTube = rat.isInTube() && dy != 0;
        if (rat.climbingTube) {
            rat.setTubeClimbDirection(dy > 0 ? 1 : -1);
        } else {
            rat.setTubeClimbDirection(0);
        }
    }

    public static final class Session {
        private final RatTubePathHelper.TubeRoute route;
        private final Vec3 target;
        private final double speed;
        private Phase phase;
        private int segmentIndex;
        private int stuckTicks;
        private BlockPos lastFeetPos;
        private BlockPos currentStepFrom;
        private BlockPos currentStepTo;
        private int currentStepTick;

        private Session(RatTubePathHelper.TubeRoute route, Vec3 target, double speed) {
            this.route = route;
            this.target = target;
            this.speed = speed;
            this.phase = Phase.APPROACH_ENTRY;
            this.segmentIndex = 0;
            this.stuckTicks = 0;
            this.lastFeetPos = BlockPos.ZERO;
        }

        public boolean targetsSame(Vec3 other, double tolerance) {
            return this.target.distanceToSqr(other) < tolerance * tolerance;
        }

        public boolean isActive() {
            return this.phase != null;
        }

        public BlockPos entryStandPos() {
            return this.route.entry().standPos();
        }

        public double speed() {
            return this.speed;
        }

        public boolean needsVanillaApproach() {
            return this.phase == Phase.APPROACH_ENTRY;
        }
    }

    public static enum Phase {
        APPROACH_ENTRY,
        ENTER_TUBE,
        TRAVERSE,
        EXIT_TUBE,
        FINISH;

    }
}

