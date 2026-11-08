package net.luckystudio.spelunkers_charm.block.custom.boulder;

import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

/**
 * Every boulder block. Each one is an {@link BoulderOre} with or without a lush (moss) shell.
 * <p>
 * New entries must be added at the end: the ordinal is synced to clients for falling boulders.
 * The block id is {@code <name>_boulder} (or just {@code boulder} for plain stone).
 */
public enum BoulderType implements StringRepresentable {
    STONE("stone", BoulderOre.NONE, false),
    IRON("iron", BoulderOre.IRON, false),
    COPPER("copper", BoulderOre.COPPER, false),
    GOLD("gold", BoulderOre.GOLD, false),
    LUSH("lush", BoulderOre.NONE, true),
    LUSH_IRON("lush_iron", BoulderOre.IRON, true),
    LUSH_COPPER("lush_copper", BoulderOre.COPPER, true),
    LUSH_GOLD("lush_gold", BoulderOre.GOLD, true),
    ZINC("zinc", BoulderOre.ZINC, false),
    LUSH_ZINC("lush_zinc", BoulderOre.ZINC, true),
    OSMIUM("osmium", BoulderOre.OSMIUM, false),
    LUSH_OSMIUM("lush_osmium", BoulderOre.OSMIUM, true),
    TIN("tin", BoulderOre.TIN, false),
    LUSH_TIN("lush_tin", BoulderOre.TIN, true),
    LEAD("lead", BoulderOre.LEAD, false),
    LUSH_LEAD("lush_lead", BoulderOre.LEAD, true);

    private final String name;
    private final BoulderOre ore;
    private final boolean lush;
    public static final EnumCodec<BoulderType> CODEC = StringRepresentable.fromEnum(BoulderType::values);
    private static final IntFunction<BoulderType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    BoulderType(String name, BoulderOre ore, boolean lush) {
        this.name = name;
        this.ore = ore;
        this.lush = lush;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public BoulderOre getOre() {
        return ore;
    }

    public boolean isLush() {
        return lush;
    }

    /** The registry name of this boulder's block, e.g. {@code lush_iron_boulder}. */
    public String getBlockName() {
        return this == STONE ? "boulder" : name + "_boulder";
    }

    public static BoulderType of(BoulderOre ore, boolean lush) {
        for (BoulderType type : values()) {
            if (type.ore == ore && type.lush == lush) return type;
        }
        return lush ? LUSH : STONE;
    }

    public static BoulderType byId(int id) {
        return BY_ID.apply(id);
    }

    public static BoulderType byName(String name) {
        return CODEC.byName(name, STONE);
    }
}
