// New locomotive model.
package net.luckystudio.spelunkers_charm.entity.custom.minecart.override;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class NewLocomotiveModel<T extends AbstractMinecart> extends HierarchicalModel<T> {

    private final ModelPart vent;
    private final ModelPart base;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart dyeables;
    private final ModelPart bone;
    private final ModelPart controls;
    private final ModelPart lever;
    private final ModelPart hook;

    public NewLocomotiveModel(ModelPart root) {
        this.root = root;
        this.vent = root.getChild("vent");
        this.base = root.getChild("base");
        this.body = root.getChild("body");
        this.dyeables = root.getChild("dyeables");
        this.bone = root.getChild("bone");
        this.controls = root.getChild("switch");
        this.lever = this.controls.getChild("lever");
        this.hook = root.getChild("hook");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition vent =
                partdefinition.addOrReplaceChild(
                        "vent",
                        CubeListBuilder.create()
                                .texOffs(0, 70)
                                .addBox(
                                        -4.0F,
                                        -23.0F,
                                        2.0F,
                                        8.0F,
                                        10.0F,
                                        2.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(-18.0F, -1.0F, 4.0F, 0.0F, 0.0F, 1.5708F));

        PartDefinition base =
                partdefinition.addOrReplaceChild(
                        "base",
                        CubeListBuilder.create(),
                        PartPose.offsetAndRotation(0.0F, 5.0F, -8.0F, -1.5708F, 0.0F, 1.5708F));

        PartDefinition left_r1 =
                base.addOrReplaceChild(
                        "left_r1",
                        CubeListBuilder.create()
                                .texOffs(0, 32)
                                .addBox(
                                        -11.0F,
                                        -17.0F,
                                        -1.0F,
                                        22.0F,
                                        18.0F,
                                        3.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition body =
                partdefinition.addOrReplaceChild(
                        "body",
                        CubeListBuilder.create()
                                .texOffs(16, 53)
                                .addBox(
                                        -6.0F,
                                        -16.0F,
                                        -8.0F,
                                        2.0F,
                                        1.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(16, 53)
                                .addBox(
                                        4.0F,
                                        -16.0F,
                                        -8.0F,
                                        2.0F,
                                        1.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(16, 53)
                                .addBox(
                                        4.0F,
                                        -16.0F,
                                        3.0F,
                                        2.0F,
                                        1.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(16, 53)
                                .addBox(
                                        -6.0F,
                                        -16.0F,
                                        3.0F,
                                        2.0F,
                                        1.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition light_r1 =
                body.addOrReplaceChild(
                        "light_r1",
                        CubeListBuilder.create()
                                .texOffs(60, 0)
                                .addBox(
                                        0.0F,
                                        -6.0F,
                                        2.0F,
                                        6.0F,
                                        6.0F,
                                        1.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(3.0F, -20.0F, 14.0F, 0.0F, 3.1416F, 0.0F));

        PartDefinition dyeables =
                partdefinition.addOrReplaceChild(
                        "dyeables",
                        CubeListBuilder.create(),
                        PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition bottom_r1 =
                dyeables.addOrReplaceChild(
                        "bottom_r1",
                        CubeListBuilder.create()
                                .texOffs(44, 0)
                                .addBox(
                                        -8.0F,
                                        -8.0F,
                                        2.0F,
                                        16.0F,
                                        12.0F,
                                        0.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(2.0F, -17.0F, 0.0F, 0.0F, 1.5708F, -1.5708F));

        PartDefinition bone =
                partdefinition.addOrReplaceChild(
                        "bone",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -6.0F,
                                        -11.0F,
                                        -1.0F,
                                        12.0F,
                                        22.0F,
                                        10.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(44, 22)
                                .addBox(
                                        -3.0F,
                                        -3.0F,
                                        9.0F,
                                        6.0F,
                                        6.0F,
                                        3.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(44, 8)
                                .addBox(
                                        -4.0F,
                                        -4.0F,
                                        12.0F,
                                        8.0F,
                                        8.0F,
                                        2.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, -1.5708F, 1.5708F));

        PartDefinition controls =
                partdefinition.addOrReplaceChild(
                        "switch",
                        CubeListBuilder.create()
                                .texOffs(0, 58)
                                .addBox(
                                        -5.0F,
                                        -18.0F,
                                        -3.0F,
                                        10.0F,
                                        2.0F,
                                        3.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offset(0.0F, 19.0F, -6.0F));

        PartDefinition lever =
                controls.addOrReplaceChild(
                        "lever",
                        CubeListBuilder.create()
                                .texOffs(42, 53)
                                .addBox(
                                        -1.0F,
                                        -10.0F,
                                        -2.0F,
                                        2.0F,
                                        10.0F,
                                        2.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(0.0F, -15.0F, 0.0F, 0.0F, 0.0F, 0.5236F));

        partdefinition.addOrReplaceChild(
                "hook",
                CubeListBuilder.create().texOffs(74, 0).addBox(-3, -26, -12, 6, 6, 1),
                PartPose.offsetAndRotation(0, 22, 0, 0, 1.5708F, 0));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    public void setState(boolean coupled, boolean brake, boolean reversed) {
        hook.visible = coupled;
        lever.zRot = brake ? 0 : (reversed ? -.5235988F : .5235988F);
    }

    @Override
    public void setupAnim(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {}

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int color) {
        hook.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        vent.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        base.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        dyeables.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        controls.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    @Override
    public ModelPart root() {
        return root;
    }
}
