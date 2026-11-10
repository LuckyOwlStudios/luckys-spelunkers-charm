package net.luckystudio.spelunkers_charm.entity.custom.minecart.link;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** A lantern hung on a minecart. Saved on the cart as a data attachment. */
public enum CartLantern implements StringRepresentable {
    NONE("none", 0),
    LANTERN("lantern", 15),
    SOUL_LANTERN("soul_lantern", 10);

    public static final Codec<CartLantern> CODEC = StringRepresentable.fromEnum(CartLantern::values);

    private final String name;
    private final int lightLevel;

    CartLantern(String name, int lightLevel) {
        this.name = name;
        this.lightLevel = lightLevel;
    }

    public int lightLevel() {
        return this.lightLevel;
    }

    public Block block() {
        return switch (this) {
            case LANTERN -> Blocks.LANTERN;
            case SOUL_LANTERN -> Blocks.SOUL_LANTERN;
            case NONE -> Blocks.AIR;
        };
    }

    public ItemStack toStack() {
        return switch (this) {
            case LANTERN -> new ItemStack(Items.LANTERN);
            case SOUL_LANTERN -> new ItemStack(Items.SOUL_LANTERN);
            case NONE -> ItemStack.EMPTY;
        };
    }

    public static CartLantern fromStack(ItemStack stack) {
        if (stack.is(Items.LANTERN)) return LANTERN;
        if (stack.is(Items.SOUL_LANTERN)) return SOUL_LANTERN;
        return NONE;
    }

    public static CartLantern byId(int id) {
        CartLantern[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
