package com.telepathicgrunt.worldblender.theblender;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds everything the blender produced. 1.20.1 biomes are immutable and datapack driven, so instead of
 * mutating the mod's biomes we store the blended data here and the mixins hand it back whenever the game
 * asks a World Blender biome's {@link BiomeGenerationSettings} or {@link MobSpawnSettings} for anything.
 */
public final class BlenderData {
    /** Blended placed features per {@link GenerationStep.Decoration} ordinal, keyed by the WB biome's settings instance. */
    public static final Map<BiomeGenerationSettings, List<HolderSet<PlacedFeature>>> FEATURES = new IdentityHashMap<>();
    /** Blended configured carvers per carving step, keyed by the WB biome's settings instance. */
    public static final Map<BiomeGenerationSettings, Map<GenerationStep.Carving, List<Holder<ConfiguredWorldCarver<?>>>>> CARVERS = new IdentityHashMap<>();
    /** Blended mob spawns per category, keyed by the WB biome's mob settings instance. */
    public static final Map<MobSpawnSettings, Map<MobCategory, WeightedRandomList<MobSpawnSettings.SpawnerData>>> MOBS = new IdentityHashMap<>();
    /** Blended mob spawn costs, keyed by the WB biome's mob settings instance. */
    public static final Map<MobSpawnSettings, Map<EntityType<?>, MobSpawnSettings.MobSpawnCost>> SPAWN_COSTS = new IdentityHashMap<>();

    /** The five World Blender biomes' holders. Used to make every structure accept World Blender biomes. */
    public static volatile List<Holder<Biome>> WB_BIOMES = List.of();
    /** Extra blocks that carvers are allowed to carve through while generating a World Blender chunk. */
    public static volatile Set<Block> EXTRA_CARVABLE_BLOCKS = Set.of();

    /** True while a World Blender chunk is being carved (thread local because chunk gen is multithreaded). */
    public static final ThreadLocal<Boolean> CARVING_WB = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final Map<HolderSet<Biome>, HolderSet<Biome>> EXTENDED_BIOMES = new IdentityHashMap<>();

    private BlenderData() {}

    /**
     * Returns an extended biome holder set that also contains every World Blender biome. This makes vanilla
     * structure biome checks (and StructureSet validity checks) pass in the World Blender dimension.
     */
    public static synchronized HolderSet<Biome> extendBiomes(HolderSet<Biome> original) {
        List<Holder<Biome>> wbBiomes = WB_BIOMES;
        if (wbBiomes.isEmpty() || original == null) {
            return original;
        }

        HolderSet<Biome> cached = EXTENDED_BIOMES.get(original);
        if (cached != null) {
            return cached;
        }

        Set<Holder<Biome>> combined = new HashSet<>();
        original.forEach(combined::add);
        combined.addAll(wbBiomes);
        HolderSet<Biome> extended = HolderSet.direct(new ArrayList<>(combined));
        EXTENDED_BIOMES.put(original, extended);
        return extended;
    }

    public static synchronized void clear() {
        FEATURES.clear();
        CARVERS.clear();
        MOBS.clear();
        SPAWN_COSTS.clear();
        EXTENDED_BIOMES.clear();
        WB_BIOMES = List.of();
        EXTRA_CARVABLE_BLOCKS = Set.of();
    }
}
