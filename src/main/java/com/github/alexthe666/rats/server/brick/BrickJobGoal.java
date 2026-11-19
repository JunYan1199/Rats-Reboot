package com.github.alexthe666.rats.server.brick;

import com.github.alexthe666.rats.server.brick.BrickJob.Action;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.inventory.container.RatContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.*;
import java.util.function.Predicate;

/**
 * 布瑞克职业工作（优先级 0，和做饭并列）：戴职业帽 + 权杖里点了「开始工作」时接管走位。
 * 近战交给老鼠原有的近战目标：这里只负责选目标、换武器、射箭、举盾；有近战目标时让出控制。
 */
public final class BrickJobGoal extends Goal {
    private final TamedRat rat;
    private BlockPos workPos;           // 当前要去干活的方块
    private Entity workEntity;          // 当前要去处理的动物/掉落物
    private int actionTicks, cooldown, scanCooldown, shootCooldown, blockTicks, wanderCooldown, digTicks, presentTicks;
    private ItemStack treasure = ItemStack.EMPTY;
    private final Map<UUID, Long> milked = new HashMap<>();
    private List<BlockPos> containers = List.of();
    private long containersAt = -1000;

    public BrickJobGoal(TamedRat rat) { this.rat = rat; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

    private BrickJob job() { return BrickJob.of(rat); }
    private BrickWork w() { return rat.work; }
    private ServerLevel level() { return (ServerLevel) rat.level(); }

    @Override public boolean canUse() {
        if (rat.level().isClientSide() || !rat.isBrick() || !w().working || rat.isBrickCooking()) return false;
        BrickJob job = job();
        if (job == BrickJob.NONE || job == BrickJob.CHEF) return false;
        if (rat.isOrderedToSit() || rat.isPassenger()) return false;
        // 有近战目标且拿着近战武器：让老鼠原有的近战目标去打
        LivingEntity t = rat.getTarget();
        if (t != null && t.isAlive() && !(rat.getMainHandItem().getItem() instanceof ProjectileWeaponItem) && blockTicks <= 0) return false;
        return true;
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    private ItemStack handBackup;
    private void restoreHand() { if (handBackup != null) { rat.getInventory().setItem(0, handBackup); handBackup = null; } }

    @Override public void stop() {
        restoreHand();
        presentTicks = 0;
        rat.setBrickAction(Action.NONE);
        actionTicks = digTicks = presentTicks = 0;
        workPos = null; workEntity = null;
    }

    @Override public void tick() {
        if (cooldown > 0) cooldown--;
        if (scanCooldown > 0) scanCooldown--;
        if (shootCooldown > 0) shootCooldown--;
        if (blockTicks > 0 && --blockTicks == 0) rat.setBrickAction(Action.NONE);
        switch (job()) {
            case GUARD -> guard();
            case SENTRY -> sentry();
            case FARMER -> farmer();
            case RANCHER -> rancher();
            case TREASURE -> treasure();
            default -> {}
        }
    }

    // ================================================================== 通用
    private boolean walkTo(Vec3 p, double reach) {
        rat.getLookControl().setLookAt(p.x, p.y, p.z, 30, 30);
        if (rat.position().distanceToSqr(p) <= reach * reach) { rat.getNavigation().stop(); return true; }
        if (rat.getNavigation().isDone() || rat.tickCount % 20 == 0) rat.getNavigation().moveTo(p.x, p.y, p.z, 1.2);
        return false;
    }
    private boolean walkTo(BlockPos p) { return walkTo(Vec3.atCenterOf(p), 1.8); }

    /** 工作区附近（外扩 6 格）的容器，10 秒刷新一次 */
    private List<IItemHandler> handlers(AABB around) {
        long now = level().getGameTime();
        if (now - containersAt > 200) {
            containersAt = now;
            List<BlockPos> found = new ArrayList<>();
            AABB box = around.inflate(6, 3, 6);
            for (int cx = (int) Math.floor(box.minX) >> 4; cx <= (int) Math.floor(box.maxX) >> 4; cx++)
                for (int cz = (int) Math.floor(box.minZ) >> 4; cz <= (int) Math.floor(box.maxZ) >> 4; cz++) {
                    if (!level().hasChunk(cx, cz)) continue;
                    for (var be : level().getChunk(cx, cz).getBlockEntities().values())
                        if (box.contains(Vec3.atCenterOf(be.getBlockPos())) && be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).isPresent())
                            found.add(be.getBlockPos().immutable());
                }
            containers = found;
        }
        List<IItemHandler> out = new ArrayList<>();
        for (BlockPos p : containers) {
            var be = level().getBlockEntity(p);
            if (be != null) be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).ifPresent(out::add);
        }
        return out;
    }

