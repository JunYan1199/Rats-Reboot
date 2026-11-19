package com.github.alexthe666.rats.client.render;

import com.github.alexthe666.rats.registry.RatVariantRegistry;
import com.github.alexthe666.rats.server.items.RatSliceItem;
import com.github.alexthe666.rats.server.misc.RatVariant;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 鼠片的物品渲染：一只被压扁、四肢摊开的小老鼠（静止模型，无动画）。
 * 方块组件沿用老鼠贴图的 UV 布局，所以耳朵、尾巴和原鼠的毛色都会保留；整体在竖直方向压扁成“片”。
 */
public class RatSliceItemRenderer extends BlockEntityWithoutLevelRenderer {
	private static RatSliceItemRenderer instance;
	private final ModelPart root;

	public static RatSliceItemRenderer get() {
		if (instance == null) instance = new RatSliceItemRenderer();
		return instance;
	}

	private RatSliceItemRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
		this.root = createModel().bakeRoot();
	}

	public static LayerDefinition createModel() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body1 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -3.0F, 0.0F, 5, 5, 6), PartPose.offset(0.0F, 20.0F, 0.0F));
		PartDefinition body2 = body1.addOrReplaceChild("lower_body", CubeListBuilder.create().texOffs(22, 0).addBox(-2.0F, -2.0F, -5.0F, 4, 4, 5), PartPose.offset(0.0F, -0.4F, 0.5F));
		PartDefinition neck = body2.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(32, 17).addBox(-1.0F, -1.0F, -1.0F, 2, 2, 1), PartPose.offset(0.0F, 0.0F, -5.0F));
		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(22, 20).addBox(-2.0F, -1.5F, -2.5F, 4, 3, 3), PartPose.offset(0.0F, 0.0F, -1.0F));
		PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(22, 26).addBox(-1.5F, -1.0F, -2.0F, 3, 2, 2), PartPose.offset(0.0F, 0.5F, -2.5F));
		snout.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(32, 26).addBox(-0.5F, -0.3F, -1.0F, 1, 1, 2), PartPose.offset(0.0F, -0.6F, -2.1F));
		snout.addOrReplaceChild("left_whisker", CubeListBuilder.create().texOffs(40, 0).addBox(0.5F, -1.5F, 0.0F, 2, 3, 0), PartPose.offsetAndRotation(1.0F, 0.0F, -1.0F, 0.0F, 0.0F, -1.2F));
		snout.addOrReplaceChild("right_whisker", CubeListBuilder.create().texOffs(35, 0).addBox(-2.5F, -1.5F, 0.0F, 2, 3, 0), PartPose.offsetAndRotation(-1.0F, 0.0F, -1.0F, 0.0F, 0.0F, 1.2F));
		head.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(37, 20).addBox(-0.5F, -0.7F, -0.5F, 1, 1, 1), PartPose.offset(2.0F, -0.5F, -1.6F));
		head.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(37, 20).addBox(-0.5F, -0.7F, -0.5F, 1, 1, 1), PartPose.offset(-2.0F, -0.5F, -1.6F));
		// 耳朵向两侧摊开，压扁后从上方也能看见
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(33, 20).mirror().addBox(0.0F, -2.0F, 0.0F, 2, 2, 0), PartPose.offsetAndRotation(1.5F, -0.5F, 0.0F, -0.5F, -0.6F, 0.9F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(33, 20).addBox(-2.0F, -2.0F, 0.0F, 2, 2, 0), PartPose.offsetAndRotation(-1.5F, -0.5F, 0.0F, -0.5F, 0.6F, -0.9F));
		// 前爪向前外侧摊开
		PartDefinition leftArm = body2.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 9).mirror().addBox(0.0F, 0.0F, -1.0F, 1, 2, 2), PartPose.offsetAndRotation(2.0F, 1.5F, -3.0F, -0.7F, 0.0F, -1.35F));
		leftArm.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(32, 13).mirror().addBox(-0.5F, 0.0F, -1.5F, 1, 1, 2), PartPose.offset(0.4F, 2.0F, -0.3F));
		PartDefinition rightArm = body2.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 9).addBox(-1.0F, 0.0F, -1.0F, 1, 2, 2), PartPose.offsetAndRotation(-2.0F, 1.5F, -3.0F, -0.7F, 0.0F, 1.35F));
		rightArm.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(32, 13).addBox(-0.5F, 0.0F, -1.5F, 1, 1, 2), PartPose.offset(-0.4F, 2.0F, -0.3F));
		// 后腿向后外侧摊开
		PartDefinition leftThigh = body1.addOrReplaceChild("left_thigh", CubeListBuilder.create().texOffs(22, 9).mirror().addBox(-1.0F, 0.0F, -1.5F, 2, 3, 3), PartPose.offsetAndRotation(2.5F, 0.0F, 5.0F, 0.6F, 0.0F, -1.35F));
		leftThigh.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(22, 15).mirror().addBox(-0.5F, 0.0F, -3.5F, 1, 1, 4), PartPose.offsetAndRotation(0.0F, 3.0F, 0.5F, 0.0F, 0.0F, 0.0F));
		PartDefinition rightThigh = body1.addOrReplaceChild("right_thigh", CubeListBuilder.create().texOffs(22, 9).addBox(-1.0F, 0.0F, -1.5F, 2, 3, 3), PartPose.offsetAndRotation(-2.5F, 0.0F, 5.0F, 0.6F, 0.0F, 1.35F));
		rightThigh.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(22, 15).addBox(-0.5F, 0.0F, -3.5F, 1, 1, 4), PartPose.offset(0.0F, 3.0F, 0.5F));
		// 尾巴：平铺在身后，带一点 S 形弯
		PartDefinition tail1 = body1.addOrReplaceChild("upper_tail", CubeListBuilder.create().texOffs(0, 11).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 8), PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, 0.0F, 0.35F, 0.0F));
		tail1.addOrReplaceChild("lower_tail", CubeListBuilder.create().texOffs(0, 11).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 8), PartPose.offsetAndRotation(0.0F, 0.0F, 8.0F, 0.0F, -0.8F, 0.0F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		RatVariant variant = RatSliceItem.variantOrNull(stack);
		ResourceLocation texture = (variant != null ? variant : RatVariantRegistry.BLUE.get()).getTexture();
		pose.pushPose();
		pose.translate(0.5F, 0.42F, 0.5F);
		pose.scale(0.55F, 0.55F * 0.32F, 0.55F);   // 竖直方向压扁成“片”
		pose.scale(-1.0F, -1.0F, 1.0F);
		pose.translate(0.0F, -1.25F, -0.32F);
		this.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}
}
