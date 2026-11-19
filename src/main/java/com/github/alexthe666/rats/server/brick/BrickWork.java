package com.github.alexthe666.rats.server.brick;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.inventory.RatMenu;
import com.github.alexthe666.rats.server.inventory.container.RatContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * 布瑞克的职业工作状态（存在 TamedRat 上，随实体存档）。国王权杖界面读写这里。
 * 子任务按职业各一个位掩码；每个职业一块工作区（两点圈地）；刷怪另有驻守点。
 */
public final class BrickWork {
    /** 每个职业的子任务（界面按这个顺序列出）；位 = 下标 */
    public static final Map<BrickJob, String[]> TASKS = Map.of(
            BrickJob.GUARD, new String[]{"follow", "patrol", "block", "archer"},
            BrickJob.SENTRY, new String[]{"post", "loot", "xp", "self"},
            BrickJob.TREASURE, new String[]{"hunt"},
            BrickJob.FARMER, new String[]{"harvest", "till", "bonemeal", "special"},
            BrickJob.RANCHER, new String[]{"breed", "shear", "milk", "collect"});
    public static final int MAX_AREA = 32, SENTRY_RADIUS = 8, TREASURE_PER_DAY = 3, XP_PER_BOTTLE = 7;

    private final TamedRat rat;
    public boolean working;
    private final EnumMap<BrickJob, Integer> masks = new EnumMap<>(BrickJob.class);
    private final EnumMap<BrickJob, BlockPos[]> areas = new EnumMap<>(BrickJob.class);
    public BlockPos post;
    public BlockPos home;
    public int breedCap = 16;
    public int storedXp;
    public long treasureDay = -1, nextTreasure = -1;
    public int treasureToday;
    public String dimension = "";
    public Component status = Component.empty();
    private long lastNotice;

    public BrickWork(TamedRat rat) { this.rat = rat; }

    public boolean task(BrickJob job, int bit) { return (masks.getOrDefault(job, 0xF) & (1 << bit)) != 0; }
    public int mask(BrickJob job) { return masks.getOrDefault(job, 0xF); }
    public void toggle(BrickJob job, int bit) { masks.put(job, mask(job) ^ (1 << bit)); }
    public BlockPos[] area(BrickJob job) { return areas.get(job); }
    public void setArea(BrickJob job, BlockPos a, BlockPos b) {
        // 限制最大 32×32（超出的那一边收回来）
        int x2 = Mth(a.getX(), b.getX()), z2 = Mth(a.getZ(), b.getZ());
        areas.put(job, new BlockPos[]{a.immutable(), new BlockPos(x2, b.getY(), z2)});
        dimension = rat.level().dimension().location().toString();
    }
    private static int Mth(int a, int b) { return Math.abs(b - a) >= MAX_AREA ? a + (b > a ? MAX_AREA - 1 : -(MAX_AREA - 1)) : b; }
    public void clearArea(BrickJob job) { areas.remove(job); if (job == BrickJob.SENTRY) post = null; }

    /** 工作区的方块范围（含两角，高度上下各扩 3 格）；没圈地返回 null */
    public AABB box(BrickJob job) {
        BlockPos[] a = areas.get(job);
        if (a == null) return null;
        return new AABB(Math.min(a[0].getX(), a[1].getX()), Math.min(a[0].getY(), a[1].getY()) - 3, Math.min(a[0].getZ(), a[1].getZ()),
                Math.max(a[0].getX(), a[1].getX()) + 1, Math.max(a[0].getY(), a[1].getY()) + 4, Math.max(a[0].getZ(), a[1].getZ()) + 1);
    }

    public void start() {
        working = true;
        if (home == null) home = rat.blockPosition().immutable();
        if (rat.getCommand() == com.github.alexthe666.rats.server.entity.rat.RatCommand.SIT) rat.setCommand(com.github.alexthe666.rats.server.entity.rat.RatCommand.WANDER);
        status = Component.translatable("brick.rats.work.started");
    }
    public void stop() { working = false; rat.setBrickAction(0); status = Component.translatable("brick.rats.work.stopped"); }

    ServerPlayer owner() {
        if (rat.level().isClientSide() || rat.getOwnerUUID() == null) return null;
        return rat.level().getServer().getPlayerList().getPlayer(rat.getOwnerUUID());
    }
    /** 给主人发一句状态（同一句 30 秒内不重复） */
    void notice(Component message) {
        long now = rat.level().getGameTime();
        if (message.equals(status) && now - lastNotice < 600) return;
        status = message; lastNotice = now;
        var p = owner();
        if (p != null) p.displayClientMessage(Component.translatable("brick.rats.work.prefix", rat.getDisplayName(), message), false);
    }
    void quiet(Component message) { status = message; }

