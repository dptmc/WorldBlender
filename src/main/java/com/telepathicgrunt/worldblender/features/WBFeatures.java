package com.telepathicgrunt.worldblender.features;

import com.telepathicgrunt.worldblender.WorldBlender;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class WBFeatures
{
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, WorldBlender.MODID);

	public static final RegistryObject<Feature<NoneFeatureConfiguration>> ANTI_FLOATING_BLOCKS_AND_SEPARATE_LIQUIDS =
			FEATURES.register("anti_floating_blocks_and_separate_liquids", AntiFloatingBlocksAndSeparateLiquids::new);
	public static final RegistryObject<Feature<NoneFeatureConfiguration>> ITEM_CLEARING =
			FEATURES.register("item_clearing", ItemClearingFeature::new);
}
