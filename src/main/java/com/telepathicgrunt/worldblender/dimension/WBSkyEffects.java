package com.telepathicgrunt.worldblender.dimension;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

// CLIENT-SIDED
public class WBSkyEffects extends DimensionSpecialEffects {
    public WBSkyEffects() {
        // cloud level, has ground, sky type, force bright lightmap, constant ambient light
        super(192.0F, true, SkyType.NORMAL, false, false);
    }

    @Override
    // sky/fog color
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float sunHeight) {
        return color.multiply(sunHeight * 0.85F + 0.06F, sunHeight * 0.90F + 0.06F, sunHeight * 0.89F + 0.10F);
    }

    @Override
    // thick fog or no
    public boolean isFoggyAt(int camX, int camY) {
        return false;
    }
}
