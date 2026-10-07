package com.telepathicgrunt.worldblender.mixin.worldgen;

import com.telepathicgrunt.worldblender.theblender.BlenderData;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Feeds the blended mob spawns back to World Blender's biomes.
 */
@Mixin(MobSpawnSettings.class)
public class MobSpawnSettingsMixin {

	@Inject(method = "getMobs", at = @At(value = "RETURN"), cancellable = true)
	private void wb_getMobs(MobCategory category, CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir) {
		Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> blended = BlenderData.MOBS.get((MobSpawnSettings) (Object) this);
		if (blended != null) {
			cir.setReturnValue(blended.getOrDefault(category, WeightedRandomList.create()));
		}
	}

	@Inject(method = "getMobSpawnCost", at = @At(value = "RETURN"), cancellable = true)
	private void wb_getMobSpawnCost(EntityType<?> entityType, CallbackInfoReturnable<MobSpawnSettings.MobSpawnCost> cir) {
		Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> blended = BlenderData.SPAWN_COSTS.get((MobSpawnSettings) (Object) this);
		if (blended != null && blended.containsKey(entityType)) {
			cir.setReturnValue(blended.get(entityType));
		}
	}
}
