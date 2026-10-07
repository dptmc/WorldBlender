package com.telepathicgrunt.worldblender;

import com.telepathicgrunt.worldblender.blocks.WBBlocks;
import com.telepathicgrunt.worldblender.blocks.WBPortalSpawning;
import com.telepathicgrunt.worldblender.configs.WBBlendingConfigs;
import com.telepathicgrunt.worldblender.configs.WBDimensionConfigs;
import com.telepathicgrunt.worldblender.configs.WBPortalConfigs;
import com.telepathicgrunt.worldblender.dimension.AltarManager;
import com.telepathicgrunt.worldblender.dimension.WBBiomeProvider;
import com.telepathicgrunt.worldblender.entities.WBEntities;
import com.telepathicgrunt.worldblender.features.WBFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(WorldBlender.MODID)
public class WorldBlender {
	public static final String MODID = "world_blender";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public WorldBlender() {

		//Set up config
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WBBlendingConfigs.GENERAL_SPEC, "world_blender-blending.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WBDimensionConfigs.GENERAL_SPEC, "world_blender-dimension.toml");
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WBPortalConfigs.GENERAL_SPEC, "world_blender-portal.toml");

		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::setup);
		modEventBus.addListener(this::registerBuiltInRegistries);
		WBBlocks.BLOCKS.register(modEventBus);
		WBBlocks.BLOCK_ENTITY_TYPES.register(modEventBus);
		WBFeatures.FEATURES.register(modEventBus);
		WBEntities.ENTITIES.register(modEventBus);

		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		forgeBus.addListener(WBPortalSpawning::BlockRightClickEvent);
		forgeBus.addListener((TickEvent.LevelTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				AltarManager.onLevelTick(event.level);
			}
		});
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> WorldBlenderClient.subscribeClientEvents());
	}

	public void setup(final FMLCommonSetupEvent event) {
	}

	// Register our custom biome source codec so the dimension json can reference it.
	private void registerBuiltInRegistries(RegisterEvent event) {
		if (event.getRegistryKey().equals(Registries.BIOME_SOURCE)) {
			event.register(Registries.BIOME_SOURCE, WBIdentifiers.WB_BIOME_SOURCE_ID, () -> WBBiomeProvider.CODEC);
		}
	}
}
