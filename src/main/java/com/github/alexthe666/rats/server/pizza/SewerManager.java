package com.github.alexthe666.rats.server.pizza;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatlantisEntityRegistry;
import com.github.alexthe666.rats.registry.RatsEntityRegistry;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 下水道副本：只有一个「下水道」维度，每个鼠洞在里面占一块（x = id × 2048）。
 * 第一次进入时生成随机下水道；清剿进度 3 波 + 鼠头头；清完出口亮起，原路回去后鼠洞塌掉，这块区域的文件在下次开服时删除。
 */
@Mod.EventBusSubscriber(modid = RatsMod.MODID)
public final class SewerManager {
    public static final ResourceKey<Level> SEWER = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(RatsMod.MODID, "sewer"));
    static final int SPACING = 2048;
    private static final Map<Integer, ServerBossEvent> BARS = new HashMap<>();
    private SewerManager() {}

    static int originX(int id) { return id * SPACING; }
    static int idAt(double x) { return (int) Math.floor((x + SPACING / 2.0) / SPACING); }
    static AABB box(int id) { return new AABB(originX(id) - 8, 0, -8, originX(id) + SewerLayout.SX + 8, 128, SewerLayout.SZ + 8); }
    static BlockPos world(int id, int x, int y, int z) { return new BlockPos(originX(id) + x, SewerLayout.BASE_Y + y, z); }
    static SewerLayout layout(MinecraftServer server, PizzaRatData.Hole h) { return new SewerLayout(server.overworld().getSeed() ^ (h.id * 0x9E3779B97F4A7C15L)); }

    // ================================================================== 进出
    /** 主世界右键鼠洞：生成（第一次）并传送进去 */
    static void enter(ServerPlayer p, PizzaRatData.Hole h) {
        ServerLevel sewer = p.server.getLevel(SEWER);
        if (sewer == null) { p.displayClientMessage(Component.translatable("rats.sewer.missing"), false); return; }
        SewerLayout L = layout(p.server, h);
        if (!h.generated) {
            generate(sewer, h.id, L);
            h.generated = true;
            PizzaRatData.get(p.server).setDirty();
        }
        BlockPos s = world(h.id, L.start[0], L.start[1], L.start[2]);
        p.teleportTo(sewer, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, -90, 0);
        p.sendSystemMessage(Component.translatable(h.wave >= 5 ? "rats.sewer.enter_done" : "rats.sewer.enter", h.stolen.getHoverName(), h.stolen.getCount()).withStyle(ChatFormatting.GOLD));
        if (h.wave == 0 && h.nextAt == 0) { h.nextAt = sewer.getGameTime() + 100; PizzaRatData.get(p.server).setDirty(); }
    }

    /** 下水道里右键出口鼠洞：原路回去；清完了并且最后一个人走了，主世界的鼠洞塌掉、副本作废 */
    static void exit(ServerPlayer p, BlockPos clicked) {
        PizzaRatData data = PizzaRatData.get(p.server);
        PizzaRatData.Hole h = data.byId(idAt(clicked.getX()));
        ServerLevel back = h == null ? p.server.overworld() : p.server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(h.dim)));
        if (back == null) back = p.server.overworld();
        BlockPos to = h == null ? back.getSharedSpawnPos() : h.pos.above();
        if (h != null && h.wave < 5) p.sendSystemMessage(Component.translatable("rats.sewer.retreat").withStyle(ChatFormatting.GRAY));
        p.teleportTo(back, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), p.getXRot());
        if (h != null && h.wave >= 5 && playersIn(p.server.getLevel(SEWER), h.id).isEmpty()) collapse(p.server, h);
    }

    static List<ServerPlayer> playersIn(ServerLevel sewer, int id) {
        if (sewer == null) return List.of();
        AABB b = box(id);
        return sewer.players().stream().filter(pl -> b.contains(pl.position())).toList();
    }

    /** 鼠洞塌掉：主世界方块换成粗泥、删掉记录、登记区域文件下次开服删除 */
    static void collapse(MinecraftServer server, PizzaRatData.Hole h) {
        ServerLevel lvl = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(h.dim)));
        if (lvl != null && lvl.getBlockState(h.pos).is(PizzaRatRegistry.RAT_HOLE.get())) {
            lvl.setBlock(h.pos, Blocks.COARSE_DIRT.defaultBlockState(), 3);
            lvl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, h.pos.getX() + 0.5, h.pos.getY() + 1, h.pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.01);
            lvl.playSound(null, h.pos, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 1, 0.7F);
        }
        PizzaRatData data = PizzaRatData.get(server);
        data.holes.remove(h); data.setDirty();
        var bar = BARS.remove(h.id);
        if (bar != null) bar.removeAllPlayers();
        markForCleanup(server, h.id);
    }

    // ================================================================== 生成
    private static BlockState state(byte b, Random r) {
        return switch (b) {
            case SewerLayout.BRICK, SewerLayout.CEIL -> Blocks.STONE_BRICKS.defaultBlockState();
            case SewerLayout.MOSSY -> Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            case SewerLayout.CRACKED -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            case SewerLayout.FLOOR -> r.nextInt(5) == 0 ? Blocks.ANDESITE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
            case SewerLayout.WATER -> Blocks.WATER.defaultBlockState();
            case SewerLayout.MUD -> Blocks.MUD.defaultBlockState();
            case SewerLayout.PILLAR -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case SewerLayout.LANTERN -> Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
            case SewerLayout.COBWEB -> Blocks.COBWEB.defaultBlockState();
            case SewerLayout.BARS, SewerLayout.GRATE -> Blocks.IRON_BARS.defaultBlockState();
            case SewerLayout.BARREL -> Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
            case SewerLayout.CHAIN -> Blocks.CHAIN.defaultBlockState();
            case SewerLayout.EXIT -> PizzaRatRegistry.RAT_HOLE.get().defaultBlockState();
            case SewerLayout.PIPE -> Blocks.WAXED_EXPOSED_CUT_COPPER.defaultBlockState();
            case SewerLayout.PIZZA_BOX -> Blocks.BROWN_CARPET.defaultBlockState();
            case SewerLayout.CANDLE -> Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + r.nextInt(3)).setValue(CandleBlock.LIT, true);
            case SewerLayout.SLIME -> Blocks.SLIME_BLOCK.defaultBlockState();
            case SewerLayout.VAULT -> Blocks.BRICKS.defaultBlockState();
            case SewerLayout.VAULT_D -> Blocks.MUD_BRICKS.defaultBlockState();
            case SewerLayout.RIB -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            default -> null;
        };
    }

    static void generate(ServerLevel sewer, int id, SewerLayout L) {
        long t0 = System.currentTimeMillis();
        Random r = new Random(id);
        int ox = originX(id);
        for (int cx = ox >> 4; cx <= (ox + SewerLayout.SX) >> 4; cx++) for (int cz = 0; cz <= SewerLayout.SZ >> 4; cz++) sewer.getChunk(cx, cz);
        int placed = 0;
        for (int y = 0; y < SewerLayout.H; y++) for (int z = 0; z < SewerLayout.SZ; z++) for (int x = 0; x < SewerLayout.SX; x++) {
            BlockState s = state(L.get(x, y, z), r);
            if (s == null) continue;
            sewer.setBlock(world(id, x, y, z), s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            placed++;
        }
        LogUtils.getLogger().info("下水道副本 #{} 生成完成：{} 个方块，用时 {} ms", id, placed, System.currentTimeMillis() - t0);
    }

    // ================================================================== 清剿进度
    private static final int[][] WAVES = {{6, 2, 0}, {6, 4, 0}, {4, 4, 3}};   // 每波：野鼠、瘟疫鼠、海盗鼠

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer().getTickCount() % 10 != 0) return;
        ServerLevel sewer = e.getServer().getLevel(SEWER);
        if (sewer == null) return;
        PizzaRatData data = PizzaRatData.get(e.getServer());
        for (PizzaRatData.Hole h : new ArrayList<>(data.holes)) {
            if (!h.generated) continue;
            List<ServerPlayer> inside = playersIn(sewer, h.id);
            ServerBossEvent bar = BARS.computeIfAbsent(h.id, k -> new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10));
            for (ServerPlayer pl : new ArrayList<>(bar.getPlayers())) if (!inside.contains(pl)) bar.removePlayer(pl);
            if (inside.isEmpty()) continue;                                 // 没人在里面就暂停
            for (ServerPlayer pl : inside) bar.addPlayer(pl);
            if (sewer.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL && h.wave < 5) {
                bar.setName(Component.literal("清剿暂停：请在简单或更高难度挑战"));bar.setColor(BossEvent.BossBarColor.YELLOW);continue;
            }
            raid(sewer, data, h, inside, bar);
        }
    }

    private static void raid(ServerLevel sewer, PizzaRatData data, PizzaRatData.Hole h, List<ServerPlayer> inside, ServerBossEvent bar) {
        long now = sewer.getGameTime();
        // 清点还活着的
        h.mobs.removeIf(u -> { Entity en = sewer.getEntity(u); return en != null && !en.isAlive(); });
        for (UUID u : h.mobs) if (sewer.getEntity(u) instanceof Mob m && (m.getTarget() == null || !m.getTarget().isAlive()))
            inside.stream().min(Comparator.comparingDouble(m::distanceToSqr)).ifPresent(m::setTarget);
        if (h.wave >= 5) {
            bar.setName(Component.translatable("rats.sewer.bar_done")); bar.setColor(BossEvent.BossBarColor.GREEN); bar.setProgress(1);
            return;
        }
        if (h.mobs.isEmpty()) {
            if (h.wave == 0 && inside.stream().noneMatch(pl -> Math.abs(pl.getZ() - SewerLayout.HALL2_Z) <= SewerLayout.HALL_R + 6)) {
                bar.setName(Component.literal("前往中段圆厅，准备清剿鼠群"));bar.setProgress(0);return;
            }
            if (h.nextAt == 0) h.nextAt = now + 120;
            if (now < h.nextAt) {
                bar.setName(Component.translatable(h.wave == 0 ? "rats.sewer.bar_coming" : "rats.sewer.bar_rest", h.wave + 1));
                bar.setColor(BossEvent.BossBarColor.YELLOW); bar.setProgress(0);
                return;
            }
            h.nextAt = 0;
            h.wave++;
            SewerLayout L = layout(sewer.getServer(), h);
            if (h.wave <= 3) spawnWave(sewer, h, L, inside);
            else spawnBoss(sewer, h, L);
            data.setDirty();
            for (ServerPlayer pl : inside) pl.sendSystemMessage(Component.translatable(h.wave <= 3 ? "rats.sewer.wave" : "rats.sewer.boss", h.wave).withStyle(ChatFormatting.RED));
            sewer.playSound(null, inside.get(0).blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0F, 1.3F);
            return;
        }
        bar.setName(Component.translatable(h.wave <= 3 ? "rats.sewer.bar_wave" : "rats.sewer.bar_boss", h.wave, h.mobs.size()));
        bar.setColor(h.wave <= 3 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED);
        bar.setProgress(h.total <= 0 ? 0 : Math.max(0, Math.min(1, h.mobs.size() / (float) h.total)));
    }

    /** 在离玩家远的刷怪点刷一波；人多就多刷几只 */
    private static void spawnWave(ServerLevel sewer, PizzaRatData.Hole h, SewerLayout L, List<ServerPlayer> inside) {
        int[] w = WAVES[h.wave - 1];
        int extra = 2 * (inside.size() - 1);
        // Form two ranks on the dry banks in the central rotunda, independent of player position.
        List<BlockPos> points = new ArrayList<>();
        for (int dz : new int[]{-4, 0, 4}) for (int dx : new int[]{-7, 7}) {
            BlockPos point = world(h.id, SewerLayout.MX + dx, SewerLayout.walkY(), SewerLayout.HALL2_Z + dz);
            sewer.getChunkAt(point);
            points.add(point);
        }
        List<Mob> mobs = new ArrayList<>();
        for (int i = 0; i < w[0] + extra; i++) mobs.add(rat(sewer, false));
        for (int i = 0; i < w[1] + extra / 2; i++) mobs.add(rat(sewer, true));
        for (int i = 0; i < w[2]; i++) { Mob m = RatlantisEntityRegistry.PIRAT.get().create(sewer); if (m != null) mobs.add(m); }
        h.mobs.clear();
        int k = 0;
        for (Mob m : mobs) {
            if (m == null) continue;
            BlockPos at = free(sewer, points.get(k++ % points.size()));
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, sewer.random.nextFloat() * 360, 0);
            m.finalizeSpawn(sewer, sewer.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            if (m instanceof Rat r) r.setPlagued(m.getPersistentData().getBoolean("RatsSewerPlague"));
            m.setPersistenceRequired();
            inside.stream().min(Comparator.comparingDouble(m::distanceToSqr)).ifPresent(m::setTarget);
            if (!sewer.addFreshEntity(m)) continue;
            sewer.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.3, m.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
            h.mobs.add(m.getUUID());
        }
        h.total = h.mobs.size();
    }

    private static Mob rat(ServerLevel sewer, boolean plague) {
        Rat r = RatsEntityRegistry.RAT.get().create(sewer);
        if (r == null) return null;
        r.getPersistentData().putBoolean("RatsSewerPlague", plague);
        makeAggressive(r);
        return r;
    }

    /** 下水道里的野鼠：不躲玩家、挨打不逃，主动找最近的玩家咬（重新加载存档后也要再处理一次） */
    static void makeAggressive(Rat r) {
        r.getPersistentData().putBoolean("RatsSewerRaid", true);
        r.goalSelector.removeAllGoals(g -> g instanceof com.github.alexthe666.rats.server.entity.ai.goal.WildRatAvoidPlayerGoal || g instanceof net.minecraft.world.entity.ai.goal.PanicGoal
                || g instanceof net.minecraft.world.entity.ai.goal.AvoidEntityGoal<?>);
        r.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(r, 1.3, true));
        r.targetSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(r, Player.class, false));
    }

    @SubscribeEvent public static void join(net.minecraftforge.event.entity.EntityJoinLevelEvent e) {
        // 新刷出来的在 rat() 里已经处理；这里只管从存档读回来的（目标列表不会存档）
        if (!e.getLevel().isClientSide() && e.loadedFromDisk() && e.getEntity() instanceof Rat r && !(r instanceof RatBoss) && !(r instanceof FoodThief)
                && r.getPersistentData().getBoolean("RatsSewerRaid")) makeAggressive(r);
    }

    /** 刷怪点附近找一个脚下是地、身上两格是空气的位置（避开杂物） */
    private static BlockPos free(ServerLevel lvl, BlockPos p) {
        for (int d = 0; d <= 3; d++) for (int dx = -d; dx <= d; dx++) for (int dz = -d; dz <= d; dz++) {
            BlockPos q = p.offset(dx, 0, dz);
            if (lvl.isEmptyBlock(q) && lvl.isEmptyBlock(q.above()) && lvl.getBlockState(q.below()).isFaceSturdy(lvl, q.below(), Direction.UP)) return q;
        }
        return p;
    }

    private static void spawnBoss(ServerLevel sewer, PizzaRatData.Hole h, SewerLayout L) {
        RatBoss boss = PizzaRatRegistry.RAT_BOSS.get().create(sewer);
        if (boss == null) return;
        BlockPos at = world(h.id, L.boss[0], L.boss[1], L.boss[2]);
        sewer.getChunkAt(at);
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        boss.finalizeSpawn(sewer, sewer.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
        boss.stolen = h.stolen.copy();
        sewer.addFreshEntity(boss);
        sewer.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 0.5, boss.getZ(), 20, 0.8, 0.4, 0.8, 0.02);
        h.mobs.clear(); h.mobs.add(boss.getUUID()); h.total = 1;
    }

    /** 鼠头头死了：这一关清完，出口鼠洞亮起来 */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void death(LivingDeathEvent e) {
        if (e.isCanceled() || !(e.getEntity().level() instanceof ServerLevel lvl) || lvl.dimension() != SEWER) return;
        if (!(e.getEntity() instanceof RatBoss boss)) {
            PizzaRatData raids = PizzaRatData.get(lvl.getServer());
            PizzaRatData.Hole raid = raids.byId(idAt(e.getEntity().getX()));
            if (raid != null && raid.mobs.remove(e.getEntity().getUUID())) raids.setDirty();
            return;
        }
        PizzaRatData data = PizzaRatData.get(lvl.getServer());
        PizzaRatData.Hole h = data.byId(idAt(boss.getX()));
        if (h == null || h.wave >= 5) return;
        h.wave = 5; h.mobs.clear(); data.setDirty();
        SewerLayout L = layout(lvl.getServer(), h);
        BlockPos exit = world(h.id, L.exit[0], L.exit[1], L.exit[2]);
        lvl.setBlock(exit, PizzaRatRegistry.RAT_HOLE.get().defaultBlockState().setValue(RatHoleBlock.LIT, true), 3);
        for (ServerPlayer pl : playersIn(lvl, h.id)) {
            pl.sendSystemMessage(Component.translatable("rats.sewer.cleared").withStyle(ChatFormatting.GREEN));
            pl.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1, 1);
        }
    }

    // ================================================================== 下次开服删掉作废副本的区域文件
    private static Path cleanupFile(MinecraftServer server) { return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("rats_sewer_cleanup.txt"); }

    static void markForCleanup(MinecraftServer server, int id) {
        try {
            Path f = cleanupFile(server);
            Files.createDirectories(f.getParent());
            Files.writeString(f, id + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex) { LogUtils.getLogger().warn("下水道：登记清理失败", ex); }
    }

    @SubscribeEvent public static void aboutToStart(ServerAboutToStartEvent e) {
        Path f = cleanupFile(e.getServer());
        if (!Files.exists(f)) return;
        try {
            Path dim = e.getServer().getWorldPath(LevelResource.ROOT).resolve("dimensions").resolve(RatsMod.MODID).resolve("sewer");
            int removed = 0;
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                int id = Integer.parseInt(line.trim());
                int rx0 = Math.floorDiv(originX(id) - 8, 512), rx1 = Math.floorDiv(originX(id) + SewerLayout.SX + 8, 512);
                for (String sub : new String[]{"region", "entities", "poi"})
                    for (int rx = rx0; rx <= rx1; rx++) for (int rz = -1; rz <= 0; rz++)
                        if (Files.deleteIfExists(dim.resolve(sub).resolve("r." + rx + "." + rz + ".mca"))) removed++;
            }
            Files.delete(f);
            LogUtils.getLogger().info("下水道：删除了 {} 个作废副本的区域文件", removed);
        } catch (Exception ex) { LogUtils.getLogger().warn("下水道：清理作废副本失败", ex); }
    }
}
