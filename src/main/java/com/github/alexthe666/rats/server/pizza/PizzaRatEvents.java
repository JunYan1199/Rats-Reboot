package com.github.alexthe666.rats.server.pizza;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * 纽约披萨鼠：偷食事件。每个玩家每个游戏日最多被偷 1 次（夜里更容易）；布瑞克出餐另有 5% 被截胡。
 * 偷食鼠逃掉 → 60～120 格外的地表出现鼠洞方块，聊天栏给坐标，地上撒一串面包屑指路。
 */
@Mod.EventBusSubscriber(modid = RatsMod.MODID)
public final class PizzaRatEvents {
    private static final ResourceLocation STEAL_ADV = new ResourceLocation(RatsMod.MODID, "pizza_rat_steal");
    private static final DustParticleOptions CRUMB = new DustParticleOptions(new Vector3f(0.86F, 0.62F, 0.28F), 1.1F);
    private static final float NIGHT_CHANCE = 0.025F, DAY_CHANCE = 0.006F;     // 每 10 秒判定一次
    public static final float INTERCEPT_CHANCE = 0.05F;
    private PizzaRatEvents() {}

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var server = e.getServer();
        long t = server.overworld().getGameTime();
        if (t % 40 == 0) trails(server);
        if (t % 200 != 0) return;
        PizzaRatData data = PizzaRatData.get(server);
        long day = server.overworld().getDayTime() / 24000L;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || !p.isAlive() || p.level().dimension().location().getPath().contains("sewer")) continue;
            if (data.lastSteal.getOrDefault(p.getUUID(), -1L) == day) continue;
            float chance = p.level().isNight() ? NIGHT_CHANCE : DAY_CHANCE;
            if (p.getRandom().nextFloat() >= chance) continue;
            if (steal(p)) { data.lastSteal.put(p.getUUID(), day); data.setDirty(); }
        }
    }

    /** 测试用：/ratspizza steal [玩家] 立刻对玩家发动一次偷食（需要 OP 2 级） */
    @SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent e) {
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("ratspizza").requires(s -> s.hasPermission(2))
                .then(net.minecraft.commands.Commands.literal("steal")
                        .executes(c -> stealCmd(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(net.minecraft.commands.Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                .executes(c -> stealCmd(c.getSource(), net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"))))));
    }
    private static int stealCmd(net.minecraft.commands.CommandSourceStack src, ServerPlayer p) {
        boolean ok = steal(p);
        src.sendSuccess(() -> Component.translatable(ok ? "rats.pizza_rat.cmd_ok" : "rats.pizza_rat.cmd_none", p.getDisplayName()), false);
        return ok ? 1 : 0;
    }

    /** 对一个玩家发动一次偷窃；附近没东西可偷就返回 false（今天还能再轮到） */
    public static boolean steal(ServerPlayer p) {
        List<FoodSources.Source> sources = FoodSources.find(p);
        if (sources.isEmpty()) return false;
        var src = sources.get(p.getRandom().nextInt(sources.size()));
        ServerLevel lvl = p.serverLevel();
        FoodThief thief = PizzaRatRegistry.FOOD_THIEF.get().create(lvl);
        if (thief == null) return false;
        Vec3 away = src.at().subtract(p.position());
        if (away.horizontalDistanceSqr() < 1) away = new Vec3(p.getRandom().nextDouble() - 0.5, 0, p.getRandom().nextDouble() - 0.5);
        if (src.ground() != null) {
            // 从玩家看不太到的那一侧溜过来叼地上的食物
            Vec3 spawn = src.at().add(away.normalize().scale(7));
            BlockPos s = lvl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(spawn));
            thief.moveTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, p.getRandom().nextFloat() * 360, 0);
            thief.finalizeSpawn(lvl, lvl.getCurrentDifficultyAt(s), MobSpawnType.EVENT, null, null);
            thief.setup(p.getUUID(), src.at(), away, FoodThief.APPROACH, src.ground().getUUID());
            lvl.addFreshEntity(thief);
            return true;
        }
        ItemStack got = src.steal().get();
        if (got.isEmpty()) return false;
        thief.moveTo(src.at().x, src.at().y, src.at().z, p.getRandom().nextFloat() * 360, 0);
        thief.finalizeSpawn(lvl, lvl.getCurrentDifficultyAt(thief.blockPosition()), MobSpawnType.EVENT, null, null);
        thief.setup(p.getUUID(), src.at(), away, FoodThief.GRAB, null);
        thief.carry(got);
        lvl.addFreshEntity(thief);
        announceSteal(lvl, thief, got);
        return true;
    }

    /** 布瑞克出餐半路被截胡（BrickKitchen 调用）：只有玩家亲手打到偷食鼠才算追回 */
    public static void intercept(TamedRat brick, ItemStack dish) {
        if (!(brick.level() instanceof ServerLevel lvl)) return;
        FoodThief thief = PizzaRatRegistry.FOOD_THIEF.get().create(lvl);
        if (thief == null) { brick.spawnAtLocation(dish); return; }
        double a = brick.getRandom().nextDouble() * Math.PI * 2;
        thief.moveTo(brick.getX() + Math.cos(a) * 1.2, brick.getY(), brick.getZ() + Math.sin(a) * 1.2, (float) Math.toDegrees(a), 0);
        thief.finalizeSpawn(lvl, lvl.getCurrentDifficultyAt(thief.blockPosition()), MobSpawnType.EVENT, null, null);
        thief.setup(brick.getOwnerUUID(), brick.position(), new Vec3(Math.cos(a), 0, Math.sin(a)), FoodThief.GRAB, null);
        thief.intercepted = true;
        thief.carry(dish);
        lvl.addFreshEntity(thief);
        if (brick.getOwner() instanceof ServerPlayer owner)
            owner.sendSystemMessage(Component.translatable("rats.pizza_rat.intercepted", brick.getDisplayName(), dish.getHoverName(), dish.getCount()).withStyle(ChatFormatting.GOLD));
        grant(lvl, thief);
    }

    static void announceSteal(ServerLevel lvl, FoodThief thief, ItemStack stack) {
        for (ServerPlayer p : lvl.players())
            if (p.distanceTo(thief) < 32) p.sendSystemMessage(Component.translatable("rats.pizza_rat.stolen", stack.getHoverName(), stack.getCount()).withStyle(ChatFormatting.GOLD));
        grant(lvl, thief);
    }

    /** 成就「它比你还努力」：附近 32 格的玩家都看到了 */
    private static void grant(ServerLevel lvl, FoodThief thief) {
        var adv = lvl.getServer().getAdvancements().getAdvancement(STEAL_ADV);
        if (adv == null) return;
        for (ServerPlayer p : lvl.players()) if (p.distanceTo(thief) < 32) p.getAdvancements().award(adv, "stolen");
    }

    static void retrieved(Player p, ItemStack stack) {
        p.sendSystemMessage(Component.translatable("rats.pizza_rat.retrieved", stack.getHoverName(), stack.getCount()).withStyle(ChatFormatting.GREEN));
    }

    /** 偷食鼠逃掉：在偷食地点外的地表放鼠洞方块，记下被偷的食物，播报坐标 */
    static void makeHole(ServerLevel lvl, FoodThief thief, UUID victim, Vec3 origin, Vec3 dir, double dist, ItemStack stolen) {
        BlockPos hole = null;
        for (int tryN = 0; tryN < 12 && hole == null; tryN++) {
            double ang = Math.atan2(dir.z, dir.x) + (tryN == 0 ? 0 : (lvl.random.nextDouble() - 0.5) * 1.4);
            double d = dist + (tryN == 0 ? 0 : lvl.random.nextInt(21) - 10);
            int x = (int) Math.floor(origin.x + Math.cos(ang) * d), z = (int) Math.floor(origin.z + Math.sin(ang) * d);
            lvl.getChunk(x >> 4, z >> 4);                                       // 远处的区块先加载
            BlockPos top = lvl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).below();
            BlockState ground = lvl.getBlockState(top);
            if (!ground.getFluidState().isEmpty() || !ground.isSolidRender(lvl, top) || top.getY() <= lvl.getMinBuildHeight() + 2) continue;
            hole = top;
        }
        if (hole == null) hole = BlockPos.containing(thief.position()).below();
        lvl.setBlock(hole, PizzaRatRegistry.RAT_HOLE.get().defaultBlockState(), 3);
        PizzaRatData data = PizzaRatData.get(lvl.getServer());
        PizzaRatData.Hole h = new PizzaRatData.Hole();
        h.pos = hole.immutable(); h.dim = lvl.dimension().location().toString(); h.stolen = stolen.copy(); h.victim = victim; h.from = origin;
        h.created = lvl.getGameTime(); h.id = data.nextId++;
        data.holes.add(h); data.setDirty();
        Player v = victim == null ? null : lvl.getPlayerByUUID(victim);
        Component where = Component.translatable("rats.pizza_rat.hole_at", hole.getX(), hole.getY() + 1, hole.getZ(), (int) Math.sqrt(hole.distToCenterSqr(origin)), direction(origin, hole)).withStyle(ChatFormatting.YELLOW);
        for (ServerPlayer p : lvl.players())
            if (p == v || p.distanceTo(thief) < 48) p.sendSystemMessage(Component.translatable("rats.pizza_rat.escaped", stolen.getHoverName(), stolen.getCount()).withStyle(ChatFormatting.GOLD).append(" ").append(where));
    }

    private static Component direction(Vec3 from, BlockPos to) {
        double dx = to.getX() + 0.5 - from.x, dz = to.getZ() + 0.5 - from.z;
        int sector = (int) Math.floorMod(Math.round(Math.toDegrees(Math.atan2(dx, -dz)) / 45.0), 8);   // 0 = 北
        return Component.translatable("rats.pizza_rat.dir." + sector);
    }

    /** 面包屑：从偷食地点到鼠洞，每 3 格一撮，只发给附近 24 格内的玩家 */
    private static void trails(net.minecraft.server.MinecraftServer server) {
        PizzaRatData data = PizzaRatData.get(server);
        if (data.holes.isEmpty()) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String dim = p.level().dimension().location().toString();
            for (PizzaRatData.Hole h : data.holes) {
                if (!h.dim.equals(dim)) continue;
                Vec3 end = Vec3.atCenterOf(h.pos);
                double len = h.from.distanceTo(end);
                if (len < 1) continue;
                Vec3 step = end.subtract(h.from).scale(1 / len);
                for (double s = 0; s <= len; s += 3) {
                    Vec3 q = h.from.add(step.scale(s));
                    if (q.distanceToSqr(p.getX(), q.y, p.getZ()) > 24 * 24) continue;
                    BlockPos g = p.serverLevel().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(q));
                    p.serverLevel().sendParticles(p, CRUMB, false, q.x, g.getY() + 0.15, q.z, 3, 0.15, 0.02, 0.15, 0);
                }
                // 鼠洞本身冒点烟
                if (p.distanceToSqr(end) < 48 * 48) p.serverLevel().sendParticles(p, net.minecraft.core.particles.ParticleTypes.SMOKE, false, end.x, end.y + 0.6, end.z, 4, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }
}
