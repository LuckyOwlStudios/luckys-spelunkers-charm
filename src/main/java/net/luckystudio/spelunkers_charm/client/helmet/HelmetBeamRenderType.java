package net.luckystudio.spelunkers_charm.client.helmet;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Render type for the mining helmet's light beam: untextured, additive (it only ever brightens what's behind it,
 * like light in the air), double-sided, depth-tested against the world but not writing depth, so the beam
 * never hides anything and is hidden by whatever is in front of it.
 */
public final class HelmetBeamRenderType extends RenderType {

    public static final RenderType BEAM = create(
            "spelunkers_charm_helmet_beam",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            8192,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setOutputState(WEATHER_TARGET)
                    .createCompositeState(false));

    // Never constructed: extending RenderType is only to reach its protected render state shards.
    private HelmetBeamRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                 boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
