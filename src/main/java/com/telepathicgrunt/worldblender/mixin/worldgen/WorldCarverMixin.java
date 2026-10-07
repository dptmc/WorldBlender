package com.telepathicgrunt.worldblender.mixin.worldgen;

import com.telepathicgrunt.worldblender.theblender.BlenderData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets carvers carve through the extra blocks World Blender paints (netherrack, end stone, ...) so caves
 * are not cut off by the blended surface.
 */
@Mixin(WorldCarver.class)
public class WorldCarverMixin {

	@Inject(method = "canReplaceBlock", at = @At(value = "HEAD"), cancellable = true)
	private void wb_canReplaceBlock(CarverConfiguration config, BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (Boolean.TRUE.equals(BlenderData.CARVING_WB.get()) && BlenderData.EXTRA_CARVABLE_BLOCKS.contains(state.getBlock())) {
			cir.setReturnValue(true);
		}
	}
}
