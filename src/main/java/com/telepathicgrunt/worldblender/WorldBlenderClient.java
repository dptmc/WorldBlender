package com.telepathicgrunt.worldblender;

import com.telepathicgrunt.worldblender.blocks.WBBlocks;
import com.telepathicgrunt.worldblender.blocks.WBPortalBlockEntityRenderer;
import com.telepathicgrunt.worldblender.dimension.WBSkyEffects;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class WorldBlenderClient {
	public static void subscribeClientEvents() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(WorldBlenderClient::onClientSetup);
		modEventBus.addListener(WorldBlenderClient::registerDimensionSpecialEffects);

		MinecraftForge.EVENT_BUS.register(WorldBlenderClient.class);
	}

	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() ->
				BlockEntityRenderers.register(WBBlocks.WORLD_BLENDER_PORTAL_BE.get(), WBPortalBlockEntityRenderer::new));
	}

	public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
		event.register(WBIdentifiers.SKY_PROPERTY_ID, new WBSkyEffects());
	}
}
