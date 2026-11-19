// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class Locomotive<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "locomotive"), "main");
	private final ModelPart vent;
	private final ModelPart base;
	private final ModelPart body;
	private final ModelPart dyeables;
	private final ModelPart bone;
	private final ModelPart switch;
	private final ModelPart lever;

	public Locomotive(ModelPart root) {
		this.vent = root.getChild("vent");
		this.base = root.getChild("base");
		this.body = root.getChild("body");
		this.dyeables = root.getChild("dyeables");
		this.bone = root.getChild("bone");
		this.switch = root.getChild("switch");
		this.lever = this.switch.getChild("lever");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition vent = partdefinition.addOrReplaceChild("vent", CubeListBuilder.create().texOffs(0, 70).addBox(-4.0F, -23.0F, 2.0F, 8.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.0F, -1.0F, 4.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition base = partdefinition.addOrReplaceChild("base", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 4.0F, -8.0F, -1.5708F, 0.0F, 1.5708F));

		PartDefinition left_r1 = base.addOrReplaceChild("left_r1", CubeListBuilder.create().texOffs(0, 32).addBox(-11.0F, -17.0F, -1.0F, 22.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 52).addBox(-6.0F, -17.0F, -8.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 52).addBox(4.0F, -17.0F, -8.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 52).addBox(4.0F, -17.0F, 3.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 52).addBox(-6.0F, -17.0F, 3.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(60, 0).addBox(-3.0F, -27.0F, -12.0F, 6.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition light_r1 = body.addOrReplaceChild("light_r1", CubeListBuilder.create().texOffs(60, 0).addBox(0.0F, -6.0F, 2.0F, 6.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -21.0F, 14.0F, 0.0F, 3.1416F, 0.0F));

		PartDefinition dyeables = partdefinition.addOrReplaceChild("dyeables", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition bottom_r1 = dyeables.addOrReplaceChild("bottom_r1", CubeListBuilder.create().texOffs(44, 0).addBox(-8.0F, -8.0F, 2.0F, 16.0F, 12.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -17.0F, 0.0F, 0.0F, 1.5708F, -1.5708F));

		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -11.0F, 0.0F, 12.0F, 22.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(44, 0).addBox(-3.0F, -3.0F, 10.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(44, 8).addBox(-4.0F, -4.0F, 12.0F, 8.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, -1.5708F, 1.5708F));

		PartDefinition switch = partdefinition.addOrReplaceChild("switch", CubeListBuilder.create().texOffs(0, 58).addBox(-5.0F, -18.0F, -3.0F, 10.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 19.0F, -6.0F));

		PartDefinition lever = switch.addOrReplaceChild("lever", CubeListBuilder.create().texOffs(42, 53).addBox(-1.0F, -10.0F, -2.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -15.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		vent.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		base.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		dyeables.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		switch.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}