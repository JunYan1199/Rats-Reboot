package com.github.alexthe666.rats.client.events;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsSoundRegistry;
import com.github.alexthe666.rats.server.entity.rat.AbstractRat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = RatsMod.MODID, value = Dist.CLIENT)
public class SingingRatMusic {
    private static final Map<AbstractRat, Voice> voices = new IdentityHashMap<>();

    public static double playbackTicks(AbstractRat rat, float partialTick) {
        Voice voice = voices.get(rat);
        long since = voice != null ? voice.started : rat.songStarted();
        return Math.max(0, rat.level().getGameTime() - since + partialTick);
    }

    @SubscribeEvent
    public static void onRatTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AbstractRat rat) || !rat.level().isClientSide || !rat.isSinging() || rat.isSilent()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != rat.level() || mc.getCameraEntity() == null || mc.getCameraEntity().distanceToSqr(rat) > 16 * 16) return;
        Voice existing = voices.get(rat);
        if (existing != null && existing.epoch == rat.songStarted()) return;
        if (existing != null) mc.getSoundManager().stop(existing);
        Voice voice = new Voice(rat);
        voices.put(rat, voice);
        mc.getSoundManager().play(voice);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || voices.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        voices.values().removeIf(voice -> {
            if (voice.valid(mc)) return false;
            mc.getSoundManager().stop(voice);
            return true;
        });
    }

    public static void reset() {
        var sounds = Minecraft.getInstance().getSoundManager();
        voices.values().forEach(sounds::stop);
        voices.clear();
    }

    private static final class Voice extends AbstractTickableSoundInstance {
        final AbstractRat rat;
        final long epoch, started;

        Voice(AbstractRat rat) {
            super(RatsSoundRegistry.SINGING_RAT.get(), SoundSource.RECORDS, RandomSource.create());
            this.rat = rat;
            epoch = rat.songStarted();
            started = rat.level().getGameTime();
            looping = true;
            volume = 1.0F;
            updatePosition();
        }

        private boolean valid(Minecraft mc) {
            return rat.level() == mc.level && rat.isAlive() && !rat.isRemoved() && !rat.isSilent() && epoch == rat.songStarted()
                    && mc.getCameraEntity() != null && mc.getCameraEntity().distanceToSqr(rat) < 18 * 18;
        }

        private void updatePosition() {
            x = rat.getX();
            y = rat.getY() + rat.getBbHeight() * .6;
            z = rat.getZ();
        }

        @Override public boolean canStartSilent() { return true; }

        @Override public void tick() {
            if (valid(Minecraft.getInstance())) updatePosition();
            else stop();
        }
    }
}
