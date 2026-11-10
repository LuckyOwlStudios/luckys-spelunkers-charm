/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.model.HierarchicalModel
 *  net.minecraft.client.model.geom.ModelPart
 *  net.minecraft.client.model.geom.PartPose
 *  net.minecraft.client.model.geom.builders.CubeDeformation
 *  net.minecraft.client.model.geom.builders.CubeListBuilder
 *  net.minecraft.client.model.geom.builders.LayerDefinition
 *  net.minecraft.client.model.geom.builders.MeshDefinition
 *  net.minecraft.client.model.geom.builders.PartDefinition
 */
package net.luckystudio.spelunkers_charm.entity.custom.lift.small;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class SmallLiftModel<T extends SmallLift> extends HierarchicalModel<T> {
    public final ModelPart root;
    private final ModelPart body;
    private final ModelPart lever;
    public final ModelPart rightGear;
    public final ModelPart leftGear;

    public SmallLiftModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.lever = this.body.getChild("lever");
        this.leftGear = this.body.getChild("leftGear");
        this.rightGear = this.body.getChild("rightGear");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition body =
                partdefinition.addOrReplaceChild(
                        "body",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -24.0f,
                                        -16.0f,
                                        -24.0f,
                                        48.0f,
                                        16.0f,
                                        48.0f,
                                        new CubeDeformation(0.0f))
                                .texOffs(0, 40)
                                .addBox(
                                        -6.0f,
                                        -18.0f,
                                        -3.0f,
                                        12.0f,
                                        2.0f,
                                        6.0f,
                                        new CubeDeformation(0.0f)),
                        PartPose.offset((float) 0.0f, (float) 24.0f, (float) 0.0f));
        PartDefinition rightGear =
                body.addOrReplaceChild(
                        "rightGear",
                        CubeListBuilder.create()
                                .texOffs(0, 64)
                                .addBox(
                                        -2.0f,
                                        -9.0f,
                                        -7.0f,
                                        4.0f,
                                        18.0f,
                                        14.0f,
                                        new CubeDeformation(0.0f))
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -7.0f,
                                        -9.0f,
                                        4.0f,
                                        14.0f,
                                        18.0f,
                                        new CubeDeformation(0.0f))
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) -24.0f,
                                (float) -8.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -1.5708f,
                                (float) 0.0f));
        PartDefinition teeth_r1 =
                rightGear.addOrReplaceChild(
                        "teeth_r1",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -1.5708f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r2 =
                rightGear.addOrReplaceChild(
                        "teeth_r2",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -1.0472f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r3 =
                rightGear.addOrReplaceChild(
                        "teeth_r3",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -0.5236f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r4 =
                rightGear.addOrReplaceChild(
                        "teeth_r4",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -3.1416f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r5 =
                rightGear.addOrReplaceChild(
                        "teeth_r5",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -2.618f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r6 =
                rightGear.addOrReplaceChild(
                        "teeth_r6",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -2.0944f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r7 =
                rightGear.addOrReplaceChild(
                        "teeth_r7",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 1.5708f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r8 =
                rightGear.addOrReplaceChild(
                        "teeth_r8",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 2.0944f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r9 =
                rightGear.addOrReplaceChild(
                        "teeth_r9",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 2.618f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r10 =
                rightGear.addOrReplaceChild(
                        "teeth_r10",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 1.0472f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r11 =
                rightGear.addOrReplaceChild(
                        "teeth_r11",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.5236f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition leftGear =
                body.addOrReplaceChild(
                        "leftGear",
                        CubeListBuilder.create()
                                .texOffs(0, 64)
                                .addBox(
                                        -2.0f,
                                        -9.0f,
                                        -7.0f,
                                        4.0f,
                                        18.0f,
                                        14.0f,
                                        new CubeDeformation(0.0f))
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -7.0f,
                                        -9.0f,
                                        4.0f,
                                        14.0f,
                                        18.0f,
                                        new CubeDeformation(0.0f))
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 24.0f,
                                (float) -8.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -1.5708f,
                                (float) 0.0f));
        PartDefinition teeth_r12 =
                leftGear.addOrReplaceChild(
                        "teeth_r12",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -1.5708f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r13 =
                leftGear.addOrReplaceChild(
                        "teeth_r13",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -1.0472f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r14 =
                leftGear.addOrReplaceChild(
                        "teeth_r14",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -0.5236f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r15 =
                leftGear.addOrReplaceChild(
                        "teeth_r15",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -3.1416f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r16 =
                leftGear.addOrReplaceChild(
                        "teeth_r16",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -2.618f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r17 =
                leftGear.addOrReplaceChild(
                        "teeth_r17",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) -2.0944f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r18 =
                leftGear.addOrReplaceChild(
                        "teeth_r18",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 1.5708f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r19 =
                leftGear.addOrReplaceChild(
                        "teeth_r19",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 2.0944f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r20 =
                leftGear.addOrReplaceChild(
                        "teeth_r20",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 2.618f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r21 =
                leftGear.addOrReplaceChild(
                        "teeth_r21",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 1.0472f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition teeth_r22 =
                leftGear.addOrReplaceChild(
                        "teeth_r22",
                        CubeListBuilder.create()
                                .texOffs(0, 0)
                                .addBox(
                                        -2.0f,
                                        -13.0f,
                                        -2.0f,
                                        4.0f,
                                        4.0f,
                                        4.0f,
                                        new CubeDeformation(-0.1f)),
                        PartPose.offsetAndRotation(
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.0f,
                                (float) 0.5236f,
                                (float) 0.0f,
                                (float) 0.0f));
        PartDefinition lever =
                body.addOrReplaceChild(
                        "lever",
                        CubeListBuilder.create()
                                .texOffs(36, 74)
                                .addBox(
                                        -1.0f,
                                        -20.0f,
                                        -1.0f,
                                        2.0f,
                                        20.0f,
                                        2.0f,
                                        new CubeDeformation(0.0f)),
                        PartPose.offset((float) 0.0f, (float) -8.0f, (float) 0.0f));
        return LayerDefinition.create((MeshDefinition) meshdefinition, (int) 256, (int) 256);
    }

    public ModelPart root() {
        return this.root;
    }

    public void setupAnim(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        ((AbstractLift) ((Object) entity))
                .setLeverAngle((AbstractLift) ((Object) entity), this.lever);
        // Actual travel drives the teeth, so reversing and stopping remain in sync.
        double height = entity.yo + (entity.getY() - entity.yo) * limbSwing;
        float angle = (float) ((height / 0.6875) % (Math.PI * 2.0));
        this.leftGear.xRot = angle;
        this.rightGear.xRot = -angle;
    }

    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
