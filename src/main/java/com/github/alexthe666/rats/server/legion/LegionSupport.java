package com.github.alexthe666.rats.server.legion;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="rats")
public final class LegionSupport {
    private record Route(TamedRat from,LivingEntity to){}
    private static final ThreadLocal<Route> route=new ThreadLocal<>();
    static void run(TamedRat caster,LivingEntity ally,String role,Runnable action){
        if(ally==caster||!("heal".equals(role)||"buff".equals(role))){action.run();return;}
        Route previous=route.get();route.set(new Route(caster,ally));
        try{action.run();}finally{if(previous==null)route.remove();else route.set(previous);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void heal(LivingHealEvent e){
        Route r=route.get();if(r!=null&&e.getEntity()==r.from){r.to.heal(e.getAmount());e.setCanceled(true);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void buff(MobEffectEvent.Applicable e){
        Route r=route.get();
        if(r!=null&&e.getEntity()==r.from&&e.getEffectInstance().getEffect().isBeneficial()){
            r.to.addEffect(new MobEffectInstance(e.getEffectInstance()),r.from);e.setResult(Event.Result.DENY);
        }
    }
    @SubscribeEvent public static void friendlyFire(LivingAttackEvent e){
        if(e.getSource().getEntity() instanceof TamedRat r && r.isTame()){
            var victim=e.getEntity();
            if(victim==r||r.isOwnedBy(victim)||victim instanceof TamableAnimal t&&r.getOwnerUUID()!=null&&r.getOwnerUUID().equals(t.getOwnerUUID()))e.setCanceled(true);
        }
    }
}
