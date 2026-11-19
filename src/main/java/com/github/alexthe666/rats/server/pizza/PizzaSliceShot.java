package com.github.alexthe666.rats.server.pizza;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

/** 鼠头头扔出的披萨片：砸中有伤害，落地变成一滩烫嘴的芝士（减速 + 一点灼烫） */
public class PizzaSliceShot extends ThrowableItemProjectile {
    float damage = 4;

    public PizzaSliceShot(EntityType<? extends PizzaSliceShot> type, Level level) { super(type, level); }
    public PizzaSliceShot(Level level, LivingEntity owner) { super(PizzaRatRegistry.PIZZA_SLICE_SHOT.get(), owner, level); }

    @Override protected Item getDefaultItem() { return PizzaRatRegistry.PIZZA_SLICE.get(); }

    @Override protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (hit.getEntity() != getOwner()) hit.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
    }

    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level().isClientSide()) return;
        AreaEffectCloud puddle = new AreaEffectCloud(level(), getX(), getY(), getZ());
        if (getOwner() instanceof LivingEntity owner) puddle.setOwner(owner);
        puddle.setRadius(2.2F);
        puddle.setRadiusOnUse(0);
        puddle.setRadiusPerTick(-0.008F);
        puddle.setDuration(140);
        puddle.setWaitTime(0);
        puddle.setParticle(new DustParticleOptions(new Vector3f(1.0F, 0.8F, 0.22F), 1.5F));
        puddle.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        puddle.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 0));
        level().addFreshEntity(puddle);
        playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 0.7F);
        discard();
    }
}
