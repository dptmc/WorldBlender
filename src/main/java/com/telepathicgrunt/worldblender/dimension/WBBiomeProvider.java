package com.telepathicgrunt.worldblender.dimension;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.telepathicgrunt.worldblender.WBIdentifiers;
import com.telepathicgrunt.worldblender.WorldBlender;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Places World Blender's five biomes using two perlin noise fields. This replaces the 1.16.5
 * {@code MainBiomeLayer} + {@code Layer}/{@code LazyArea} setup, which no longer exists in 1.20.1.
 */
public class WBBiomeProvider extends BiomeSource {
    public static final Codec<WBBiomeProvider> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("seed").orElse(0L).forGetter(provider -> provider.seed),
            Codec.intRange(1, 20).fieldOf("biome_size").orElse(2).forGetter(provider -> provider.biomeSize),
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(provider -> provider.biomeSet)
    ).apply(instance, WBBiomeProvider::new));

    private final long seed;
    private final int biomeSize;
    private final HolderSet<Biome> biomeSet;
    private final PerlinSimplexNoise perlinGen;

    @Nullable
    private final Holder<Biome> generalBiome;
    @Nullable
    private final Holder<Biome> mountainousBiome;
    @Nullable
    private final Holder<Biome> coldHillsBiome;
    @Nullable
    private final Holder<Biome> oceanBiome;
    @Nullable
    private final Holder<Biome> frozenOceanBiome;

    public WBBiomeProvider(long seed, int biomeSize, HolderSet<Biome> biomeSet) {
        this.seed = seed;
        this.biomeSize = biomeSize;
        this.biomeSet = biomeSet;
        this.perlinGen = new PerlinSimplexNoise(RandomSource.create(seed), ImmutableList.of(-2, -1, 0));

        Map<ResourceKey<Biome>, Holder<Biome>> byKey = new HashMap<>();
        biomeSet.forEach(holder -> holder.unwrapKey().ifPresent(key -> byKey.put(key, holder)));

        this.generalBiome = byKey.get(WBIdentifiers.GENERAL_BLENDED_BIOME_ID);
        this.mountainousBiome = byKey.get(WBIdentifiers.MOUNTAINOUS_BLENDED_BIOME_ID);
        this.coldHillsBiome = byKey.get(WBIdentifiers.COLD_HILLS_BLENDED_BIOME_ID);
        this.oceanBiome = byKey.get(WBIdentifiers.OCEAN_BLENDED_BIOME_ID);
        this.frozenOceanBiome = byKey.get(WBIdentifiers.FROZEN_OCEAN_BLENDED_BIOME_ID);
    }

    private Holder<Biome> fallback() {
        return biomeSet.get(0);
    }

    private Holder<Biome> holder(@Nullable Holder<Biome> holder) {
        return holder != null ? holder : fallback();
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomeSet.stream();
    }

    public HolderSet<Biome> biomeSet() {
        return this.biomeSet;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        double frequency = 0.055D / this.biomeSize;
        double perlinNoise = this.perlinGen.getValue(x * frequency, z * frequency, false);
        double perlinNoise2 = this.perlinGen.getValue(x * frequency * 1.36D + 1000D, z * frequency * 1.36D + 1000D, false);

        if (perlinNoise > 0.51D) {
            return holder(this.mountainousBiome);
        }
        else if (perlinNoise > -0.6D) {
            if (perlinNoise2 < -0.62D) {
                return holder(this.coldHillsBiome);
            }
            else {
                return holder(this.generalBiome);
            }
        }
        else {
            RandomSource random = RandomSource.create(this.seed + x * 341873128712L + z * 132897987541L);
            return (random.nextInt(100) / 800D + perlinNoise % 0.4D) > -0.2D
                    ? holder(this.oceanBiome)
                    : holder(this.frozenOceanBiome);
        }
    }
}
