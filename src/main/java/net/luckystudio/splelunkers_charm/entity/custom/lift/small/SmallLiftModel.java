package net.luckystudio.splelunkers_charm.entity.custom.lift.small;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

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

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-24.0F, -16.0F, -24.0F, 48.0F, 16.0F, 48.0F, new CubeDeformation(0.0F))
				.texOffs(0, 40).addBox(-6.0F, -18.0F, -3.0F, 12.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition rightGear = body.addOrReplaceChild("rightGear", CubeListBuilder.create().texOffs(0, 64).addBox(-2.0F, -9.0F, -7.0F, 4.0F, 18.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-2.0F, -7.0F, -9.0F, 4.0F, 14.0F, 18.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-24.0F, -8.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition teeth_r1 = rightGear.addOrReplaceChild("teeth_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r2 = rightGear.addOrReplaceChild("teeth_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r3 = rightGear.addOrReplaceChild("teeth_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition teeth_r4 = rightGear.addOrReplaceChild("teeth_r4", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1416F, 0.0F, 0.0F));

		PartDefinition teeth_r5 = rightGear.addOrReplaceChild("teeth_r5", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r6 = rightGear.addOrReplaceChild("teeth_r6", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r7 = rightGear.addOrReplaceChild("teeth_r7", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r8 = rightGear.addOrReplaceChild("teeth_r8", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r9 = rightGear.addOrReplaceChild("teeth_r9", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r10 = rightGear.addOrReplaceChild("teeth_r10", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r11 = rightGear.addOrReplaceChild("teeth_r11", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition leftGear = body.addOrReplaceChild("leftGear", CubeListBuilder.create().texOffs(0, 64).addBox(-2.0F, -9.0F, -7.0F, 4.0F, 18.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-2.0F, -7.0F, -9.0F, 4.0F, 14.0F, 18.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(24.0F, -8.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition teeth_r12 = leftGear.addOrReplaceChild("teeth_r12", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r13 = leftGear.addOrReplaceChild("teeth_r13", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r14 = leftGear.addOrReplaceChild("teeth_r14", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition teeth_r15 = leftGear.addOrReplaceChild("teeth_r15", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1416F, 0.0F, 0.0F));

		PartDefinition teeth_r16 = leftGear.addOrReplaceChild("teeth_r16", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r17 = leftGear.addOrReplaceChild("teeth_r17", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r18 = leftGear.addOrReplaceChild("teeth_r18", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r19 = leftGear.addOrReplaceChild("teeth_r19", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r20 = leftGear.addOrReplaceChild("teeth_r20", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r21 = leftGear.addOrReplaceChild("teeth_r21", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r22 = leftGear.addOrReplaceChild("teeth_r22", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition lever = body.addOrReplaceChild("lever", CubeListBuilder.create().texOffs(36, 74).addBox(-1.0F, -20.0F, -1.0F, 2.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public ModelPart root() {
		return this.root;
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		entity.setLeverAngle(entity, this.lever);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		root.render(poseStack, buffer, packedLight, packedOverlay);
	}
}