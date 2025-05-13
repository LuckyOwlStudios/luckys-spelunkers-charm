package luckyowlstudios.mods.luckysspelunkerscharm.block.util;

import net.minecraft.world.level.block.state.properties.EnumProperty;
import luckyowlstudios.mods.luckysspelunkerscharm.block.util.enums.GeyserState;
import luckyowlstudios.mods.luckysspelunkerscharm.block.util.enums.GeyserType;

public class ModBlockStateProperties {
    public static final EnumProperty<GeyserState> GEYSER_STATE = EnumProperty.create("state", GeyserState.class);
    public static final EnumProperty<GeyserType> GEYSER_TYPE = EnumProperty.create("type", GeyserType.class);
}
