package com.telepathicgrunt.worldblender.mixin.worldgen;

import com.telepathicgrunt.worldblender.theblender.BlenderData;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

/**
 * Feeds the blended features/carvers back to World Blender's biomes, whose datapack-defined generation
 * settings are otherwise empty. 1.20.1 biomes are immutable so this is how we "add" content to them.
 */
@Mixin(BiomeGenerationSettings.class)
public class BiomeGenerationSettingsMixin {

	@Inject(method = "features", at = @At(value = "RETURN"), cancellable = true)
	private void wb_features(CallbackInfoReturnable<List<HolderSet<PlacedFeature>>> cir) {
		List<HolderSet<PlacedFeature>> blended = BlenderData.FEATURES.get((BiomeGenerationSettings) (Object) this);
		if (blended != null) {
			cir.setReturnValue(blended);
		}
	}

	@Inject(method = "getCarvers", at = @At(value = "RETURN"), cancellable = true)
	private void wb_carvers(GenerationStep.Carving carving, CallbackInfoReturnable<Iterable<Holder<ConfiguredWorldCarver<?>>>> cir) {
		Map<GenerationStep.Carving, List<Holder<ConfiguredWorldCarver<?>>>> blended = BlenderData.CARVERS.get((BiomeGenerationSettings) (Object) this);
		if (blended != null) {
			cir.setReturnValue(blended.getOrDefault(carving, List.of()));
		}
	}
}
