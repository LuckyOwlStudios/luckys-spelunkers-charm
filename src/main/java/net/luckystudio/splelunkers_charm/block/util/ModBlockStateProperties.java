package net.luckystudio.splelunkers_charm.block.util;

import net.luckystudio.splelunkers_charm.block.util.enums.BlockPart;
import net.luckystudio.splelunkers_charm.block.util.enums.GeyserState;
import net.luckystudio.splelunkers_charm.block.util.enums.GeyserType;
import net.luckystudio.splelunkers_charm.block.custom.boulder.HangingType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class ModBlockStateProperties {
    public static final IntegerProperty ROCKS = IntegerProperty.create("rocks", 1, 3);
    public static final EnumProperty<GeyserState> GEYSER_STATE = EnumProperty.create("state", GeyserState.class);
    public static final EnumProperty<GeyserType> GEYSER_TYPE = EnumProperty.create("type", GeyserType.class);
    public static final EnumProperty<BlockPart> BLOCK_PART = EnumProperty.create("block_part", BlockPart.class);
    public static final EnumProperty<HangingType> HANGING_TYPE = EnumProperty.create("hanging_type", HangingType.class);
    public static final BooleanProperty DISPLAY = BooleanProperty.create("display");
}
