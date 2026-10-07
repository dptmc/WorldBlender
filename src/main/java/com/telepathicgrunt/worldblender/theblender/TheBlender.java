package com.telepathicgrunt.worldblender.theblender;

import com.google.common.collect.ImmutableMap;
import com.telepathicgrunt.worldblender.WBIdentifiers;
import com.telepathicgrunt.worldblender.WorldBlender;
import com.telepathicgrunt.worldblender.configs.WBBlendingConfigs;
import com.telepathicgrunt.worldblender.surfacebuilder.SurfaceBlender;
import com.telepathicgrunt.worldblender.surfacebuilder.WBSurfaceSystem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TheBlender {
	// Prevent modded mobs from drowning out vanilla or other mod's mobs.
	private static final Map<MobCategory, Integer> MAX_WEIGHT_PER_GROUP = ImmutableMap.<MobCategory, Integer>builder()
			.put(MobCategory.CREATURE, 15)
			.put(MobCategory.MONSTER, 120)
			.put(MobCategory.WATER_AMBIENT, 30)
			.put(MobCategory.WATER_CREATURE, 12)
			.put(MobCategory.AMBIENT, 15)
			.build();

	/**
	 * Kickstarts the blender. Must run once the datapack registries are complete but before any level is
	 * created/used for generation (see MinecraftServerMixin).
	 */
	public static void blendTheWorld(RegistryAccess registryAccess) {
		Optional<Registry<Biome>> biomeRegistryOpt = registryAccess.registry(Registries.BIOME);
		if (biomeRegistryOpt.isEmpty()) {
			return;
		}

		final long startNanos = System.nanoTime();

		Registry<Biome> biomes = biomeRegistryOpt.get();
		Registry<PlacedFeature> placedFeatures = registryAccess.registryOrThrow(Registries.PLACED_FEATURE);
		Registry<ConfiguredWorldCarver<?>> configuredCarvers = registryAccess.registryOrThrow(Registries.CONFIGURED_CARVER);
		Registry<EntityType<?>> entityTypes = registryAccess.registryOrThrow(Registries.ENTITY_TYPE);

		BlenderData.clear();
		ConfigBlacklisting.setupBlackLists();

		Blender blender = new Blender(biomes, placedFeatures, entityTypes, configuredCarvers);
		blender.collect();
		blender.applyToWorldBlenderBiomes(registryAccess);

		final long blendTimeMS = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
		WorldBlender.LOGGER.debug("Blend time: {}ms", blendTimeMS);
	}

	private static final class Blender {
		private final Registry<Biome> biomes;
		private final Registry<PlacedFeature> placedFeatures;
		private final Registry<EntityType<?>> entityTypes;
		private final Registry<ConfiguredWorldCarver<?>> configuredCarvers;

		private final List<List<Holder<PlacedFeature>>> blendedFeaturesByStage = new ArrayList<>();
		private final List<List<Holder<PlacedFeature>>> smallPlantsByStage = new ArrayList<>();
		private final Map<GenerationStep.Carving, List<Holder<ConfiguredWorldCarver<?>>>> blendedCarversByStage = new HashMap<>();
		private final Map<MobCategory, List<MobSpawnSettings.SpawnerData>> blendedMobs = new IdentityHashMap<>();
		private final Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> blendedSpawnCosts = new HashMap<>();
		private final SurfaceBlender blendedSurface = new SurfaceBlender();
		private boolean bambooFound = false;

		private final Set<Object> checkedWorldgenObjects = new HashSet<>();
		private final Set<EntityType<?>> checkedMobs = new HashSet<>();

		private Blender(Registry<Biome> biomes, Registry<PlacedFeature> placedFeatures,
						Registry<EntityType<?>> entityTypes, Registry<ConfiguredWorldCarver<?>> configuredCarvers) {
			this.biomes = biomes;
			this.placedFeatures = placedFeatures;
			this.entityTypes = entityTypes;
			this.configuredCarvers = configuredCarvers;

			for (GenerationStep.Decoration ignored : GenerationStep.Decoration.values()) {
				blendedFeaturesByStage.add(new ArrayList<>());
				smallPlantsByStage.add(new ArrayList<>());
			}
			for (GenerationStep.Carving carving : GenerationStep.Carving.values()) {
				blendedCarversByStage.put(carving, new ArrayList<>());
			}
			for (MobCategory category : MobCategory.values()) {
				blendedMobs.put(category, new ArrayList<>());
			}
		}

		private void collect() {
			for (Holder.Reference<Biome> biomeHolder : biomes.holders().toList()) {
				ResourceLocation biomeId = biomeHolder.key().location();
				if (biomeId.getNamespace().equals(WorldBlender.MODID)) continue;

				Biome biome = biomeHolder.value();
				if (shouldSkipBiome(biomeId, WBBlendingConfigs.allowVanillaBiomeImport.get(), WBBlendingConfigs.allowModdedBiomeImport.get())) {
					continue;
				}

				addBiomeFeatures(biome.getGenerationSettings());
				addBiomeCarvers(biome.getGenerationSettings());
				addBiomeNaturalMobs(biome.getMobSettings());
			}

			completeBlending();
		}

		private void addBiomeFeatures(BiomeGenerationSettings settings) {
			List<HolderSet<PlacedFeature>> biomeFeatures = settings.features();
			for (int stage = 0; stage < biomeFeatures.size() && stage < blendedFeaturesByStage.size(); stage++) {
				for (Holder<PlacedFeature> featureHolder : biomeFeatures.get(stage)) {
					if (!checkedWorldgenObjects.add(featureHolder)) continue;

					ResourceLocation placedId = featureHolder.unwrapKey().map(ResourceKey::location).orElse(null);
					ResourceLocation configuredId = featureHolder.value().feature().unwrapKey().map(ResourceKey::location).orElse(null);

					if (shouldSkip(placedId, WBBlendingConfigs.allowVanillaFeatures.get(), WBBlendingConfigs.allowModdedFeatures.get(), ConfigBlacklisting.BlacklistType.FEATURE)) {
						continue;
					}

					if (FeatureGrouping.isBamboo(placedId)) {
						bambooFound = true;
						continue;
					}
					if (WBBlendingConfigs.disallowFireLavaBasaltFeatures.get() && FeatureGrouping.isFireOrBasalt(placedId, configuredId)) {
						continue;
					}

					// small plants (grass/flowers) get added dead last so trees have a chance
					if (FeatureGrouping.isSmallPlant(placedId)) {
						smallPlantsByStage.get(stage).add(featureHolder);
						continue;
					}

					// modded trees get priority over everything else in their stage
					boolean isVanilla = placedId != null && placedId.getNamespace().equals("minecraft");
					if (!isVanilla && FeatureGrouping.isLargePlant(placedId)) {
						blendedFeaturesByStage.get(stage).add(0, featureHolder);
						continue;
					}

					blendedFeaturesByStage.get(stage).add(featureHolder);
				}
			}
		}

		private void addBiomeCarvers(BiomeGenerationSettings settings) {
			for (GenerationStep.Carving carving : GenerationStep.Carving.values()) {
				List<Holder<ConfiguredWorldCarver<?>>> blended = blendedCarversByStage.get(carving);
				for (Holder<ConfiguredWorldCarver<?>> carverHolder : settings.getCarvers(carving)) {
					if (!checkedWorldgenObjects.add(carverHolder)) continue;

					ResourceLocation carverId = carverHolder.unwrapKey().map(ResourceKey::location).orElse(null);
					if (shouldSkip(carverId, WBBlendingConfigs.allowVanillaCarvers.get(), WBBlendingConfigs.allowModdedCarvers.get(), ConfigBlacklisting.BlacklistType.CARVER)) {
						continue;
					}
					if (blended.contains(carverHolder)) continue;
					blended.add(carverHolder);
				}
			}
		}

		private void addBiomeNaturalMobs(MobSpawnSettings spawnSettings) {
			for (MobCategory category : MobCategory.values()) {
				int maxWeight = MAX_WEIGHT_PER_GROUP.getOrDefault(category, Integer.MAX_VALUE);
				List<MobSpawnSettings.SpawnerData> blended = blendedMobs.get(category);

				for (MobSpawnSettings.SpawnerData spawnEntry : spawnSettings.getMobs(category).unwrap()) {
					if (!checkedMobs.add(spawnEntry.type)) continue;
					if (blended.stream().anyMatch(existing -> existing.type == spawnEntry.type)) continue;

					ResourceLocation entityTypeId = entityTypes.getKey(spawnEntry.type);
					if (shouldSkip(entityTypeId, WBBlendingConfigs.allowVanillaSpawns.get(), WBBlendingConfigs.allowModdedSpawns.get(), ConfigBlacklisting.BlacklistType.SPAWN)) {
						continue;
					}

					int weight = Math.max(1, Math.min(maxWeight, spawnEntry.getWeight().asInt()));
					blended.add(new MobSpawnSettings.SpawnerData(spawnEntry.type, weight, spawnEntry.minCount, spawnEntry.maxCount));
				}

				for (EntityType<?> entityType : spawnSettings.getEntityTypes()) {
					MobSpawnSettings.MobSpawnCost cost = spawnSettings.getMobSpawnCost(entityType);
					if (cost != null) {
						blendedSpawnCosts.putIfAbsent(entityType, cost);
					}
				}
			}
		}

		private void completeBlending() {
			// add grass, flower, and other small plants now so they generate after trees
			for (GenerationStep.Decoration stage : GenerationStep.Decoration.values()) {
				List<Holder<PlacedFeature>> stageFeatures = blendedFeaturesByStage.get(stage.ordinal());
				for (Holder<PlacedFeature> feature : smallPlantsByStage.get(stage.ordinal())) {
					if (!stageFeatures.contains(feature)) {
						stageFeatures.add(feature);
					}
				}
			}

			// add 1 configured bamboo so it is dead last
			if (bambooFound) {
				placedFeatures.getHolder(ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation("minecraft", "bamboo_light")))
						.ifPresent(holder -> blendedFeaturesByStage.get(GenerationStep.Decoration.VEGETAL_DECORATION.ordinal()).add(holder));
			}

			// the mod's own cleanup features go last so they can contain/clean up everything else
			blendedFeaturesByStage.get(GenerationStep.Decoration.LOCAL_MODIFICATIONS.ordinal())
					.add(placedFeature(WBIdentifiers.ANTI_FLOATING_LIQUIDS_PLACED_ID));
			blendedFeaturesByStage.get(GenerationStep.Decoration.LOCAL_MODIFICATIONS.ordinal())
					.add(placedFeature(WBIdentifiers.ITEM_CLEARING_PLACED_ID));

			WBSurfaceSystem.save(blendedSurface);
			BlenderData.EXTRA_CARVABLE_BLOCKS = blendedSurface.blocksToCarve();
		}

		@Nullable
		private Holder<PlacedFeature> placedFeature(ResourceLocation id) {
			return placedFeatures.getHolder(ResourceKey.create(Registries.PLACED_FEATURE, id)).orElse(null);
		}

		private void applyToWorldBlenderBiomes(RegistryAccess registryAccess) {
			Registry<Biome> biomes = registryAccess.registryOrThrow(Registries.BIOME);

			List<Holder<Biome>> wbBiomes = new ArrayList<>();
			for (ResourceKey<Biome> key : List.of(
					WBIdentifiers.GENERAL_BLENDED_BIOME_ID,
					WBIdentifiers.COLD_HILLS_BLENDED_BIOME_ID,
					WBIdentifiers.MOUNTAINOUS_BLENDED_BIOME_ID,
					WBIdentifiers.OCEAN_BLENDED_BIOME_ID,
					WBIdentifiers.FROZEN_OCEAN_BLENDED_BIOME_ID)) {
				biomes.getHolder(key).ifPresent(holder -> wbBiomes.add((Holder<Biome>) holder));
			}
			BlenderData.WB_BIOMES = List.copyOf(wbBiomes);

			// build the finished holder sets per stage
			List<HolderSet<PlacedFeature>> blendedFeatureSets = new ArrayList<>();
			for (List<Holder<PlacedFeature>> stage : blendedFeaturesByStage) {
				blendedFeatureSets.add(HolderSet.direct(stage));
			}

			for (Holder<Biome> wbBiome : wbBiomes) {
				Biome biome = wbBiome.value();
				BiomeGenerationSettings generationSettings = biome.getGenerationSettings();
				BlenderData.FEATURES.put(generationSettings, List.copyOf(blendedFeatureSets));
				BlenderData.CARVERS.put(generationSettings, new HashMap<>(blendedCarversByStage));

				MobSpawnSettings mobSettings = biome.getMobSettings();
				Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>> mobs = new HashMap<>();
				for (Map.Entry<MobCategory, List<MobSpawnSettings.SpawnerData>> entry : blendedMobs.entrySet()) {
					mobs.put(entry.getKey(), WeightedRandomList.create(entry.getValue()));
				}
				BlenderData.MOBS.put(mobSettings, mobs);
				BlenderData.SPAWN_COSTS.put(mobSettings, Map.copyOf(blendedSpawnCosts));
			}
		}
	}

	private static boolean shouldSkip(@Nullable ResourceLocation id, boolean allowVanilla, boolean allowModded, @Nullable ConfigBlacklisting.BlacklistType blacklist) {
		if (id == null) return true;

		boolean isVanilla = id.getNamespace().equals("minecraft");
		if (isVanilla && !allowVanilla) return true;
		if (!isVanilla && !allowModded) return true;

		return blacklist != null && ConfigBlacklisting.isResourceLocationBlacklisted(blacklist, id);
	}

	private static boolean shouldSkipBiome(ResourceLocation id, boolean allowVanilla, boolean allowModded) {
		if (id == null) return true;

		boolean isVanilla = id.getNamespace().equals("minecraft");
		if (isVanilla && !allowVanilla) return true;
		if (!isVanilla && !allowModded) return true;

		return ConfigBlacklisting.isResourceLocationBlacklisted(ConfigBlacklisting.BlacklistType.BLANKET, id);
	}
}
