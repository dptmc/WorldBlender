package com.telepathicgrunt.worldblender.blocks;

import com.telepathicgrunt.worldblender.WorldBlender;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class WBBlocks
{
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, WorldBlender.MODID);
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, WorldBlender.MODID);

	public static final RegistryObject<Block> WORLD_BLENDER_PORTAL = BLOCKS.register("world_blender_portal", WBPortalBlock::new);
	public static final RegistryObject<BlockEntityType<WBPortalBlockEntity>> WORLD_BLENDER_PORTAL_BE =
			BLOCK_ENTITY_TYPES.register("world_blender_portal", () ->
					BlockEntityType.Builder.of(WBPortalBlockEntity::new, WORLD_BLENDER_PORTAL.get()).build(null));
}
