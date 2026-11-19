package com.github.alexthe666.rats.server.pizza;

import com.github.alexthe666.rats.registry.RatVariantRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 鼠头头：下水道副本的小 Boss。老鼠模组原版模型放大 2 倍，反戴棒球帽、眼上一道疤、金牙，叼着一整张缺了一块的披萨。
 * 强度跟主人进度成长（挖到钻石 / 进下界 / 进末地），四个招式：披萨抡飞、奶酪冲撞、喊小弟、扔披萨片。
 */
public class RatBoss extends Rat {
    /** 招式状态（同步给客户端做姿势）：见常量 */
    public static final int NONE = 0, SPIN = 1, CHARGE_WINDUP = 2, CHARGING = 3, WHISTLE = 4, THROW = 5, STUNNED = 6;
    private static final EntityDataAccessor<Byte> SKILL = SynchedEntityData.defineId(RatBoss.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> SKILL_TICKS = SynchedEntityData.defineId(RatBoss.class, EntityDataSerializers.INT);
    private static final DustParticleOptions CHEESE = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.25F), 1.4F);
    /** 每档：血、伤害、护甲 */
    private static final double[][] TIERS = {{120, 6, 6}, {180, 8, 8}, {260, 11, 10}, {360, 14, 14}};
    private static final ResourceLocation[] UNLOCK = {new ResourceLocation("minecraft:story/mine_diamond"), new ResourceLocation("minecraft:story/enter_the_nether"), new ResourceLocation("minecraft:end/root")};

    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.rats.rat_boss"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private int tier = -1, spinCd = 40, chargeCd = 80, throwCd = 60;
    private boolean called66, called33, hit;
    private Vec3 chargeDir = Vec3.ZERO;
    /** 被偷走的那份食物（下水道副本生成时写入），死亡时掉回来 */
    public ItemStack stolen = ItemStack.EMPTY;

    public RatBoss(EntityType<? extends Rat> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 40;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return Rat.createAttributes().add(Attributes.MAX_HEALTH, 120).add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.ARMOR, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.ATTACK_KNOCKBACK, 0.8);
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(SKILL, (byte) NONE); entityData.define(SKILL_TICKS, 0); }
    public int skill() { return entityData.get(SKILL); }
    public int skillTicks() { return entityData.get(SKILL_TICKS); }
    private void skill(int s, int ticks) { entityData.set(SKILL, (byte) s); entityData.set(SKILL_TICKS, ticks); }

    @Override protected void registerGoals() {
        super.registerGoals();
        // 去掉野鼠原来的胆小、偷箱子、挖洞、找奶酪等行为，换成 Boss 的
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override public boolean canUse() { return skill() == NONE && super.canUse(); }
            @Override public boolean canContinueToUse() { return skill() == NONE && super.canContinueToUse(); }
        });
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Rat.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor accessor, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        data = super.finalizeSpawn(accessor, difficulty, type, data, tag);
        setPlagued(false);
        setColorVariant(RatVariantRegistry.HOODED.get());
        setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        return data;
    }

    // ---------------------------------------------------------------- 强度：按附近玩家（进副本的人）的进度
    private void applyTier() {
        Player p = level().getNearestPlayer(this, 64);
        int t = 0;
        if (p instanceof ServerPlayer sp) for (ResourceLocation id : UNLOCK) {
            var adv = sp.server.getAdvancements().getAdvancement(id);
            if (adv != null && sp.getAdvancements().getOrStartProgress(adv).isDone()) t++;
        }
        tier = t;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(TIERS[t][0]);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(TIERS[t][1]);
        getAttribute(Attributes.ARMOR).setBaseValue(TIERS[t][2]);
        setHealth(getMaxHealth());
        xpReward = 40 + 25 * t;
    }
    private float damage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE); }

    // ---------------------------------------------------------------- 招式
    @Override public void aiStep() {
        super.aiStep();
        removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);   // 不怕猫
        if (level().isClientSide()) {
            if (skill() == CHARGE_WINDUP || skill() == CHARGING) level().addParticle(CHEESE, getRandomX(0.6), getY() + 0.2, getRandomZ(0.6), 0, 0, 0);
            return;
        }
        if (tier < 0) applyTier();
        bar.setProgress(getHealth() / getMaxHealth());
        LivingEntity t = getTarget();
        int s = skill(), ticks = skillTicks() + 1;
        if (spinCd > 0) spinCd--;
        if (chargeCd > 0) chargeCd--;
        if (throwCd > 0) throwCd--;
        if (s != NONE) {
            entityData.set(SKILL_TICKS, ticks);
            if (s != CHARGING) getNavigation().stop();
            switch (s) {
                case SPIN -> { if (ticks == 8) spin(); if (ticks >= 20) skill(NONE, 0); }
                case CHARGE_WINDUP -> {
                    if (t != null) getLookControl().setLookAt(t, 60, 60);
                    if (ticks >= 20) {
                        Vec3 to = t == null ? getLookAngle() : t.position().subtract(position());
                        chargeDir = new Vec3(to.x, 0, to.z).normalize();
                        hit = false;
                        playSound(SoundEvents.RAVAGER_ROAR, 0.8F, 1.8F);
                        skill(CHARGING, 0);
                    }
                }
                case CHARGING -> charge(t, ticks);
                case WHISTLE -> { if (ticks == 10) callMinions(t); if (ticks >= 30) skill(NONE, 0); }
                case THROW -> { if (t != null) getLookControl().setLookAt(t, 60, 60); if (ticks == 6 && t != null) throwSlice(t); if (ticks >= 14) skill(NONE, 0); }
                case STUNNED -> { if (ticks >= 40) skill(NONE, 0); }
                default -> skill(NONE, 0);
            }
            return;
        }
        if (t == null || !t.isAlive()) return;
        float hp = getHealth() / getMaxHealth();
        double dist = distanceTo(t);
        if (!called66 && hp <= 0.66F) { called66 = true; skill(WHISTLE, 0); playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 2.0F, 1.6F); }
        else if (!called33 && hp <= 0.33F) { called33 = true; skill(WHISTLE, 0); playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 2.0F, 1.9F); }
        else if (dist < 3.2 && spinCd <= 0) { spinCd = 120; skill(SPIN, 0); }
        else if (dist > 6 && dist < 16 && chargeCd <= 0 && hasLineOfSight(t)) { chargeCd = 200; skill(CHARGE_WINDUP, 0); playSound(SoundEvents.HOGLIN_ANGRY, 1.0F, 0.6F); }
        else if (dist > 4 && dist < 14 && throwCd <= 0 && hasLineOfSight(t)) { throwCd = 140; skill(THROW, 0); }
    }

    /** 披萨抡飞：叼着整张披萨转一圈，身边 3.6 格内的都被抡飞 */
    private void spin() {
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.7F);
        ((ServerLevel) level()).sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.5, getZ(), 6, 1.6, 0.1, 1.6, 0);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.6, 1.0, 3.6), e -> e != this && !(e instanceof Rat) && e.isAlive())) {
            if (distanceTo(e) > 3.8) continue;
            e.hurt(damageSources().mobAttack(this), damage() * 1.2F);
            e.knockback(1.6, getX() - e.getX(), getZ() - e.getZ());
        }
    }

    /** 奶酪冲撞：直线冲 24 tick；撞到目标击飞，撞墙自己晕 2 秒 */
    private void charge(LivingEntity t, int ticks) {
        setDeltaMovement(chargeDir.x * 0.95, getDeltaMovement().y, chargeDir.z * 0.95);
        setYRot((float) Math.toDegrees(Math.atan2(-chargeDir.x, chargeDir.z))); yBodyRot = getYRot();
        if (!hit) for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.4), e -> e != this && !(e instanceof Rat) && e.isAlive())) {
            e.hurt(damageSources().mobAttack(this), damage() * 1.5F);
            e.knockback(2.0, -chargeDir.x, -chargeDir.z);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.55, 0));
            hit = true;
        }
        if (horizontalCollision && ticks > 3) {
            playSound(SoundEvents.ANVIL_LAND, 0.7F, 1.4F);
            ((ServerLevel) level()).sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.6, getZ(), 18, 0.5, 0.4, 0.5, 0.2);
            setDeltaMovement(Vec3.ZERO);
            skill(STUNNED, 0);
        } else if (ticks >= 24 || hit) skill(NONE, 0);
    }

    /** 喊小弟：吹口哨叫 3 只野鼠 / 瘟疫鼠 */
    private void callMinions(LivingEntity t) {
        ServerLevel lvl = (ServerLevel) level();
        for (int i = 0; i < 3; i++) {
            Rat r = RatsEntityRegistry.RAT.get().create(lvl);
            if (r == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            r.moveTo(getX() + Math.cos(a) * 2.5, getY(), getZ() + Math.sin(a) * 2.5, random.nextFloat() * 360, 0);
            r.finalizeSpawn(lvl, lvl.getCurrentDifficultyAt(r.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            r.setPlagued(random.nextBoolean());
            if (t != null) r.setTarget(t);
            lvl.addFreshEntity(r);
            lvl.sendParticles(ParticleTypes.POOF, r.getX(), r.getY() + 0.2, r.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        }
    }

    /** 扔披萨片：从披萨上掰一块扔过去，落地变成烫嘴的芝士坑 */
    private void throwSlice(LivingEntity t) {
        PizzaSliceShot shot = new PizzaSliceShot(level(), this);
        double dx = t.getX() - getX(), dz = t.getZ() - getZ(), dy = t.getY(0.4) - shot.getY();
        shot.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.18, dz, 1.1F, 3);
        shot.damage = damage() * 0.7F;
        level().addFreshEntity(shot);
        playSound(SoundEvents.WITCH_THROW, 1.0F, 0.8F);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (skill() == STUNNED) amount *= 1.5F;                // 撞墙晕住时多吃伤害
        return super.hurt(source, amount);
    }

    // ---------------------------------------------------------------- 掉落：被偷的那份 + 披萨片 + 整合包里的随机食物
    private static List<Item> foods;
    private static List<Item> foods() {
        if (foods == null) {
            foods = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS) {
                var props = item.getFoodProperties(new ItemStack(item), null);
                if (item.isEdible() && props != null && props.getEffects().isEmpty() && props.getNutrition() >= 3) foods.add(item);
            }
        }
        return foods;
    }
    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean playerKill) {
        if (!stolen.isEmpty()) spawnAtLocation(stolen.copy());
        spawnAtLocation(new ItemStack(PizzaRatRegistry.PIZZA_SLICE.get(), 2 + random.nextInt(3)));
        List<Item> pool = foods();
        int n = 4 + Math.max(0, tier) + looting;
        for (int i = 0; i < n && !pool.isEmpty(); i++) spawnAtLocation(new ItemStack(pool.get(random.nextInt(pool.size())), 1 + random.nextInt(3)));
    }
    @Override public int getExperienceReward() { return xpReward; }

    // ---------------------------------------------------------------- 杂项
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) { return InteractionResult.PASS; }   // 不能驯服 / 喂奶酪
    @Override public boolean canDigThroughBlocks() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isBaby() { return false; }
    @Override public boolean canFallInLove() { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) { return null; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean canChangeDimensions() { return false; }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BossTier", tier);
        tag.putBoolean("Called66", called66); tag.putBoolean("Called33", called33);
        if (!stolen.isEmpty()) tag.put("Stolen", stolen.save(new CompoundTag()));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        tier = tag.contains("BossTier") ? tag.getInt("BossTier") : -1;
        called66 = tag.getBoolean("Called66"); called33 = tag.getBoolean("Called33");
        stolen = tag.contains("Stolen") ? ItemStack.of(tag.getCompound("Stolen")) : ItemStack.EMPTY;
        if (hasCustomName()) bar.setName(getDisplayName());
    }
}
