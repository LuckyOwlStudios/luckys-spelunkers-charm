package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

/** The locomotive's model, made in Blockbench. The lever on its side tilts when the direction is switched. */
public class LocomotiveModel extends HierarchicalModel<AbstractMinecart> {

    /** How far the lever tilts each way, in radians. */
    private static final float LEVER_TILT = 0.6F;

    private final ModelPart root;
    private final ModelPart lever;

    public LocomotiveModel(ModelPart root) {
        this.root = root;
        this.lever = root.getChild("switch").getChild("lever");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("vent", CubeListBuilder.create().texOffs(0, 70).addBox(-4.0F, -23.0F, 2.0F, 8.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-18.0F, -1.0F, 4.0F, 0.0F, 0.0F, 1.5708F));

        PartDefinition base = partdefinition.addOrReplaceChild("base", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 4.0F, -8.0F, -1.5708F, 0.0F, 1.5708F));
        base.addOrReplaceChild("left_r1", CubeListBuilder.create().texOffs(0, 32).addBox(-11.0F, -17.0F, -1.0F, 22.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 52).addBox(-6.0F, -17.0F, -8.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 52).addBox(4.0F, -17.0F, -8.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 52).addBox(4.0F, -17.0F, 3.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 52).addBox(-6.0F, -17.0F, 3.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(60, 0).addBox(-3.0F, -27.0F, -12.0F, 6.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.5708F, 0.0F));
        body.addOrReplaceChild("light_r1", CubeListBuilder.create().texOffs(60, 0).addBox(0.0F, -6.0F, 2.0F, 6.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -21.0F, 14.0F, 0.0F, 3.1416F, 0.0F));

        PartDefinition dyeables = partdefinition.addOrReplaceChild("dyeables", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 1.5708F, 0.0F));
        dyeables.addOrReplaceChild("bottom_r1", CubeListBuilder.create().texOffs(44, 0).addBox(-8.0F, -8.0F, 2.0F, 16.0F, 12.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -17.0F, 0.0F, 0.0F, 1.5708F, -1.5708F));

        partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -11.0F, 0.0F, 12.0F, 22.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(44, 0).addBox(-3.0F, -3.0F, 10.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(44, 8).addBox(-4.0F, -4.0F, 12.0F, 8.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, -1.5708F, 1.5708F));

        PartDefinition lever = partdefinition.addOrReplaceChild("switch", CubeListBuilder.create().texOffs(0, 58).addBox(-5.0F, -18.0F, -3.0F, 10.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 19.0F, -6.0F));
        lever.addOrReplaceChild("lever", CubeListBuilder.create().texOffs(42, 53).addBox(-1.0F, -10.0F, -2.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -15.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(AbstractMinecart entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // The lever swings along the length of the cart, one way for forward and the other for reversed.
        boolean reversed = entity instanceof LocomotiveMinecart locomotive && locomotive.isReversed();
        this.lever.zRot = reversed ? LEVER_TILT : -LEVER_TILT;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
