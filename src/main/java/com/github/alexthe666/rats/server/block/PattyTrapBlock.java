package com.github.alexthe666.rats.server.block;

import com.github.alexthe666.rats.registry.RatsSoundRegistry;
import com.github.alexthe666.rats.server.block.entity.PattyTrapBlockEntity;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.github.alexthe666.rats.server.items.RatSliceItem;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public class PattyTrapBlock extends BaseEntityBlock {
    private static final VoxelShape NS = box(3,0,1,13,4,15), EW = box(1,0,3,15,4,13);

    public PattyTrapBlock() {
        super(Properties.of().strength(.6f).sound(SoundType.WOOD).noCollission().noOcclusion());
        registerDefaultState(defaultBlockState().setValue(RatTrapBlock.FACING, Direction.NORTH).setValue(RatTrapBlock.SHUT,false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        builder.add(RatTrapBlock.FACING,RatTrapBlock.SHUT);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(RatTrapBlock.FACING,ctx.getHorizontalDirection().getOpposite());
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(RatTrapBlock.FACING).getAxis()==Direction.Axis.Z ? NS : EW;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos next) {
        return side==Direction.DOWN && !canSurvive(state,level,pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state,side,neighbor,level,pos,next);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PattyTrapBlockEntity(pos,state); }

    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if(level.isClientSide || !(entity instanceof Rat rat) || !rat.isAlive() || rat.isRemoved() || rat.isTame() || rat.getOwnerUUID()!=null || rat.isDeadInTrap() || rat.isPassenger() || rat.isVehicle()) return;
        if(!(level.getBlockEntity(pos) instanceof PattyTrapBlockEntity trap) || trap.occupied()) return;
        if(!getShape(state,level,pos,CollisionContext.empty()).bounds().move(pos).intersects(rat.getBoundingBox())) return;
        trap.store(RatSliceItem.pressed(rat));
        rat.discard();
        level.setBlockAndUpdate(pos,state.setValue(RatTrapBlock.SHUT,true));
        level.playSound(null,pos,RatsSoundRegistry.RAT_TRAP_CLOSE.get(),SoundSource.BLOCKS,1,1);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(!state.getValue(RatTrapBlock.SHUT)) return InteractionResult.PASS;
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof PattyTrapBlockEntity trap && trap.occupied()) {
            int slot=player.getInventory().getFreeSlot();
            if(slot<0) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.rats.rat_press.full"),true);
                return InteractionResult.FAIL;
            }
            player.getInventory().setItem(slot,trap.take());
            level.setBlockAndUpdate(pos,state.setValue(RatTrapBlock.SHUT,false));
            level.playSound(null,pos,RatsSoundRegistry.RAT_TRAP_OPEN.get(),SoundSource.BLOCKS,1,1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if(!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof PattyTrapBlockEntity trap && trap.occupied())
            net.minecraft.world.Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.3,pos.getZ()+.5,trap.take());
        super.onRemove(state,level,pos,next,moving);
    }
}
