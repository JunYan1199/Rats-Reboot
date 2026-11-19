package com.github.alexthe666.rats.server.entity.rat;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static com.github.alexthe666.rats.server.entity.rat.Mastery.*;
import static com.github.alexthe666.rats.server.entity.rat.RatEvolution.*;

@Mod.EventBusSubscriber(modid="rats")
public final class MasteryDefense {
    private MasteryDefense(){}
    static void before(LivingHurtEvent ev){
        var e=ev.getEntity();var src=ev.getSource();
        if(e.level().isClientSide||ev.getAmount()<=0||isSecondary())return;
        MasteryMagic.modify(ev);
        boolean fire=src.is(DamageTypeTags.IS_FIRE),shot=src.is(DamageTypeTags.IS_PROJECTILE),boom=src.is(DamageTypeTags.IS_EXPLOSION);
        boolean lightning=src.is(DamageTypes.LIGHTNING_BOLT)||src.typeHolder().unwrapKey().map(k->k.location().getPath().contains("lightning")).orElse(false);
        boolean direct=src.getDirectEntity() instanceof LivingEntity&&src.getEntity()==src.getDirectEntity();
        float amount=ev.getAmount();
        for(var r:guards(e)){
            if(fire&&can(r,EMBER,2)&&!on(e,"FireGuard")&&use(r,"FireGuardCD",360)){timed(e,"FireGuard",100);EvolutionEffects.ward(r,e);}
            if(shot&&e.onGround()&&can(r,DUNE,2)&&!on(e,"Trench")&&use(r,"TrenchCD",280)){timed(e,"Trench",80);EvolutionEffects.ward(r,e);}
            if(boom&&can(r,CRAG,2)&&r.distanceToSqr(e)<=36&&!on(e,"Cover")&&use(r,"CoverCD",320))
                for(var ally:friends(r,6)){timed(ally,"Cover",80);EvolutionEffects.ward(r,ally);}
            if(lightning&&can(r,VOLT,2)&&!on(e,"RodHit")&&use(r,"RodCD",300)){
                timed(e,"RodHit",1);amount*=.4f;
                MasteryMagic.armLightning(r);EvolutionEffects.arc(r,e,r);
            }
            if(direct&&e instanceof Player&&can(r,BRAWL,2)&&r.distanceToSqr(e)<=16&&!on(e,"ParryHit")&&use(r,"ParryCD",200)){
                timed(e,"ParryHit",1);EvolutionEffects.ward(r,e);amount*=.65f;var attacker=(LivingEntity)src.getEntity();if(r.getCommand()!=RatCommand.SPELL_COMBAT&&enemy(r,attacker))nested(()->r.doHurtTarget(attacker));
            }
            if(direct&&can(r,LUNAR,2)&&!on(e,"RearHit")&&src.getSourcePosition()!=null&&e.getLookAngle().dot(src.getSourcePosition().subtract(e.position()).normalize())<0&&use(r,"RearCD",200)){
                timed(e,"RearHit",1);EvolutionEffects.ward(r,e);amount*=r.level().isNight()?.50f:.65f;
            }
            if(can(r,MOTH,2)&&e instanceof TamedRat caster&&caster.legion.isCasting()&&!on(e,"Cocoon")&&use(r,"CocoonCD",200)){
                amount*=.75f;timed(e,"Cocoon",60);EvolutionEffects.ward(r,e);
            }
            if(can(r,ALLOY,2)&&r!=e&&direct&&r.distanceToSqr(e)<=16&&state(r).getBoolean("Anchored")&&!on(e,"SharedHit")){
                timed(e,"SharedHit",1);EvolutionEffects.mark(r,e);float share=amount*.15f;amount-=share;nested(()->r.hurt(src,share));
            }
        }
        if(fire&&on(e,"FireGuard"))amount*=.5f;
        if(shot&&e.onGround()&&on(e,"Trench"))amount*=.6f;
        if(boom&&on(e,"Cover"))amount*=.5f;
        if(on(e,"SoulGuard"))amount*=.6f;
        ev.setAmount(amount);
    }
    @SubscribeEvent public static void knockback(LivingKnockBackEvent ev){if(on(ev.getEntity(),"Cocoon"))ev.setCanceled(true);}
    static void after(LivingDamageEvent ev){
        var e=ev.getEntity();var protectors=guards(e);float amount=ev.getAmount();
        for(var r:protectors)if(can(r,SEER,1)&&amount>e.getMaxHealth()*.08&&use(r,"ForesightCD",400)){amount*=.6;EvolutionEffects.ward(r,e);break;}
        float hp=e.getHealth(),max=e.getMaxHealth();boolean tide=false;
        for(var r:protectors){
            if(can(r,FROST,2)&&hp>=max*.60&&hp-amount<max*.60&&!on(e,"FrostShield")&&use(r,"ShieldCD",400)){
                timed(e,"FrostShield",160);EvolutionEffects.ward(r,e);state(e).putFloat("ShieldHP",Math.min(180,max*.06f));
            }
        }
        if(on(e,"FrostShield")){
            float shield=state(e).getFloat("ShieldHP"),used=Math.min(shield,amount);
            state(e).putFloat("ShieldHP",shield-used);amount-=used;
        }
        ev.setAmount(amount);
        if(amount<=0||hp<=amount)return;
        double recovery=0;
        for(var r:protectors){
            if(!tide&&can(r,TIDE,1)&&hp>=max*.5&&hp-amount<max*.5&&use(r,"RescueCD",360)){
                recovery+=max*(e.isInWater()?.07:.04);tide=true;EvolutionEffects.stream(r,r,e);
            }
        }
        if(on(e,"Wish")&&hp>=max*.4&&hp-amount<max*.4){state(e).remove("Wish");recovery+=max*.06;}
        double total=recovery;
        if(total>0)e.getServer().tell(new net.minecraft.server.TickTask(e.getServer().getTickCount(),()->heal(e,total)));
        if(e instanceof TamedRat r&&can(r,WISP,2)&&hp-amount<max*.2&&!on(r,"ReturnCD")&&r.getOwner()!=null&&r.distanceToSqr(r.getOwner())<=576){
            BlockPos p=r.getOwner().blockPosition();
            for(BlockPos tile:BlockPos.spiralAround(p,3,net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.SOUTH)){
                var floor=tile.below();
                if(!r.level().getBlockState(floor).isFaceSturdy(r.level(),floor,net.minecraft.core.Direction.UP)||!r.level().getFluidState(tile).isEmpty())continue;
                var destination=Vec3.atBottomCenterOf(tile);var bounds=r.getBoundingBox().move(destination.subtract(r.position()));
                if(!r.level().getWorldBorder().isWithinBounds(tile)||!r.level().noCollision(r,bounds))continue;
                EvolutionEffects.stream(r,EvolutionEffects.center(r),destination.add(0,r.getBbHeight()*.5,0));
                r.teleportTo(destination.x,destination.y,destination.z);r.getNavigation().stop();timed(r,"ReturnCD",1200);timed(r,"SoulGuard",60);break;
            }
        }
    }
}

