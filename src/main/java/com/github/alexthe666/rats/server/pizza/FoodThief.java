package com.github.alexthe666.rats.server.pizza;

import com.github.alexthe666.rats.registry.RatVariantRegistry;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 偷食鼠（纽约披萨鼠）：老鼠模组原版模型，叼着放大的被偷食物往鼠洞方向跑。
 * 被打死就把食物还回来；跑远了（离受害玩家 28 格以上）或跑满 45 秒就钻进鼠洞 → 生成鼠洞方块、播报坐标。
 */
public class FoodThief extends Rat {
    private static final EntityDataAccessor<ItemStack> CARRIED = SynchedEntityData.defineId(FoodThief.class, EntityDataSerializers.ITEM_STACK);
    public static final int APPROACH = 0, GRAB = 1, FLEE = 2;
    private int state = FLEE, ticks;
    @Nullable private UUID victim;
    @Nullable private UUID targetItem;          // 去叼地上掉落物时，那个掉落物
    private Vec3 origin = Vec3.ZERO, dir = new Vec3(1, 0, 0);
    private double holeDistance = 90;
    /** 是从布瑞克手里截胡的（只有玩家亲手打到才算追回） */
    public boolean intercepted;

    public FoodThief(EntityType<? extends Rat> type, Level level) { super(type, level); setPersistenceRequired(); }

    public static AttributeSupplier.Builder createThiefAttributes() {
        return Rat.createAttributes().add(Attributes.MAX_HEALTH, 6).add(Attributes.MOVEMENT_SPEED, 0.38).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(CARRIED, ItemStack.EMPTY); }
    public ItemStack carried() { return entityData.get(CARRIED); }
    public void carry(ItemStack s) { entityData.set(CARRIED, s.copy()); }
    public int state() { return state; }

    /** 设定一次偷窃：victim = 被偷的玩家，origin = 偷食地点，away = 逃跑方向（远离玩家） */
    public void setup(@Nullable UUID victim, Vec3 origin, Vec3 away, int state, @Nullable UUID targetItem) {
        this.victim = victim; this.origin = origin; this.state = state; this.targetItem = targetItem; this.ticks = 0;
        Vec3 h = new Vec3(away.x, 0, away.z);
        this.dir = h.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : h.normalize();
        this.holeDistance = 60 + random.nextInt(61);
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor a, DifficultyInstance d, MobSpawnType t, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        data = super.finalizeSpawn(a, d, t, data, tag);
        setPlagued(false);
        if (random.nextInt(3) == 0) setColorVariant(RatVariantRegistry.HOODED.get());
        return data;
    }

    @Override public void aiStep() {
        super.aiStep();
        removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);   // 偷东西的时候不怕猫（看守猫会直接扑上来）
        if (level().isClientSide()) {
            if (state == FLEE && random.nextInt(3) == 0 && !carried().isEmpty())
                level().addParticle(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, carried()), getX(), getY() + 0.1, getZ(), 0, 0.05, 0);
            return;
        }
        ticks++;
        switch (state) {
            case APPROACH -> {
                Entity e = targetItem == null ? null : ((ServerLevel) level()).getEntity(targetItem);
                if (!(e instanceof ItemEntity item) || !item.isAlive()) { discard(); return; }   // 掉落物没了，悄悄溜走
                getNavigation().moveTo(item, 1.3);
                if (distanceToSqr(item) < 1.0) {
                    ItemStack s = item.getItem().copy();
                    item.discard();
                    carry(s);
                    PizzaRatEvents.announceSteal((ServerLevel) level(), this, s);
                    state = GRAB; ticks = 0;
                } else if (ticks > 400) discard();
            }
            case GRAB -> {
                getNavigation().stop();
                if (ticks == 1) playSound(SoundEvents.FOX_EAT, 0.8F, 1.6F);
                if (ticks >= 12) { state = FLEE; ticks = 0; }
            }
            case FLEE -> flee();
            default -> state = FLEE;
        }
    }

    private void flee() {
        Player v = victim == null ? null : level().getPlayerByUUID(victim);
        // 被追近了就往远离玩家的方向偏一点
        Vec3 run = dir;
        if (v != null && v.distanceTo(this) < 8) {
            Vec3 away = position().subtract(v.position()); away = new Vec3(away.x, 0, away.z);
            if (away.lengthSqr() > 1e-4) run = dir.add(away.normalize()).normalize();
        }
        if (ticks % 20 == 1 || getNavigation().isDone()) {
            Vec3 goal = position().add(run.scale(14));
            getNavigation().moveTo(goal.x, goal.y, goal.z, 1.75);
        }
        if (ticks % 6 == 0) playSound(SoundEvents.SILVERFISH_STEP, 0.4F, 1.6F);
        boolean far = v == null || v.distanceTo(this) > 28 || v.level() != level();
        if ((far && ticks > 100) || ticks > 900) escape();
    }

    /** 钻进鼠洞：在偷食地点外 60～120 格的地表生成鼠洞方块，播报坐标，留面包屑 */
    private void escape() {
        ServerLevel lvl = (ServerLevel) level();
        ItemStack s = carried();
        if (!s.isEmpty()) PizzaRatEvents.makeHole(lvl, this, victim, origin, dir, holeDistance, s);
        lvl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 8, 0.2, 0.1, 0.2, 0.02);
        carry(ItemStack.EMPTY);
        discard();
    }

    /** 被打死：食物掉回来；截胡的那份只有玩家亲手打到才算追回 */
    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean playerKill) {
        ItemStack s = carried();
        if (s.isEmpty()) return;
        boolean byPlayer = source.getEntity() instanceof Player;
        if (intercepted && !byPlayer && !(source.getEntity() instanceof net.minecraft.world.entity.animal.Cat)) { escape(); return; }
        spawnAtLocation(s.copy());
        carry(ItemStack.EMPTY);
        if (source.getEntity() instanceof Player p) PizzaRatEvents.retrieved(p, s);
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) { return InteractionResult.PASS; }
    @Override public boolean canDigThroughBlocks() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isBaby() { return false; }
    @Override public boolean canFallInLove() { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) { return null; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Carried", carried().save(new CompoundTag()));
        tag.putInt("ThiefState", state); tag.putInt("ThiefTicks", ticks);
        if (victim != null) tag.putUUID("Victim", victim);
        tag.putDouble("OX", origin.x); tag.putDouble("OY", origin.y); tag.putDouble("OZ", origin.z);
        tag.putDouble("DX", dir.x); tag.putDouble("DZ", dir.z); tag.putDouble("HoleDist", holeDistance);
        tag.putBoolean("Intercepted", intercepted);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        carry(ItemStack.of(tag.getCompound("Carried")));
        state = tag.getInt("ThiefState") == APPROACH ? FLEE : tag.getInt("ThiefState"); ticks = tag.getInt("ThiefTicks");
        victim = tag.hasUUID("Victim") ? tag.getUUID("Victim") : null;
        origin = new Vec3(tag.getDouble("OX"), tag.getDouble("OY"), tag.getDouble("OZ"));
        dir = new Vec3(tag.getDouble("DX"), 0, tag.getDouble("DZ"));
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(1, 0, 0);
        holeDistance = tag.contains("HoleDist") ? tag.getDouble("HoleDist") : 90;
        intercepted = tag.getBoolean("Intercepted");
    }
}
