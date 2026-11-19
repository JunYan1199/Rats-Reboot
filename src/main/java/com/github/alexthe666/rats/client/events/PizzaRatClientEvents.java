package com.github.alexthe666.rats.client.events;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.render.entity.RatBossRenderer;
import com.github.alexthe666.rats.server.pizza.PizzaRatRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 纽约披萨鼠的客户端注册：鼠头头渲染器、披萨片投射物渲染器 */
@Mod.EventBusSubscriber(modid = RatsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PizzaRatClientEvents {
    private PizzaRatClientEvents() {}

    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(PizzaRatRegistry.RAT_BOSS.get(), RatBossRenderer::new);
        e.registerEntityRenderer(PizzaRatRegistry.PIZZA_SLICE_SHOT.get(), ThrownItemRenderer::new);
        e.registerEntityRenderer(PizzaRatRegistry.FOOD_THIEF.get(), com.github.alexthe666.rats.client.render.entity.FoodThiefRenderer::new);
    }
}