    /** 先从自己背包、再从附近容器里拿 count 个满足条件的物品；拿不到返回空 */
    private ItemStack take(AABB around, Predicate<ItemStack> want, int count) {
        RatContainer inv = (RatContainer) rat.getInventory();
        for (int i = RatContainer.BACKPACK; i < RatContainer.SIZE; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && want.test(s) && s.getCount() >= count) return inv.removeItem(i, count);
        }
        for (IItemHandler h : handlers(around))
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (!s.isEmpty() && want.test(s) && s.getCount() >= count) {
                    ItemStack got = h.extractItem(i, count, false);
                    if (got.getCount() == count) return got;
                    if (!got.isEmpty()) ItemHandlerHelper.insertItemStacked(h, got, false);
                }
            }
        return ItemStack.EMPTY;
    }
    private boolean has(AABB around, Predicate<ItemStack> want) {
        RatContainer inv = (RatContainer) rat.getInventory();
        for (int i = 0; i < RatContainer.SIZE; i++) if (want.test(inv.getItem(i))) return true;
        for (IItemHandler h : handlers(around)) for (int i = 0; i < h.getSlots(); i++) if (want.test(h.getStackInSlot(i))) return true;
        return false;
    }

    /** 把收获放进「存放物品」箱子（奶酪魔杖设置）；没设置或放不下就先装在自己身上 */
    private void stash(ItemStack stack) {
        if (stack.isEmpty()) return;
        var dep = rat.getDepositPos();
        if (dep.isPresent() && dep.get().dimension() == level().dimension()) {
            var be = level().getBlockEntity(dep.get().pos());
            if (be != null) {
                var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, rat.depositFacing);
                if (cap.isPresent()) stack = ItemHandlerHelper.insertItemStacked(cap.orElseThrow(IllegalStateException::new), stack, false);
            }
        } else if (rat.tickCount % 600 == 0) w().notice(Component.translatable("brick.rats.work.no_deposit"));
        if (stack.isEmpty()) return;
        int moved = ((RatContainer) rat.getInventory()).collect(stack, stack.getCount(), false);
        stack.shrink(moved);
        if (!stack.isEmpty()) {
            w().notice(Component.translatable("brick.rats.work.full"));
            rat.spawnAtLocation(stack);
        }
    }

    /** 把一件工具从背包换到主手（主手原来的东西换回去） */
    private boolean equip(Predicate<ItemStack> want) {
        RatContainer inv = (RatContainer) rat.getInventory();
        if (want.test(inv.getItem(0))) return true;
        for (int i = RatContainer.BACKPACK; i < RatContainer.SIZE; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && want.test(s)) {
                ItemStack hand = inv.getItem(0);
                inv.setItem(0, s);
                inv.setItem(i, hand);
                return true;
            }
        }
        return false;
    }
    private void act(int action, int ticks) { rat.setBrickAction(action); actionTicks = ticks; }
    /** 正在播一段动作：返回 true 表示还在播 */
    private boolean acting() {
        if (actionTicks > 0) { actionTicks--; if (actionTicks == 0) rat.setBrickAction(Action.NONE); return actionTicks > 0; }
        return false;
    }

    // ================================================================== 武器
    static double meleeDamage(ItemStack s) {
        if (s.isEmpty() || s.getItem() instanceof ProjectileWeaponItem) return 0;
        double d = 0;
        for (var m : s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) d += m.getAmount();
        return d;
    }
    private boolean hasArrows(AABB around) { return has(around, s -> s.is(Items.ARROW) || s.getItem() instanceof ArrowItem); }
    /** 自动换武器：怪远且会射箭 → 弓；否则伤害最高的近战武器 */
    private void pickWeapon(LivingEntity target, boolean archer, AABB around) {
        double dist = rat.distanceTo(target);
        if (archer && dist > 5 && hasArrows(around) && equip(s -> s.getItem() instanceof ProjectileWeaponItem)) return;
        RatContainer inv = (RatContainer) rat.getInventory();
        int best = -1; double bestDmg = meleeDamage(inv.getItem(0));
        for (int i = RatContainer.BACKPACK; i < RatContainer.SIZE; i++) {
            double d = meleeDamage(inv.getItem(i));
            if (d > bestDmg) { bestDmg = d; best = i; }
        }
        if (best >= 0) { ItemStack hand = inv.getItem(0); inv.setItem(0, inv.getItem(best)); inv.setItem(best, hand); }
    }
    private void shoot(LivingEntity t, AABB around) {
        double dist = rat.distanceTo(t);
        rat.getLookControl().setLookAt(t, 30, 30);
        if (dist > 10) rat.getNavigation().moveTo(t, 1.2);
        else if (dist < 4) {
            Vec3 away = rat.position().subtract(t.position()).normalize().scale(4).add(rat.position());
            rat.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
        } else rat.getNavigation().stop();
        if (shootCooldown > 0 || !rat.hasLineOfSight(t)) return;
        ItemStack bow = rat.getMainHandItem();
        boolean infinite = bow.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS) > 0;
        ItemStack ammo = infinite ? new ItemStack(Items.ARROW) : take(around, s -> s.getItem() instanceof ArrowItem, 1);
        if (ammo.isEmpty()) { w().notice(Component.translatable("brick.rats.work.no_arrows")); return; }
        AbstractArrow arrow = ProjectileUtil.getMobArrow(rat, ammo, 1.0F);
        double dx = t.getX() - rat.getX(), dy = t.getY(0.3333) - arrow.getY(), dz = t.getZ() - rat.getZ();
        arrow.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.6F, 4);
        if (infinite) arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        level().addFreshEntity(arrow);
        rat.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.2F);
        bow.hurtAndBreak(1, rat, r -> {});
        shootCooldown = 30;
    }
    /** 副手有盾、敌人贴脸：举盾一会儿（BrickWorkEvents 里挡掉正面的伤害） */
    private boolean tryBlock(LivingEntity t) {
        if (!(rat.getOffhandItem().getItem() instanceof ShieldItem) || blockTicks > 0 || cooldown > 0) return false;
        if (rat.distanceTo(t) > 3.2) return false;
        blockTicks = 30; cooldown = 70;
        rat.setBrickAction(Action.BLOCK);
        return true;
    }
    public static boolean blocking(TamedRat rat) { return rat.isBrick() && rat.getBrickAction() == Action.BLOCK; }

    private LivingEntity nearestEnemy(AABB box, Vec3 from) {
        LivingEntity best = null; double bd = Double.MAX_VALUE;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> e instanceof Enemy && e.isAlive() && !(e instanceof TamedRat))) {
            double d = e.position().distanceToSqr(from);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    // ================================================================== 侍卫
    private void guard() {
        LivingEntity owner = rat.getOwner();
        boolean follow = w().task(BrickJob.GUARD, 0), patrol = w().task(BrickJob.GUARD, 1), block = w().task(BrickJob.GUARD, 2), archer = w().task(BrickJob.GUARD, 3);
        boolean ownerHere = owner != null && owner.level() == rat.level() && owner.distanceTo(rat) < 32;
        AABB around = ownerHere ? owner.getBoundingBox().inflate(8, 4, 8) : w().box(BrickJob.GUARD) != null ? w().box(BrickJob.GUARD) : rat.getBoundingBox().inflate(8, 4, 8);
        LivingEntity t = rat.getTarget();
        if (t == null || !t.isAlive()) {
            t = null;
            if (follow && ownerHere) {
                LivingEntity attacker = owner.getLastHurtByMob();
                if (attacker != null && attacker.isAlive() && attacker != rat && attacker.distanceTo(owner) < 16) t = attacker;
                if (t == null) t = nearestEnemy(owner.getBoundingBox().inflate(8, 4, 8), owner.position());
            } else if (patrol && !ownerHere && w().box(BrickJob.GUARD) != null) t = nearestEnemy(w().box(BrickJob.GUARD), rat.position());
            if (t != null) rat.setTarget(t);
        }
        if (t != null) {
            pickWeapon(t, archer, around);
            if (block && tryBlock(t)) { rat.getLookControl().setLookAt(t, 30, 30); return; }
            if (rat.getMainHandItem().getItem() instanceof ProjectileWeaponItem) shoot(t, around);
            return;                                   // 近战：canUse 会让出控制
        }
        if (follow && ownerHere) {
            if (rat.distanceTo(owner) > 24) rat.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            else if (rat.distanceTo(owner) > 4) rat.getNavigation().moveTo(owner, 1.25);
            else rat.getNavigation().stop();
            w().quiet(Component.translatable("brick.rats.work.guarding"));
        } else if (patrol && w().box(BrickJob.GUARD) != null) {
            wanderIn(w().box(BrickJob.GUARD));
            w().quiet(Component.translatable("brick.rats.work.patrolling"));
        } else if (patrol) w().quiet(Component.translatable("brick.rats.work.need_area"));
    }
    private void wanderIn(AABB box) {
        if (--wanderCooldown > 0 && !rat.getNavigation().isDone()) return;
        wanderCooldown = 80 + rat.getRandom().nextInt(80);
        double x = box.minX + rat.getRandom().nextDouble() * (box.maxX - box.minX), z = box.minZ + rat.getRandom().nextDouble() * (box.maxZ - box.minZ);
        BlockPos p = level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
        rat.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 1.0);
    }

    // ================================================================== 刷怪（拿剑，驻守点半径 8）
    private void sentry() {
        BrickWork w = w();
        if (w.post == null) { w.quiet(Component.translatable("brick.rats.work.need_post")); return; }
        Vec3 post = Vec3.atBottomCenterOf(w.post);
        AABB zone = new AABB(w.post).inflate(BrickWork.SENTRY_RADIUS, 4, BrickWork.SENTRY_RADIUS);
        // 自保：低血吃东西；剑快坏了停手
        if (w.task(BrickJob.SENTRY, 3)) {
            if (rat.getHealth() < rat.getMaxHealth() * 0.5F && cooldown <= 0) {
                ItemStack food = take(zone, s -> s.isEdible() && !s.is(Items.ROTTEN_FLESH), 1);
                if (!food.isEmpty()) {
                    var props = food.getFoodProperties(rat);
                    rat.heal(props == null ? 2 : props.getNutrition());
                    rat.playSound(SoundEvents.GENERIC_EAT, 0.8F, 1.3F);
                    cooldown = 40;
                }
            }
            ItemStack sword = rat.getMainHandItem();
            if (sword.isDamageableItem() && sword.getMaxDamage() - sword.getDamageValue() <= Math.max(2, sword.getMaxDamage() / 10)) {
                rat.setTarget(null);
                w.notice(Component.translatable("brick.rats.work.weapon_low", sword.getHoverName()));
                walkTo(post, 1.0);
                return;
            }
        }
        if (!(meleeDamage(rat.getMainHandItem()) > 0)) equip(s -> meleeDamage(s) > 0);
        if (w.task(BrickJob.SENTRY, 0)) {
            LivingEntity t = nearestEnemy(zone, post);
            if (t != null) { rat.setTarget(t); return; }   // 让近战目标去打
        }
        // 捡掉落物
        if (w.task(BrickJob.SENTRY, 1)) {
            if (!(workEntity instanceof ItemEntity ie) || !ie.isAlive()) {
                workEntity = level().getEntitiesOfClass(ItemEntity.class, zone, ItemEntity::isAlive).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(rat))).orElse(null);
            }
            if (workEntity instanceof ItemEntity ie && ie.isAlive()) {
                if (walkTo(ie.position(), 1.2)) { stash(ie.getItem().copy()); ie.discard(); workEntity = null; }
                return;
            }
        }
        // 经验 → 经验瓶
        if (w.task(BrickJob.SENTRY, 2)) while (w.storedXp >= BrickWork.XP_PER_BOTTLE) { w.storedXp -= BrickWork.XP_PER_BOTTLE; stash(new ItemStack(Items.EXPERIENCE_BOTTLE)); }
        walkTo(post, 1.0);
        w.quiet(Component.translatable("brick.rats.work.on_post"));
    }

    // ================================================================== 农夫（工作区）
    private static boolean special(BlockState s) {
        Block b = s.getBlock();
        return b == Blocks.MELON || b == Blocks.PUMPKIN || b instanceof SugarCaneBlock
                || b instanceof CocoaBlock && s.getValue(CocoaBlock.AGE) >= 2
                || b instanceof SweetBerryBushBlock && s.getValue(SweetBerryBushBlock.AGE) >= 3;
    }
    private boolean ripe(BlockPos p, BlockState s) {
        if (s.getBlock() instanceof CropBlock crop) return crop.isMaxAge(s);
        if (s.getBlock() instanceof SugarCaneBlock) return level().getBlockState(p.below()).getBlock() instanceof SugarCaneBlock && !(level().getBlockState(p.below(2)).getBlock() instanceof SugarCaneBlock);
        return special(s);
    }
    private static boolean seed(ItemStack s) { return s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof CropBlock; }

    private void farmer() {
        BrickWork w = w();
        AABB box = w.box(BrickJob.FARMER);
        if (box == null) { w.quiet(Component.translatable("brick.rats.work.need_area")); return; }
        if (acting()) return;
        equip(s -> s.getItem() instanceof HoeItem);
        if (workPos != null) {
            if (!walkTo(workPos)) return;
            doFarm(workPos, box);
            workPos = null;
            return;
        }
        if (scanCooldown > 0) { wanderIn(box); return; }
        scanCooldown = 20;
        boolean harvest = w.task(BrickJob.FARMER, 0), till = w.task(BrickJob.FARMER, 1), meal = w.task(BrickJob.FARMER, 2), spec = w.task(BrickJob.FARMER, 3);
        boolean hoe = rat.getMainHandItem().getItem() instanceof HoeItem;
        boolean seeds = till && has(box, BrickJobGoal::seed), bone = meal && has(box, s -> s.is(Items.BONE_MEAL));
        BlockPos best = null; double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX - 1, box.maxY - 1, box.maxZ - 1))) {
            BlockState s = level().getBlockState(p);
            boolean job = false;
            if (harvest && s.getBlock() instanceof CropBlock && ripe(p, s)) job = true;
            else if (spec && special(s) && ripe(p, s)) job = true;
            else if (bone && s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s)) job = true;
            else if (till && hoe && (s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT_PATH)) && level().isEmptyBlock(p.above())) job = true;
            else if (seeds && s.is(Blocks.FARMLAND) && level().isEmptyBlock(p.above())) job = true;
            if (!job) continue;
            double d = p.distSqr(rat.blockPosition());
            if (d < bd) { bd = d; best = p.immutable(); }
        }
        workPos = best;
        if (best == null) { w.quiet(Component.translatable("brick.rats.work.farm_idle")); wanderIn(box); }
        else w.quiet(Component.translatable("brick.rats.work.farming"));
    }
    private void doFarm(BlockPos p, AABB box) {
        BlockState s = level().getBlockState(p);
        BrickWork w = w();
        if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s) && w.task(BrickJob.FARMER, 0)) {
            List<ItemStack> drops = Block.getDrops(s, level(), p, null, rat, rat.getMainHandItem());
            ItemStack seedItem = s.getBlock().getCloneItemStack(level(), p, s);
            level().setBlock(p, crop.getStateForAge(0), 3);               // 补种：用一颗收下来的种子
            boolean used = false;
            for (ItemStack d : drops) { if (!used && ItemStack.isSameItem(d, seedItem)) { d.shrink(1); used = true; } stash(d); }
            if (!used) level().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            act(Action.HOE, 14);
        } else if (special(s) && ripe(p, s) && w.task(BrickJob.FARMER, 3)) {
            if (s.getBlock() instanceof CocoaBlock) { Block.getDrops(s, level(), p, null, rat, ItemStack.EMPTY).forEach(this::stash); level().setBlock(p, s.setValue(CocoaBlock.AGE, 0), 3); }
            else if (s.getBlock() instanceof SweetBerryBushBlock) { stash(new ItemStack(Items.SWEET_BERRIES, 2 + rat.getRandom().nextInt(2))); level().setBlock(p, s.setValue(SweetBerryBushBlock.AGE, 1), 3); }
            else { Block.getDrops(s, level(), p, null, rat, rat.getMainHandItem()).forEach(this::stash); level().destroyBlock(p, false, rat); }
            act(Action.HOE, 14);
        } else if (s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s) && w.task(BrickJob.FARMER, 2)) {
            ItemStack bone = take(box, x -> x.is(Items.BONE_MEAL), 1);
            if (!bone.isEmpty()) { BoneMealItem.growCrop(bone, level(), p); level().levelEvent(1505, p, 0); act(Action.HOE, 10); }
        } else if ((s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT_PATH)) && level().isEmptyBlock(p.above()) && rat.getMainHandItem().getItem() instanceof HoeItem) {
            level().setBlock(p, Blocks.FARMLAND.defaultBlockState(), 11);
            level().playSound(null, p, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1, 1);
            rat.getMainHandItem().hurtAndBreak(1, rat, r -> {});
            act(Action.HOE, 14);
        } else if (s.is(Blocks.FARMLAND) && level().isEmptyBlock(p.above()) && w.task(BrickJob.FARMER, 1)) {
            ItemStack sd = take(box, BrickJobGoal::seed, 1);
            if (!sd.isEmpty() && sd.getItem() instanceof BlockItem bi) {
                BlockState plant = bi.getBlock().defaultBlockState();
                if (plant.canSurvive(level(), p.above())) { level().setBlock(p.above(), plant, 3); act(Action.HOE, 10); }
                else stash(sd);
            }
        }
    }

    // ================================================================== 牧场主（工作区）
    private void rancher() {
        BrickWork w = w();
        AABB box = w.box(BrickJob.RANCHER);
        if (box == null) { w.quiet(Component.translatable("brick.rats.work.need_area")); return; }
        if (acting()) return;
        if (workEntity != null && workEntity.isAlive()) {
            if (!walkTo(workEntity.position(), 1.6)) return;
            doRanch(workEntity, box);
            workEntity = null;
            return;
        }
        workEntity = null;
        if (scanCooldown > 0) { wanderIn(box); return; }
        scanCooldown = 30;
        long now = level().getGameTime();
        List<Animal> animals = level().getEntitiesOfClass(Animal.class, box, Animal::isAlive);
        // 捡掉落：蛋、羽毛、羊毛等
        if (w.task(BrickJob.RANCHER, 3)) {
            var item = level().getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(rat)));
            if (item.isPresent()) { workEntity = item.get(); return; }
        }
        if (w.task(BrickJob.RANCHER, 1) && has(box, s -> s.getItem() instanceof ShearsItem)) {
            var sheep = animals.stream().filter(a -> a instanceof Shearable sh && sh.readyForShearing()).min(Comparator.comparingDouble(a -> a.distanceToSqr(rat)));
            if (sheep.isPresent()) { workEntity = sheep.get(); return; }
        }
        if (w.task(BrickJob.RANCHER, 2) && has(box, s -> s.is(Items.BUCKET))) {
            var cow = animals.stream().filter(a -> a instanceof Cow && !a.isBaby() && milked.getOrDefault(a.getUUID(), 0L) < now).min(Comparator.comparingDouble(a -> a.distanceToSqr(rat)));
            if (cow.isPresent()) { workEntity = cow.get(); return; }
        }
        if (w.task(BrickJob.RANCHER, 0)) {
            Map<Object, Integer> counts = new HashMap<>();
            for (Animal a : animals) counts.merge(a.getType(), 1, Integer::sum);
            for (Animal a : animals) {
                if (a.getAge() != 0 || a.isInLove() || !a.canFallInLove() || counts.get(a.getType()) >= w.breedCap) continue;
                // 同种里还要有另一只能配对的
                boolean mate = animals.stream().anyMatch(b -> b != a && b.getType() == a.getType() && b.getAge() == 0 && (b.isInLove() || b.canFallInLove()));
                if (mate && has(box, a::isFood)) { workEntity = a; return; }
            }
        }
        w.quiet(Component.translatable("brick.rats.work.ranch_idle"));
        wanderIn(box);
    }
    private void doRanch(Entity e, AABB box) {
        long now = level().getGameTime();
        if (e instanceof ItemEntity ie) { stash(ie.getItem().copy()); ie.discard(); return; }
        if (e instanceof Shearable sh && sh.readyForShearing() && equip(s -> s.getItem() instanceof ShearsItem)) {
            sh.shear(SoundSource.NEUTRAL);
            rat.getMainHandItem().hurtAndBreak(1, rat, r -> {});
            act(Action.SHEAR, 18);
            return;
        }
        if (e instanceof Cow cow && !cow.isBaby()) {
            ItemStack bucket = take(box, s -> s.is(Items.BUCKET), 1);
            if (!bucket.isEmpty()) {
                rat.playSound(SoundEvents.COW_MILK, 1, 1);
                stash(new ItemStack(Items.MILK_BUCKET));
                milked.put(cow.getUUID(), now + 2400);              // 同一头牛 2 分钟挤一次
                act(Action.MILK, 20);
            }
            return;
        }
        if (e instanceof Animal a && a.getAge() == 0 && a.canFallInLove()) {
            ItemStack food = take(box, a::isFood, 1);
            if (!food.isEmpty()) {
                a.setInLove(rat.getOwner() instanceof net.minecraft.world.entity.player.Player p ? p : null);
                act(Action.SHEAR, 10);
            }
        }
    }

    // ================================================================== 寻宝鼠
    private void treasure() {
        BrickWork w = w();
        long time = level().getDayTime(), day = time / 24000L, now = level().getGameTime();
        if (day != w.treasureDay) { w.treasureDay = day; w.treasureToday = 0; w.nextTreasure = now + 600 + rat.getRandom().nextInt(4000); }
        BlockPos home = w.home != null ? w.home : rat.blockPosition();
        // 献宝：走到主人身边，举起来，交给主人
        if (!treasure.isEmpty()) {
            LivingEntity owner = rat.getOwner();
            if (owner instanceof net.minecraft.server.level.ServerPlayer p && p.level() == rat.level() && p.distanceTo(rat) < 48) {
                if (presentTicks > 0) {
                    rat.getLookControl().setLookAt(p, 30, 30);
                    if (--presentTicks == 0) {
                        Component name = treasure.getHoverName();
                        int n = treasure.getCount();
                        restoreHand();
                        if (!p.getInventory().add(treasure)) p.drop(treasure, false);
                        p.sendSystemMessage(Component.translatable("brick.rats.treasure.found", rat.getDisplayName(), name, n));
                        rat.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.6F);
                        treasure = ItemStack.EMPTY;
                        rat.setBrickAction(Action.NONE);
                    }
                    return;
                }
                if (walkTo(p.position(), 2.2)) {
                    presentTicks = 40; rat.setBrickAction(Action.PRESENT);
                    // 献宝时爪子里举着宝藏：主手的工具先收起来，交完再拿回
                    handBackup = rat.getInventory().getItem(0);
                    rat.getInventory().setItem(0, treasure.copy());
                }
                return;
            }
            // 主人不在：放进存放箱，等主人回来再说
            Component name = treasure.getHoverName(); int n = treasure.getCount();
            stash(treasure); treasure = ItemStack.EMPTY;
            w.notice(Component.translatable("brick.rats.treasure.stored", name, n));
            return;
        }
        // 挖宝
        if (digTicks > 0) {
            if (digTicks % 10 == 0) level().levelEvent(2001, rat.blockPosition().below(), Block.getId(level().getBlockState(rat.blockPosition().below())));
            if (--digTicks == 0) { treasure = BrickTreasure.roll(level(), (net.minecraft.server.level.ServerPlayer) (rat.getOwner() instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null), rat.getRandom()); rat.setBrickAction(Action.NONE); }
            return;
        }
        if (w.treasureToday < BrickWork.TREASURE_PER_DAY && now >= w.nextTreasure) {
            w.treasureToday++;
            w.nextTreasure = now + 2500 + rat.getRandom().nextInt(4500);
            equip(s -> s.getItem() instanceof ShovelItem);
            rat.getNavigation().stop();
            digTicks = 70;
            rat.setBrickAction(Action.DIG);
            w.quiet(Component.translatable("brick.rats.treasure.digging"));
            return;
        }
        // 在家附近闲逛，偶尔假装挖两下
        wanderIn(new AABB(home).inflate(10, 3, 10));
        if (rat.getNavigation().isDone() && rat.getRandom().nextInt(400) == 0) act(Action.DIG, 30);
        acting();
        w.quiet(Component.translatable("brick.rats.treasure.wandering", BrickWork.TREASURE_PER_DAY - w.treasureToday));
    }
}
