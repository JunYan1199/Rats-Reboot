package com.github.alexthe666.rats.server.entity.rat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;
import java.util.function.Supplier;
import static com.github.alexthe666.rats.server.entity.rat.Mastery.*;
import static com.github.alexthe666.rats.server.entity.rat.RatEvolution.*;

@Mod.EventBusSubscriber(modid="rats")
public final class MasteryMagic {
    private MasteryMagic(){}
    public record Cast(TamedRat rat,LivingEntity target,String role,boolean book,double multiplier,boolean rod,boolean resonance){}
    private static final ThreadLocal<Cast> current=new ThreadLocal<>();
    public static Cast prepare(TamedRat r,LivingEntity t,String role,boolean book){
        boolean attack="attack".equals(role);
        boolean rod=attack&&rodSource(r)!=null;
        boolean resonance=attack&&on(r,book?"BoostBook":"BoostFocus");
        return new Cast(r,t,role,book,(rod?1.2:1)*(resonance?1.25:1),rod,resonance);
    }
    private static TamedRat rodSource(TamedRat r){
        var data=r.getPersistentData().getCompound("RatsEvolutionMastery");
        if(!on(r,"RodOffer")||!data.hasUUID("RodSource")||!(r.level() instanceof net.minecraft.server.level.ServerLevel world))return null;
        return world.getEntity(data.getUUID("RodSource")) instanceof TamedRat emitter&&on(emitter,"RodArmed")&&friend(r,emitter)?emitter:null;
    }
    static void armLightning(TamedRat r){
        timed(r,"RodArmed",120);
        for(var e:friends(r,12))if(e instanceof TamedRat){timed(e,"RodOffer",120);state(e).putUUID("RodSource",r.getUUID());}
    }
    public static void started(Cast c){
        if(c.rod){var emitter=rodSource(c.rat);if(emitter!=null)state(emitter).remove("RodArmed");}
        if(c.resonance)state(c.rat).remove(c.book?"BoostBook":"BoostFocus");
    }
    public static int windup(TamedRat r,int ticks){return on(r,"Wind")?Math.max(1,(int)Math.ceil(ticks*.85)):ticks;}
    public static <T>T with(Cast c,Supplier<T> callback){
        var prev=current.get();current.set(c);try{return callback.get();}finally{if(prev==null)current.remove();else current.set(prev);}
    }
    public static void run(Cast c,Runnable callback){with(c,()->{callback.run();return null;});}
    public static void finished(Cast c){
        if(can(c.rat,SEER,2)&&use(c.rat,"ResonanceCD",240)){
            friends(c.rat,8).stream().filter(e->e!=c.rat&&e instanceof TamedRat)
                .map(e->(TamedRat)e).filter(a->a.legion.hasAttackSchool(a,!c.book))
                .min(java.util.Comparator.comparingDouble(c.rat::distanceToSqr))
                .ifPresent(a->{timed(a,c.book?"BoostFocus":"BoostBook",160);EvolutionEffects.stream(c.rat,c.rat,a);});
        }
        if("buff".equals(c.role)&&can(c.rat,RIBBON,2)&&friend(c.rat,c.target)&&use(c.rat,"WishCD",360)){timed(c.target,"Wish",160);EvolutionEffects.ward(c.rat,c.target);}
    }
    @SubscribeEvent public static void spawned(EntityJoinLevelEvent ev){
        var c=current.get();if(c==null||ev.getLevel().isClientSide||ev.getEntity() instanceof LivingEntity)return;
        var e=ev.getEntity();
        // Player-sized projectiles can originate just inside the floor when the caster is a rat.
        if(e instanceof Projectile projectile && projectile.getOwner()==c.rat
                && ("attack".equals(c.role)||"control".equals(c.role)) && e.distanceToSqr(c.rat)<2
                && e.getY()<c.rat.getY()+.08){
            double rise=Math.max(.12,c.rat.getEyeY()-e.getY());
            if(ev.getLevel().noCollision(e,e.getBoundingBox().move(0,rise,0)))e.setPos(e.getX(),e.getY()+rise,e.getZ());
        }
        var n=e.getPersistentData();n.putUUID("RatSpellSource",c.rat.getUUID());n.putDouble("RatSpellBoost",c.multiplier);n.putString("RatSpellRole",c.role);
        if("control".equals(c.role)&&e.getClass().getSimpleName().equals("FrostShacklesEntity")&&e.distanceToSqr(c.target)<6)mark(c.rat,c.target,"Control",120);
    }
    static void modify(LivingHurtEvent e){
        if(!(e.getSource().getEntity() instanceof TamedRat caster))return;
        var c=current.get();double mult=1;
        if(c!=null&&c.rat==caster)mult=c.multiplier;
        else if(e.getSource().getDirectEntity()!=null){
            var n=e.getSource().getDirectEntity().getPersistentData();if(n.hasUUID("RatSpellSource")&&n.getUUID("RatSpellSource").equals(caster.getUUID()))mult=n.getDouble("RatSpellBoost");
        }
        if(mult!=1)e.setAmount((float)(e.getAmount()*mult));
    }
    static void landed(TamedRat r,LivingEntity t,net.minecraft.world.damagesource.DamageSource source){
        var c=current.get();var direct=source.getDirectEntity();
        if(c!=null&&c.rat==r&&"control".equals(c.role)||direct!=null&&"control".equals(direct.getPersistentData().getString("RatSpellRole")))mark(r,t,"Control",120);
    }
    @SubscribeEvent public static void debuff(MobEffectEvent.Added e){
        var c=current.get();if(c!=null&&"control".equals(c.role)&&!e.getEffectInstance().getEffect().isBeneficial()&&enemy(c.rat,e.getEntity()))mark(c.rat,e.getEntity(),"Control",120);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void echo(LivingHealEvent ev){
        var c=current.get();var recipient=ev.getEntity();
        if(isSecondary()||c==null||!"heal".equals(c.role)||!can(c.rat,RIBBON,1)||!friend(c.rat,recipient)||on(c.rat,"EchoCD"))return;
        double effective=Math.min(ev.getAmount(),recipient.getMaxHealth()-recipient.getHealth());
        if(effective>0){var other=wounded(c.rat,8,recipient);if(other!=null){timed(c.rat,"EchoCD",100);heal(other,Math.min(100,effective*.35));EvolutionEffects.stream(c.rat,recipient,other);}}
    }
}

