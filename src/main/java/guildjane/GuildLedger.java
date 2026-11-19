package guildjane;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import java.util.*;

@Mod.EventBusSubscriber(modid=GuildJane.ID)
public class GuildLedger extends SavedData {
    final CompoundTag data;
    GuildLedger(){this(new CompoundTag());}
    GuildLedger(CompoundTag data){this.data=data;}
    static GuildLedger get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(GuildLedger::new,GuildLedger::new,"guildjane_ledger");}
    @Override public CompoundTag save(CompoundTag dest){return data.copy();}
    CompoundTag player(ServerPlayer p){
        String id=p.getStringUUID();if(!data.contains(id)){data.put(id,new CompoundTag());setDirty();}
        return data.getCompound(id);
    }
    boolean unlocked(){return data.getBoolean("unlocked");}
    boolean night(){return unlocked()&&data.getBoolean("night");}
    void outfit(boolean night){data.putBoolean("night",night);setDirty();}
    static List<TamedRat> available(ServerPlayer p,net.minecraft.world.phys.AABB station){
        return p.level().getEntitiesOfClass(TamedRat.class,station.inflate(12),r->r.isAlive()&&r.isOwnedBy(p)&&!r.isPassenger()&&!r.isVehicle()&&!r.isLeashed()).stream().sorted(Comparator.comparingDouble(r->r.distanceToSqr(station.getCenter()))).toList();
    }
    boolean dispatch(ServerPlayer p,net.minecraft.world.phys.AABB station,List<UUID> picks,Bounty quest){
        var state=player(p);if(picks.isEmpty()||picks.size()>3||!quest.exists()||(quest.food()&&quest.held(p)<quest.count()))return false;
        var team=available(p,station).stream().filter(r->picks.contains(r.getUUID())).toList();
        if(team.size()!=picks.size())return false;
        var list=new ListTag();
        for(var rat:team){var n=new CompoundTag();if(!rat.save(n))return false;n.putInt("GuildShownLevel",rat.getRatLevel());list.add(n);}
        var job=new CompoundTag();job.put("away",list);job.putString("dim",p.level().dimension().location().toString());
        job.putDouble("x",p.getX());job.putDouble("y",p.getY());job.putDouble("z",p.getZ());
        job.putLong("duration",quest.duration());job.putLong("due",p.server.overworld().getGameTime()+quest.duration());
        job.putString("lead",team.get(0).getName().getString());job.putInt("count",team.size());
        quest.book(p,job,team.stream().mapToInt(TamedRat::getRatLevel).average().orElseThrow());
        String slot=state.contains("job")?"job_"+UUID.randomUUID():"job";state.put(slot,job);state.putString("latestJob",slot);setDirty();team.forEach(Entity::discard);return true;
    }
    static boolean ready(CompoundTag job){return job.contains("away")&&job.getList("away",10).isEmpty();}
    static String story(CompoundTag j){
        if(j.contains("story"))return j.getString("story");
        String lead=j.getString("lead");
        if(!j.getBoolean("success"))return lead+"在碎石坡前停了下来。小队沿着来时的记号撤回公会，这次没带回矿样。";
        return j.getBoolean("hard")?lead+"顺着矿车留下的车辙摸到旧矿口。队员们把矿样滚进布袋，回来的时候，一路都听得见袋子响。":lead+"先钻过倒下的栅栏，在桥洞下找到了邮袋。封蜡完好，袋角多了一排帮忙拖拽的小牙印。";
    }
    static List<String> jobs(CompoundTag me){
        return me.getAllKeys().stream().filter(k->(k.equals("job")||k.startsWith("job_"))&&me.contains(k,10)).sorted().toList();
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||e.getServer().getTickCount()%20!=0)return;
        var server=e.getServer();var ledger=get(server);
        for(String key:ledger.data.getAllKeys()){
            if(!ledger.data.contains(key,10))continue;
            for(String slot:jobs(ledger.data.getCompound(key))){
            var job=ledger.data.getCompound(key).getCompound(slot);
            if(!job.contains("away")||ready(job)||server.overworld().getGameTime()<job.getLong("due"))continue;
            var w=server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation(job.getString("dim"))));
            if(w==null)continue;
            var origin=new Vec3(job.getDouble("x"),job.getDouble("y"),job.getDouble("z"));
            if(!w.hasChunkAt(BlockPos.containing(origin)))continue;
            var rats=job.getList("away",10);
            for(int i=rats.size()-1;i>=0;i--){
                var tag=rats.getCompound(i);
                if(w.getEntity(tag.getUUID("UUID"))!=null){rats.remove(i);ledger.setDirty();continue;}
                var entity=EntityType.create(tag,w).orElse(null);
                if(!(entity instanceof TamedRat rat))continue;
                rat.moveTo(origin.x,origin.y,origin.z,0,0);
                if(w.addFreshEntity(rat)){rats.remove(i);ledger.setDirty();}
            }
            var owner=server.getPlayerList().getPlayer(UUID.fromString(key));
            if(ready(job)&&owner!=null)owner.displayClientMessage(net.minecraft.network.chat.Component.literal("鼠队已归来 · 找公会接待员结算"),true);
            }
        }
    }
}
