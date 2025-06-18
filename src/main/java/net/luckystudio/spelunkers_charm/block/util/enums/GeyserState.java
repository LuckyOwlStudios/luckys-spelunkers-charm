package net.luckystudio.spelunkers_charm.block.util.enums;

import net.minecraft.util.StringRepresentable;

public enum GeyserState implements StringRepresentable {
    DORMANT("inactive"),
    CHARGING("charging"),
    ERUPTING("erupting");

    private final String name;

    GeyserState(final String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
