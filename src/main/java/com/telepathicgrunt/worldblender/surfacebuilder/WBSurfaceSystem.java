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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.server.level.WorldGenRegion;

/**
 * Replaces vanilla surface generation with World Blender's blended bands of surface material.
 * Adapted from the 1.16.5 {@code BlendedSurfaceBuilder}.
 */
public class WBSurfaceSystem {
    public static final WBSurfaceSystem INSTANCE = new WBSurfaceSystem();

    private static SurfaceBlender blender;
    private PerlinSimplexNoise perlinGen;
    private long perlinSeed = Long.MIN_VALUE;

    public static void save(SurfaceBlender surfaceBlender) {
        blender = surfaceBlender;
    }

    private void setPerlinSeed(long seed) {
        if (perlinGen == null || perlinSeed != seed) {
            perlinGen = new PerlinSimplexNoise(RandomSource.create(seed), ImmutableList.of(-1, 0));
            perlinSeed = seed;
        }
    }

    public void buildSurface(WorldGenRegion region, ChunkAccess chunk, BlockState defaultBlock) {
        if (blender == null || blender.materials().isEmpty()) {
            WorldBlender.LOGGER.warn("WBSurfaceSystem not properly initialized!");
            return;
        }

        setPerlinSeed(region.getSeed());

        final int minY = chunk.getMinBuildHeight();
        final int seaLevel = region.getSeaLevel();
        final int chunkOriginX = chunk.getPos().getMinBlockX();
        final int chunkOriginZ = chunk.getPos().getMinBlockZ();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkOriginX + x;
                int worldZ = chunkOriginZ + z;
                SurfaceBlender.SurfaceMaterial chosen = weightedRandomSurface(worldX, worldZ);
                RandomSource random = RandomSource.create(region.getSeed() + (long) worldX * 341873128712L + (long) worldZ * 132897987541L);

                int startHeight = Math.min(chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), chunk.getMaxBuildHeight() - 1);
                if (startHeight <= minY) {
                    startHeight = chunk.getMaxBuildHeight();
                }

                BlockState activeBlock = chosen.middle();
                int depth = -1;

                for (int y = startHeight; y >= minY; --y) {
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
                        depth = (int) (0.3D + 3.0D + random.nextDouble() * 0.25D); // mirrors vanilla's maxDepth noise term

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
                    else if (chosen.wholeColumn()) {
                        toPlace = chosen.bottom();
                    }
                    else {
                        continue;
                    }

                    chunk.setBlockState(pos, toPlace, false);
                }
            }
        }
    }

    private boolean isReplaceableTerrain(BlockState state, BlockState defaultBlock) {
        Block block = state.getBlock();
        return block == defaultBlock.getBlock() || block == Blocks.DEEPSLATE || block == Blocks.STONE;
    }

    private SurfaceBlender.SurfaceMaterial weightedRandomSurface(int x, int z) {
        int chosenIndex = 2; // grass surface
        double noiseScale = WBDimensionConfigs.surfaceScale.get();
        var materials = blender.materials();

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
                double threshold = blender.baseScale() + Math.min(configIndex / 150D, 0.125D);
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