    // ---------------------------------------------------------------- 盔甲栏属性（每秒同步一次）
    private static final UUID[] ARMOR_IDS = new UUID[16];
    static { for (int i = 0; i < 16; i++) ARMOR_IDS[i] = UUID.nameUUIDFromBytes(("rats_brick_armor_" + i).getBytes()); }
    private final Map<AttributeInstance, Set<UUID>> applied = new HashMap<>();

    public void tickArmor() {
        if (rat.level().isClientSide() || rat.tickCount % 20 != 0) return;
        clear();
        if (!rat.isBrick()) return;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = rat.getInventory().getItem(RatContainer.BRICK_ARMOR + i);
            EquipmentSlot type = RatMenu.BRICK_ARMOR_SLOTS[i];
            if (!(stack.getItem() instanceof ArmorItem)) continue;
            int n = 0;
            for (var e : stack.getAttributeModifiers(type).entries()) {
                AttributeInstance inst = rat.getAttribute(e.getKey());
                if (inst == null || n >= 4) continue;
                UUID id = ARMOR_IDS[i * 4 + n++];
                AttributeModifier m = e.getValue();
                inst.addTransientModifier(new AttributeModifier(id, "brick armor", m.getAmount(), m.getOperation()));
                applied.computeIfAbsent(inst, k -> new HashSet<>()).add(id);
            }
        }
    }
    private void clear() {
        applied.forEach((inst, ids) -> ids.forEach(inst::removeModifier));
        applied.clear();
    }
    public static void clearArmor(TamedRat rat) { rat.work.clear(); }

    /** 受到伤害时盔甲掉耐久（同原版：每 4 点伤害 1 点，至少 1） */
    public void hurtArmor(float damage) {
        int cost = Math.max(1, (int) (damage / 4));
        for (int i = 0; i < 4; i++) {
            ItemStack stack = rat.getInventory().getItem(RatContainer.BRICK_ARMOR + i);
            if (stack.getItem() instanceof ArmorItem && stack.isDamageableItem()) {
                final int slot = RatContainer.BRICK_ARMOR + i;
                stack.hurtAndBreak(cost, rat, r -> r.getInventory().setItem(slot, ItemStack.EMPTY));
            }
        }
    }

    // ---------------------------------------------------------------- 存档
    public void save(CompoundTag root) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Working", working);
        masks.forEach((job, m) -> t.putInt("Mask_" + job.name(), m));
        areas.forEach((job, a) -> { t.put("AreaA_" + job.name(), NbtUtils.writeBlockPos(a[0])); t.put("AreaB_" + job.name(), NbtUtils.writeBlockPos(a[1])); });
        if (post != null) t.put("Post", NbtUtils.writeBlockPos(post));
        if (home != null) t.put("Home", NbtUtils.writeBlockPos(home));
        t.putInt("BreedCap", breedCap);
        t.putInt("StoredXp", storedXp);
        t.putLong("TreasureDay", treasureDay);
        t.putLong("NextTreasure", nextTreasure);
        t.putInt("TreasureToday", treasureToday);
        t.putString("Dim", dimension);
        root.put("BrickWork", t);
    }
    public void load(CompoundTag root) {
        if (!root.contains("BrickWork")) return;
        CompoundTag t = root.getCompound("BrickWork");
        working = t.getBoolean("Working");
        for (BrickJob job : BrickJob.values()) {
            if (t.contains("Mask_" + job.name())) masks.put(job, t.getInt("Mask_" + job.name()));
            if (t.contains("AreaA_" + job.name())) areas.put(job, new BlockPos[]{NbtUtils.readBlockPos(t.getCompound("AreaA_" + job.name())), NbtUtils.readBlockPos(t.getCompound("AreaB_" + job.name()))});
        }
        post = t.contains("Post") ? NbtUtils.readBlockPos(t.getCompound("Post")) : null;
        home = t.contains("Home") ? NbtUtils.readBlockPos(t.getCompound("Home")) : null;
        breedCap = t.contains("BreedCap") ? t.getInt("BreedCap") : 16;
        storedXp = t.getInt("StoredXp");
        treasureDay = t.getLong("TreasureDay");
        nextTreasure = t.getLong("NextTreasure");
        treasureToday = t.getInt("TreasureToday");
        dimension = t.getString("Dim");
    }
}
