package com.github.alexthe666.rats.server.brick;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** 寻宝鼠的宝藏：读 data/rats/brick_treasure.json（可被数据包覆盖），按主人进度解锁档次 */
public final class BrickTreasure {
    private static final ResourceLocation TABLE = new ResourceLocation("rats", "brick_treasure.json");
    private record Entry(Item item, int min, int max, int weight) {}
    private record Tier(ResourceLocation unlock, List<Entry> entries) {}

    private static List<Tier> load(ServerLevel level) {
        List<Tier> tiers = new ArrayList<>();
        var res = level.getServer().getResourceManager().getResource(TABLE);
        if (res.isEmpty()) return tiers;
        try (var reader = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (var t : GsonHelper.getAsJsonArray(root, "tiers")) {
                JsonObject o = t.getAsJsonObject();
                ResourceLocation unlock = o.has("unlock") && !o.get("unlock").isJsonNull() ? new ResourceLocation(o.get("unlock").getAsString()) : null;
                List<Entry> entries = new ArrayList<>();
                for (var e : GsonHelper.getAsJsonArray(o, "entries")) {
                    JsonObject eo = e.getAsJsonObject();
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(GsonHelper.getAsString(eo, "item")));
                    if (item == null || item == Items.AIR) continue;          // 整合包里没有的跳过
                    entries.add(new Entry(item, GsonHelper.getAsInt(eo, "min", 1), GsonHelper.getAsInt(eo, "max", 1), GsonHelper.getAsInt(eo, "weight", 1)));
                }
                if (!entries.isEmpty()) tiers.add(new Tier(unlock, entries));
            }
        } catch (Exception e) { LogUtils.getLogger().warn("brick_treasure.json 读取失败", e); }
        return tiers;
    }

    private static boolean unlocked(ServerPlayer p, ResourceLocation id) {
        if (id == null) return true;
        if (p == null) return false;
        var adv = p.server.getAdvancements().getAdvancement(id);
        return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
    }

    public static ItemStack roll(ServerLevel level, ServerPlayer owner, RandomSource random) {
        List<Tier> open = new ArrayList<>();
        for (Tier t : load(level)) if (unlocked(owner, t.unlock())) open.add(t);
        if (open.isEmpty()) return new ItemStack(Items.IRON_NUGGET, 3 + random.nextInt(5));
        Tier tier = random.nextFloat() < 0.55F ? open.get(open.size() - 1) : open.get(random.nextInt(open.size()));
        int total = tier.entries().stream().mapToInt(Entry::weight).sum(), r = random.nextInt(Math.max(1, total));
        for (Entry e : tier.entries()) {
            r -= e.weight();
            if (r < 0) return new ItemStack(e.item(), e.min() + random.nextInt(Math.max(1, e.max() - e.min() + 1)));
        }
        return new ItemStack(Items.IRON_NUGGET, 3);
    }
}
