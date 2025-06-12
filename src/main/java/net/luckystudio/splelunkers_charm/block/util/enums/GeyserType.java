package net.luckystudio.splelunkers_charm.block.util.enums;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum GeyserType implements StringRepresentable {
    NONE("none"),
    WATER("water"),
    SNOW("snow"),
    LAVA("lava");

    private final String name;

    GeyserType(final String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}
