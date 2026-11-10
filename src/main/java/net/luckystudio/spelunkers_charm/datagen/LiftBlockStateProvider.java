package net.luckystudio.spelunkers_charm.datagen;

import net.luckystudio.spelunkers_charm.SpelunkersCharm;
import net.luckystudio.spelunkers_charm.block.custom.DirectionalLiftTrackBlock;
import net.luckystudio.spelunkers_charm.block.custom.LiftTrackBlock;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Static rack models; a station's display is independent of its command pulse. */
public final class LiftBlockStateProvider extends BlockStateProvider {
    public LiftBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, SpelunkersCharm.MOD_ID, helper);
    }

    @Override
    public String getName() {
        return "Elevator block states and models";
    }

    @Override
    protected void registerStatesAndModels() {
        basicTrack();
        motorTrack();
        directionalTrack();
        detectorTrack();
    }

    private ModelFile rackModel(String name, String front, String side, String end) {
        return models().withExistingParent(name, modLoc("block/template_lift_track"))
                .texture("front", modLoc("block/" + front))
                .texture("side", modLoc("block/" + side))
                .texture("end", modLoc("block/" + end))
                .texture("particle", modLoc("block/" + front))
                .renderType("cutout");
    }

    private void basicTrack() {
        Block block = ModBlocks.BASIC_LIFT_TRACK.get();
        ModelFile model =
                rackModel(
                        "basic_lift_track",
                        "wooden_lift_track_front",
                        "wooden_lift_track_side",
                        "wooden_lift_track_end");
        getVariantBuilder(block)
                .forAllStatesExcept(
                        state -> rotated(model, state.getValue(LiftTrackBlock.FACING)),
                        LiftTrackBlock.POWERED,
                        LiftTrackBlock.LIFT_PRESENT);
        itemModels().simpleBlockItem(block);
    }

    private void motorTrack() {
        Block block = ModBlocks.POWERED_LIFT_TRACK.get();
        ModelFile idle =
                rackModel(
                        "powered_lift_track",
                        "powered_lift_track_front",
                        "powered_lift_track_side",
                        "powered_lift_track_end");
        ModelFile powered =
                rackModel(
                        "powered_lift_track_powered",
                        "powered_lift_track_front_powered",
                        "powered_lift_track_side",
                        "powered_lift_track_end_powered");
        getVariantBuilder(block)
                .forAllStatesExcept(
                        state ->
                                rotated(
                                        state.getValue(LiftTrackBlock.POWERED) ? powered : idle,
                                        state.getValue(LiftTrackBlock.FACING)),
                        LiftTrackBlock.LIFT_PRESENT);
        itemModels().simpleBlockItem(block);
    }

    private void directionalTrack() {
        Block block = ModBlocks.DIRECTIONAL_LIFT_TRACK.get();
        ModelFile up = directionalModel("up", false);
        ModelFile upLit = directionalModel("up", true);
        ModelFile down = directionalModel("down", false);
        ModelFile downLit = directionalModel("down", true);
        getVariantBuilder(block)
                .forAllStatesExcept(
                        state -> {
                            boolean lit = state.getValue(DirectionalLiftTrackBlock.LIT);
                            boolean upward =
                                    state.getValue(DirectionalLiftTrackBlock.VERTICAL_DIRECTION)
                                            == Direction.UP;
                            return rotated(
                                    upward ? (lit ? upLit : up) : (lit ? downLit : down),
                                    state.getValue(LiftTrackBlock.FACING));
                        },
                        LiftTrackBlock.POWERED,
                        LiftTrackBlock.LIFT_PRESENT,
                        DirectionalLiftTrackBlock.SIGNAL_HELD);
        itemModels()
                .withExistingParent(
                        "directional_lift_track", modLoc("block/directional_lift_track_up"));
    }

    private ModelFile directionalModel(String direction, boolean lit) {
        String suffix = lit ? "_powered" : "";
        return rackModel(
                "directional_lift_track_" + direction + suffix,
                direction + "_powered_lift_track_front" + suffix,
                direction + "_powered_lift_track_side" + suffix,
                "directional_powered_lift_track_end" + suffix);
    }

    private void detectorTrack() {
        Block block = ModBlocks.DETECTOR_LIFT_TRACK.get();
        ModelFile idle =
                rackModel(
                        "detector_lift_track",
                        "detector_lift_track_front",
                        "powered_lift_track_side",
                        "powered_lift_track_end");
        ModelFile occupied =
                rackModel(
                        "powered_detector_lift_track",
                        "powered_detector_lift_track_front",
                        "powered_lift_track_side",
                        "powered_lift_track_end_powered");
        getVariantBuilder(block)
                .forAllStatesExcept(
                        state ->
                                rotated(
                                        state.getValue(LiftTrackBlock.POWERED) ? occupied : idle,
                                        state.getValue(LiftTrackBlock.FACING)),
                        LiftTrackBlock.LIFT_PRESENT);
        itemModels().simpleBlockItem(block);
    }

    private ConfiguredModel[] rotated(ModelFile model, Direction facing) {
        return ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(((int) facing.toYRot() + 180) % 360)
                .build();
    }
}
