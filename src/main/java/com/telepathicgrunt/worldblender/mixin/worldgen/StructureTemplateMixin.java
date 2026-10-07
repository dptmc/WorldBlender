package com.telepathicgrunt.worldblender.mixin.worldgen;

import com.telepathicgrunt.worldblender.configs.WBDimensionConfigs;
import com.telepathicgrunt.worldblender.dimension.WBBiomeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevent template structures from being placed at the bottom of the world when disallowed in config.
 */
@Mixin(StructureTemplate.class)
public class StructureTemplateMixin {

    @Inject(method = "placeInWorld", at = @At(value = "HEAD"), cancellable = true)
    private void wb_removeWorldBottomStructures(ServerLevelAccessor level, BlockPos pos, BlockPos pivot,
                                                StructurePlaceSettings settings, RandomSource random, int flags,
                                                CallbackInfoReturnable<Boolean> cir)
    {
        if (WBDimensionConfigs.removeWorldBottomStructures.get()
                && level.getLevel().getChunkSource().getGenerator().getBiomeSource() instanceof WBBiomeProvider
                && pos.getY() <= level.getMinBuildHeight() + 1)
        {
            cir.setReturnValue(false);
        }
    }
}
