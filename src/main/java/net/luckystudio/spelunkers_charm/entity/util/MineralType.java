package net.luckystudio.spelunkers_charm.entity.util;

import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.common.asm.enumextension.*;

import java.util.function.IntFunction;
import java.util.function.Supplier;

@NamedEnum(1)
@NetworkedEnum(NetworkedEnum.NetworkCheck.CLIENTBOUND)
public enum MineralType implements StringRepresentable, IExtensibleEnum {
    IRON(Items.IRON_INGOT, "iron"),
    COPPER(Items.COPPER_INGOT, "copper"),
    GOLD(Items.GOLD_INGOT, "gold"),
    DIAMOND(Items.DIAMOND, "diamond"),
    NETHERITE(Items.NETHERITE_INGOT, "netherite");

    private final String name;
    private final Supplier<Item> mineralSupplier;
    final Supplier<Item> entityItem;
    public static final EnumCodec<MineralType> CODEC = StringRepresentable.fromEnum(MineralType::values);
    private static final IntFunction<MineralType> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    @ReservedConstructor
    MineralType(Item mineral, String name) {
        this.name = name;
        this.mineralSupplier = () -> mineral;
        this.entityItem = () -> Items.AIR;
    }

    /**
     * @param planks A supplier of the block to be dropped when the boat is destroyed by fall damage
     * @param name The name of this boat type
     * @param boatItem A supplier of the item to be dropped when a normal boat or raft of this type is picked up
     */
    private MineralType(
            Supplier<Item> planks,
            String name,
            Supplier<Item> boatItem
    ) {
        this.name = name;
        this.mineralSupplier = planks;
        this.entityItem = boatItem;
    }

    public static MineralType getMineralType(ItemStack stack) {
        Item item = stack.getItem();
        for (MineralType type : values()) {
            if (item == type.getMineral()) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    public Item getMineral() {
        return this.mineralSupplier.get();
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Get a boat type by its enum ordinal
     */
    public static MineralType byId(int id) {
        return BY_ID.apply(id);
    }

    public static MineralType byName(String name) {
        return CODEC.byName(name, IRON);
    }

    public static ExtensionInfo getExtensionInfo() {
        return ExtensionInfo.nonExtended(MineralType.class);
    }
}
