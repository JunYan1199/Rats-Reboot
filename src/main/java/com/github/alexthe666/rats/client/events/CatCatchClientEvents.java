package com.github.alexthe666.rats.client.events;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.render.entity.layer.CatSatchelLayer;
import com.github.alexthe666.rats.registry.RatsMenuRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** 猫捕鼠的客户端注册：挎包界面、猫身上的挎包外观层。 */
@Mod.EventBusSubscriber(modid = RatsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CatCatchClientEvents {

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> MenuScreens.<ChestMenu, ContainerScreen>register(RatsMenuRegistry.CAT_SATCHEL_CONTAINER.get(), ContainerScreen::new));
	}

	@SubscribeEvent
	public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
		event.register(RatSpinController.SPIN_KEY);
	}

	@SubscribeEvent
	public static void reloadListeners(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
		event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> HeldRatSliceHandEvents.clearTextures());
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> {
            com.github.alexthe666.rats.client.model.entity.SingingRatModel.reload(rm);
            SingingRatMusic.reset();
        });
	}

	@SubscribeEvent
	public static void addLayers(EntityRenderersEvent.AddLayers event) {
		try {
			EntityRenderer<?> renderer = event.getRenderer(EntityType.CAT);
			if (renderer instanceof LivingEntityRenderer<?, ?> living) {
				CatSatchelLayer.attach(living);
			}
		} catch (Throwable t) {
			RatsMod.LOGGER.warn("Rats: could not attach cat satchel layer", t);
		}
	}
}
