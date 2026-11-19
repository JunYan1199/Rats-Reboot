package guildjane;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.registries.*;

final class GuildHall {
    static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,GuildJane.ID);
    static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,GuildJane.ID);
    static final RegistryObject<Block> BOARD=BLOCKS.register("guild_task_board",Board::new);
    static final RegistryObject<Item> DEED=ITEMS.register("guild_deed",Deed::new);
    static final ResourceLocation HALL=new ResourceLocation(GuildJane.ID,"hall");

    enum Tile implements StringRepresentable {
        P00,P01,P02,P10,P11,P12,P20,P21,P22;
        public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    static final class Board extends HorizontalDirectionalBlock {
        static final EnumProperty<Tile> PART=EnumProperty.create("part",Tile.class);
        Board(){
            super(Properties.copy(Blocks.DARK_OAK_PLANKS).noOcclusion().noLootTable());
            registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,Tile.P00));
        }
        protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART);}
        public BlockState rotate(BlockState state,Rotation r){return state.setValue(FACING,r.rotate(state.getValue(FACING)));}
        public BlockState mirror(BlockState state,Mirror m){return rotate(state,m.getRotation(state.getValue(FACING)));}
        public VoxelShape getShape(BlockState s,BlockGetter w,BlockPos p,CollisionContext c){
            return switch(s.getValue(FACING)){
                case NORTH -> box(0,0,13,16,16,16);
                case SOUTH -> box(0,0,0,16,16,3);
                case EAST -> box(0,0,0,3,16,16);
                default -> box(13,0,0,16,16,16);
            };
        }
        public InteractionResult use(BlockState s,Level w,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            if(player instanceof ServerPlayer p&&hand==InteractionHand.MAIN_HAND)Conversation.openBoard(p,pos);
            return InteractionResult.sidedSuccess(w.isClientSide);
        }
    }
    static final class Deed extends Item {
        Deed(){super(new Item.Properties().stacksTo(1));}
        public InteractionResult useOn(UseOnContext c){
            Player player=c.getPlayer();
            if(player==null||c.getClickedFace()!=Direction.UP)return InteractionResult.PASS;
            if(!(c.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
            var template=level.getStructureManager().get(HALL).orElse(null);
            if(template==null)return InteractionResult.FAIL;
            Rotation r=switch(c.getHorizontalDirection()){
                case WEST -> Rotation.CLOCKWISE_90; case NORTH -> Rotation.CLOCKWISE_180;
                case EAST -> Rotation.COUNTERCLOCKWISE_90; default -> Rotation.NONE;
            };
            var settings=new StructurePlaceSettings().setRotation(r).setIgnoreEntities(false);
            var origin=c.getClickedPos().above().subtract(StructureTemplate.calculateRelativePosition(settings,new BlockPos(9,0,0)));
            var bounds=template.getBoundingBox(settings,origin);
            var first=new BlockPos(bounds.minX(),bounds.minY(),bounds.minZ());
            var last=new BlockPos(bounds.maxX(),bounds.maxY(),bounds.maxZ());
            if(level.isOutsideBuildHeight(first)||level.isOutsideBuildHeight(last)||!level.hasChunksAt(first,last)
                ||!level.getWorldBorder().isWithinBounds(first)||!level.getWorldBorder().isWithinBounds(last)){
                player.displayClientMessage(Component.literal("公会地契需要一片已加载、位于世界边界内的平地。"),true);
                return InteractionResult.FAIL;
            }
            for(var pos:BlockPos.betweenClosed(first,last)){
                if(!level.getBlockState(pos).canBeReplaced()||!level.getFluidState(pos).isEmpty()||
                    !level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,c.getItemInHand())){
                    player.displayClientMessage(Component.literal("前方需要 19 × 18 格空地，上方留空 15 格。"),true);
                    return InteractionResult.FAIL;
                }
            }
            if(!template.placeInWorld(level,origin,origin,settings,level.getRandom(),Block.UPDATE_ALL))return InteractionResult.FAIL;
            if(!player.getAbilities().instabuild)c.getItemInHand().shrink(1);
            player.displayClientMessage(Component.literal("冒险公会已开门，接待员在柜台后等你。"),true);
            return InteractionResult.CONSUME;
        }
    }
}
