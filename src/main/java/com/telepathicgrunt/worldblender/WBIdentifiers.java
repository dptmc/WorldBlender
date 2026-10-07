package com.telepathicgrunt.worldblender;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public class WBIdentifiers {
    public static final ResourceLocation MOD_DIMENSION_ID = new ResourceLocation(WorldBlender.MODID, WorldBlender.MODID);
    public static final ResourceKey<Level> WB_WORLD_KEY = ResourceKey.create(Registries.DIMENSION, MOD_DIMENSION_ID);
    public static final ResourceLocation WB_BIOME_SOURCE_ID = new ResourceLocation(WorldBlender.MODID, "biome_source");
    public static final ResourceLocation SKY_PROPERTY_ID = new ResourceLocation(WorldBlender.MODID, "sky_property");

    public static final ResourceKey<Biome> GENERAL_BLENDED_BIOME_ID = biome("general_blended");
    public static final ResourceKey<Biome> COLD_HILLS_BLENDED_BIOME_ID = biome("cold_hills_blended");
    public static final ResourceKey<Biome> MOUNTAINOUS_BLENDED_BIOME_ID = biome("mountainous_blended");
    public static final ResourceKey<Biome> OCEAN_BLENDED_BIOME_ID = biome("ocean_blended");
    public static final ResourceKey<Biome> FROZEN_OCEAN_BLENDED_BIOME_ID = biome("frozen_ocean_blended");

    public static final ResourceLocation ALTAR_ID = new ResourceLocation(WorldBlender.MODID, "portal_altar");

    // Placed features that the blender injects into its own biomes. They are datapack-defined so the
    // holders exist in the dynamic registries before blending runs.
    public static final ResourceLocation PORTAL_ALTAR_PLACED_ID = new ResourceLocation(WorldBlender.MODID, "portal_altar");
    public static final ResourceLocation ANTI_FLOATING_LIQUIDS_PLACED_ID = new ResourceLocation(WorldBlender.MODID, "anti_floating_blocks_and_separate_liquids");
    public static final ResourceLocation ITEM_CLEARING_PLACED_ID = new ResourceLocation(WorldBlender.MODID, "item_clearing");

    private static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation(WorldBlender.MODID, path));
    }
}
