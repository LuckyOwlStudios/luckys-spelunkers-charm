package net.luckystudio.spelunkers_charm.entity.custom.lift.large;

import net.luckystudio.spelunkers_charm.SpelunkersCharmClient;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLift;
import net.luckystudio.spelunkers_charm.entity.custom.lift.AbstractLiftRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class LargeLiftRenderer<T extends LargeLift> extends AbstractLiftRenderer<T> {

    public LargeLiftRenderer(EntityRendererProvider.Context context) {
        super(context, new LargeLiftModel<>(context.bakeLayer(SpelunkersCharmClient.LARGE_LIFT)), AbstractLift.Type.LARGE);
    }
}
