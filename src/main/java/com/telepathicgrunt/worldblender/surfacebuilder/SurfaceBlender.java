package com.telepathicgrunt.worldblender.surfacebuilder;

import com.telepathicgrunt.worldblender.configs.WBBlendingConfigs;
import com.telepathicgrunt.worldblender.theblender.ConfigBlacklisting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds the surface materials that World Blender blends together.
 *
 * 1.16.5 collected a surface config from every biome. 1.20.1 removed per-biome surface builders entirely
 * (surfaces now live in the dimension's global {@code surface_rule}), so this list is a curated set that
 * matches the original mod's look. See HISTORY.md "Known gaps".
 */
public class SurfaceBlender {
    /** A single blended surface: the very top block, the block under it, and the deep/underwater block. */
    public record SurfaceMaterial(BlockState top, BlockState middle, BlockState bottom, boolean wholeColumn) {}

    private final List<SurfaceMaterial> surfaces = new ArrayList<>();
    private final double baseScale;

    public SurfaceBlender() {
        // Mutable is on purpose; surface block blacklist can remove entries.
        List<SurfaceMaterial> defaults = new ArrayList<>();
        defaults.add(column(Blocks.NETHERRACK));    // nether road
        defaults.add(column(Blocks.END_STONE));     // end borders
        if (WBBlendingConfigs.allowVanillaSurfaces.get() && WBBlendingConfigs.allowVanillaBiomeImport.get()) {
            defaults.add(surface(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.GRAVEL));
            defaults.add(surface(Blocks.PODZOL, Blocks.DIRT, Blocks.GRAVEL));
            defaults.add(surface(Blocks.RED_SAND, Blocks.WHITE_TERRACOTTA, Blocks.GRAVEL));
            defaults.add(surface(Blocks.SAND, Blocks.SAND, Blocks.SANDSTONE));
            defaults.add(surface(Blocks.MYCELIUM, Blocks.DIRT, Blocks.GRAVEL));
            defaults.add(surface(Blocks.SNOW_BLOCK, Blocks.DIRT, Blocks.GRAVEL));
            defaults.add(surface(Blocks.COARSE_DIRT, Blocks.DIRT, Blocks.GRAVEL));
            defaults.add(surface(Blocks.ANDESITE, Blocks.STONE, Blocks.STONE));
            defaults.add(surface(Blocks.GRAVEL, Blocks.GRAVEL, Blocks.STONE));
        }

        // remove the surfaces that we disallow through blacklist but keep nether/end road
        for (int i = defaults.size() - 1; i > 1; i--) {
            SurfaceMaterial material = defaults.get(i);
            Block topBlock = material.top().getBlock();
            boolean isBlacklisted = ConfigBlacklisting.isResourceLocationBlacklisted(
                    ConfigBlacklisting.BlacklistType.SURFACE_BLOCK,
                    BuiltInRegistries.BLOCK.getKey(topBlock)
            );
            if (!isBlacklisted) {
                this.surfaces.add(0, material);
            }
        }
        // re-add nether/end at the front in the right order
        this.surfaces.add(0, defaults.get(1));
        this.surfaces.add(0, defaults.get(0));

        baseScale = 0.6D / this.surfaces.size();
    }

    private static SurfaceMaterial surface(Block top, Block middle, Block bottom) {
        return new SurfaceMaterial(top.defaultBlockState(), middle.defaultBlockState(), bottom.defaultBlockState(), false);
    }

    private static SurfaceMaterial column(Block block) {
        BlockState state = block.defaultBlockState();
        return new SurfaceMaterial(state, state, state, true);
    }

    public List<SurfaceMaterial> materials() {
        return this.surfaces;
    }

    public double baseScale() {
        return this.baseScale;
    }

    public void addIfMissing(SurfaceMaterial material) {
        boolean alreadyPresent = this.surfaces.stream().anyMatch(existing ->
                existing.top() == material.top() && existing.middle() == material.middle() && existing.bottom() == material.bottom());
        if (!alreadyPresent) {
            this.surfaces.add(material);
        }
    }

    // Returns what carvers should carve through so they don't get cut off by unique blocks added to the surface.
    public Set<Block> blocksToCarve() {
        Set<Block> carvableBlocks = new HashSet<>();
        carvableBlocks.add(Blocks.NETHERRACK);
        carvableBlocks.add(Blocks.END_STONE);

        for (SurfaceMaterial surface : this.surfaces) {
            Block bottomBlock = surface.bottom().getBlock();
            if (!BuiltInRegistries.BLOCK.getKey(bottomBlock).getNamespace().equals("minecraft")) {
                carvableBlocks.add(bottomBlock);
            }
        }
        return carvableBlocks;
    }
}
