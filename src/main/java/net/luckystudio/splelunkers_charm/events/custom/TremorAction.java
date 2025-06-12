package net.luckystudio.splelunkers_charm.events.custom;

import net.minecraft.util.StringRepresentable;

public enum TremorAction implements StringRepresentable {
    start("start"),
    stop("stop");

    private final String name;

    TremorAction(final String name) {
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
