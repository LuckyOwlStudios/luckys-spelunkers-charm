// Cargo count label.
package net.luckystudio.spelunkers_charm.entity.custom.minecart;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

public final class CargoCountLabel {
    private CargoCountLabel() {}

    @Nullable
    public static Component forAmount(int amount) {
        return amount > 0
                ? Component.literal(amount + "x")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                : null;
    }
}
