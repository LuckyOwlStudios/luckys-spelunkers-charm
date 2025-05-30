package net.luckystudio.splelunkers_charm.item.potion;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(BuiltInRegistries.POTION, SpelunkersCharm.MOD_ID);

    public static final Holder<Potion> HASTE_POTION = POTIONS.register("haste_potion",
            () -> new Potion(new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0)));

    public static final Holder<Potion> LONG_HASTE_POTION = POTIONS.register("long_haste_potion",
            () -> new Potion(new MobEffectInstance(MobEffects.DIG_SPEED, 9600, 0)));

    public static final Holder<Potion> STRONG_HASTE_POTION = POTIONS.register("strong_haste_potion",
            () -> new Potion(new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 1)));

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}
