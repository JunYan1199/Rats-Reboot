package com.github.alexthe666.rats.client.render.entity.layer;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.catcatch.CatSatchel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

/**
 * 猫咪捕鼠挎包外观：一条绕过猫背的皮质背带 + 挂在右侧的小挎包（带翻盖和奶酪色搭扣）。
 * 跟随猫的身体部件（站立、坐下、趴下都贴合）。
 */
public class CatSatchelLayer extends RenderLayer<Cat, EntityModel<Cat>> {
	public static final ResourceLocation TEXTURE = new ResourceLocation(RatsMod.MODID, "textures/entity/cat_rat_satchel.png");
	private final ModelPart satchel;

	public CatSatchelLayer(RenderLayerParent<Cat, EntityModel<Cat>> parent) {
		super(parent);
		this.satchel = createModel().bakeRoot();
	}

	/** 坐标系为猫的 body 部件本地坐标：x 左右，y 沿身体从前到后（3~19），z 为腹到背（-8 腹，-2 背）。 */
	public static LayerDefinition createModel() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// 背带：一圈套在身体中段
		root.addOrReplaceChild("strap", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 9.0F, -8.0F, 4, 2, 6, new CubeDeformation(0.25F)), PartPose.ZERO);
		// 包身：挂在右侧，从背线往下垂
		root.addOrReplaceChild("bag", CubeListBuilder.create().texOffs(0, 8).addBox(2.3F, 7.5F, -7.0F, 2, 5, 4), PartPose.ZERO);
		// 翻盖：盖住包身上半部（靠背的一侧）
		root.addOrReplaceChild("flap", CubeListBuilder.create().texOffs(14, 8).addBox(2.3F, 7.5F, -4.2F, 2, 5, 2, new CubeDeformation(0.2F)), PartPose.ZERO);
		// 奶酪色搭扣：在翻盖下沿外侧
		root.addOrReplaceChild("buckle", CubeListBuilder.create().texOffs(0, 18).addBox(4.3F, 9.5F, -4.8F, 1, 1, 1), PartPose.ZERO);
		// 左侧小口袋，左右更平衡
		root.addOrReplaceChild("pouch", CubeListBuilder.create().texOffs(14, 16).addBox(-3.6F, 8.5F, -6.5F, 1, 3, 3), PartPose.ZERO);
		return LayerDefinition.create(mesh, 32, 32);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void attach(LivingEntityRenderer<?, ?> renderer) {
		LivingEntityRenderer raw = renderer;
		raw.addLayer(new CatSatchelLayer((RenderLayerParent<Cat, EntityModel<Cat>>) raw));
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int light, Cat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		if (cat.isInvisible() || cat.isBaby()) return;
		CatSatchel s = CatSatchel.get(cat);
		if (s == null || !s.isEquipped()) return;
		if (!(this.getParentModel() instanceof OcelotModel<?> model)) return;
		pose.pushPose();
		model.body.translateAndRotate(pose);
		this.satchel.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}
}
