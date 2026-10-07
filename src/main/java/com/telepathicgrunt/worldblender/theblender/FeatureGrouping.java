package com.telepathicgrunt.worldblender.theblender;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

/**
 * Classifies features so the blender can order or drop them.
 *
 * 1.16.5 did this by round-tripping a ConfiguredFeature through its JSON codec. 1.20.1 features are
 * PlacedFeatures with resource locations, so we classify by id instead (simpler and faster).
 */
public final class FeatureGrouping {
    private static final List<String> BAMBOO_KEYWORDS = List.of("bamboo");
    private static final List<String> FIRE_KEYWORDS = List.of("lava", "fire");
    private static final List<String> BASALT_KEYWORDS = List.of("basalt", "delta");
    private static final List<String> SMALL_PLANT_KEYWORDS = List.of("grass", "flower", "rose", "plant", "bush", "fern");
    private static final List<String> LARGE_PLANT_KEYWORDS = List.of("tree", "huge_mushroom", "big_mushroom", "poplar", "twiglet", "mangrove", "bramble");

    private FeatureGrouping() {}

    public static boolean isBamboo(ResourceLocation placedId) {
        return matchesPath(placedId, BAMBOO_KEYWORDS);
    }

    public static boolean isFireOrBasalt(ResourceLocation placedId, ResourceLocation configuredId) {
        return matchesPath(placedId, FIRE_KEYWORDS)
                || matchesPath(configuredId, FIRE_KEYWORDS)
                || matchesPath(configuredId, BASALT_KEYWORDS);
    }

    public static boolean isSmallPlant(ResourceLocation placedId) {
        return matchesPath(placedId, SMALL_PLANT_KEYWORDS);
    }

    public static boolean isLargePlant(ResourceLocation placedId) {
        return matchesPath(placedId, LARGE_PLANT_KEYWORDS);
    }

    private static boolean matchesPath(ResourceLocation id, List<String> keywords) {
        if (id == null) return false;
        String path = id.getPath().toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (path.contains(keyword)) return true;
        }
        return false;
    }
}
