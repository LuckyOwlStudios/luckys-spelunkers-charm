package net.luckystudio.spelunkers_charm.entity.util;

import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.asm.enumextension.*;

import java.util.function.IntFunction;
import java.util.function.Supplier;

// Copied from the Boat Class in Minecraft, with modifications for a wood type enum.
// I just made my own for the sake of consistency with the rest of the mod.
// This class doesn't ask for if it's a raft or whatever, nor the stick item, and a couple of other things.
// We also added crimson and warped cause they weren't in the original enum
@NamedEnum(1)
@NetworkedEnum(NetworkedEnum.NetworkCheck.CLIENTBOUND)
public enum WoodType implements StringRepresentable {
    OAK(Blocks.OAK_PLANKS, "oak"),
    SPRUCE(Blocks.SPRUCE_PLANKS, "spruce"),
    BIRCH(Blocks.BIRCH_PLANKS, "birch"),
    JUNGLE(Blocks.JUNGLE_PLANKS, "jungle"),
    ACACIA(Blocks.ACACIA_PLANKS, "acacia"),
    CHERRY(Blocks.CHERRY_PLANKS, "cherry"),
    DARK_OAK(Blocks.DARK_OAK_PLANKS, "dark_oak"),
    MANGROVE(Blocks.MANGROVE_PLANKS, "mangrove"),
    BAMBOO(Blocks.BAMBOO_PLANKS, "bamboo"),
    CRIMSON(Blocks.CRIMSON_PLANKS, "crimson"),
    WARPED(Blocks.WARPED_PLANKS, "warped");

    private final String name;
    private final Supplier<Block> planksSupplier;
    final Supplier<Item> boatItem;
    public static final EnumCodec<WoodType> CODEC = StringRepresentable.fromEnum(WoodType::values);
    private static final IntFunction<WoodType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    @ReservedConstructor
    WoodType(Block planks, String name) {
        this.name = name;
        this.planksSupplier = () -> planks;
        this.boatItem = () -> Items.AIR;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    public Block getPlanks() {
        return this.planksSupplier.get();
    }

    public static WoodType getWoodType(ItemStack stack) {
        Item item = stack.getItem();
        for (WoodType type : values()) {
            if (item == type.getPlanks().asItem()) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Get a boat type by its enum ordinal
     */
    public static WoodType byId(int id) {
        return BY_ID.apply(id);
    }

    public static WoodType byName(String name) {
        return CODEC.byName(name, OAK);
    }
}
