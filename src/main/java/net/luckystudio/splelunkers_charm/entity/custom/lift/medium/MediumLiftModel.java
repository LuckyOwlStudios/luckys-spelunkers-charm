package net.luckystudio.splelunkers_charm.entity.custom.lift.medium;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class MediumLiftModel<T extends MediumLift> extends HierarchicalModel<T> {

	private final ModelPart root;
	private final ModelPart body;
	public final ModelPart gear1;
	public final ModelPart gear2;
	public final ModelPart gear3;
	public final ModelPart gear4;
	public final ModelPart lever;

	public MediumLiftModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.gear1 = this.body.getChild("gear1");
		this.gear2 = this.body.getChild("gear2");
		this.gear3 = this.body.getChild("gear3");
		this.gear4 = this.body.getChild("gear4");
		this.lever = this.body.getChild("lever");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-40.0F, -16.0F, -40.0F, 80.0F, 16.0F, 80.0F, new CubeDeformation(0.0F))
		.texOffs(44, 72).addBox(-6.0F, -18.0F, -3.0F, 12.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition gear1 = body.addOrReplaceChild("gear1", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, -9.0F, -7.0F, 4.0F, 18.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -7.0F, -9.0F, 4.0F, 14.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(40.0F, -8.0F, -16.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition teeth_r1 = gear1.addOrReplaceChild("teeth_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r2 = gear1.addOrReplaceChild("teeth_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r3 = gear1.addOrReplaceChild("teeth_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition teeth_r4 = gear1.addOrReplaceChild("teeth_r4", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1416F, 0.0F, 0.0F));

		PartDefinition teeth_r5 = gear1.addOrReplaceChild("teeth_r5", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r6 = gear1.addOrReplaceChild("teeth_r6", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r7 = gear1.addOrReplaceChild("teeth_r7", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r8 = gear1.addOrReplaceChild("teeth_r8", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r9 = gear1.addOrReplaceChild("teeth_r9", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r10 = gear1.addOrReplaceChild("teeth_r10", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r11 = gear1.addOrReplaceChild("teeth_r11", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition gear2 = body.addOrReplaceChild("gear2", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, -9.0F, -7.0F, 4.0F, 18.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -7.0F, -9.0F, 4.0F, 14.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(40.0F, -8.0F, 16.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition teeth_r12 = gear2.addOrReplaceChild("teeth_r12", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r13 = gear2.addOrReplaceChild("teeth_r13", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r14 = gear2.addOrReplaceChild("teeth_r14", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition teeth_r15 = gear2.addOrReplaceChild("teeth_r15", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1416F, 0.0F, 0.0F));

		PartDefinition teeth_r16 = gear2.addOrReplaceChild("teeth_r16", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r17 = gear2.addOrReplaceChild("teeth_r17", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r18 = gear2.addOrReplaceChild("teeth_r18", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r19 = gear2.addOrReplaceChild("teeth_r19", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r20 = gear2.addOrReplaceChild("teeth_r20", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r21 = gear2.addOrReplaceChild("teeth_r21", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r22 = gear2.addOrReplaceChild("teeth_r22", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition gear3 = body.addOrReplaceChild("gear3", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, -9.0F, -7.0F, 4.0F, 18.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -7.0F, -9.0F, 4.0F, 14.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-40.0F, -8.0F, -16.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition teeth_r23 = gear3.addOrReplaceChild("teeth_r23", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r24 = gear3.addOrReplaceChild("teeth_r24", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r25 = gear3.addOrReplaceChild("teeth_r25", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition teeth_r26 = gear3.addOrReplaceChild("teeth_r26", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1416F, 0.0F, 0.0F));

		PartDefinition teeth_r27 = gear3.addOrReplaceChild("teeth_r27", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r28 = gear3.addOrReplaceChild("teeth_r28", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r29 = gear3.addOrReplaceChild("teeth_r29", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r30 = gear3.addOrReplaceChild("teeth_r30", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r31 = gear3.addOrReplaceChild("teeth_r31", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r32 = gear3.addOrReplaceChild("teeth_r32", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r33 = gear3.addOrReplaceChild("teeth_r33", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition gear4 = body.addOrReplaceChild("gear4", CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, -9.0F, -7.0F, 4.0F, 18.0F, 14.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -7.0F, -9.0F, 4.0F, 14.0F, 18.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(-40.0F, -8.0F, 16.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition teeth_r34 = gear4.addOrReplaceChild("teeth_r34", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r35 = gear4.addOrReplaceChild("teeth_r35", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r36 = gear4.addOrReplaceChild("teeth_r36", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5236F, 0.0F, 0.0F));

		PartDefinition teeth_r37 = gear4.addOrReplaceChild("teeth_r37", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1416F, 0.0F, 0.0F));

		PartDefinition teeth_r38 = gear4.addOrReplaceChild("teeth_r38", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r39 = gear4.addOrReplaceChild("teeth_r39", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r40 = gear4.addOrReplaceChild("teeth_r40", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition teeth_r41 = gear4.addOrReplaceChild("teeth_r41", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.0944F, 0.0F, 0.0F));

		PartDefinition teeth_r42 = gear4.addOrReplaceChild("teeth_r42", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 2.618F, 0.0F, 0.0F));

		PartDefinition teeth_r43 = gear4.addOrReplaceChild("teeth_r43", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.0472F, 0.0F, 0.0F));

		PartDefinition teeth_r44 = gear4.addOrReplaceChild("teeth_r44", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -13.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5236F, 0.0F, 0.0F));

		PartDefinition lever = body.addOrReplaceChild("lever", CubeListBuilder.create().texOffs(42, 56).addBox(-1.0F, -20.0F, -1.0F, 2.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 512, 512);
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
}