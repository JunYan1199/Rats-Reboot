package com.github.alexthe666.rats.client.render.entity.layer;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.client.model.entity.RatModel;
import com.github.alexthe666.rats.server.entity.rat.Rat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import com.github.alexthe666.rats.server.misc.RatColorUtil;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;

public class RatOverlayLayer extends RenderLayer<Rat, RatModel<Rat>> {

	private static final RenderType DYE_TEX = RenderType.entityNoOutline(new ResourceLocation(RatsMod.MODID, "textures/entity/rat/dyed_part.png"));
	private static final RenderType PLAGUE_TEX = RenderType.entityNoOutline(new ResourceLocation(RatsMod.MODID, "textures/entity/rat/plague_overlay.png"));
	private static final RenderType TOGA_TEX = RenderType.entitySmoothCutout(new ResourceLocation(RatsMod.MODID, "textures/entity/rat/upgrades/toga.png"));

	public RatOverlayLayer(RenderLayerParent<Rat, RatModel<Rat>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack stack, MultiBufferSource buffer, int light, Rat rat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		if (rat.isDyed()) {
			float[] color = RatColorUtil.getDyeRgb(DyeColor.byId(rat.getDyeColor()));
			this.getParentModel().renderToBuffer(stack, buffer.getBuffer(DYE_TEX), light,
				LivingEntityRenderer.getOverlayCoords(rat, 0), color[0], color[1], color[2], 1.0F);
		}
		if (rat.hasPlague()) {
			VertexConsumer consumer = buffer.getBuffer(PLAGUE_TEX);
			this.getParentModel().renderToBuffer(stack, consumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		}
		if (rat.hasToga()) {
			VertexConsumer consumer = buffer.getBuffer(TOGA_TEX);
			this.getParentModel().renderToBuffer(stack, consumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		}
	}
}
