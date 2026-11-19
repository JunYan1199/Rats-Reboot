package com.github.alexthe666.rats.client.model.hats;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class SpellHatModel extends AbstractHatModel {
    public SpellHatModel(ModelPart root) { super(root); }

    private static MeshDefinition hatMesh() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return mesh;
    }

    public static LayerDefinition mage() {
        MeshDefinition mesh = hatMesh();
        mesh.getRoot().getChild("head").addOrReplaceChild("crown", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-5.5F, -8F, -5.5F, 11F, 1F, 11F)
            .texOffs(0, 14).addBox(-5F, -8.45F, -5F, 10F, 1F, 10F)
            .texOffs(0, 26).addBox(-3.5F, -11.4F, -3.5F, 7F, 3F, 7F)
            .texOffs(34, 29).addBox(-3.65F, -10F, -3.65F, 7.3F, 1.4F, 7.3F)
            .texOffs(0, 39).addBox(-2.5F, -14.3F, -2.5F, 5F, 3F, 5F)
            .texOffs(24, 40).addBox(-1.5F, -16.9F, -1.5F, 3F, 3F, 3F)
            .texOffs(43, 40).addBox(-0.55F, -18.4F, -0.55F, 1.1F, 1.6F, 1.1F)
            .texOffs(64, 0).addBox(-1.25F, -10.9F, -4F, 2.5F, 2.5F, 0.6F)
            .texOffs(78, 0).addBox(-0.75F, -10.45F, -4.38F, 1.5F, 1.5F, 0.5F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    public static LayerDefinition wizard() {
        MeshDefinition mesh = hatMesh();
        mesh.getRoot().getChild("head").addOrReplaceChild("crown", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-6.5F, -8F, -5.5F, 13F, 1F, 11F)
            .texOffs(0, 14).addBox(-5.5F, -8.4F, -4.5F, 11F, 1F, 9F)
            .texOffs(0, 26).addBox(-3.5F, -11.5F, -3.5F, 7F, 3.5F, 7F)
            .texOffs(34, 29).addBox(-3.6F, -9.9F, -3.6F, 7.2F, 1.5F, 7.2F)
            .texOffs(0, 39).addBox(-2.5F, -14.4F, -2.55F, 5F, 3F, 5F)
            .texOffs(24, 40).addBox(-2.9F, -16.3F, -1.7F, 3.5F, 2.2F, 3.5F)
            .texOffs(43, 40).addBox(-4.4F, -17.1F, -1.2F, 3F, 1.7F, 2.5F)
            .texOffs(59, 40).addBox(-5.2F, -16.65F, -0.85F, 1.6F, 2F, 1.7F)
            .texOffs(64, 0).addBox(-1.15F, -10.4F, -4F, 2.3F, 2.3F, 0.7F)
            .texOffs(78, 0).addBox(-0.65F, -9.9F, -4.35F, 1.3F, 1.3F, 0.5F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }
}
