package net.luckystudio.spelunkers_charm.mixin;


import net.luckystudio.spelunkers_charm.SpelunkersCharmConfig;
import net.luckystudio.spelunkers_charm.datagen.biomes.biomeTags.ModBiomeTags;
import net.luckystudio.spelunkers_charm.init.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// We need to fix the SurfaceSystem.class to allow for biome-specific default blocks
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    /**
     * Modifies the `blockstate` local variable inside `doFill`.
     * Inject point is just after:
     *     blockstate = this.settings.value().defaultBlock();
     */
    @ModifyVariable(method = "doFill", at = @At(value = "STORE", ordinal = 1), name = "blockstate")
    private BlockState changeDefaultStone(BlockState original, Blender blender, StructureManager structureManager, RandomState random, ChunkAccess chunk, int minCellY, int cellCountY) {
        if (!SpelunkersCharmConfig.STONE_REPLACERS.get()) return original;
        Holder<Biome> biomeHolder = chunk.getNoiseBiome(8, 64, 8);
//         Replace default block (stone) based on biome
//        if (original.is(Blocks.STONE)) {
//            if (biomeHolder.is(ModBiomeTags.IS_COLD_CAVE)) {
//                original = ModBlocks.PERMAFROST.get().defaultBlockState();
//            } else if (biomeHolder.is(ModBiomeTags.IS_HOT_CAVE)) {
//                original = ModBlocks.DUNESTONE.get().defaultBlockState();
//            } else if (biomeHolder.is(ModBiomeTags.IS_JUNGLE_CAVE)) {
//                original = ModBlocks.WILDSTONE.get().defaultBlockState();
//            }
//        }
        return original;
    }
}
