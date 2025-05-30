package net.luckystudio.splelunkers_charm.entity.custom.lift;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.entity.util.MineralType;
import net.luckystudio.splelunkers_charm.entity.util.WoodType;
import net.minecraft.Util;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class AbstractLiftRenderer<T extends AbstractLift> extends EntityRenderer<T> {

    private static final String PATH = "textures/entity/lift/";

    private static final Map<WoodType, String> WOOD_TYPE = Util.make(Maps.newHashMap(), map -> {
        map.put(WoodType.OAK, "oak.png");
        map.put(WoodType.SPRUCE, "spruce.png");
        map.put(WoodType.BIRCH, "birch.png");
        map.put(WoodType.JUNGLE, "jungle.png");
        map.put(WoodType.ACACIA, "acacia.png");
        map.put(WoodType.CHERRY, "cherry.png");
        map.put(WoodType.DARK_OAK, "dark_oak.png");
        map.put(WoodType.MANGROVE, "mangrove.png");
        map.put(WoodType.BAMBOO, "bamboo.png");
        map.put(WoodType.CRIMSON, "mangrove.png");
        map.put(WoodType.WARPED, "bamboo.png");
    });

    private static final Map<MineralType, String> MINERAL_TYPE = Util.make(Maps.newHashMap(), map -> {
        map.put(MineralType.IRON, "iron.png");
        map.put(MineralType.COPPER, "copper.png");
        map.put(MineralType.GOLD, "gold.png");
        map.put(MineralType.DIAMOND, "diamond.png");
        map.put(MineralType.NETHERITE, "netherite.png");
    });

    private final EntityModel<T> woodModel;
    private final EntityModel<T> mineralModel;
    private final AbstractLift.Type liftType;

    public AbstractLiftRenderer(EntityRendererProvider.Context context, EntityModel<T> model, AbstractLift.Type liftType) {
        super(context);
        this.woodModel = model;
        this.mineralModel = model;
        this.liftType = liftType;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));

        handleShakeWhenHit(entity, partialTick, poseStack);
        if (entity instanceof AbstractLift abstractLift) {
            VertexConsumer woodVertexConsumer = bufferSource.getBuffer(woodModel.renderType(getTextureLocation(abstractLift)));
            this.woodModel.setupAnim(entity, partialTick, 0.0F, 0.1F, 0.0F, 0.0F);
            woodModel.renderToBuffer(poseStack, woodVertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

            VertexConsumer mineralVertexConsumer = bufferSource.getBuffer(mineralModel.renderType(getMineralLocation(abstractLift)));
            this.mineralModel.setupAnim(entity, partialTick, 0.0F, 0.1F, 0.0F, 0.0F);
            mineralModel.renderToBuffer(poseStack, mineralVertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
    }

    private void handleShakeWhenHit(AbstractLift entity, float partialTick, PoseStack poseStack) {
        float f5 = (float)entity.getHurtTime() - partialTick;
        float f6 = entity.getDamage() - partialTick;
        if (f6 < 0.0F) {
            f6 = 0.0F;
        }

        if (f5 > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(
                    Mth.sin(f5) * f5 * f6 / 10.0F * (float)entity.getHurtDir()
            ));
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(AbstractLift entity) {
        WoodType woodType = entity.getWoodType();
        String woodTexture = WOOD_TYPE.get(woodType);
        AbstractLift.Type liftType = this.liftType;
        return SpelunkersCharm.id(PATH + getTypeName(liftType) + woodTexture);
    }

    public ResourceLocation getMineralLocation(AbstractLift entity) {
        MineralType mineralType = entity.getMineralType();
        String mineralTexture = MINERAL_TYPE.get(mineralType);
        AbstractLift.Type liftType = this.liftType;
        return SpelunkersCharm.id(PATH + getTypeName(liftType) + mineralTexture);
    }

    private String getTypeName(AbstractLift.Type type) {
        return switch (type) {
            case SMALL -> "small/";
            case MEDIUM -> "medium/";
            case LARGE -> "large/";
        };
    }
}
