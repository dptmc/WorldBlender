package com.telepathicgrunt.worldblender.features;

import com.telepathicgrunt.worldblender.configs.WBDimensionConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Map;


public class AntiFloatingBlocksAndSeparateLiquids extends Feature<NoneFeatureConfiguration>
{
	public AntiFloatingBlocksAndSeparateLiquids()
	{
		super(NoneFeatureConfiguration.CODEC);
	}

	private static final Map<MapColor, Block> COLOR_MAP;
	static {
		COLOR_MAP = new HashMap<>();
		COLOR_MAP.put(MapColor.NONE, Blocks.CYAN_TERRACOTTA);
		COLOR_MAP.put(MapColor.GRASS, Blocks.GREEN_TERRACOTTA);
		COLOR_MAP.put(MapColor.SAND, Blocks.WHITE_TERRACOTTA);
		COLOR_MAP.put(MapColor.WOOL, Blocks.WHITE_TERRACOTTA);
		COLOR_MAP.put(MapColor.FIRE, Blocks.RED_TERRACOTTA);
		COLOR_MAP.put(MapColor.ICE, Blocks.LIGHT_BLUE_TERRACOTTA);
		COLOR_MAP.put(MapColor.METAL, Blocks.WHITE_TERRACOTTA);
		COLOR_MAP.put(MapColor.PLANT, Blocks.GREEN_TERRACOTTA);
		COLOR_MAP.put(MapColor.SNOW, Blocks.WHITE_TERRACOTTA);
		COLOR_MAP.put(MapColor.CLAY, Blocks.CYAN_TERRACOTTA);
		COLOR_MAP.put(MapColor.DIRT, Blocks.BROWN_TERRACOTTA);
		COLOR_MAP.put(MapColor.STONE, Blocks.CYAN_TERRACOTTA);
		COLOR_MAP.put(MapColor.WATER, Blocks.LIGHT_BLUE_TERRACOTTA);
		COLOR_MAP.put(MapColor.WOOD, Blocks.TERRACOTTA);
		COLOR_MAP.put(MapColor.QUARTZ, Blocks.WHITE_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_ORANGE, Blocks.ORANGE_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_MAGENTA, Blocks.MAGENTA_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_LIGHT_BLUE, Blocks.LIGHT_BLUE_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_YELLOW, Blocks.YELLOW_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_LIGHT_GREEN, Blocks.LIME_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_PINK, Blocks.PINK_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_GRAY, Blocks.GRAY_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_LIGHT_GRAY, Blocks.LIGHT_GRAY_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_CYAN, Blocks.CYAN_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_PURPLE, Blocks.PURPLE_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_BLUE, Blocks.BLUE_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_BROWN, Blocks.BROWN_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_GREEN, Blocks.GREEN_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_RED, Blocks.RED_TERRACOTTA);
		COLOR_MAP.put(MapColor.COLOR_BLACK, Blocks.BLACK_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_WHITE, Blocks.WHITE_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_ORANGE, Blocks.ORANGE_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_MAGENTA, Blocks.MAGENTA_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_LIGHT_BLUE, Blocks.LIGHT_BLUE_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_YELLOW, Blocks.YELLOW_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_LIGHT_GREEN, Blocks.LIME_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_PINK, Blocks.PINK_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_GRAY, Blocks.GRAY_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_LIGHT_GRAY, Blocks.LIGHT_GRAY_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_CYAN, Blocks.CYAN_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_PURPLE, Blocks.PURPLE_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_BLUE, Blocks.BLUE_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_BROWN, Blocks.BROWN_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_GREEN, Blocks.GREEN_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_RED, Blocks.RED_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_BLACK, Blocks.BLACK_TERRACOTTA);
		COLOR_MAP.put(MapColor.CRIMSON_STEM, Blocks.RED_TERRACOTTA);
		COLOR_MAP.put(MapColor.WARPED_STEM, Blocks.CYAN_TERRACOTTA);
		COLOR_MAP.put(MapColor.DEEPSLATE, Blocks.GRAY_TERRACOTTA);
		COLOR_MAP.put(MapColor.CRIMSON_HYPHAE, Blocks.RED_TERRACOTTA);
		COLOR_MAP.put(MapColor.WARPED_HYPHAE, Blocks.CYAN_TERRACOTTA);
		COLOR_MAP.put(MapColor.NETHER, Blocks.RED_TERRACOTTA);
		COLOR_MAP.put(MapColor.TERRACOTTA_RED, Blocks.RED_TERRACOTTA);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
	{
		//this feature is completely turned off.
		if(!WBDimensionConfigs.preventFallingBlocks.get() &&
			!WBDimensionConfigs.containFloatingLiquids.get() &&
			!WBDimensionConfigs.preventLavaTouchingWater.get())
		{
			return false;
		}

		WorldGenLevel level = context.level();
		BlockPos origin = context.origin();
		ChunkAccess cachedChunk = level.getChunk(origin);

		final int chunkOriginX = cachedChunk.getPos().getMinBlockX();
		final int chunkOriginZ = cachedChunk.getPos().getMinBlockZ();
		final int minY = level.getMinBuildHeight();
		final int seaLevel = level.getSeaLevel();
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				boolean setblock = false;
				int worldX = chunkOriginX + x;
				int worldZ = chunkOriginZ + z;

				int maxHeight = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ), seaLevel);
				maxHeight = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX + 1, worldZ), maxHeight);
				maxHeight = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ + 1), maxHeight);
				maxHeight = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX - 1, worldZ), maxHeight);
				maxHeight = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ - 1), maxHeight);

				mutable.set(worldX, Math.min(maxHeight, level.getMaxBuildHeight() - 1), worldZ);
				BlockState lastBlockstate = Blocks.STONE.defaultBlockState();

				//checks the column downward
				for(; mutable.getY() >= minY; mutable.move(Direction.DOWN)) {
					BlockState currentBlockstate = getStateAt(level, cachedChunk, mutable);

					// current block is a lava-tagged fluid
					if (WBDimensionConfigs.preventLavaTouchingWater.get() &&
							currentBlockstate.getFluidState().is(FluidTags.LAVA))
					{
						for (Direction face : Direction.values()) {
							mutable.move(face);
							BlockState neighboringBlockstate = getStateAt(level, cachedChunk, mutable);
							mutable.move(face.getOpposite());

							if (neighboringBlockstate.getFluidState().is(FluidTags.WATER)) {
								level.setBlock(mutable, Blocks.OBSIDIAN.defaultBlockState(), 2);
								setblock = true;
								break;
							}
						}
					}

					if(!setblock){
						if(isReplaceable(currentBlockstate)) {
							setblock = preventfalling(level, cachedChunk, mutable, lastBlockstate, currentBlockstate);
							if(!setblock){
								liquidContaining(level, cachedChunk, mutable, lastBlockstate, currentBlockstate);
							}
						}
						else if(!currentBlockstate.isSolid() && !currentBlockstate.getFluidState().isEmpty()) {
							preventfalling(level, cachedChunk, mutable, lastBlockstate, currentBlockstate);
						}
					}

					lastBlockstate = currentBlockstate;
				}
			}
		}

		return true;
	}

	private static BlockState getStateAt(WorldGenLevel level, ChunkAccess cachedChunk, BlockPos pos) {
		if (cachedChunk.getPos().x == (pos.getX() >> 4) && cachedChunk.getPos().z == (pos.getZ() >> 4)) {
			return cachedChunk.getBlockState(pos);
		}
		return level.getBlockState(pos);
	}

	private static boolean isReplaceable(BlockState state) {
		return state.isAir() || state.canBeReplaced();
	}

	/**
	 * Will place a Terracotta block at the mutable position if the above block is a FallingBlock
	 */
	private static boolean preventfalling(WorldGenLevel level, ChunkAccess cachedChunk, BlockPos.MutableBlockPos mutable, BlockState lastBlockstate, BlockState currentBlockstate)
	{
		if(!WBDimensionConfigs.preventFallingBlocks.get()) return false;

		if(lastBlockstate.getBlock() instanceof FallingBlock) {
			setReplacementBlock(level, cachedChunk, mutable, lastBlockstate, currentBlockstate);
			return true;
		}
		return false;
	}

	/**
	 * Will place a Terracotta block at the mutable position if above, north, west, east, or south is a liquid block
	 */
	private static boolean liquidContaining(WorldGenLevel level, ChunkAccess cachedChunk, BlockPos.MutableBlockPos mutable, BlockState lastBlockstate, BlockState currentBlockstate)
	{
		if(!WBDimensionConfigs.containFloatingLiquids.get()) return false;

		boolean touchingLiquid = false;
		BlockState neighboringBlockstate = null;

		//if above is liquid, we need to contain it
		if(!lastBlockstate.getFluidState().isEmpty()) {
			touchingLiquid = true;
			neighboringBlockstate = lastBlockstate;
		}
		//if side is liquid, we need to contain it
		else {
			for(Direction face : Direction.Plane.HORIZONTAL) {
				mutable.move(face);
				neighboringBlockstate = getStateAt(level, cachedChunk, mutable);
				mutable.move(face.getOpposite());

				if(!neighboringBlockstate.getFluidState().isEmpty()) {
					touchingLiquid = true;
					break;
				}
			}
		}

		if(touchingLiquid) {
			setReplacementBlock(level, cachedChunk, mutable, neighboringBlockstate, currentBlockstate);
			return true;
		}
		return false;
	}

	private static void setReplacementBlock(WorldGenLevel level, ChunkAccess cachedChunk, BlockPos.MutableBlockPos mutable, BlockState neighboringBlockstate, BlockState currentBlockstate) {
		MapColor targetMaterial = neighboringBlockstate.getMapColor(level, mutable);
		Block replacement = COLOR_MAP.getOrDefault(targetMaterial, Blocks.CYAN_TERRACOTTA);

		if(currentBlockstate.hasBlockEntity()) {
			level.setBlock(mutable, replacement.defaultBlockState(), 2);
		}
		else {
			cachedChunk.setBlockState(mutable, replacement.defaultBlockState(), false);
		}
	}
}
