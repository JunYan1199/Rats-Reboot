package com.github.alexthe666.rats.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * 手持鼠片时显示的「被握住脖子吊着」的老鼠：与老鼠实体相同的方块和贴图 UV，只改姿势；
 * 被握住时眼睛放大一圈（只在这个模型里，不改老鼠贴图和实体）。
 */
public final class HeldRatSliceModel {
	private static ModelPart root;
	private static Vector3f grip;

	private HeldRatSliceModel() {
	}

	private static float d(float deg) {
		return deg * Mth.DEG_TO_RAD;
	}

	public static LayerDefinition create() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition r = mesh.getRoot();
		// 躯干竖直：头朝上、屁股朝下
		PartDefinition body1 = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -3.0F, 0.0F, 5, 5, 6), PartPose.offsetAndRotation(0, 0, 0, d(-88), 0, 0));
		PartDefinition body2 = body1.addOrReplaceChild("lower_body", CubeListBuilder.create().texOffs(22, 0).addBox(-2.0F, -2.0F, -5.0F, 4, 4, 5), PartPose.offsetAndRotation(0, -0.4F, 0.5F, d(-6), 0, 0));
		PartDefinition neck = body2.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(32, 17).addBox(-1.0F, -1.0F, -1.0F, 2, 2, 1), PartPose.offsetAndRotation(0, 0, -5, d(48), 0, 0));
		// 脸转向正前方、略微低头
		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(22, 20).addBox(-2.0F, -1.5F, -2.5F, 4, 3, 3), PartPose.offsetAndRotation(0, 0, -1, d(62), 0, 0));
		PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(22, 26).addBox(-1.5F, -1.0F, -2.0F, 3, 2, 2), PartPose.offset(0, 0.5F, -2.5F));
		snout.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(32, 26).addBox(-0.5F, -0.3F, -1.0F, 1, 1, 2), PartPose.offset(0, -0.6F, -2.1F));
		snout.addOrReplaceChild("left_whisker", CubeListBuilder.create().texOffs(40, 0).addBox(0.5F, -1.5F, 0.0F, 2, 3, 0), PartPose.offset(1, 0, -1));
		snout.addOrReplaceChild("right_whisker", CubeListBuilder.create().texOffs(35, 0).addBox(-2.5F, -1.5F, 0.0F, 2, 3, 0), PartPose.offset(-1, 0, -1));
		// 被握住脖子：眼睛往外凸
		head.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(37, 20).addBox(-0.5F, -0.7F, -0.5F, 1, 1, 1), PartPose.offset(2.25F, -0.5F, -1.75F));
		head.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(37, 20).addBox(-0.5F, -0.7F, -0.5F, 1, 1, 1), PartPose.offset(-2.25F, -0.5F, -1.75F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(33, 20).mirror().addBox(0.0F, -2.0F, 0.0F, 2, 2, 0), PartPose.offsetAndRotation(1.5F, -0.5F, 0, 0.2F, -0.6F, 0.15F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(33, 20).addBox(-2.0F, -2.0F, 0.0F, 2, 2, 0), PartPose.offsetAndRotation(-1.5F, -0.5F, 0, 0.2F, 0.6F, -0.15F));
		// 前爪耷拉
		PartDefinition la = body2.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 9).mirror().addBox(0.0F, 0.0F, -1.0F, 1, 2, 2), PartPose.offsetAndRotation(2, 1.5F, -3, d(78), 0, d(-6)));
		la.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(32, 13).mirror().addBox(-0.5F, 0.0F, -1.5F, 1, 1, 2), PartPose.offsetAndRotation(0.4F, 2, -0.3F, d(30), 0, 0));
		PartDefinition ra = body2.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 9).addBox(-1.0F, 0.0F, -1.0F, 1, 2, 2), PartPose.offsetAndRotation(-2, 1.5F, -3, d(78), 0, d(6)));
		ra.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(32, 13).addBox(-0.5F, 0.0F, -1.5F, 1, 1, 2), PartPose.offsetAndRotation(-0.4F, 2, -0.3F, d(30), 0, 0));
		// 后腿和尾巴下垂
		PartDefinition lt = body1.addOrReplaceChild("left_thigh", CubeListBuilder.create().texOffs(22, 9).mirror().addBox(-1.0F, 0.0F, -1.5F, 2, 3, 3), PartPose.offsetAndRotation(2.5F, 0, 5, d(80), 0, d(-12)));
		lt.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(22, 15).mirror().addBox(-0.5F, 0.0F, -3.5F, 1, 1, 4), PartPose.offsetAndRotation(0, 3, 0.5F, d(-15), 0, 0));
		PartDefinition rt = body1.addOrReplaceChild("right_thigh", CubeListBuilder.create().texOffs(22, 9).addBox(-1.0F, 0.0F, -1.5F, 2, 3, 3), PartPose.offsetAndRotation(-2.5F, 0, 5, d(80), 0, d(12)));
		rt.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(22, 15).addBox(-0.5F, 0.0F, -3.5F, 1, 1, 4), PartPose.offsetAndRotation(0, 3, 0.5F, d(-15), 0, 0));
		PartDefinition t1 = body1.addOrReplaceChild("upper_tail", CubeListBuilder.create().texOffs(0, 11).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 8), PartPose.offsetAndRotation(0, 0, 6, d(12), d(6), 0));
		t1.addOrReplaceChild("lower_tail", CubeListBuilder.create().texOffs(0, 11).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 8), PartPose.offsetAndRotation(0, 0, 8, d(-8), d(-14), 0));
		return LayerDefinition.create(mesh, 64, 32);
	}

	public static ModelPart root() {
		if (root == null) {
			root = create().bakeRoot();
			ModelPart head = root.getChild("body").getChild("lower_body").getChild("neck").getChild("head");
			for (String eye : new String[]{"left_eye", "right_eye"}) {
				ModelPart e = head.getChild(eye);
				e.xScale = e.yScale = e.zScale = 1.35F;
			}
		}
		return root;
	}

	private static Vector3f center;

	/** 身体中点（转鼠鼠时的旋转中心）在模型坐标中的位置（单位：格）。 */
	public static Vector3f center() {
		if (center == null) {
			PoseStack p = new PoseStack();
			root().getChild("body").translateAndRotate(p);
			Vector4f v = p.last().pose().transform(new Vector4f(0, -0.5F / 16.0F, 1.0F / 16.0F, 1));
			center = new Vector3f(v.x(), v.y(), v.z());
		}
		return center;
	}

	/** 握点（脖子正中）在模型坐标中的位置（单位：格）。 */
	public static Vector3f grip() {
		if (grip == null) {
			ModelPart body = root().getChild("body");
			ModelPart lower = body.getChild("lower_body");
			ModelPart neck = lower.getChild("neck");
			PoseStack p = new PoseStack();
			body.translateAndRotate(p);
			lower.translateAndRotate(p);
			neck.translateAndRotate(p);
			Vector4f v = p.last().pose().transform(new Vector4f(0, 0, -0.5F / 16.0F, 1));
			grip = new Vector3f(v.x(), v.y(), v.z());
		}
		return grip;
	}
}
