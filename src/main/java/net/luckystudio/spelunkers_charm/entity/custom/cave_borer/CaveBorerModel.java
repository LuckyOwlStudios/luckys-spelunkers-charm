package net.luckystudio.spelunkers_charm.entity.custom.cave_borer;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Three separate pieces, each centered on the origin and facing +Z: the head (with two jaws), a body
 * segment and the tail. {@link CaveBorerRenderer} draws one piece per segment along the worm's body.
 * Sizes are for the small borer; bigger ones are scaled up.
 */
public class CaveBorerModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(SpelunkersCharm.id("cave_borer"), "main");

    public final ModelPart head;
    public final ModelPart upperJaw;
    public final ModelPart lowerJaw;
    public final ModelPart body;
    public final ModelPart tail;

    public CaveBorerModel(ModelPart root) {
        this.head = root.getChild("head");
        this.upperJaw = this.head.getChild("upper_jaw");
        this.lowerJaw = this.head.getChild("lower_jaw");
        this.body = root.getChild("body");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        head.addOrReplaceChild("upper_jaw", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3.5F, -3.0F, 0.0F, 7.0F, 3.0F, 4.0F), PartPose.offset(0.0F, -0.5F, 4.0F));
        head.addOrReplaceChild("lower_jaw", CubeListBuilder.create()
                .texOffs(22, 16).addBox(-3.5F, 0.0F, 0.0F, 7.0F, 3.0F, 4.0F), PartPose.offset(0.0F, 0.5F, 4.0F));

        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, -4.0F, -3.5F, 8.0F, 8.0F, 7.0F), PartPose.ZERO);

        root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }
}
