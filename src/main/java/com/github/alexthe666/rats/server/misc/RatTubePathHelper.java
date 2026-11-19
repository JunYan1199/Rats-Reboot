package com.github.alexthe666.rats.server.misc;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.block.RatCageBlock;
import com.github.alexthe666.rats.server.block.RatTubeBlock;
import com.github.alexthe666.rats.server.block.entity.RatTubeBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.misc.RatUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class RatTubePathHelper {
    private static final int MAX_NETWORK_SCAN = 512;
    private static final int ROUTE_SEARCH_RADIUS = 24;
    private static final double TUBE_SEGMENT_ROUTE_COST = 0.35;
    private static final double TUBE_EQUAL_ROUTE_BIAS = 0.5;

    private RatTubePathHelper() {
    }

    public static BooleanProperty getOpenProperty(Direction direction) {
        return switch (direction) {
            default -> throw new IllegalStateException("Unexpected direction");
            case DOWN -> RatTubeBlock.OPEN_DOWN;
            case UP -> RatTubeBlock.OPEN_UP;
            case NORTH -> RatTubeBlock.OPEN_NORTH;
            case SOUTH -> RatTubeBlock.OPEN_SOUTH;
            case WEST -> RatTubeBlock.OPEN_WEST;
            case EAST -> RatTubeBlock.OPEN_EAST;
        };
    }

    public static BooleanProperty getConnectProperty(Direction direction) {
        return switch (direction) {
            default -> throw new IllegalStateException("Unexpected direction");
            case DOWN -> RatTubeBlock.DOWN;
            case UP -> RatTubeBlock.UP;
            case NORTH -> RatTubeBlock.NORTH;
            case SOUTH -> RatTubeBlock.SOUTH;
            case WEST -> RatTubeBlock.WEST;
            case EAST -> RatTubeBlock.EAST;
        };
    }

    public static boolean isPassageOpen(BlockState state, Direction direction) {
        if (!(state.getBlock() instanceof RatTubeBlock)) {
            return false;
        }
        return (Boolean)state.getValue((Property)RatTubePathHelper.getOpenProperty(direction));
    }

    public static boolean hasOpenPassage(BlockState state) {
        if (!(state.getBlock() instanceof RatTubeBlock)) {
            return false;
        }
        for (BooleanProperty openProp : RatTubeBlock.ALL_OPEN_PROPS) {
            if (!((Boolean)state.getValue((Property)openProp)).booleanValue()) continue;
            return true;
        }
        return false;
    }

    public static boolean isTubeLink(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState neighbor = level.getBlockState(pos.relative(direction));
        return neighbor.getBlock() instanceof RatTubeBlock || neighbor.getBlock() instanceof RatCageBlock;
    }

    public static boolean canMountEntrance(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState neighbor = level.getBlockState(pos.relative(direction));
        return !(neighbor.getBlock() instanceof RatTubeBlock);
    }

    public static boolean isTubeSegment(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof RatTubeBlock;
    }

    public static boolean isConnectedTo(BlockState state, Direction direction) {
        if (!(state.getBlock() instanceof RatTubeBlock)) {
            return false;
        }
        return (Boolean)state.getValue((Property)RatTubePathHelper.getConnectProperty(direction));
    }

    public static boolean isInTubeNetworkWithPassage(Level level, BlockPos pos) {
        try {
            if (!RatTubePathHelper.isTubeSegment((BlockGetter)level, pos)) {
                return false;
            }
            HashSet<BlockPos> visited = new HashSet<BlockPos>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
            queue.add(pos);
            visited.add(pos);
            int scanned = 0;
            while (!queue.isEmpty() && scanned < 512) {
                BlockPos current = (BlockPos)queue.poll();
                ++scanned;
                BlockState state = level.getBlockState(current);
                if (!(state.getBlock() instanceof RatTubeBlock)) continue;
                if (RatTubePathHelper.hasOpenPassage(state)) {
                    return true;
                }
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (!visited.add(next) || !RatTubePathHelper.isTubeSegment((BlockGetter)level, next)) continue;
                    queue.add(next);
                }
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Tube network scan failed at {}: {}", pos, exception.getMessage(), exception);
            throw exception;
        }
    }

    public static boolean canEnterFrom(BlockGetter level, BlockPos tubePos, Direction fromStandToTube) {
        BlockState tubeState = level.getBlockState(tubePos);
        if (!(tubeState.getBlock() instanceof RatTubeBlock)) {
            return false;
        }
        return RatTubePathHelper.isPassageOpen(tubeState, fromStandToTube.getOpposite());
    }

    public static boolean isPassableForRat(BlockGetter level, BlockPos pos, TamedRat rat) {
        try {
            if (rat != null && rat.isInTube()) {
                return RatTubePathHelper.isTubeSegment(level, pos) || RatUtils.isOpenRatTube(level, pos);
            }
            if (RatTubePathHelper.isTubeSegment(level, pos)) {
                if (level instanceof Level) {
                    Level world = (Level)level;
                    return RatTubePathHelper.isInTubeNetworkWithPassage(world, pos);
                }
                return RatTubePathHelper.hasOpenPassage(level.getBlockState(pos));
            }
            if (RatUtils.isOpenRatTube(level, pos)) {
                return true;
            }
            for (Direction direction : Direction.values()) {
                BlockPos tubePos = pos.relative(direction);
                if (!RatTubePathHelper.isTubeSegment(level, tubePos) || !RatTubePathHelper.canEnterFrom(level, tubePos, direction)) continue;
                return true;
            }
            return false;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Tube passability failed at {}: {}", pos, exception.getMessage(), exception);
            throw exception;
        }
    }

    @Nullable
    public static BlockPathTypes resolveTubeBlockPathTypes(BlockGetter level, BlockPos pos, TamedRat rat) {
        try {
            if (rat == null) {
                return null;
            }
            if (RatTubePathHelper.isTubeSegment(level, pos) && RatTubePathHelper.isPassableForRat(level, pos, rat)) {
                return BlockPathTypes.RAIL;
            }
            if (!RatTubePathHelper.isTubeSegment(level, pos)) {
                for (Direction direction : Direction.values()) {
                    Level world;
                    BlockPos tubePos = pos.relative(direction);
                    if (!RatTubePathHelper.isTubeSegment(level, tubePos) || !RatTubePathHelper.canEnterFrom(level, tubePos, direction) || !(level instanceof Level) || !RatTubePathHelper.isInTubeNetworkWithPassage(world = (Level)level, tubePos)) continue;
                    return BlockPathTypes.RAIL;
                }
            }
            return null;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Tube path type failed at {}: {}", pos, exception.getMessage(), exception);
            throw exception;
        }
    }

    public static boolean isPathfindableTube(BlockState state, BlockPos pos, BlockGetter level) {
        Level world;
        if (!(state.getBlock() instanceof RatTubeBlock)) {
            return false;
        }
        if (level instanceof Level && RatTubePathHelper.isInTubeNetworkWithPassage(world = (Level)level, pos)) {
            return true;
        }
        return state.isPathfindable(level,pos,PathComputationType.LAND);
    }

    public static void setEntranceMount(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RatTubeBlockEntity) {
            RatTubeBlockEntity tube = (RatTubeBlockEntity)blockEntity;
            tube.setEntranceData(side, true);
        }
    }

    public static void clearEntranceMount(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RatTubeBlockEntity) {
            RatTubeBlockEntity tube = (RatTubeBlockEntity)blockEntity;
            tube.setEntranceData(null,false);
        }
    }

    public static Optional<TubeRoute> planRoute(Level level, BlockPos ratFeet, Vec3 target) {
        try {
            BlockPos goalBlock = BlockPos.containing((double)target.x, (double)target.y, (double)target.z);
            BlockPos networkSeed = RatTubePathHelper.findNearestTubeSegment(level, ratFeet, 24);
            if (networkSeed == null) {
                networkSeed = RatTubePathHelper.findNearestTubeSegment(level, goalBlock, 24);
            }
            if (networkSeed == null) {
                return Optional.empty();
            }
            if (!RatTubePathHelper.isInTubeNetworkWithPassage(level, networkSeed)) {
                return Optional.empty();
            }
            List<TubeEntrance> entrances = RatTubePathHelper.collectNetworkEntrances(level, networkSeed);
            if (entrances.isEmpty()) {
                return Optional.empty();
            }
            double directCost = RatTubePathHelper.manhattanDistance(ratFeet, goalBlock);
            TubeRoute best = null;
            double bestCost = directCost + 0.5;
            for (TubeEntrance entry : entrances) {
                for (TubeEntrance exit : entrances) {
                    double cost;
                    List<BlockPos> segments;
                    if (entry.tubePos().equals(exit.tubePos()) && entry.openSide() == exit.openSide() || (segments = RatTubePathHelper.buildTubeSegmentPath(level, entry.tubePos(), exit.tubePos())).isEmpty() || !((cost = (double)RatTubePathHelper.manhattanDistance(entry.standPos(), ratFeet) + (double)segments.size() * 0.35 + (double)RatTubePathHelper.manhattanDistance(exit.standPos(), goalBlock)) <= bestCost)) continue;
                    bestCost = cost;
                    best = new TubeRoute(entry, segments, exit, target);
                }
            }
            if (best == null) {
                return Optional.empty();
            }
            RatsMod.LOGGER.debug("Tube route planned: {} segments, entry {}, exit {}", best.segments().size(), best.entry().standPos(), best.exit().standPos());
            return Optional.of(best);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Tube route planning failed at {} -> {}: {}", ratFeet, target, exception.getMessage(), exception);
            throw exception;
        }
    }

    public static List<TubeEntrance> collectNetworkEntrances(Level level, BlockPos networkSeed) {
        try {
            Set<BlockPos> network = RatTubePathHelper.collectTubeNetwork(level, networkSeed);
            ArrayList<TubeEntrance> entrances = new ArrayList<TubeEntrance>();
            for (BlockPos tubePos : network) {
                BlockState tubeState = level.getBlockState(tubePos);
                if (!(tubeState.getBlock() instanceof RatTubeBlock)) continue;
                for (Direction openSide : Direction.values()) {
                    if (!RatTubePathHelper.isPassageOpen(tubeState, openSide) || !RatTubePathHelper.canMountEntrance((BlockGetter)level, tubePos, openSide)) continue;
                    entrances.add(new TubeEntrance(tubePos, tubePos.relative(openSide), openSide));
                }
            }
            return entrances;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Tube entrance collection failed at {}: {}", networkSeed, exception.getMessage(), exception);
            throw exception;
        }
    }

    public static List<BlockPos> buildTubeSegmentPath(Level level, BlockPos start, BlockPos end) {
        try {
            BlockPos current;
            if (!RatTubePathHelper.isTubeSegment((BlockGetter)level, start) || !RatTubePathHelper.isTubeSegment((BlockGetter)level, end)) {
                return List.of();
            }
            if (start.equals(end)) {
                return List.of(start);
            }
            HashMap<BlockPos, BlockPos> parent = new HashMap<BlockPos, BlockPos>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
            parent.put(start, null);
            queue.add(start);
            while (!queue.isEmpty() && parent.size() < 512 && !(current = (BlockPos)queue.poll()).equals(end)) {
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (!RatTubePathHelper.isTubeSegment((BlockGetter)level, next) || parent.containsKey(next)) continue;
                    parent.put(next, current);
                    queue.add(next);
                }
            }
            if (!parent.containsKey(end)) {
                return List.of();
            }
            ArrayList<BlockPos> path = new ArrayList<BlockPos>();
            BlockPos cursor = end;
            while (cursor != null) {
                path.add(cursor);
                cursor = (BlockPos)parent.get(cursor);
            }
            Collections.reverse(path);
            return path;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Tube segment path failed {} -> {}: {}", start, end, exception.getMessage(), exception);
            throw exception;
        }
    }

    @Nullable
    public static BlockPos findNearestTubeSegment(Level level, BlockPos origin, int radius) {
        try {
            BlockPos nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed((BlockPos)origin.offset(-radius, -radius, -radius), (BlockPos)origin.offset(radius, radius, radius))) {
                double dist;
                if (!RatTubePathHelper.isTubeSegment((BlockGetter)level, pos) || !((dist = pos.distSqr((Vec3i)origin)) < nearestDist)) continue;
                nearestDist = dist;
                nearest = pos.immutable();
            }
            return nearest;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("Nearest tube search failed at {}: {}", origin, exception.getMessage(), exception);
            throw exception;
        }
    }

    private static Set<BlockPos> collectTubeNetwork(Level level, BlockPos seed) {
        HashSet<BlockPos> visited = new HashSet<BlockPos>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
        queue.add(seed);
        visited.add(seed);
        int scanned = 0;
        while (!queue.isEmpty() && scanned < 512) {
            BlockPos current = (BlockPos)queue.poll();
            ++scanned;
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!visited.add(next) || !RatTubePathHelper.isTubeSegment((BlockGetter)level, next)) continue;
                queue.add(next);
            }
        }
        return visited;
    }

    private static int manhattanDistance(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX()) + Math.abs(first.getY() - second.getY()) + Math.abs(first.getZ() - second.getZ());
    }

    public record TubeEntrance(BlockPos tubePos, BlockPos standPos, Direction openSide) {
    }

    public record TubeRoute(TubeEntrance entry, List<BlockPos> segments, TubeEntrance exit, Vec3 target) {
    }
}

