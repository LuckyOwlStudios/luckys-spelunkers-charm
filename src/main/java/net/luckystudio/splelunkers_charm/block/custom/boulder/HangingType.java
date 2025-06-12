package net.luckystudio.splelunkers_charm.block.custom.boulder;

import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum HangingType implements StringRepresentable {
    NONE("none"),
    TIED("rope"),
    CHAINED("chain")
    ;

    private final String name;
    public static final EnumCodec<HangingType> CODEC = StringRepresentable.fromEnum(HangingType::values);
    private static final IntFunction<HangingType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    HangingType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static HangingType byId(int id) {
        return BY_ID.apply(id);
    }

    public static HangingType byName(String name) {
        return CODEC.byName(name, NONE);
    }
}
