package net.luckystudio.splelunkers_charm.block.util;

import net.luckystudio.splelunkers_charm.block.util.enums.GeyserState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class ModBlockStateProperties {
    public static final EnumProperty<GeyserState> GEYSER_STATE = EnumProperty.create("state", GeyserState.class);
}
