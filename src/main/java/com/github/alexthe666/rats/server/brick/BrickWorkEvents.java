package com.github.alexthe666.rats.server.brick;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 布瑞克职业的事件：盔甲栏属性、举盾格挡、盔甲/武器耐久、刷怪攒经验 */
@Mod.EventBusSubscriber(modid = RatsMod.MODID)
public final class BrickWorkEvents {
    private BrickWorkEvents() {}

    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent e) {
        if (e.getEntity() instanceof TamedRat r && !r.level().isClientSide()) r.work.tickArmor();
    }

    /** 举盾时挡掉正面来的伤害（同原版玩家格挡：攻击者在前方 90° 内） */
    @SubscribeEvent public static void attack(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof TamedRat r) || !BrickJobGoal.blocking(r) || e.getSource().is(DamageTypeTags.BYPASSES_SHIELD)) return;
        Vec3 from = e.getSource().getSourcePosition();
        if (from == null) return;
        Vec3 look = r.getViewVector(1.0F), to = from.subtract(r.position()).normalize();
        if (to.x * look.x + to.z * look.z <= 0) return;
        e.setCanceled(true);
        r.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.0F);
        var shield = r.getOffhandItem();
        if (shield.isDamageableItem() && e.getAmount() >= 3) shield.hurtAndBreak(1 + (int) e.getAmount() / 3, r, x -> {});
    }

    @SubscribeEvent public static void hurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof TamedRat r && r.isBrick() && !e.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) r.work.hurtArmor(e.getAmount());
        if (e.getSource().getEntity() instanceof TamedRat r && r.isBrick() && e.getSource().getDirectEntity() == r) {
            var weapon = r.getMainHandItem();
            if (weapon.isDamageableItem() && BrickJobGoal.meleeDamage(weapon) > 0) weapon.hurtAndBreak(1, r, x -> {});
        }
    }

    /** 刷怪击杀：老鼠杀怪原版不掉经验，这里把应得的经验记到布瑞克身上，攒够了做成经验瓶 */
    @SubscribeEvent public static void death(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof TamedRat r) || !r.isBrick() || BrickJob.of(r) != BrickJob.SENTRY || !r.work.working) return;
        if (!(e.getEntity() instanceof Enemy) || !(e.getEntity() instanceof Mob mob)) return;
        r.work.storedXp += Math.max(1, mob.getExperienceReward());
    }
}
