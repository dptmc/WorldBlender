package com.telepathicgrunt.worldblender.mixin.dimensions;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.datafixers.DataFixer;
import com.telepathicgrunt.worldblender.configs.WBBlendingConfigs;
import com.telepathicgrunt.worldblender.theblender.IdentifierPrinting;
import com.telepathicgrunt.worldblender.theblender.TheBlender;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;

/**
 * Blends all biomes into World Blender's own biomes right after the server (and therefore the datapack
 * registries) is constructed but before any level is created/generated. This ordering matters because the
 * level's structure state is computed from the biomes during level creation.
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void wb_blendTheWorld(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess,
								  PackRepository packRepository, WorldStem worldStem, Proxy proxy,
								  DataFixer dataFixer, Services services,
								  ChunkProgressListenerFactory progressListenerFactory, CallbackInfo ci)
	{
		MinecraftServer server = (MinecraftServer) (Object) this;
		RegistryAccess registryAccess = server.registryAccess();

		if (registryAccess.registry(Registries.BIOME).isPresent()) {
			if (WBBlendingConfigs.resourceLocationDump.get()) {
				IdentifierPrinting.printAllResourceLocations(registryAccess);
			}
			TheBlender.blendTheWorld(registryAccess);
		}
	}
}
