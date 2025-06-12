package net.luckystudio.splelunkers_charm.block.util.enums;

import net.minecraft.util.StringRepresentable;

public enum BlockPart implements StringRepresentable {
    BOTTOM_CORNER("bottom_corner"),
    BOTTOM_SIDE("bottom_side"),
    BOTTOM_MIDDLE("bottom_middle"),
    MIDDLE_CORNER("middle_corner"),
    MIDDLE_SIDE("middle_side"),
    TOP_CORNER("top_corner"),
    TOP_SIDE("top_side"),
    TOP_MIDDLE("top_middle");


    private final String name;

    BlockPart(final String name) {
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
