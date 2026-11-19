package com.github.alexthe666.rats.client.render.entity;

import com.github.alexthe666.rats.client.model.entity.RatModel;
import com.github.alexthe666.rats.client.render.entity.layer.RatEyesLayer;
import com.github.alexthe666.rats.server.pizza.FoodThief;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;

/** 偷食鼠：原版老鼠模型；嘴里叼着放大的被偷食物，平放着拖在身体左后方（同预览） */
public class FoodThiefRenderer extends AbstractRatRenderer<FoodThief, RatModel<FoodThief>> {
    public FoodThiefRenderer(EntityRendererProvider.Context context) {
        super(context, new RatModel<>());
        addLayer(new RatEyesLayer<>(this));
        addLayer(new RenderLayer<FoodThief, RatModel<FoodThief>>(this) {
            @Override public void render(PoseStack p, MultiBufferSource buffers, int light, FoodThief rat, float limb, float speed, float partial, float age, float yaw, float pitch) {
                var stack = rat.carried();
                if (stack.isEmpty()) return;
                p.pushPose();
                getParentModel().translateToHead(p);
                p.translate(0, (0.5F + 1.1F) / 16F, (-2.5F - 1.4F) / 16F);   // 嘴边（模型坐标 Y 向下、-Z 朝前）
                p.mulPose(Axis.XP.rotation(-0.19F));                         // 抵消原版低头
                p.mulPose(Axis.YP.rotationDegrees(-140));                    // 往左后方拖
                p.mulPose(Axis.ZP.rotationDegrees(12));                      // 远端贴地
                p.mulPose(Axis.XP.rotationDegrees(90));                      // 平放
                float s = 1.45F;
                p.translate(0.5F * s, 0, 0);                                 // 叼住的是食物的一角
                p.scale(s, s, s);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, LivingEntityRenderer.getOverlayCoords(rat, 0), p, buffers, rat.level(), rat.getId());
                p.popPose();
            }
        });
    }
}
