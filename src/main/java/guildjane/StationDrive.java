package guildjane;
import com.simibubi.create.content.kinetics.motor.*;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.*;

final class StationDrive extends CreativeMotorBlock {
    static final RegistryObject<Block> BLOCK=GuildHall.BLOCKS.register("wireless_drive",StationDrive::new);
    static final RegistryObject<BlockEntityType<Drive>> TYPE=PowerStation.TYPES.register("wireless_drive",()->BlockEntityType.Builder.of(Drive::new,BLOCK.get()).build(null));
    static void register(){}
    StationDrive(){super(Properties.copy(Blocks.IRON_BLOCK).noLootTable());}
    public BlockEntityType<? extends CreativeMotorBlockEntity> getBlockEntityType(){return TYPE.get();}
    static boolean attach(ServerPlayer p,PowerStation.Unit u,BlockPos target,Direction first){
        var w=p.serverLevel();var s=w.getBlockState(target);if(!(s.getBlock() instanceof IRotate shaft))return false;
        var dirs=new java.util.LinkedHashSet<Direction>();dirs.add(first);dirs.addAll(java.util.List.of(Direction.values()));
        for(var d:dirs){var pos=target.relative(d);if(!shaft.hasShaftTowards(w,target,s,d)||!w.hasChunkAt(pos)||!w.mayInteract(p,pos)||!w.getWorldBorder().isWithinBounds(pos)||w.isOutsideBuildHeight(pos))continue;
            if(w.getBlockEntity(pos) instanceof Drive old&&u.getBlockPos().equals(old.station)&&old.getBlockState().getValue(FACING)==d.getOpposite())return true;
            if(!w.isEmptyBlock(pos))continue;
            if(!w.setBlockAndUpdate(pos,BLOCK.get().defaultBlockState().setValue(FACING,d.getOpposite())))continue;
            if(w.getBlockEntity(pos) instanceof Drive drive){drive.station=u.getBlockPos();drive.setChanged();return true;}
        }return false;
    }
    static class Drive extends CreativeMotorBlockEntity {
        BlockPos station;boolean running;int age;
        Drive(BlockPos p,BlockState s){super(TYPE.get(),p,s);}
        protected Block getStressConfigKey(){return ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("create:creative_motor"));}
        public float getGeneratedSpeed(){return running?convertToDirection(256,getBlockState().getValue(FACING)):0;}
        protected void write(CompoundTag t,boolean client){super.write(t,client);if(station!=null)t.putLong("station",station.asLong());t.putBoolean("running",running);}
        protected void read(CompoundTag t,boolean client){super.read(t,client);station=t.contains("station")?BlockPos.of(t.getLong("station")):null;running=t.getBoolean("running");}
        public void tick(){super.tick();if(level==null||level.isClientSide)return;
            var target=worldPosition.relative(getBlockState().getValue(FACING));boolean active=false;
            if(station!=null&&level.hasChunkAt(station)){
                if(!(level.getBlockEntity(station) instanceof PowerStation.Unit u)||!u.links.contains(target.asLong())||!(level.getBlockState(target).getBlock() instanceof IRotate)){if(++age>2)level.removeBlock(worldPosition,false);return;}
                age=0;active=!u.animal.isEmpty()&&u.mode.equals("机械动力应力");
            }else if(station==null&&++age>5){level.removeBlock(worldPosition,false);return;}
            if(active!=running){running=active;updateGeneratedRotation();sendData();setChanged();}
        }
    }
}
