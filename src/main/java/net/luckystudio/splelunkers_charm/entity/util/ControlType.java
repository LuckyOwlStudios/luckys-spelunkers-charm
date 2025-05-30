package net.luckystudio.splelunkers_charm.entity.util;

import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum ControlType implements StringRepresentable {
    AUTOMATIC("automatic"),
    LEVER("lever"),
    PRESSURE_PLATE("pressure_plate"),;

    private final String name;
    public static final EnumCodec<ControlType> CODEC = StringRepresentable.fromEnum(ControlType::values);
    private static final IntFunction<ControlType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    ControlType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * Get a boat type by its enum ordinal
     */
    public static ControlType byId(int id) {
        return BY_ID.apply(id);
    }

    public static ControlType byName(String name) {
        return CODEC.byName(name, AUTOMATIC);
    }
}
