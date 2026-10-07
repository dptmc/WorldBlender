package com.telepathicgrunt.worldblender.mixin.worldgen;

import com.telepathicgrunt.worldblender.dimension.WBBiomeProvider;
import com.telepathicgrunt.worldblender.surfacebuilder.WBSurfaceSystem;
import com.telepathicgrunt.worldblender.theblender.BlenderData;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces vanilla surface generation with World Blender's blended surface bands, and flags carvers so they
 * can carve through the blocks the blended surface paints.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorMixin {

	@Inject(
			method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
			at = @At(value = "HEAD"),
			cancellable = true
	)
	private void wb_buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk, CallbackInfo ci) {
		if (((ChunkGeneratorAccessor) (Object) this).wb_getBiomeSource() instanceof WBBiomeProvider) {
			BlockState defaultBlock = ((NoiseBasedChunkGenerator) (Object) this).generatorSettings().value().defaultBlock();
			WBSurfaceSystem.INSTANCE.buildSurface(region, chunk, defaultBlock);
			ci.cancel();
		}
	}

	@Inject(
			method = "applyCarvers(Lnet/minecraft/server/level/WorldGenRegion;JLnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/GenerationStep$Carving;)V",
			at = @At(value = "HEAD")
	)
	private void wb_startCarving(WorldGenRegion region, long seed, RandomState randomState, net.minecraft.world.level.biome.BiomeManager biomeManager,
								 StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving carving, CallbackInfo ci) {
		BlenderData.CARVING_WB.set(((ChunkGeneratorAccessor) (Object) this).wb_getBiomeSource() instanceof WBBiomeProvider);
	}

	@Inject(
			method = "applyCarvers(Lnet/minecraft/server/level/WorldGenRegion;JLnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/GenerationStep$Carving;)V",
			at = @At(value = "RETURN")
	)
	private void wb_stopCarving(WorldGenRegion region, long seed, RandomState randomState, net.minecraft.world.level.biome.BiomeManager biomeManager,
								StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving carving, CallbackInfo ci) {
		BlenderData.CARVING_WB.set(false);
	}
}
