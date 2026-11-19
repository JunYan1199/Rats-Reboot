package com.github.alexthe666.rats.client.events;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.registry.RatsSoundRegistry;
import com.github.alexthe666.rats.server.items.RatSliceItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 「转鼠鼠」：主手拿着鼠片时按快捷键（默认 J），老鼠像转笔一样在手上打转，同时循环播放 BGM；
 * 再按一次或换掉手里的物品就停。只在本地客户端，不发包，别人看不到也听不到。
 */
@Mod.EventBusSubscriber(modid = RatsMod.MODID, value = Dist.CLIENT)
public class RatSpinController {
	public static final KeyMapping SPIN_KEY = new KeyMapping("key.rats.spin_rat_slice", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.rats");

	private static boolean spinning;
	private static int spinTicks;
	private static SpinBgm bgm;

	public static boolean spinning() {
		return spinning;
	}

	/** 已经转了多少刻（含插值），渲染用 */
	public static float spinTime(float partialTick) {
		return spinTicks + partialTick;
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		boolean holding = mc.player != null && mc.player.getMainHandItem().getItem() instanceof RatSliceItem;
		while (SPIN_KEY.consumeClick()) {
			if (spinning) stop();
			else if (holding) start();
		}
		if (spinning) {
			if (!holding || mc.level == null) stop();
			else spinTicks++;
		}
	}

	public static void start() {
		spinning = true;
		spinTicks = 0;
		try {
			bgm = new SpinBgm();
			Minecraft.getInstance().getSoundManager().play(bgm);
		} catch (Throwable t) {
			// 没放 BGM 文件也照样转
			bgm = null;
		}
	}

	public static void stop() {
		spinning = false;
		spinTicks = 0;
		if (bgm != null) {
			Minecraft.getInstance().getSoundManager().stop(bgm);
			bgm = null;
		}
	}

	/** 循环播放、跟随玩家（不衰减），转停了就自动停止；音量归「唱片机/音符盒」。 */
	static final class SpinBgm extends AbstractTickableSoundInstance {
		SpinBgm() {
			super(RatsSoundRegistry.RAT_SPIN_BGM.get(), SoundSource.RECORDS, RandomSource.create());
			this.looping = true;
			this.delay = 0;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.volume = 1.0F;
		}

		@Override
		public void tick() {
			if (!spinning) this.stop();
		}
	}
}
