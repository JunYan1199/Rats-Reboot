package guildjane;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class GuildDuty {
    private final GuildJane.Receptionist jane;
    private Vec3 desk;
    private float facing;
    // Walk around the open end of the counter, inspect the hall, then retrace the route.
    private static final double[][] STOPS={{3,0},{3,2.6},{4.6,3.1},{3,2.6},{3,0},{0,0}};
    private int stop=-1,wait,pathTimeout,thinking;
    private UUID guest;
    int talkTicks;
    GuildDuty(GuildJane.Receptionist jane){this.jane=jane;}
    boolean atDesk(){return stop<0;}
    Vec3 home(){return desk;}
    void speak(ServerPlayer player,int ticks){
        guest=player.getUUID();talkTicks=ticks;
        jane.getNavigation().stop();pathTimeout=0;
        jane.getEntityData().set(GuildJane.Receptionist.SPEAK_UNTIL,ticks);
        jane.level().broadcastEntityEvent(jane,(byte)62);
    }
    private void schedule(){wait=760+jane.getRandom().nextInt(950);}
    private Vec3 point(int index){
        Vec3 f=Vec3.directionFromRotation(0,facing);
        return desk.add(-f.z*STOPS[index][0]+f.x*STOPS[index][1],0,f.x*STOPS[index][0]+f.z*STOPS[index][1]);
    }
    private void advance(){
        jane.getNavigation().stop();pathTimeout=0;
        if(++stop==STOPS.length){stop=-1;schedule();}
        else if(stop==3)thinking=40+jane.getRandom().nextInt(50);
    }
    void tick(){
        if(desk==null){desk=jane.position();facing=jane.getYRot();schedule();}
        if(talkTicks>0)talkTicks--;
        Player customer=guest==null?null:jane.level().getPlayerByUUID(guest);
        boolean inTalk=customer instanceof ServerPlayer p&&customer.distanceToSqr(jane)<36&&Conversation.engaged(p,jane);
        if(!inTalk){customer=Conversation.otherGuest(jane);inTalk=customer!=null;guest=inTalk?customer.getUUID():null;}
        Player gaze=inTalk?customer:jane.level().getNearestPlayer(jane,5.5);
        if(gaze!=null&&(inTalk||atDesk()||thinking>0)){
            jane.getLookControl().setLookAt(gaze,13,10);
        }
        if(inTalk){jane.getNavigation().stop();return;}
        if(atDesk()){
            jane.setYRot(Mth.approachDegrees(jane.getYRot(),facing,5));jane.setYBodyRot(jane.getYRot());
            if(--wait<=0&&(gaze==null||gaze.distanceToSqr(jane)>11)){stop=0;pathTimeout=0;}
            return;
        }
        if(thinking>0){thinking--;return;}
        Vec3 destination=point(stop);
        double remaining=jane.position().distanceToSqr(destination);
        if(remaining<(stop==STOPS.length-1?.034:.10)){advance();return;}
        if(clearFloorTo(destination)){
            if(!jane.getNavigation().isDone())jane.getNavigation().stop();
            jane.getMoveControl().setWantedPosition(destination.x,destination.y,destination.z,.66);
            return;
        }
        if(pathTimeout>0&&--pathTimeout>0&&!jane.getNavigation().isDone())return;
        if(jane.tickCount%10!=0)return;
        var path=jane.getNavigation().createPath(BlockPos.containing(destination),0);
        if(path==null||!path.canReach()||!insideHall(path)){
            if(stop<STOPS.length-1){stop=STOPS.length-1;thinking=18;}else thinking=80;
            return;
        }
        jane.getNavigation().moveTo(path,.66);
        pathTimeout=85;
    }
    private boolean insideHall(net.minecraft.world.level.pathfinder.Path path){
        for(int i=0;i<path.getNodeCount();i++){
            var node=path.getNode(i);
            if(Math.abs(node.y-desk.y)>1.25||Math.hypot(node.x+.5-desk.x,node.z+.5-desk.z)>7.2)return false;
        }
        return true;
    }
    // Probe from ankle height to let the receptionist cross the carpet edge.
    private boolean clearFloorTo(Vec3 end){
        var delta=end.subtract(jane.position());int steps=Math.max(1,(int)Math.ceil(delta.length()*3));
        var hull=jane.getBoundingBox();
        for(int i=1;i<=steps;i++){
            var offset=delta.scale((double)i/steps);
            var probe=hull.move(offset.x,.15,offset.z);
            if(!jane.level().noCollision(jane,probe)||jane.level().noCollision(jane,probe.move(0,-.48,0)))return false;
        }
        return true;
    }
    void save(CompoundTag n){
        if(desk==null)return;
        n.putDouble("DeskX",desk.x);n.putDouble("DeskY",desk.y);n.putDouble("DeskZ",desk.z);
        n.putFloat("DeskYaw",facing);n.putInt("DutyStop",stop);n.putInt("DeskWait",wait);
    }
    void load(CompoundTag n){
        if(!n.contains("DeskX"))return;
        desk=new Vec3(n.getDouble("DeskX"),n.getDouble("DeskY"),n.getDouble("DeskZ"));facing=n.getFloat("DeskYaw");
        stop=Mth.clamp(n.getInt("DutyStop"),-1,STOPS.length-1);wait=n.getInt("DeskWait");
    }
}
