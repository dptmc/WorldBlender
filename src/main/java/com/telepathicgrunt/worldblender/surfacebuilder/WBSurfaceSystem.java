package com.telepathicgrunt.worldblender.surfacebuilder;

import com.google.common.collect.ImmutableList;
import com.telepathicgrunt.worldblender.WorldBlender;
import com.telepathicgrunt.worldblender.configs.WBDimensionConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.server.level.WorldGenRegion;

/**
 * Replaces vanilla surface generation with World Blender's blended bands of surface material.
 * Adapted from the 1.16.5 {@code BlendedSurfaceBuilder}.
 *
 * Performance notes:
 * <ul>
 *   <li>Band surfaces only scan near the surface instead of all the way to the bottom of the world.</li>
 *   <li>Whole-column surfaces (nether road / end borders) use section-local block access and skip empty
 *       sections entirely, which avoids the thousands of redundant lookups the naive column walk did.</li>
 *   <li>The perlin noise instance is cached per thread.</li>
 * </ul>
 */
public class WBSurfaceSystem {
    public static final WBSurfaceSystem INSTANCE = new WBSurfaceSystem();

    /** How far below the top surface a band surface is allowed to search for replaceable terrain. */
    private static final int MAX_BAND_SEARCH_DEPTH = 96;
    /** Vanilla's surface max depth is ~3.25; used as the whole-column band thickness. */
    private static final int BAND_DEPTH = 3;

    private static volatile SurfaceBlender blender;
    private static final ThreadLocal<PerlinNoiseCache> PERLIN = ThreadLocal.withInitial(PerlinNoiseCache::new);

    private static final class PerlinNoiseCache {
        PerlinSimplexNoise noise;
        long seed = Long.MIN_VALUE;
    }

    public static void save(SurfaceBlender surfaceBlender) {
        blender = surfaceBlender;
    }

    private static PerlinSimplexNoise perlinFor(long seed) {
        PerlinNoiseCache cache = PERLIN.get();
        if (cache.noise == null || cache.seed != seed) {
            cache.noise = new PerlinSimplexNoise(RandomSource.create(seed), ImmutableList.of(-1, 0));
            cache.seed = seed;
        }
        return cache.noise;
    }

    public void buildSurface(WorldGenRegion region, ChunkAccess chunk, BlockState defaultBlock) {
        SurfaceBlender currentBlender = blender;
        if (currentBlender == null || currentBlender.materials().isEmpty()) {
            WorldBlender.LOGGER.warn("WBSurfaceSystem not properly initialized!");
            return;
        }

        final PerlinSimplexNoise perlinGen = perlinFor(region.getSeed());
        final int minY = chunk.getMinBuildHeight();
        final int maxY = chunk.getMaxBuildHeight();
        final int seaLevel = region.getSeaLevel();
        final int chunkOriginX = chunk.getPos().getMinBlockX();
        final int chunkOriginZ = chunk.getPos().getMinBlockZ();
        final Block defaultBlockType = defaultBlock.getBlock();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final LevelChunkSection[] sections = chunk.getSections();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkOriginX + x;
                int worldZ = chunkOriginZ + z;
                SurfaceBlender.SurfaceMaterial chosen = weightedRandomSurface(currentBlender, perlinGen, worldX, worldZ);
                RandomSource random = RandomSource.create(region.getSeed() + (long) worldX * 341873128712L + (long) worldZ * 132897987541L);

                int startHeight = Math.min(chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), maxY - 1);
                if (startHeight <= minY) {
                    startHeight = maxY - 1;
                }

