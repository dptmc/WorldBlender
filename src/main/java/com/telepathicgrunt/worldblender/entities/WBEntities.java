package com.telepathicgrunt.worldblender.entities;

import com.telepathicgrunt.worldblender.WorldBlender;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class WBEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WorldBlender.MODID);

    public static final RegistryObject<EntityType<ItemClearingEntity>> ITEM_CLEARING_ENTITY = ENTITIES.register("item_clearing_entity", () ->
            EntityType.Builder.<ItemClearingEntity>of(ItemClearingEntity::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F).clientTrackingRange(0).build("item_clearing_entity"));
}
