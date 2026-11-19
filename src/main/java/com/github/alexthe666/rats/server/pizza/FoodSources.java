package com.github.alexthe666.rats.server.pizza;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 偷食鼠能偷的地方：玩家附近地上的食物、附近容器（含布瑞克出餐箱）、附近放在地上的精致存储终端方块（不偷玩家身上的无线终端）。
 * 精致存储全部走反射，没装就跳过。
 */
final class FoodSources {
    static final int RANGE = 16, MAX_TAKE = 16;

    /** 一个可偷的来源：at = 偷食鼠出现的位置，steal = 真的拿走（返回拿到的食物） */
    record Source(String kind, Vec3 at, ItemEntity ground, Supplier<ItemStack> steal) {}

    static boolean food(ItemStack s) { return !s.isEmpty() && s.isEdible(); }

    static List<Source> find(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        List<Source> out = new ArrayList<>();
        AABB box = p.getBoundingBox().inflate(RANGE, 6, RANGE);
        // 地上的食物
        for (ItemEntity e : lvl.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && food(e.getItem()) && e.getAge() > 100))
            out.add(new Source("ground", e.position(), e, null));
        // 附近容器 / 精致存储终端方块
        BlockPos c = p.blockPosition();
        for (int cx = (c.getX() - RANGE) >> 4; cx <= (c.getX() + RANGE) >> 4; cx++)
            for (int cz = (c.getZ() - RANGE) >> 4; cz <= (c.getZ() + RANGE) >> 4; cz++) {
                if (!lvl.hasChunk(cx, cz)) continue;
                for (BlockEntity be : lvl.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (!box.contains(Vec3.atCenterOf(pos))) continue;
                    Vec3 at = Vec3.atBottomCenterOf(free(lvl, pos));
                    if (isRsGrid(be)) {
                        Object net = Rs.network(be);
                        if (net != null && Rs.hasFood(net)) out.add(new Source("terminal", at, null, () -> Rs.take(net)));
                        continue;
                    }
                    var cap = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve();
                    if (cap.isEmpty()) continue;
                    IItemHandler h = cap.get();
                    for (int i = 0; i < h.getSlots(); i++) if (food(h.getStackInSlot(i))) {
                        out.add(new Source("container", at, null, () -> takeFrom(h)));
                        break;
                    }
                }
            }
        // 不偷玩家身上的无线终端，只偷放在地上的终端方块（上面已处理）
        return out;
    }

    private static ItemStack takeFrom(IItemHandler h) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < h.getSlots(); i++) if (food(h.getStackInSlot(i))) slots.add(i);
        if (slots.isEmpty()) return ItemStack.EMPTY;
        int slot = slots.get(new Random().nextInt(slots.size()));
        return h.extractItem(slot, MAX_TAKE, false);
    }

    /** 容器旁边能站的位置（找不到就容器上方） */
    private static BlockPos free(ServerLevel lvl, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos q = pos.relative(d);
            if (lvl.isEmptyBlock(q) && lvl.getBlockState(q.below()).isFaceSturdy(lvl, q.below(), Direction.UP)) return q;
        }
        return pos.above();
    }

    private static boolean isRsGrid(BlockEntity be) {
        var id = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType());
        if (id == null) return false;
        return (id.getNamespace().equals("refinedstorage") || id.getNamespace().equals("refinedstorageaddons")) && id.getPath().contains("grid");
    }

    /** 精致存储（反射）：网络里有没有食物、拿一组食物 */
    static final class Rs {
        private static final String API = "com.refinedmods.refinedstorage.api.";
        private static boolean warned;
        private static void warn(Throwable e) { if (!warned) { warned = true; LogUtils.getLogger().warn("披萨鼠：读取精致存储失败", e); } }

        static Object network(BlockEntity be) {
            try {
                Object node = be.getClass().getMethod("getNode").invoke(be);
                return node == null ? null : node.getClass().getMethod("getNetwork").invoke(node);
            } catch (ReflectiveOperationException | LinkageError e) { warn(e); return null; }
        }

        private static List<ItemStack> foods(Object net) throws ReflectiveOperationException {
            List<ItemStack> out = new ArrayList<>();
            Class<?> inet = Class.forName(API + "network.INetwork");
            if (!(boolean) inet.getMethod("canRun").invoke(net)) return out;
            Object cache = inet.getMethod("getItemStorageCache").invoke(net);
            Object list = Class.forName(API + "storage.cache.IStorageCache").getMethod("getList").invoke(cache);
            for (Object entry : (Collection<?>) Class.forName(API + "util.IStackList").getMethod("getStacks").invoke(list)) {
                ItemStack s = (ItemStack) Class.forName(API + "util.StackListEntry").getMethod("getStack").invoke(entry);
                if (food(s)) out.add(s);
            }
            return out;
        }

        static boolean hasFood(Object net) {
            try { return !foods(net).isEmpty(); } catch (ReflectiveOperationException | LinkageError e) { warn(e); return false; }
        }

        static ItemStack take(Object net) {
            try {
                List<ItemStack> f = foods(net);
                if (f.isEmpty()) return ItemStack.EMPTY;
                ItemStack pick = f.get(new Random().nextInt(f.size()));
                Class<?> act = Class.forName(API + "util.Action");
                Object perform = act.getField("PERFORM").get(null);
                ItemStack got = (ItemStack) Class.forName(API + "network.INetwork").getMethod("extractItem", ItemStack.class, int.class, act).invoke(net, pick, MAX_TAKE, perform);
                return got == null ? ItemStack.EMPTY : got;
            } catch (ReflectiveOperationException | LinkageError e) { warn(e); return ItemStack.EMPTY; }
        }
    }
}
