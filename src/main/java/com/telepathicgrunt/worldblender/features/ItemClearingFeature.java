package com.telepathicgrunt.worldblender.features;

import com.telepathicgrunt.worldblender.entities.ItemClearingEntity;
import com.telepathicgrunt.worldblender.entities.WBEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


public class ItemClearingFeature extends Feature<NoneFeatureConfiguration>
{
	public ItemClearingFeature()
	{
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
	{
		WorldGenLevel level = context.level();
		ItemClearingEntity itemClearingEntity = WBEntities.ITEM_CLEARING_ENTITY.get().create(level.getLevel());
		if(itemClearingEntity == null){
			BlockPos position = context.origin();
			com.telepathicgrunt.worldblender.WorldBlender.LOGGER.warn("Error with spawning clearing item entity at: ({}, {}, {})", position.getX(), position.getY(), position.getZ());
			return false;
		}

		BlockPos position = context.origin();
		itemClearingEntity.moveTo((double)position.getX() + 0.5D, level.getMaxBuildHeight() - 1, (double)position.getZ() + 0.5D, 0.0F, 0.0F);
		level.addFreshEntity(itemClearingEntity);
		return true;
	}
}
