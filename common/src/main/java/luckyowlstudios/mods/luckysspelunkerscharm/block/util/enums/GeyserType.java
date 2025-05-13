package luckyowlstudios.mods.luckysspelunkerscharm.block.util.enums;

import net.minecraft.util.StringRepresentable;

public enum GeyserType implements StringRepresentable {
    NONE("none"),
    WATER("water"),
    FIRE("fire");

    private final String name;

    GeyserType(final String name) {
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
