package com.telepathicgrunt.worldblender.dimension;

import com.telepathicgrunt.worldblender.WBIdentifiers;
import com.telepathicgrunt.worldblender.WorldBlender;
import com.telepathicgrunt.worldblender.blocks.WBBlocks;
import com.telepathicgrunt.worldblender.blocks.WBPortalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Makes sure the unbreakable escape portal always exists at world origin in the World Blender dimension.
 * Managers are kept in a static map and ticked from the Forge level tick event.
 */
public class AltarManager
{
	private static final Map<ResourceKey<Level>, AltarManager> MANAGERS = new HashMap<>();

	private static final StructurePlaceSettings PLACEMENT_SETTINGS = new StructurePlaceSettings()
			.setMirror(Mirror.NONE)
			.setRotation(Rotation.NONE)
			.setIgnoreEntities(false);

	private boolean altarMade;
	/** If generating the altar fails, don't hammer it every tick. */
	private int failedAttempts;
	private int retryCooldown;

	public static void onLevelTick(Level level) {
		if (level instanceof ServerLevel serverLevel && serverLevel.dimension().equals(WBIdentifiers.WB_WORLD_KEY)) {
			get(serverLevel).tick(serverLevel);
		}
	}

	private static AltarManager get(ServerLevel level) {
		return MANAGERS.computeIfAbsent(level.dimension(), key -> new AltarManager(level));
	}

	private AltarManager(ServerLevel serverLevel) {
		this.altarMade = WBWorldSavedData.get(serverLevel).getWBAltarState();
	}

	public boolean isAltarMade() {
		return altarMade;
	}

	public void tick(ServerLevel level)
	{
		if (this.altarMade) return;

		if (this.retryCooldown > 0) {
			--this.retryCooldown;
			return;
		}

		if (!isWorldOriginTicking(level)) return;

		if (generate(level)) {
			this.altarMade = true;
			WBWorldSavedData.get(level).setWBAltarState(true);
		}
		else if (++this.failedAttempts >= 3) {
			// Give up until the next world load instead of pasting the altar every tick forever.
			WorldBlender.LOGGER.warn("Gave up generating the World Blender portal altar after {} attempts.", this.failedAttempts);
			this.altarMade = true;
		}
		else {
			// Wait a bit before retrying to avoid doing full structure placement every tick.
			this.retryCooldown = 100;
		}
	}

	private static boolean generate(ServerLevel level) {
		Optional<StructureTemplate> templateOpt = level.getStructureManager().get(WBIdentifiers.ALTAR_ID);
		if (templateOpt.isEmpty()) {
			WorldBlender.LOGGER.warn("World Blender portal altar NBT does not exist!");
			return false;
		}
		StructureTemplate altarTemplate = templateOpt.get();

		BlockPos.MutableBlockPos finalPosition = new BlockPos.MutableBlockPos();
		finalPosition.set(0, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0), 0);

		// go past trees to the world surface
		BlockState blockState = level.getBlockState(finalPosition);
		while (finalPosition.getY() > level.getMinBuildHeight() + 12 && !blockState.isSolid()) {
			finalPosition.move(Direction.DOWN);
			blockState = level.getBlockState(finalPosition);
		}

		finalPosition.move(Direction.UP);
		BlockPos placePos = finalPosition.offset(-5, -2, -5).immutable();
		level.setBlock(finalPosition.below(), Blocks.AIR.defaultBlockState(), 3);
		altarTemplate.placeInWorld(level, placePos, placePos, PLACEMENT_SETTINGS, RandomSource.create(level.getSeed()), 3);
		finalPosition.move(Direction.DOWN);

		// extra check to make sure the portal is placed
		level.setBlock(finalPosition, WBBlocks.WORLD_BLENDER_PORTAL.get().defaultBlockState(), 3);

		// make the portal block unremoveable in the altar
		BlockEntity blockEntity = level.getBlockEntity(finalPosition);
		if (blockEntity instanceof WBPortalBlockEntity portalBlockEntity) {
			portalBlockEntity.makeNotRemoveable();
		}

		return true;
	}

	private static boolean isWorldOriginTicking(ServerLevel level)
	{
		for (int x = -1; x <= 0; ++x)
		{
			for (int z = -1; z <= 0; ++z)
			{
				LevelChunk chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false) instanceof LevelChunk levelChunk ? levelChunk : null;
				if (chunk == null) {
					return false;
				}
				if (!chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)) {
					return false;
				}
			}
		}
		return true;
	}
}
