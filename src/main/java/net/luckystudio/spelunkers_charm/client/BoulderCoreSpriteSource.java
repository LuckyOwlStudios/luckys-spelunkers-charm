package net.luckystudio.spelunkers_charm.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Paints another mod's raw ore block texture into the cut face of our modded ore boulders.
 * <p>
 * For each sprite in {@code sprites}, the pixels marked in {@code mask} are replaced with the first texture in
 * {@code cores} that exists, tiled every 16 pixels. The texture is read from the other mod at load time, so
 * nothing from that mod is shipped in our jar. If none of the {@code cores} exist, the sprite is left as we
 * painted it.
 * <p>
 * Used from {@code assets/minecraft/atlases/blocks.json}.
 */
public record BoulderCoreSpriteSource(List<ResourceLocation> sprites, ResourceLocation mask, List<ResourceLocation> cores) implements SpriteSource {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final MapCodec<BoulderCoreSpriteSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.listOf().fieldOf("sprites").forGetter(BoulderCoreSpriteSource::sprites),
            ResourceLocation.CODEC.fieldOf("mask").forGetter(BoulderCoreSpriteSource::mask),
            ResourceLocation.CODEC.listOf().fieldOf("cores").forGetter(BoulderCoreSpriteSource::cores)
    ).apply(instance, BoulderCoreSpriteSource::new));

    /** Set when the type is registered in {@link net.luckystudio.spelunkers_charm.SpelunkersCharmClient}. */
    public static SpriteSourceType TYPE;

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        Optional<Resource> core = Optional.empty();
        for (ResourceLocation id : cores) {
            core = resourceManager.getResource(TEXTURE_ID_CONVERTER.idToFile(id));
            if (core.isPresent()) break;
        }
        if (core.isEmpty()) return;
        Optional<Resource> maskResource = resourceManager.getResource(TEXTURE_ID_CONVERTER.idToFile(mask));
        if (maskResource.isEmpty()) {
            LOGGER.warn("Missing boulder core mask {}", mask);
            return;
        }
        for (ResourceLocation sprite : sprites) {
            Optional<Resource> base = resourceManager.getResource(TEXTURE_ID_CONVERTER.idToFile(sprite));
            if (base.isEmpty()) {
                LOGGER.warn("Missing boulder texture {}", sprite);
                continue;
            }
            Resource baseResource = base.get();
            Resource coreResource = core.get();
            Resource maskRes = maskResource.get();
            output.add(sprite, loader -> {
                SpriteContents contents = compose(sprite, baseResource, maskRes, coreResource);
                // If anything went wrong, fall back to the texture as we painted it.
                return contents != null ? contents : loader.loadSprite(sprite, baseResource);
            });
        }
    }

    @Nullable
    private static SpriteContents compose(ResourceLocation sprite, Resource base, Resource mask, Resource core) {
        try {
            NativeImage image = read(base);
            try (NativeImage maskImage = read(mask); NativeImage coreImage = read(core)) {
                if (maskImage.getWidth() != image.getWidth() || maskImage.getHeight() != image.getHeight()) {
                    LOGGER.warn("Boulder core mask does not match the size of {}", sprite);
                    image.close();
                    return null;
                }
                // Raw block textures may be animated strips; only the first square frame is used.
                int coreSize = Math.min(coreImage.getWidth(), coreImage.getHeight());
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        if ((maskImage.getPixelRGBA(x, y) >>> 24) == 0) continue;
                        int u = (x % 16) * coreSize / 16;
                        int v = (y % 16) * coreSize / 16;
                        image.setPixelRGBA(x, y, coreImage.getPixelRGBA(u, v));
                    }
                }
            }
            return new SpriteContents(sprite, new FrameSize(image.getWidth(), image.getHeight()), image, ResourceMetadata.EMPTY);
        } catch (IOException e) {
            LOGGER.error("Could not build boulder texture {}", sprite, e);
            return null;
        }
    }

    private static NativeImage read(Resource resource) throws IOException {
        try (InputStream stream = resource.open()) {
            return NativeImage.read(stream);
        }
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }
}
