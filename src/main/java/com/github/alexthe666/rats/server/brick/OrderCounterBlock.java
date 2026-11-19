package com.github.alexthe666.rats.server.brick;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import com.github.alexthe666.rats.registry.RatsBlockEntityRegistry;

public final class OrderCounterBlock extends BaseEntityBlock {
    private static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public OrderCounterBlock(){
        super(BlockBehaviour.Properties.of().strength(2F).sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){return defaultBlockState().setValue(FACING,ctx.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        var bell=switch(state.getValue(FACING)){
            case SOUTH -> Block.box(8,16,2,14,19.2,8);
            case NORTH -> Block.box(2,16,8,8,19.2,14);
            case WEST -> Block.box(8,16,8,14,19.2,14);
            default -> Block.box(2,16,2,8,19.2,8);
        };
        return Shapes.or(Shapes.block(),bell);
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new OrderCounter(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level w,BlockState s,BlockEntityType<T> type){
        return w.isClientSide?null:createTickerHelper(type,RatsBlockEntityRegistry.ORDER_COUNTER.get(),OrderCounter::tick);
    }
    @Override public InteractionResult use(BlockState s,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof OrderCounter counter)CounterNetwork.show(sp,counter);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
