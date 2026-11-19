package com.github.alexthe666.rats.server.legion;
import com.github.alexthe666.rats.server.entity.rat.MasteryMagic;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.inventory.container.RatContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import java.util.*;

public final class RatLegion {
    public static final List<String> ROLES=List.of("attack","control","heal","buff","free");
    private static final boolean IRON=ModList.get().isLoaded("irons_spellbooks"), GOETY=ModList.get().isLoaded("goety");
    UUID group,marked;
    String role="free";
    long markExpires,next;
    private final Map<String,Long> ready=new HashMap<>();
    private record BuffTarget(String spell,UUID ally) {}
    private final Map<BuffTarget,Long> buffed=new HashMap<>();
    private LegionSpell active;
    private LivingEntity target;
    private int elapsed;
    private long rejectedUntil;
    public void startSpellMode(TamedRat r){next=0;stop(r,true);r.spellBattleStatus("search",0);}
    private void updateSpellMode(TamedRat r,ServerLevel w){
        if(r.getCommand()!=com.github.alexthe666.rats.server.entity.rat.RatCommand.SPELL_COMBAT||(r.tickCount+r.getId())%10!=0)return;
        List<LegionSpell> all=equipped(r);
        var usable=all.stream().filter(s->role.equals("free")||role.equals(SpellRoles.get(s.id()))).toList();
        long count=usable.stream().map(LegionSpell::id).distinct().count();
        String status;
        if(count==0){
            boolean carrier=false,written=false;
            for(int i=0;i<RatContainer.SIZE;i=i==0?RatContainer.BACKPACK:i+1){
                var stack=r.getInventory().getItem(i);if(!isCarrier(stack))continue;
                carrier=true;written|=IRON&&IronCast.carrier(stack)&&IronCast.hasPages(stack)||GOETY&&GoetyCast.carrier(stack)&&GoetyCast.hasFoci(stack);
            }
            status=!carrier?"no_book":!all.isEmpty()?"role":written?"unsupported":"empty";
        }else if(active!=null)status="casting";
        else{
            var target=enemy(r,w);if(target!=null&&r.getTarget()!=target)r.setTarget(target);
            boolean healOnly=usable.stream().allMatch(s->"heal".equals(SpellRoles.get(s.id())));
            boolean wounded=healOnly&&allies(r,w).stream().anyMatch(e->e.getHealth()<e.getMaxHealth()-.1f);
            status=rejectedUntil>w.getGameTime()?"conditions":healOnly&&!wounded?"healthy":target==null&&!wounded?"search":usable.stream().allMatch(s->ready.getOrDefault(s.id(),0L)>w.getGameTime())?"cooldown":"ready";
        }
        r.spellBattleStatus(status,count);
    }
    private int actualTicks;
    private int castTicks;
    private MasteryMagic.Cast castContext;
    public boolean isCasting(){return active!=null;}
    public boolean sameSquad(RatLegion other){return Objects.equals(group,other.group);}
    public boolean hasAttackSchool(TamedRat r,boolean book){return ("free".equals(role)||"attack".equals(role))&&equipped(r).stream().anyMatch(s->"attack".equals(SpellRoles.get(s.id()))&&s.isBook()==book);}
    public boolean shortenCooldown(long now,int ticks){
        var candidate=ready.entrySet().stream().filter(e->e.getValue()>now&&(active==null||!active.id().equals(e.getKey()))).max(Map.Entry.comparingByValue());
        candidate.ifPresent(e->e.setValue(Math.max(now,e.getValue()-ticks)));return candidate.isPresent();
    }
    public void assign(TamedRat rat,UUID squad,String job){
        stop(rat,true);group=squad;role=job;marked=null;buffed.clear();next=0;
    }
    public void mark(LivingEntity enemy,long now){marked=enemy.getUUID();markExpires=now+2400;next=0;}
    public void save(CompoundTag root){
        CompoundTag n=new CompoundTag();
        if(group!=null){n.putUUID("Squad",group);n.putString("Role",role);}
        if(!ready.isEmpty()){CompoundTag c=new CompoundTag();ready.forEach(c::putLong);n.put("Cooldowns",c);}
        if(!n.isEmpty())root.put("SpellLegion",n);
    }
    public void load(CompoundTag root){
        CompoundTag n=root.getCompound("SpellLegion");group=n.hasUUID("Squad")?n.getUUID("Squad"):null;
        role=group!=null&&ROLES.contains(n.getString("Role"))?n.getString("Role"):"free";
        var c=n.getCompound("Cooldowns");ready.clear();for(String k:c.getAllKeys())ready.put(k,c.getLong(k));
        marked=null;active=null;target=null;next=0;buffed.clear();
    }
    public static boolean isCarrier(ItemStack stack){return !stack.isEmpty()&&(IRON&&IronCast.carrier(stack)||GOETY&&GoetyCast.carrier(stack));}
    public static boolean practiceTarget(LivingEntity e){return e.getType().is(PRACTICE);}
    private static final net.minecraft.tags.TagKey<EntityType<?>> PRACTICE=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,new net.minecraft.resources.ResourceLocation("rats","spell_practice_targets"));
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="rats")
    public static final class PracticeOrders {
        @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public static void swing(net.minecraftforge.event.entity.player.AttackEntityEvent event){
            if(!(event.getEntity().level() instanceof ServerLevel w)||!(event.getTarget() instanceof LivingEntity target)||!practiceTarget(target))return;
            var owner=event.getEntity();
            for(var r:w.getEntitiesOfClass(TamedRat.class,owner.getBoundingBox().inflate(32),a->a.isTame()&&owner.getUUID().equals(a.getOwnerUUID())&&!a.isOrderedToSit()&&a.distanceToSqr(target)<=32*32)){
                r.legion.mark(target,w.getGameTime());
            }
        }
    }
    static boolean hostile(LivingEntity e,LivingEntity ally){
        return e!=ally && e.isAlive() && (e instanceof Enemy || practiceTarget(e)) && !(e instanceof TamableAnimal t&&t.isTame()) && !ally.isAlliedTo(e);
    }
    private static List<LegionSpell> equipped(TamedRat r){
        List<LegionSpell> spells=new ArrayList<>();
        for(int i=0;i<RatContainer.SIZE;i=i==0?RatContainer.BACKPACK:i+1){
            ItemStack stack=r.getInventory().getItem(i);
            if(stack.isEmpty())continue;
            if(IRON&&IronCast.carrier(stack))IronCast.collect(r,i,stack,spells);
            if(GOETY&&GoetyCast.carrier(stack))GoetyCast.collect(r,i,stack,spells);
        }
        return spells;
    }
    public long countEquipped(TamedRat r){
        return equipped(r).stream().filter(s->role.equals("free")||role.equals(SpellRoles.get(s.id()))).map(LegionSpell::id).distinct().count();
    }
    private LivingEntity enemy(TamedRat r,ServerLevel w){
        if(marked!=null){
            Entity e=w.getEntity(marked);
            if(e instanceof LivingEntity l&&hostile(l,r)&&w.getGameTime()<markExpires&&r.distanceToSqr(l)<=48*48)return l;
            marked=null;
        }
        if(r.getTarget()!=null&&hostile(r.getTarget(),r)&&r.distanceToSqr(r.getTarget())<32*32)return r.getTarget();
        if(r.getOwner() instanceof Player p && p.getLastHurtMob()!=null && p.tickCount-p.getLastHurtMobTimestamp()<160 && hostile(p.getLastHurtMob(),r)&&r.distanceToSqr(p.getLastHurtMob())<32*32)return p.getLastHurtMob();
        boolean spellMode=r.getCommand()==com.github.alexthe666.rats.server.entity.rat.RatCommand.SPELL_COMBAT;
        int range=spellMode?32:20;
        return w.getEntitiesOfClass(Mob.class,r.getBoundingBox().inflate(range),m->hostile(m,r)&&r.distanceToSqr(m)<=range*range&&(r.hasLineOfSight(m)||spellMode&&r.distanceToSqr(m)<16*16)).stream().min(Comparator.comparingDouble(r::distanceToSqr)).orElse(null);
    }
    private List<LivingEntity> allies(TamedRat r,ServerLevel w){
        List<LivingEntity> a=new ArrayList<>();
        for(TamedRat other:w.getEntitiesOfClass(TamedRat.class,r.getBoundingBox().inflate(16),t->t.isAlive()&&Objects.equals(r.getOwnerUUID(),t.getOwnerUUID())&&Objects.equals(group,t.legion.group)))a.add(other);
        LivingEntity owner=r.getOwner(); if(owner!=null && r.distanceToSqr(owner)<=256 && owner.isAlive())a.add(owner);
        return a;
    }
    private void aim(TamedRat r,LivingEntity t){
        if(r==t)return;
        Vec3 point=t.getBoundingBox().getCenter();
        Vec3 delta=point.subtract(r.getEyePosition());
        float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
        r.setYRot(yaw);r.setYHeadRot(yaw);r.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));
        r.getLookControl().setLookAt(point.x,point.y,point.z,180,180);
    }
    private void stop(TamedRat r,boolean cancel){
        if(active==null)return;
        var spell=active;var recipient=target;active=null;target=null;
        var context=castContext;castContext=null;
        try{MasteryMagic.run(context,()->LegionSupport.run(r,recipient,SpellRoles.get(spell.id()),()->spell.finish(cancel)));
            if(!cancel)MasteryMagic.finished(context);}
        catch(RuntimeException | LinkageError ex){ready.put(spell.id(),r.level().getGameTime()+1200);RatsMod.LOGGER.warn("Rat failed to finish spell {}",spell.id(),ex);}
    }
    public void tick(TamedRat r,ServerLevel w){
        updateSpellMode(r,w);
        if(!IRON&&!GOETY)return;
        long now=w.getGameTime();
        if(active==null&&now<next)return;
        if(active==null)next=now+10;
        if(r.isBaby()||!r.isTame()||r.isOrderedToSit()||r.isPassenger()||r.isNoAi()||!r.isAlive()||r.isBrickCooking()){stop(r,true);return;}
        if(active!=null){
            try{
                if(!target.isAlive()||target.level()!=w||r.distanceToSqr(target)>48*48||!active.available()||!r.hasLineOfSight(target)&&target!=r){stop(r,true);return;}
                aim(r,target);
                actualTicks++;
                int duration=active.duration();
                int due=(int)Math.min(duration,(long)actualTicks*duration/Math.max(1,castTicks));
                while(elapsed<due){MasteryMagic.run(castContext,()->LegionSupport.run(r,target,SpellRoles.get(active.id()),()->active.tick(elapsed)));elapsed++;}
                if(elapsed>=duration){
                    if("buff".equals(SpellRoles.get(active.id())))buffed.put(new BuffTarget(active.id(),target.getUUID()),now+r.spellCooldownTicks((int)Math.min(Integer.MAX_VALUE,Math.max(200L,active.cooldown()*3L))));
                    stop(r,false);next=now+r.spellCooldownTicks(12);
                }
            }catch(RuntimeException | LinkageError ex){if(active!=null)ready.put(active.id(),now+1200);RatsMod.LOGGER.warn("Rat interrupted spell {}",active==null?"":active.id(),ex);stop(r,true);next=now+40;}
            return;
        }
        List<LegionSpell> spells=equipped(r);
        if(spells.isEmpty())return;
        ready.values().removeIf(time->time<=now);
        for(long end:ready.values())next=Math.min(next,end);buffed.values().removeIf(time->time<=now);
        spells.removeIf(s->SpellRoles.get(s.id())==null||ready.containsKey(s.id())||!role.equals("free")&&!role.equals(SpellRoles.get(s.id())));
        if(spells.isEmpty())return;
        var enemy=enemy(r,w);var allies=allies(r,w);
        var wounded=allies.stream().filter(a->a.getHealth()<a.getMaxHealth()-.1f).min(Comparator.comparingDouble(a->a.getHealth()/a.getMaxHealth())).orElse(null);

        Collections.rotate(spells,r.getRandom().nextInt(spells.size()));
        if(wounded!=null)spells.sort(Comparator.comparingInt(s->"heal".equals(SpellRoles.get(s.id()))?0:1));
        for(var spell:spells){
            String job=SpellRoles.get(spell.id());
            LivingEntity t=switch(job){case "heal"->wounded;case "buff"->enemy==null?null:allies.stream()
                        .filter(a->!buffed.containsKey(new BuffTarget(spell.id(),a.getUUID())))
                        .min(Comparator.comparingDouble(a->a==r?1000:r.distanceToSqr(a))).orElse(null);default->enemy;};
            if(t==null||t!=r&&!r.hasLineOfSight(t))continue;
            if(job.equals("attack")||job.equals("control"))r.setTarget(t);
            aim(r,t);
            try{
                var context=MasteryMagic.prepare(r,t,job,spell.isBook());
                if(!MasteryMagic.with(context,()->spell.begin(t))){rejectedUntil=now+25;ready.put(spell.id(),now+20);continue;}
                active=spell;target=t;elapsed=0;actualTicks=0;castContext=context;MasteryMagic.started(context);
                castTicks=MasteryMagic.windup(r,spell.duration());
                ready.put(spell.id(),now+castTicks+r.spellCooldownTicks(spell.cooldown()));
                w.sendParticles(ParticleTypes.ENCHANT,r.getX(),r.getEyeY(),r.getZ(),6,.18,.15,.18,.22);
                return;
            }catch(RuntimeException | LinkageError ex){ready.put(spell.id(),now+1200);RatsMod.LOGGER.warn("Rat could not start {}",spell.id(),ex);}
        }
    }
}
