package com.telepathicgrunt.worldblender.mixin.worldgen;

import com.telepathicgrunt.worldblender.theblender.BlenderData;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes every *allowed* structure accept World Blender's biomes. In 1.20.1 a structure spawns only in biomes
 * listed in its {@link Structure#biomes()} holder set, so adding our biomes there is how we get structures
 * into the World Blender dimension (the dimension itself uses all structure sets).
 *
 * Structures disabled by {@code allowVanillaStructures}/{@code allowModdedStructures} or the structure
 * blacklist are left alone so they do not spawn in World Blender.
 */
@Mixin(Structure.class)
public class StructureMixin {

	@Inject(method = "biomes", at = @At(value = "RETURN"), cancellable = true)
	private void wb_addBlenderBiomes(CallbackInfoReturnable<HolderSet<Biome>> cir) {
		if (!BlenderData.isStructureAllowed((Structure) (Object) this)) {
			return;
		}

		HolderSet<Biome> original = cir.getReturnValue();
		HolderSet<Biome> extended = BlenderData.extendBiomes(original);
		if (extended != original) {
			cir.setReturnValue(extended);
		}
	}
}
