package net.luckystudio.splelunkers_charm.block.util.enums;

import net.minecraft.util.StringRepresentable;

public enum BlockPart implements StringRepresentable {
    SIDE_TOP_CORNER("side_top_corner"),
    SIDE_TOP_MIDDLE("side_top_middle"),
    SIDE_MIDDLE_CORNER("side_middle_corner"),
    SIDE_MIDDLE_MIDDLE("side_middle_middle"),
    SIDE_BOTTOM_CORNER("side_bottom_corner"),
    SIDE_BOTTOM_MIDDLE("side_bottom_middle"),
    TOP("top"),
    BOTTOM("bottom");

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
