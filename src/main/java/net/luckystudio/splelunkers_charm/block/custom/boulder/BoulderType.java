package net.luckystudio.splelunkers_charm.block.custom.boulder;

import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum BoulderType implements StringRepresentable {
    STONE("stone"),
    IRON("iron"),
    COPPER("stone"),
    GOLD("mossy"),
    LUSH("lush"),
    LUSH_IRON("lush_iron"),
    LUSH_COPPER("lush_copper"),
    LUSH_GOLD("lush_gold")
    ;

    private final String name;
    public static final EnumCodec<BoulderType> CODEC = StringRepresentable.fromEnum(BoulderType::values);
    private static final IntFunction<BoulderType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    BoulderType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static BoulderType byId(int id) {
        return BY_ID.apply(id);
    }

    public static BoulderType byName(String name) {
        return CODEC.byName(name, IRON);
    }
}
