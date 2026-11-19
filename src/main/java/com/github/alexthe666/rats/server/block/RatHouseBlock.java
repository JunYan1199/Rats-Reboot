package com.github.alexthe666.rats.server.block;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.server.block.entity.RatHouseBlockEntity;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RatHouseBlock
extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionProperty.create((String)"facing", (Predicate)Direction.Plane.HORIZONTAL);
    private static final VoxelShape FULL_SHAPE = Shapes.block();
    private static final VoxelShape CLIMBABLE_COLLISION_SHAPE = Block.box((double)1.0, (double)0.0, (double)1.0, (double)15.0, (double)16.0, (double)15.0);

    public RatHouseBlock(BlockBehaviour.Properties properties) {
        super(properties);
        try {
            this.registerDefaultState((BlockState)((BlockState)this.getStateDefinition().any()).setValue((Property)FACING, (Comparable)Direction.NORTH));
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0438\u043d\u0438\u0446\u0438\u0430\u043b\u0438\u0437\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u0431\u043b\u043e\u043a rat_house: {}", exception.getMessage(), exception);
            throw exception;
        }
    }



    public static BlockPos getEntryPos(BlockState state, BlockPos pos) {
        try {
            Direction facing = state.hasProperty((Property)FACING) ? (Direction)state.getValue((Property)FACING) : Direction.NORTH;
            return pos.relative(facing);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u0447\u0438\u0441\u043b\u0438\u0442\u044c \u0432\u0445\u043e\u0434 \u0434\u043e\u043c\u0438\u043a\u0430 \u0432 {}: {}", pos, exception.getMessage(), exception);
            throw exception;
        }
    }

    public static Vec3 getEntryCenter(BlockState state, BlockPos pos) {
        try {
            return Vec3.atBottomCenterOf((Vec3i)RatHouseBlock.getEntryPos(state, pos)).add(0.0, 0.1, 0.0);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u0447\u0438\u0441\u043b\u0438\u0442\u044c \u0446\u0435\u043d\u0442\u0440 \u0432\u0445\u043e\u0434\u0430 \u0434\u043e\u043c\u0438\u043a\u0430 \u0432 {}: {}", pos, exception.getMessage(), exception);
            throw exception;
        }
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_SHAPE;
    }

    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CLIMBABLE_COLLISION_SHAPE;
    }

    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        try {
            if (entity == null) {
                return false;
            }
            double localX = entity.getX() - (double)pos.getX();
            double localZ = entity.getZ() - (double)pos.getZ();
            double entityY = entity.getY();
            boolean insideHorizontalBounds = localX >= -0.1 && localX <= 1.1 && localZ >= -0.1 && localZ <= 1.1;
            boolean nearSide = localX <= 0.18 || localX >= 0.82 || localZ <= 0.18 || localZ >= 0.82;
            boolean nearHeight = entityY >= (double)pos.getY() - 0.35 && entityY <= (double)pos.getY() + 1.65;
            return insideHorizontalBounds && nearSide && nearHeight;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043b\u0435\u0441\u0442\u043d\u0438\u0446\u0443 \u0434\u043e\u043c\u0438\u043a\u0430 \u0432 {} \u0434\u043b\u044f \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0438 {}: {}", pos, entity == null ? "null" : entity.getUUID(), exception.getMessage(), exception);
            throw exception;
        }
    }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        try {
            RatHouseBlockEntity house;
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RatHouseBlockEntity && (house = (RatHouseBlockEntity)blockEntity).hasStoredRat() && random.nextInt(8) == 0) {
                Vec3 entry = RatHouseBlock.getEntryCenter(state, pos);
                level.addParticle((ParticleOptions)ParticleTypes.HAPPY_VILLAGER, entry.x, entry.y + 0.35, entry.z, 0.0, 0.02, 0.0);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043a\u0430\u0437\u0430\u0442\u044c \u0438\u043d\u0434\u0438\u043a\u0430\u0442\u043e\u0440 \u0437\u0430\u043d\u044f\u0442\u043e\u0433\u043e \u0434\u043e\u043c\u0438\u043a\u0430 \u0432 {}: {}", pos, exception.getMessage(), exception);
            throw exception;
        }
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
ItemStack stack = player.getItemInHand(hand);

        try {
            if (!stack.isEmpty() || player.isCrouching()) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof RatHouseBlockEntity)) {
                throw new IllegalStateException("\u0423 \u0434\u043e\u043c\u0438\u043a\u0430 \u043d\u0435\u0442 RatHouseBlockEntity");
            }
            RatHouseBlockEntity house = (RatHouseBlockEntity)blockEntity;
            return house.releaseRatToPlayer(player) ? InteractionResult.CONSUME : InteractionResult.CONSUME;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u0442\u044c \u041f\u041a\u041c \u043f\u043e rat_house \u0432 {}: {}", pos, exception.getMessage(), exception);
            throw exception;
        }
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        try {
            Direction playerFacing = context.getHorizontalDirection();
            Direction holeFacing = playerFacing.getOpposite();
            return (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)holeFacing);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u044b\u0441\u0442\u0430\u0432\u0438\u0442\u044c FACING \u0434\u043b\u044f rat_house \u0432 {}: {}", context.getClickedPos(), exception.getMessage(), exception);
            throw exception;
        }
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)rotation.rotate((Direction)state.getValue((Property)FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation((Direction)state.getValue((Property)FACING)));
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return RatHouseBlock.createTickerHelper(type, (BlockEntityType)((BlockEntityType)RatsBlockEntityRegistry.RAT_HOUSE.get()), RatHouseBlockEntity::tick);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        try {
            return new RatHouseBlockEntity(pos, state);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c block entity rat_house \u0432 {}: {}", pos, exception.getMessage(), exception);
            throw exception;
        }
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        try {
            BlockEntity blockEntity;
            if (!state.is(newState.getBlock()) && (blockEntity = level.getBlockEntity(pos)) instanceof RatHouseBlockEntity) {
                RatHouseBlockEntity house = (RatHouseBlockEntity)blockEntity;
                Vec3 releasePos = RatHouseBlock.getEntryCenter(state, pos);
                house.releaseRatToWorld(releasePos.x, releasePos.y, releasePos.z);
                house.onRemoved();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0443\u0434\u0430\u043b\u0438\u0442\u044c rat_house \u0432 {}: {}", pos, exception.getMessage(), exception);
            throw exception;
        }
    }
}

