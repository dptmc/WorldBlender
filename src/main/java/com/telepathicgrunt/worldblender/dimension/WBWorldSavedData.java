package com.telepathicgrunt.worldblender.dimension;

import com.telepathicgrunt.worldblender.WorldBlender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;


public class WBWorldSavedData extends SavedData {
    private static final String ALTAR_DATA = WorldBlender.MODID + "AltarMade";
    private boolean wbAltarMade;

    public WBWorldSavedData() {}

    public WBWorldSavedData(boolean altarMade) {
        this.wbAltarMade = altarMade;
    }

    public static WBWorldSavedData get(Level world) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return new WBWorldSavedData();
        }

        return serverLevel.getDataStorage().computeIfAbsent(WBWorldSavedData::load, WBWorldSavedData::new, ALTAR_DATA);
    }

    public static WBWorldSavedData load(CompoundTag data) {
        return new WBWorldSavedData(data.getBoolean("WBAltarMade"));
    }

    @Override
    public CompoundTag save(CompoundTag data) {
        data.putBoolean("WBAltarMade", wbAltarMade);
        return data;
    }

    public void setWBAltarState(boolean state) {
        this.wbAltarMade = state;
        this.setDirty();
    }

    public boolean getWBAltarState() {
        return this.wbAltarMade;
    }
}
