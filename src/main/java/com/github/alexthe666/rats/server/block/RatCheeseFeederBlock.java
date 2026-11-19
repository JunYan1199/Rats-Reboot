package com.github.alexthe666.rats.server.block;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;
import com.github.alexthe666.rats.server.block.entity.RatCheeseFeederBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RatCheeseFeederBlock
extends BaseEntityBlock {
    public static final BooleanProperty HAS_CHEESE = BooleanProperty.create((String)"has_cheese");
    private static final VoxelShape SHAPE = Block.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)2.0, (double)16.0);

    public RatCheeseFeederBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.getStateDefinition().any()).setValue((Property)HAS_CHEESE, (Comparable)Boolean.valueOf(false)));
    }



    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        try {
            if (!state.is(newState.getBlock())) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof RatCheeseFeederBlockEntity) {
                    RatCheeseFeederBlockEntity feeder = (RatCheeseFeederBlockEntity)blockEntity;
                    Containers.dropContents((Level)level, (BlockPos)pos, (Container)feeder);
                    level.updateNeighbourForOutputSignal(pos, (Block)this);
                }
                super.onRemove(state, level, pos, newState, isMoving);
            }
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0443\u0434\u0430\u043b\u0438\u0442\u044c \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 \u0434\u043b\u044f \u0441\u044b\u0440\u0430 \u0432 {}", pos, exception);
            throw exception;
        }
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
ItemStack stack = player.getItemInHand(hand);

        try {
            if (player.isCrouching()) {
                return InteractionResult.PASS;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof RatCheeseFeederBlockEntity)) {
                throw new IllegalStateException("\u0423 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0438 \u043d\u0435\u0442 block entity");
            }
            RatCheeseFeederBlockEntity feeder = (RatCheeseFeederBlockEntity)blockEntity;
            if (!stack.isEmpty() && RatCheeseFeederBlockEntity.isAllowedCheese(stack)) {
                if (level.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }
                int inserted = feeder.addCheeseFrom(stack);
                if (inserted <= 0) {
                    RatsMod.LOGGER.warn("\u0418\u0433\u0440\u043e\u043a {} \u043d\u0435 \u0441\u043c\u043e\u0433 \u043f\u043e\u043b\u043e\u0436\u0438\u0442\u044c \u0441\u044b\u0440 \u0432 \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 {}: \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c \u043f\u043e\u043b\u043e\u043d", player.getGameProfile().getName(), pos);
                    return InteractionResult.CONSUME;
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4f, 1.0f);
                return InteractionResult.CONSUME;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            player.openMenu(state.getMenuProvider(level, pos));
            return InteractionResult.CONSUME;
        }
        catch (RuntimeException exception) {
            RatsMod.LOGGER.error("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0431\u0440\u0430\u0431\u043e\u0442\u0430\u0442\u044c \u043a\u043e\u0440\u043c\u0443\u0448\u043a\u0443 \u0434\u043b\u044f \u0441\u044b\u0440\u0430 \u0432 {}", pos, exception);
            throw exception;
        }
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RatCheeseFeederBlockEntity(pos, state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return RatCheeseFeederBlock.createTickerHelper(type, (BlockEntityType)((BlockEntityType)RatsBlockEntityRegistry.RAT_CHEESE_FEEDER.get()), RatCheeseFeederBlockEntity::tick);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{HAS_CHEESE});
    }
}

