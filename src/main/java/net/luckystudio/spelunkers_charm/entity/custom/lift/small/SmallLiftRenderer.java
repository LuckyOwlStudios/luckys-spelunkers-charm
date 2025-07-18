package net.luckystudio.spelunkers_charm.entity.custom.lift.small;

import net.luckystudio.spelunkers_charm.SpelunkersCharmClient;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLiftRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SmallLiftRenderer<T extends SmallLift> extends AbstractLiftRenderer<T> {

    public SmallLiftRenderer(EntityRendererProvider.Context context) {
        super(context, new SmallLiftModel<>(context.bakeLayer(SpelunkersCharmClient.SMALL_LIFT)), AbstractLift.Type.SMALL);
    }
}
