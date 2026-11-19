package com.github.alexthe666.rats.server.entity.rat;

import com.github.alexthe666.rats.server.inventory.container.RatContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import static com.github.alexthe666.rats.server.entity.rat.RatEvolution.*;

@Mod.EventBusSubscriber(modid="rats")
public final class Mastery {
    private Mastery(){}
    private static final String KEY="RatsEvolutionMastery";
    private static final UUID ARMOR=UUID.fromString("fd08c080-21c7-4277-aaa0-000000000001"), SPEED=UUID.fromString("fd08c080-21c7-4277-aaa0-000000000002");
    private static final TagKey<EntityType<?>> BOSSES=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("rats","evolution_bosses"));
    private static final ThreadLocal<Boolean> secondary=ThreadLocal.withInitial(()->false);
    public static int tier(TamedRat r){return !r.isTame()||r.getEvolution()==NONE||r.getRatLevel()<50?0:r.getRatLevel()>=100?2:1;}
    static CompoundTag state(LivingEntity e){
        var root=e.getPersistentData();
        if(!root.contains(KEY))root.put(KEY,new CompoundTag());
        return root.getCompound(KEY);
    }
    static long now(Entity e){return e.level().getGameTime();}
    static boolean on(LivingEntity e,String key){return e.getPersistentData().getCompound(KEY).getLong(key)>now(e);}
    static void timed(LivingEntity e,String key,int ticks){state(e).putLong(key,now(e)+ticks);}
    static boolean use(TamedRat r,String key,int ticks){if(on(r,key))return false;timed(r,key,ticks);return true;}
    static boolean can(TamedRat r,RatEvolution form,int rank){return r.getEvolution()==form&&tier(r)>=rank;}
    static boolean friend(TamedRat r,LivingEntity t){
        if(r==t)return true;
        if(r.getOwnerUUID()==null)return false;
        if(r.getOwnerUUID().equals(t.getUUID()))return true;
        return t instanceof TamedRat a && a.isTame() && r.getOwnerUUID().equals(a.getOwnerUUID())&&r.legion.sameSquad(a.legion);
    }
    static List<LivingEntity> friends(TamedRat r,int radius){
        return r.level().getEntitiesOfClass(LivingEntity.class,r.getBoundingBox().inflate(radius),e->e.isAlive()&&e.distanceToSqr(r)<=radius*radius&&friend(r,e));
    }
    static List<TamedRat> guards(LivingEntity target){
        if(!(target instanceof TamedRat || target instanceof Player))return List.of();
        return target.level().getEntitiesOfClass(TamedRat.class,target.getBoundingBox().inflate(8),r->r.isAlive()&&r.distanceToSqr(target)<=64&&tier(r)>0&&friend(r,target));
    }
    static boolean enemy(TamedRat r,LivingEntity e){return e!=r&&e.isAlive()&&!r.isAlliedTo(e)&&!friend(r,e)&&!(e instanceof TamableAnimal tame&&tame.isTame())&&!(e instanceof Player)&&(e instanceof Enemy||r.getTarget()==e);}
    static List<LivingEntity> foes(TamedRat r,LivingEntity center,int radius){
        return r.level().getEntitiesOfClass(LivingEntity.class,center.getBoundingBox().inflate(radius),e->e.distanceToSqr(center)<=radius*radius&&enemy(r,e));
    }
    static boolean boss(LivingEntity e){return e.getType().is(BOSSES)||e.getType().is(TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("forge","bosses")));}
    static void nested(Runnable action){boolean previous=secondary.get();secondary.set(true);try{action.run();}finally{secondary.set(previous);}}
    static void hit(TamedRat r,LivingEntity e,String kind,double damage){
        if(!enemy(r,e)||damage<=0)return;
        var reg=r.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var type=reg.getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("rats","evolution_"+kind)));
        nested(()->e.hurt(new DamageSource(type,r),(float)damage));
    }
    static double attack(TamedRat r){return r.getAttributeValue(Attributes.ATTACK_DAMAGE);}
    static void heal(LivingEntity e,double amount){if(amount>0&&e.isAlive())nested(()->e.heal((float)amount));}
    static LivingEntity wounded(TamedRat r,int range,LivingEntity except){
        return friends(r,range).stream().filter(e->e!=except&&e.getHealth()<e.getMaxHealth()).min(Comparator.comparingDouble(e->e.getHealth()/e.getMaxHealth())).orElse(null);
    }
    static void modifier(TamedRat r,Attribute attr,UUID id,double amount,AttributeModifier.Operation op){
        var a=r.getAttribute(attr);if(a==null)return;
        var old=a.getModifier(id);if(old!=null&&old.getAmount()==amount)return;
        if(old!=null)a.removeModifier(id);
        if(amount!=0)a.addTransientModifier(new AttributeModifier(id,"Rat evolution mastery",amount,op));
    }
    static void haste(TamedRat r,String type,int duration){timed(r,type,duration);speed(r);}
    static void speed(TamedRat r){modifier(r,Attributes.MOVEMENT_SPEED,SPEED,on(r,"Wind")?.25:on(r,"Haul")?.20:0,AttributeModifier.Operation.MULTIPLY_TOTAL);}
    public static void deposited(TamedRat r){
        if(!can(r,PLAIN,2)||!use(r,"DepositCD",300))return;
        for(var e:friends(r,8))if(e instanceof TamedRat a&&(a==r||a.getDepositPos().isPresent())){haste(a,"Haul",160); EvolutionEffects.stream(r,r,a);}
    }
    public static void harvested(TamedRat r,BlockPos pos,ItemStack produce){
        if(can(r,LEAF,2)&&!produce.isEmpty()&&!on(r,"CropCD")&&r.getRandom().nextInt(5)==0){timed(r,"CropCD",40);Block.popResource(r.level(),pos,produce.copyWithCount(1));EvolutionEffects.burst(r,Vec3.atCenterOf(pos));}
    }
    public static void training(TamedRat r,int xp){
        if(!can(r,PLAIN,1)||on(r,"TrainingCD"))return;
        friends(r,12).stream().filter(e->e!=r&&e instanceof TamedRat).map(e->(TamedRat)e).filter(a->a.getRatLevel()<100)
            .min(Comparator.comparingInt(TamedRat::getRatLevel)).ifPresent(a->{a.grantRatExperience(Math.round(xp*.25f));timed(r,"TrainingCD",100);EvolutionEffects.stream(r,r,a);});
    }
    static void repair(TamedRat r){
        if(on(r,"RepairCD"))return;
        var inv=r.getInventory();ItemStack iron=ItemStack.EMPTY,best=ItemStack.EMPTY;
        for(int i=0;i<RatContainer.SIZE;i=i==0?RatContainer.BACKPACK:i+1){
            var s=inv.getItem(i);if(s.is(Items.IRON_NUGGET))iron=s;
            if(i==0&&s.isDamageableItem()&&s.isDamaged())best=s;
        }
        if(iron.isEmpty())return;
        if(r.getOwner()!=null&&r.distanceToSqr(r.getOwner())<=64){
            var s=r.getOwner().getMainHandItem();
            if(s.isDamaged()&&(best.isEmpty()||(double)s.getDamageValue()/s.getMaxDamage()>(double)best.getDamageValue()/best.getMaxDamage()))best=s;
        }
        if(best.isEmpty())return;
        best.setDamageValue(Math.max(0,best.getDamageValue()-4));iron.shrink(1);inv.setChanged();timed(r,"RepairCD",600);
        EvolutionEffects.burst(r,r.getOwner()!=null && best==r.getOwner().getMainHandItem()?r.getOwner():r);
    }
    private static boolean fighting(TamedRat r){
        return r.getTarget()!=null&&r.getTarget().isAlive()||r.getLastHurtByMob()!=null&&r.tickCount-r.getLastHurtByMobTimestamp()<100||r.getLastHurtMob()!=null&&r.tickCount-r.getLastHurtMobTimestamp()<100;
    }
    static boolean still(TamedRat r){
        var n=state(r);var p=r.position();
        if(!n.contains("StillSince")||p.distanceToSqr(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"))>.0225){
            n.putDouble("X",p.x);n.putDouble("Y",p.y);n.putDouble("Z",p.z);n.putLong("StillSince",now(r));return false;
        }
        return now(r)-n.getLong("StillSince")>=60;
    }
    public static void tick(TamedRat r){
        if(r.getPersistentData().contains(KEY))speed(r);
        if(tier(r)==0)return;
        var f=r.getEvolution();var n=state(r);
        if(f==ALLOY){boolean anchored=still(r);modifier(r,Attributes.ARMOR,ARMOR,anchored?6:0,AttributeModifier.Operation.ADDITION);if(anchored&&!n.getBoolean("Anchored"))EvolutionEffects.ward(r,r);n.putBoolean("Anchored",anchored);}
        if(f==GALE&&r.getCommand()!=RatCommand.SPELL_COMBAT&&on(r,"Diving")){var target=r.getTarget();if(target!=null&&r.distanceToSqr(target)<6.25){n.remove("Diving");hit(r,target,"strike",attack(r)*1.2);EvolutionEffects.burst(r,target);}}
        if(f==DRAKE&&r.getCommand()!=RatCommand.SPELL_COMBAT&&n.getInt("Breaths")>0&&now(r)>=n.getLong("NextBreath")){
            n.putInt("Breaths",n.getInt("Breaths")-1);timed(r,"NextBreath",10);
            Vec3 look=r.getTarget()!=null?r.getTarget().position().subtract(r.position()).normalize():r.getLookAngle();
            EvolutionEffects.stream(r,EvolutionEffects.center(r),EvolutionEffects.center(r).add(look.scale(6)));
            for(var e:foes(r,r,8))if(r.hasLineOfSight(e)&&look.dot(e.position().subtract(r.position()).normalize())>.70)hit(r,e,"breath",attack(r)*.25);
        }
        if(f==LEAF)still(r);
        if((r.tickCount+r.getId())%5!=0)return;
        if(f==ALLOY&&tier(r)==2)repair(r);
        if(f==VENOM&&tier(r)==2&&!on(r,"CureCD"))for(var e:friends(r,8)){
            if(e.removeEffect(MobEffects.POISON)||e.removeEffect(MobEffects.WITHER)||clearPoison(e)){heal(e,e.getMaxHealth()*.03);timed(r,"CureCD",240);EvolutionEffects.stream(r,r,e);break;}
        }
        boolean combat=fighting(r);
        if(f==LEAF&&combat&&still(r)&&!on(r,"RootCD")){var e=wounded(r,6,null);if(e!=null){heal(e,e.getMaxHealth()*.015);timed(r,"RootCD",60);EvolutionEffects.stream(r,r,e);}}
        if(f==GALE&&tier(r)==2&&combat&&!n.getBoolean("Combat")&&use(r,"WindCD",400))
            for(var e:friends(r,12))if(e instanceof TamedRat a){haste(a,"Wind",120);EvolutionEffects.stream(r,r,a);}
        n.putBoolean("Combat",combat);
        if(!combat)return;
        var t=r.getTarget();
        if(f==GALE&&r.getCommand()!=RatCommand.SPELL_COMBAT&&t!=null&&r.onGround()&&r.hasLineOfSight(t)&&r.distanceToSqr(t)>=16&&r.distanceToSqr(t)<=100&&use(r,"DiveCD",180)){
            var dir=t.position().subtract(r.position()).normalize();r.setDeltaMovement(dir.scale(.95).add(0,.42,0));r.hasImpulse=true;timed(r,"Diving",30);EvolutionEffects.burst(r,r);
        }
        if(f==DRAKE&&r.getCommand()!=RatCommand.SPELL_COMBAT&&tier(r)==2&&t!=null&&r.distanceToSqr(t)<=64&&r.hasLineOfSight(t)&&use(r,"BreathCD",200)){n.putInt("Breaths",4);n.putLong("NextBreath",now(r));}
        if(f==SPARK&&tier(r)==2&&!on(r,"ChargeCD"))for(var e:friends(r,8))if(e!=r&&e instanceof TamedRat a&&a.legion.shortenCooldown(now(r),r.isSparkCharged()?60:20)){timed(r,"ChargeCD",240);EvolutionEffects.arc(r,r,a);EvolutionEffects.ward(r,a);break;}
    }
    static boolean clearPoison(LivingEntity e){if(!on(e,"Toxin"))return false;state(e).remove("Toxin");return true;}
    @SubscribeEvent public static void ailments(LivingEvent.LivingTickEvent ev){
        var e=ev.getEntity();if(e.level().isClientSide||!e.getPersistentData().contains(KEY))return;
        var s=state(e);
        if(on(e,"Freeze")){e.setDeltaMovement(0,Math.min(0,e.getDeltaMovement().y),0);if(e instanceof Mob m)m.getNavigation().stop();}
        if(on(e,"Toxin")&&now(e)>=s.getLong("ToxinTick")&&s.hasUUID("Poisoner")&&e.level() instanceof ServerLevel w&&w.getEntity(s.getUUID("Poisoner")) instanceof TamedRat r){
            timed(e,"ToxinTick",40);hit(r,e,"toxin",attack(r)*.08*s.getInt("Layers"));
        }
    }
    static boolean combo(TamedRat r,LivingEntity e,String key,int gap,int goal){
        var n=state(r);int count=on(r,key)&&n.hasUUID(key+"Target")&&e.getUUID().equals(n.getUUID(key+"Target"))?n.getInt(key+"Count")+1:1;
        n.putUUID(key+"Target",e.getUUID());n.putInt(key+"Count",count>=goal?0:count);timed(r,key,gap);return count>=goal;
    }
    static void line(TamedRat r,LivingEntity target,int length,String kind,double scale){
        Vec3 a=r.position(),dir=target.position().subtract(a).horizontalDistanceSqr()<.0001?r.getLookAngle():target.position().subtract(a).normalize();
        EvolutionEffects.stream(r,a.add(0,.25,0),a.add(dir.scale(length)).add(0,.25,0));
        for(var e:foes(r,r,length+1)){var delta=e.position().subtract(a);double along=dir.dot(delta);if(along>=0&&along<=length&&delta.subtract(dir.scale(along)).lengthSqr()<=2.25&&r.hasLineOfSight(e))hit(r,e,kind,attack(r)*scale);}
    }
    static void doAttack(TamedRat r,LivingEntity e,DamageSource source,float actual){
        if(tier(r)==0||!enemy(r,e))return;
        boolean melee=source.getDirectEntity()==r&&(source.is(DamageTypes.MOB_ATTACK)||source.is(DamageTypes.MOB_ATTACK_NO_AGGRO));
        var n=state(r);double atk=attack(r);
        switch(r.getEvolution()){
            case EMBER -> {if(melee&&!on(r,"FireCD")&&combo(r,e,"Ember",200,3)){timed(r,"FireCD",160);EvolutionEffects.ring(r,e.position().add(0,.15,0),2.2f);for(var mob:foes(r,e,3))hit(r,mob,"fire",atk*.6);}}
            case TIDE -> {if(tier(r)==2&&melee&&use(r,"WaveCD",r.isInWater()?100:200))line(r,e,6,"wave",.7);}
            case VOLT -> {if(use(r,"ArcCD",100)){var others=foes(r,e,6);others.remove(e);others.sort(Comparator.comparingDouble(e::distanceToSqr));for(int i=0;i<Math.min(2,others.size());i++){var next=others.get(i);EvolutionEffects.arc(r,i==0?e:others.get(i-1),next);hit(r,next,"electric",atk*(i==0?.65:.40));}}}
            case FROST -> {if(e.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)&&use(r,"FreezeCD",200)){EvolutionEffects.ward(r,e);if(boss(e))hit(r,e,"frost",atk*.7);else{timed(e,"Freeze",40);e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,10));}}}
            case BRAWL -> {if(melee&&combo(r,e,"Punch",80,3)){hit(r,e,"strike",atk*.65);EvolutionEffects.burst(r,e);}}
            case VENOM -> {var s=state(e);int layers=on(e,"Toxin")?s.getInt("Layers"):0;if(layers==0)timed(e,"ToxinTick",40);s.putUUID("Poisoner",r.getUUID());s.putInt("Layers",Math.min(4,layers+1));timed(e,"Toxin",121);EvolutionEffects.mark(r,e);}
            case DUNE -> {if(melee&&r.onGround()&&e.onGround()&&use(r,"RiftCD",160))line(r,e,6,"strike",.85);}
            case MOTH -> {if(use(r,"DustCD",240)){mark(r,e,"Dust",120);EvolutionEffects.burst(r,e);e.addEffect(new MobEffectInstance(MobEffects.GLOWING,120));}}
            case WISP -> {if(marked(r,e,"Control")&&!on(r,"SiphonCD")){var ally=wounded(r,8,null);if(ally!=null){heal(ally,Math.min(60,actual*.2));timed(r,"SiphonCD",80);EvolutionEffects.stream(r,e,ally);}}}
            case LUNAR -> {if(e.getHealth()<e.getMaxHealth()*.5&&use(r,"MoonCD",160)){hit(r,e,"strike",atk*(r.level().isNight()?1:.7));EvolutionEffects.burst(r,e);}}
            case SPARK -> {if(use(r,"SparkCD",160)){boolean high=r.isSparkCharged();EvolutionEffects.arc(r,r,e);hit(r,e,"electric",atk*(high?1.1:.45));if(high)foes(r,e,6).stream().filter(m->m!=e).min(Comparator.comparingDouble(e::distanceToSqr)).ifPresent(m->{EvolutionEffects.arc(r,e,m);hit(r,m,"electric",atk*.55);});}}
            default -> {}
        }
    }
    static void mark(TamedRat r,LivingEntity e,String kind,int ticks){state(e).putUUID(kind+"Owner",r.getUUID());timed(e,kind,ticks);}
    static boolean marked(TamedRat r,LivingEntity target,String kind){
        var s=target.getPersistentData().getCompound(KEY);return on(target,kind)&&s.hasUUID(kind+"Owner")&&target.level() instanceof ServerLevel w&&w.getEntity(s.getUUID(kind+"Owner")) instanceof TamedRat source&&friend(r,source);
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent ev){MasteryDefense.before(ev);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent ev){
        if(secondary.get()||ev.getEntity().level().isClientSide||ev.getAmount()<=0)return;
        // Extra damage must land after the original LivingEntity.hurt has finished setting health and invulnerability.
        var e=ev.getEntity();var src=ev.getSource();
        if(src.getEntity() instanceof TamedRat r&&enemy(r,e)){
            MasteryMagic.landed(r,e,src);
            if(src.is(DamageTypeTags.IS_PROJECTILE)&&marked(r,e,"Dust"))ev.setAmount(ev.getAmount()*1.15f);
            if(can(r,DRAKE,1)&&boss(e)){var s=state(r);int stacks=on(r,"Scale")&&s.hasUUID("ScaleTarget")&&s.getUUID("ScaleTarget").equals(e.getUUID())?s.getInt("Scales"):0;if(stacks<5)EvolutionEffects.mark(r,e);stacks=Math.min(5,stacks+1);s.putUUID("ScaleTarget",e.getUUID());s.putInt("Scales",stacks);timed(r,"Scale",120);ev.setAmount(ev.getAmount()*(1+.03f*stacks));}
            else if(r.getEvolution()==DRAKE)state(r).remove("Scale");
            float dealt=Math.min(e.getHealth(),ev.getAmount());((ServerLevel)e.level()).getServer().tell(new net.minecraft.server.TickTask(e.getServer().getTickCount(),()->{if(!r.isRemoved()&&!e.isRemoved())doAttack(r,e,src,dealt);}));
        }
        if(e instanceof TamedRat r&&can(r,CRAG,1)&&r.getCommand()!=RatCommand.SPELL_COMBAT&&src.getDirectEntity() instanceof LivingEntity attacker&&src.getDirectEntity()==src.getEntity()&&enemy(r,attacker)&&use(r,"CrystalCD",60)){
            EvolutionEffects.ring(r,r.position().add(0,.15,0),2.2f);
            for(var mob:foes(r,r,3))hit(r,mob,"strike",r.getAttributeValue(Attributes.ARMOR)*2);
        }
        MasteryDefense.after(ev);
    }
    static boolean isSecondary(){return secondary.get();}
}

