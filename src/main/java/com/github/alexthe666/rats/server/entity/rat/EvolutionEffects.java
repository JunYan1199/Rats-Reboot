package com.github.alexthe666.rats.server.entity.rat;

import net.minecraft.core.particles.DustParticleOptions;
import com.github.alexthe666.rats.server.message.EvolutionEffectPacket;
import com.github.alexthe666.rats.server.message.EvolutionEffectPacket.Kind;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

/** Server-side presentation of successful mastery activations. No damaging spell entities are spawned. */
final class EvolutionEffects {
    private static final boolean IRON = ModList.get().isLoaded("irons_spellbooks");
    private static final net.minecraft.resources.ResourceLocation ELECTRIC = new net.minecraft.resources.ResourceLocation("goety","electric");
    private static final Map<TamedRat, Long> nextMark = new WeakHashMap<>();

    private EvolutionEffects() {}

    static int color(TamedRat rat) {
        return switch (rat.getEvolution()) {
            case PLAIN -> 0xE0BD83;
            case EMBER -> 0xFF6230;
            case TIDE -> 0x28A9FF;
            case VOLT -> 0x76DAFF;
            case LEAF -> 0x70D954;
            case FROST -> 0xB8F6FF;
            case BRAWL -> 0xE8584F;
            case VENOM -> 0xAD5AF4;
            case DUNE -> 0xE8AE52;
            case GALE -> 0x86F3CF;
            case SEER -> 0xF696DF;
            case MOTH -> 0xC2E558;
            case CRAG -> 0xC48660;
            case WISP -> 0x867BFF;
            case DRAKE -> 0x526BFF;
            case LUNAR -> 0x7953AE;
            case ALLOY -> 0xAABFD1;
            case RIBBON -> 0xFF8EBB;
            case SPARK -> rat.isSparkCharged() ? 0xFFE451 : 0xEFAB39;
            case NONE -> 0xFFFFFF;
        };
    }

    private static Vector3f rgb(int color) {
        return new Vector3f((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
    }

    static Vec3 center(LivingEntity entity) {
        return entity.getBoundingBox().getCenter();
    }

    private static void motes(ServerLevel world, Vec3 at, int tint, int count, double spread) {
        ParticleOptions particle = IRON ? Iron.spark(tint) : new DustParticleOptions(rgb(tint), .85f);
        world.sendParticles(particle, at.x, at.y, at.z, count, spread, spread * .65, spread, .035);
    }

    private static void show(ServerLevel world, TamedRat rat, Kind kind, Vec3 from, Vec3 to, float radius) {
        long seed=world.getGameTime()*31L + rat.getId()*13L + Double.doubleToLongBits(to.x);
        new EvolutionEffectPacket(kind,rat.getEvolution(),from,to,color(rat),radius,rat.isSparkCharged(),seed).send(world);
    }

    static void burst(TamedRat rat, LivingEntity target) {
        burst(rat, center(target));
    }

    static void burst(TamedRat rat, Vec3 at) {
        if (rat.level() instanceof ServerLevel world) {
            show(world,rat,Kind.BURST,at,at,.55f);
            motes(world, at, color(rat), 8, .24);
        }
    }

    static void mark(TamedRat rat, LivingEntity target) {
        // Multi-hit spells can add several stacks in a single tick.
        long now = rat.level().getGameTime();
        if (now < nextMark.getOrDefault(rat, 0L)) return;
        nextMark.put(rat, now + 6);
        burst(rat, target);
    }

    static void ward(TamedRat rat, LivingEntity target) {
        ring(rat, target.position().add(0, .12, 0), Math.max(.55f, Math.min(1.25f, target.getBbWidth())));
    }

    static void ring(TamedRat rat, Vec3 at, float radius) {
        if (!(rat.level() instanceof ServerLevel world)) return;
        int tint = color(rat);
        show(world,rat,Kind.WARD,at,at,radius);
        if (IRON) world.sendParticles(Iron.wave(tint, radius), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        else for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            motes(world, at.add(radius * Math.cos(angle), 0, radius * Math.sin(angle)), tint, 1, .015);
        }
        motes(world, at.add(0, .18, 0), tint, 5, .22);
    }

    static void stream(TamedRat rat, LivingEntity from, LivingEntity to) {
        stream(rat, center(from), center(to));
    }

    static void stream(TamedRat rat, Vec3 from, Vec3 to) {
        if (!(rat.level() instanceof ServerLevel world)) return;
        int tint = color(rat);
        show(world,rat,Kind.STREAM,from,to,.55f);
        if (from.distanceToSqr(to) > .16) {
            if (IRON) world.sendParticles(Iron.trace(tint, to), from.x, from.y, from.z, 1, 0, 0, 0, 0);
            else dotted(world, from, to, tint);
        }
        motes(world, to, tint, 6, .15);
    }

    static void arc(TamedRat rat, LivingEntity from, LivingEntity to) {
        if (!(rat.level() instanceof ServerLevel world)) return;
        Vec3 a = center(from), b = center(to);
        int tint = color(rat);
        boolean full = rat.isSparkCharged();
        if (a.distanceToSqr(b) < .04) { burst(rat, to); return; }
        show(world,rat,Kind.ARC,a,b,full?1f:.55f);
        if (full && net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getValue(ELECTRIC) instanceof net.minecraft.core.particles.SimpleParticleType sparks)
            world.sendParticles(sparks,b.x,b.y,b.z,3,.15,.10,.15,.04);
        motes(world, b, tint, full ? 14 : 6, full ? .32 : .14);
        if (full) motes(world, b, 0xFFF8BD, 5, .13);
    }

    private static void dotted(ServerLevel world, Vec3 from, Vec3 to, int tint) {
        int steps = Math.max(2, Math.min(12, (int)Math.ceil(from.distanceTo(to) * 3)));
        for (int i = 0; i <= steps; i++) motes(world, from.lerp(to, i / (double)steps), tint, 1, .015);
    }

    private static final class Iron {
        static ParticleOptions spark(int tint) {
            return new io.redspace.ironsspellbooks.particle.SparkParticleOptions(rgb(tint));
        }
        static ParticleOptions wave(int tint, float radius) {
            return new io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions(rgb(tint), radius);
        }
        static ParticleOptions trace(int tint, Vec3 end) {
            return new io.redspace.ironsspellbooks.particle.TraceParticleOptions(end.toVector3f(), rgb(tint));
        }
    }

}
