package net.luckystudio.spelunkers_charm.entity.custom.minecart.override;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;

public class NewMinecartModel<T extends Entity> extends HierarchicalModel<T> {

	private final ModelPart root;

    public NewMinecartModel(ModelPart root) {
		this.root = root;
        ModelPart body = root.getChild("body");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 38).addBox(-8.0F, -27.0F, -10.0F, 16.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 5).addBox(-6.0F, -29.0F, -11.0F, 12.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
				.texOffs(50, 7).mirror().addBox(-6.0F, -17.0F, -8.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(50, 7).addBox(4.0F, -17.0F, -8.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(50, 7).addBox(4.0F, -17.0F, 3.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
				.texOffs(50, 7).mirror().addBox(-6.0F, -17.0F, 3.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition front_r1 = body.addOrReplaceChild("front_r1", CubeListBuilder.create().texOffs(0, 5).addBox(-6.0F, -1.0F, -1.5F, 12.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -28.0F, 9.5F, 0.0F, 3.1416F, 0.0F));

		PartDefinition left_r1 = body.addOrReplaceChild("left_r1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-11.0F, -1.0F, -1.0F, 22.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(8.0F, -28.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition left_r2 = body.addOrReplaceChild("left_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-11.0F, -1.0F, -1.0F, 22.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, -28.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition left_r3 = body.addOrReplaceChild("left_r3", CubeListBuilder.create().texOffs(0, 28).addBox(-8.0F, -4.0F, -1.0F, 16.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -23.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition right_r1 = body.addOrReplaceChild("right_r1", CubeListBuilder.create().texOffs(0, 28).addBox(-8.0F, -4.0F, -1.0F, 16.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, -23.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition back_r1 = body.addOrReplaceChild("back_r1", CubeListBuilder.create().texOffs(0, 38).addBox(-8.0F, -4.0F, -1.0F, 16.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -23.0F, 9.0F, 0.0F, 3.1416F, 0.0F));

		PartDefinition bottom_r1 = body.addOrReplaceChild("bottom_r1", CubeListBuilder.create().texOffs(0, 10).addBox(-10.0F, -10.0F, 0.0F, 20.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -17.0F, 0.0F, 0.0F, 1.5708F, -1.5708F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
		this.root.render(poseStack, buffer, packedLight, packedOverlay);
	}

	@Override
	public ModelPart root() {
		return this.root;
	}
}