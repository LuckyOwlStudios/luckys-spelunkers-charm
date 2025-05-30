package net.luckystudio.splelunkers_charm.entity.util;

import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum MovementDirection implements StringRepresentable {
    STATIONARY("stationary"),
    UP("up"),
    DOWN("down");

    private final String name;
    public static final EnumCodec<MovementDirection> CODEC = StringRepresentable.fromEnum(MovementDirection::values);
    private static final IntFunction<MovementDirection> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    MovementDirection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * Get a boat type by its enum ordinal
     */
    public static MovementDirection byId(int id) {
        return BY_ID.apply(id);
    }

    public static MovementDirection byName(String name) {
        return CODEC.byName(name, STATIONARY);
    }
}
