package net.luckystudio.splelunkers_charm.entity.custom.lift.medium;

import net.luckystudio.splelunkers_charm.SpelunkersCharmClient;
import net.luckystudio.splelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.splelunkers_charm.entity.custom.lift.AbstractLiftRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class MediumLiftRenderer<T extends MediumLift> extends AbstractLiftRenderer<T> {

    public MediumLiftRenderer(EntityRendererProvider.Context context) {
        super(context, new MediumLiftModel<>(context.bakeLayer(SpelunkersCharmClient.MEDIUM_LIFT)), AbstractLift.Type.MEDIUM);
    }
}
