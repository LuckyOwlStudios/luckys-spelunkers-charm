package net.luckystudio.splelunkers_charm.datagen.advancements;

import net.luckystudio.splelunkers_charm.SpelunkersCharm;
import net.luckystudio.splelunkers_charm.init.ModBlocks;
import net.luckystudio.splelunkers_charm.init.ModEntityType;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.StartRidingTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import java.util.function.Consumer;

// Look at VanillaHusbandryAdvancements class for help on how to create advancements
public class ModAdvancementProvider implements AdvancementProvider.AdvancementGenerator {

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> consumer, ExistingFileHelper existingFileHelper) {
        AdvancementHolder sit_on_boulder = Advancement.Builder.advancement()
                .parent(ROOT_ADVENTURE)
                .display(ModBlocks.IRON_BOULDER,
                        Component.translatable("advancement.luckysarmory.sit_on_boulder.title"),
                        Component.translatable("advancement.luckysarmory.sit_on_boulder.description"),
                        ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                        AdvancementType.CHALLENGE, true, true, false)
                .rewards(AdvancementRewards.Builder.experience(10))

                // Found this out through the VanillaHusbandryAdvancements "ride_a_boat_with_a_goat" advancement
                .addCriterion(
                        "ride_a_boat_with_a_goat",
                        StartRidingTrigger.TriggerInstance.playerStartsRiding(
                                EntityPredicate.Builder.entity()
                                        .vehicle(EntityPredicate.Builder.entity().of(ModEntityType.BOULDER.get()))
                        )
                )
                .save(consumer, getNameId("sit_on_boulder"));
    }

    AdvancementHolder ROOT_ADVENTURE = new AdvancementHolder(
            ResourceLocation.withDefaultNamespace("adventure/adventuring_time"),
            null // you can pass `null` because it's not used in this context
    );

    protected static Advancement.Builder getAdvancement(AdvancementHolder parent, ItemLike display, String name, AdvancementType frame, boolean showToast, boolean announceToChat, boolean hidden) {
        return Advancement.Builder.advancement().parent(parent).display(display,
                Component.translatable("advancement." + name),
                Component.translatable("advancement." + name + ".desc"),
                null, frame, showToast, announceToChat, hidden);
    }

    private String getNameId(String id) {
        return SpelunkersCharm.MOD_ID + ":" + id;
    }
}