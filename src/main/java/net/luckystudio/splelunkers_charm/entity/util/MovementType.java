package net.luckystudio.splelunkers_charm.entity.util;

import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum MovementType implements StringRepresentable {
    STATIONARY("stationary"),
    UP("up"),
    DOWN("down"),
    FALLING("falling");

    private final String name;
    public static final EnumCodec<MovementType> CODEC = StringRepresentable.fromEnum(MovementType::values);
    private static final IntFunction<MovementType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    MovementType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * Get a boat type by its enum ordinal
     */
    public static MovementType byId(int id) {
        return BY_ID.apply(id);
    }

    public static MovementType byName(String name) {
        return CODEC.byName(name, STATIONARY);
    }
}
