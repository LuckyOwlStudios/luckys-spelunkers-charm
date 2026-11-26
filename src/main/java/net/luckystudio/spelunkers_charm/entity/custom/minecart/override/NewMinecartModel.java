// New minecart model.
package net.luckystudio.spelunkers_charm.entity.custom.minecart.override;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class NewMinecartModel<T extends AbstractMinecart> extends HierarchicalModel<T> {

    private final ModelPart hook1;
    private final ModelPart hook2;
    private final ModelPart root;
    private final ModelPart body;

    public NewMinecartModel(ModelPart root) {
        this.root = root;
        this.hook1 = root.getChild("hook1");
        this.hook2 = root.getChild("hook2");
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition hook1 =
                partdefinition.addOrReplaceChild(
                        "hook1",
                        CubeListBuilder.create()
                                .texOffs(50, 0)
                                .addBox(
                                        -3.0F,
                                        -26.0F,
                                        -11.0F,
                                        6.0F,
                                        6.0F,
                                        1.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition hook2 =
                partdefinition.addOrReplaceChild(
                        "hook2",
                        CubeListBuilder.create(),
                        PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition hook_r1 =
                hook2.addOrReplaceChild(
                        "hook_r1",
                        CubeListBuilder.create()
                                .texOffs(50, 0)
                                .addBox(
                                        0.0F,
                                        -6.0F,
                                        2.0F,
                                        6.0F,
                                        6.0F,
                                        1.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(3.0F, -20.0F, 13.0F, 0.0F, 3.1416F, 0.0F));

        PartDefinition body =
                partdefinition.addOrReplaceChild(
                        "body",
                        CubeListBuilder.create()
                                .texOffs(0, 38)
                                .addBox(
                                        -8.0F,
                                        -27.0F,
                                        -10.0F,
                                        16.0F,
                                        8.0F,
                                        2.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(0, 5)
                                .addBox(
                                        -6.0F,
                                        -29.0F,
                                        -11.0F,
                                        12.0F,
                                        2.0F,
                                        3.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(50, 7)
                                .mirror()
                                .addBox(
                                        -6.0F,
                                        -17.0F,
                                        -8.0F,
                                        2.0F,
                                        1.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F))
                                .mirror(false)
                                .texOffs(50, 7)
                                .addBox(
                                        4.0F,
                                        -17.0F,
                                        -8.0F,
                                        2.0F,
                                        1.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(50, 7)
                                .addBox(
                                        4.0F,
                                        -17.0F,
                                        3.0F,
                                        2.0F,
                                        1.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F))
                                .texOffs(50, 7)
                                .mirror()
                                .addBox(
                                        -6.0F,
                                        -17.0F,
                                        3.0F,
                                        2.0F,
                                        1.0F,
                                        5.0F,
                                        new CubeDeformation(0.0F))
                                .mirror(false),
                        PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition front_r1 =
                body.addOrReplaceChild(
                        "front_r1",
                        CubeListBuilder.create()
                                .texOffs(0, 5)
                                .addBox(
                                        -6.0F,
                                        -1.0F,
                                        -1.5F,
                                        12.0F,
                                        2.0F,
                                        3.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(0.0F, -28.0F, 9.5F, 0.0F, 3.1416F, 0.0F));

        PartDefinition left_r1 =
                body.addOrReplaceChild(
                        "left_r1",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .mirror()
                                .addBox(
                                        -11.0F,
                                        -1.0F,
                                        -1.0F,
                                        22.0F,
                                        2.0F,
                                        3.0F,
                                        new CubeDeformation(0.0F))
                                .mirror(false),
                        PartPose.offsetAndRotation(8.0F, -28.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition left_r2 =
                body.addOrReplaceChild(
                        "left_r2",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -11.0F,
                                        -1.0F,
                                        -1.0F,
                                        22.0F,
                                        2.0F,
                                        3.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(-8.0F, -28.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition left_r3 =
                body.addOrReplaceChild(
                        "left_r3",
                        CubeListBuilder.create()
                                .texOffs(0, 28)
                                .addBox(
                                        -8.0F,
                                        -4.0F,
                                        -1.0F,
                                        16.0F,
                                        8.0F,
                                        2.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(-7.0F, -23.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

        PartDefinition right_r1 =
                body.addOrReplaceChild(
                        "right_r1",
                        CubeListBuilder.create()
                                .texOffs(0, 28)
                                .addBox(
                                        -8.0F,
                                        -4.0F,
                                        -1.0F,
                                        16.0F,
                                        8.0F,
                                        2.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(7.0F, -23.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition back_r1 =
                body.addOrReplaceChild(
                        "back_r1",
                        CubeListBuilder.create()
                                .texOffs(0, 38)
                                .addBox(
                                        -8.0F,
                                        -4.0F,
                                        -1.0F,
                                        16.0F,
                                        8.0F,
                                        2.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(0.0F, -23.0F, 9.0F, 0.0F, 3.1416F, 0.0F));

        PartDefinition bottom_r1 =
                body.addOrReplaceChild(
                        "bottom_r1",
                        CubeListBuilder.create()
                                .texOffs(0, 10)
                                .addBox(
                                        -10.0F,
                                        -10.0F,
                                        0.0F,
                                        20.0F,
                                        16.0F,
                                        2.0F,
                                        new CubeDeformation(0.0F)),
                        PartPose.offsetAndRotation(2.0F, -17.0F, 0.0F, 0.0F, 1.5708F, -1.5708F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void setHooks(int mask) {
        hook1.visible = (mask & 1) != 0;
        hook2.visible = (mask & 2) != 0;
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
        hook1.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        hook2.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    @Override
    public ModelPart root() {
        return root;
    }
}