                if (chosen.wholeColumn()) {
                    placeWholeColumn(chunk, sections, x, z, worldX, worldZ, startHeight, minY, defaultBlockType, chosen.bottom());
                }
                else {
                    placeSurfaceBand(chunk, pos, random, chosen, defaultBlock, x, z, worldX, worldZ, startHeight, minY, seaLevel);
                }
            }
        }
    }

    /** Replaces every default-block block in the column with the surface's bottom block, skipping empty sections. */
    private void placeWholeColumn(ChunkAccess chunk, LevelChunkSection[] sections, int x, int z, int worldX, int worldZ,
                                  int startHeight, int minY, Block defaultBlockType, BlockState replacement) {
        int topSection = chunk.getSectionIndex(startHeight);
        for (int sectionIndex = topSection; sectionIndex >= 0; sectionIndex--) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            if (!section.maybeHas(state -> state.getBlock() == defaultBlockType)) {
                continue;
            }

            int sectionBottomY = minY + (sectionIndex << 4);
            int yStart = Math.min(15, startHeight - sectionBottomY);
            for (int y = yStart; y >= 0; y--) {
                if (section.getBlockState(x, y, z).getBlock() == defaultBlockType) {
                    section.setBlockState(x, y, z, replacement, false);
                }
            }
        }
    }

    /** Paints a thin band of surface material around the top of the column. */
    private void placeSurfaceBand(ChunkAccess chunk, BlockPos.MutableBlockPos pos, RandomSource random,
                                  SurfaceBlender.SurfaceMaterial chosen, BlockState defaultBlock,
                                  int x, int z, int worldX, int worldZ, int startHeight, int minY, int seaLevel) {
        final int searchBottom = Math.max(minY, startHeight - MAX_BAND_SEARCH_DEPTH);
        BlockState activeBlock = chosen.middle();
        int depth = -1;

        for (int y = startHeight; y >= searchBottom; --y) {
            pos.set(worldX, y, worldZ);
            BlockState currentBlock = chunk.getBlockState(pos);

            if (currentBlock.isAir()) {
                depth = -1;
                continue;
            }
            if (!isReplaceableTerrain(currentBlock, defaultBlock)) {
                continue;
            }

            final BlockState toPlace;
            if (depth == -1) {
                depth = (int) (0.3D + BAND_DEPTH + random.nextDouble() * 0.25D);

                BlockState topLayer = chosen.top();
                if (depth <= 0) {
                    topLayer = Blocks.AIR.defaultBlockState();
                    activeBlock = defaultBlock;
                }
                else if (y >= seaLevel - 4 && y <= seaLevel + 1) {
                    activeBlock = chosen.middle();
                }

                if (y < seaLevel && topLayer.isAir()) {
                    topLayer = defaultBlock; // let the world fill water later; we do not place water here
                }

                if (y >= seaLevel - 1) {
                    toPlace = topLayer;
                }
                else if (y < seaLevel - 7 - depth) {
                    activeBlock = defaultBlock;
                    toPlace = chosen.bottom();
                }
                else {
                    toPlace = activeBlock;
                }
            }
            else if (depth > 0) {
                --depth;
                toPlace = activeBlock;

                if (depth == 0 && (activeBlock.getBlock() == Blocks.SAND || activeBlock.getBlock() == Blocks.RED_SAND)) {
                    depth = random.nextInt(4) + Math.max(0, y - 63);
                    activeBlock = activeBlock.getBlock() == Blocks.RED_SAND
                            ? Blocks.RED_SANDSTONE.defaultBlockState()
                            : Blocks.SANDSTONE.defaultBlockState();
                }
            }
            else {
                // Once the band is done and we're in plain stone we are finished for this column.
                break;
            }

            chunk.setBlockState(pos, toPlace, false);
        }
    }

    private static boolean isReplaceableTerrain(BlockState state, BlockState defaultBlock) {
        Block block = state.getBlock();
        return block == defaultBlock.getBlock() || block == Blocks.DEEPSLATE || block == Blocks.STONE;
    }

    private static SurfaceBlender.SurfaceMaterial weightedRandomSurface(SurfaceBlender currentBlender, PerlinSimplexNoise perlinGen, int x, int z) {
        int chosenIndex = 2; // grass surface
        double noiseScale = WBDimensionConfigs.surfaceScale.get();
        var materials = currentBlender.materials();

        for (int configIndex = 0; configIndex < materials.size(); configIndex++) {
            if (configIndex == 0) {
                if (Math.abs(perlinGen.getValue(x / noiseScale, z / noiseScale, true)) < 0.035D) {
                    chosenIndex = 0;
                    break;
                }
            }
            else if (configIndex == 1) {
                if (Math.abs(perlinGen.getValue(x / noiseScale, z / noiseScale, true)) < 0.06D) {
                    chosenIndex = 1;
                    break;
                }
            }
            else {
                double offset = 200D * configIndex;
                double scaling = 200D + configIndex * 4D;
                double threshold = currentBlender.baseScale() + Math.min(configIndex / 150D, 0.125D);
                if (Math.abs(perlinGen.getValue((x + offset) / scaling, (z + offset) / scaling, true)) < threshold) {
                    chosenIndex = configIndex;
                    break;
                }
            }
        }

        int index = Math.min(chosenIndex, materials.size() - 1);
        return materials.get(index);
    }
}
