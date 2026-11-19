package com.github.alexthe666.rats.server.pizza;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** 披萨鼠存档（存在主世界）：每个玩家今天有没有被偷过、所有还没清掉的鼠洞（含被偷食物和面包屑路线） */
public final class PizzaRatData extends SavedData {
    public static final class Hole {
        public BlockPos pos; public String dim; public ItemStack stolen; public UUID victim; public Vec3 from; public long created; public int id;
        /** 下水道副本：是否已生成、进度（0 未开始，1~3 第几波，4 鼠头头，5 清完）、当前这波还活着的怪、这波总数、下一波开始时间 */
        public boolean generated; public int wave; public final List<UUID> mobs = new ArrayList<>(); public int total; public long nextAt;
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putBoolean("Generated", generated); t.putInt("Wave", wave); t.putInt("Total", total); t.putLong("NextAt", nextAt);
            ListTag m = new ListTag();
            for (UUID u : mobs) { CompoundTag e = new CompoundTag(); e.putUUID("U", u); m.add(e); }
            t.put("Mobs", m);
            t.put("Pos", NbtUtils.writeBlockPos(pos)); t.putString("Dim", dim); t.put("Stolen", stolen.save(new CompoundTag()));
            if (victim != null) t.putUUID("Victim", victim);
            t.putDouble("FX", from.x); t.putDouble("FY", from.y); t.putDouble("FZ", from.z); t.putLong("Created", created); t.putInt("Id", id);
            return t;
        }
        static Hole load(CompoundTag t) {
            Hole h = new Hole();
            h.pos = NbtUtils.readBlockPos(t.getCompound("Pos")); h.dim = t.getString("Dim"); h.stolen = ItemStack.of(t.getCompound("Stolen"));
            h.victim = t.hasUUID("Victim") ? t.getUUID("Victim") : null;
            h.from = new Vec3(t.getDouble("FX"), t.getDouble("FY"), t.getDouble("FZ")); h.created = t.getLong("Created"); h.id = t.getInt("Id");
            h.generated = t.getBoolean("Generated"); h.wave = t.getInt("Wave"); h.total = t.getInt("Total"); h.nextAt = t.getLong("NextAt");
            for (Tag e : t.getList("Mobs", Tag.TAG_COMPOUND)) h.mobs.add(((CompoundTag) e).getUUID("U"));
            return h;
        }
    }

    public final Map<UUID, Long> lastSteal = new HashMap<>();
    public final List<Hole> holes = new ArrayList<>();
    public int nextId = 1;

    public static PizzaRatData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PizzaRatData::load, PizzaRatData::new, "rats_pizza_rat");
    }

    public Hole byId(int id) { for (Hole h : holes) if (h.id == id) return h; return null; }

    public Hole holeAt(BlockPos pos, String dim) {
        for (Hole h : holes) if (h.pos.equals(pos) && h.dim.equals(dim)) return h;
        return null;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag days = new ListTag();
        lastSteal.forEach((u, d) -> { CompoundTag t = new CompoundTag(); t.putUUID("U", u); t.putLong("D", d); days.add(t); });
        tag.put("LastSteal", days);
        ListTag hs = new ListTag();
        for (Hole h : holes) hs.add(h.save());
        tag.put("Holes", hs);
        tag.putInt("NextId", nextId);
        return tag;
    }

    static PizzaRatData load(CompoundTag tag) {
        PizzaRatData d = new PizzaRatData();
        for (Tag t : tag.getList("LastSteal", Tag.TAG_COMPOUND)) d.lastSteal.put(((CompoundTag) t).getUUID("U"), ((CompoundTag) t).getLong("D"));
        for (Tag t : tag.getList("Holes", Tag.TAG_COMPOUND)) d.holes.add(Hole.load((CompoundTag) t));
        d.nextId = Math.max(1, tag.getInt("NextId"));
        return d;
    }
}
