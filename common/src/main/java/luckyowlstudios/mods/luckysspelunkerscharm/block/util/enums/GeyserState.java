package luckyowlstudios.mods.luckysspelunkerscharm.block.util.enums;

import net.minecraft.util.StringRepresentable;

public enum GeyserState implements StringRepresentable {
    DORMANT("dormant"),
    CHARGING("charging"),
    ACTIVE("active");

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
