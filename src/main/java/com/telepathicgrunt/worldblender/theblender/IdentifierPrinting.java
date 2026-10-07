package com.telepathicgrunt.worldblender.theblender;

import com.telepathicgrunt.worldblender.WorldBlender;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Paths;

public class IdentifierPrinting
{
	/**
	 * Prints out all the resource locations that the World Blender blacklists use.
	 * Will create 6 sections: Biomes, Features, Structures, Carvers, Entities, and Blocks.
	 */
	public static void printAllResourceLocations(RegistryAccess registryAccess)
	{
		try(PrintStream printStream = new PrintStream(Paths.get(FMLPaths.CONFIGDIR.get().toString(), "world_blender-identifier_dump.txt").toString()))
		{
			printOutSection(printStream, registryAccess.registryOrThrow(Registries.BIOME), "BIOMES");
			printStream.println();
			printOutSection(printStream, registryAccess.registryOrThrow(Registries.PLACED_FEATURE), "PLACED FEATURES");
			printStream.println();
			printOutSection(printStream, registryAccess.registryOrThrow(Registries.CONFIGURED_FEATURE), "CONFIGURED FEATURES");
			printStream.println();
			printOutSection(printStream, registryAccess.registryOrThrow(Registries.STRUCTURE), "STRUCTURES");
			printStream.println();
			printOutSection(printStream, registryAccess.registryOrThrow(Registries.CONFIGURED_CARVER), "CARVERS");
			printStream.println();
			printOutSection(printStream, registryAccess.registryOrThrow(Registries.ENTITY_TYPE), "ENTITIES");
			printStream.println();
			printOutSection(printStream, registryAccess.registryOrThrow(Registries.BLOCK), "BLOCKS");

			WorldBlender.LOGGER.warn("Created identifier file at config/world_blender-identifier_dump.txt");
		}
		catch (FileNotFoundException e)
		{
			WorldBlender.LOGGER.warn("FAILED TO CREATE AND WRITE TO config/world_blender-identifier_dump.txt. SEE LATEST.LOG AND SHOW IT TO WORLD BLENDER DEV.");
			e.printStackTrace();
		}
	}

	private static <T> void printOutSection(PrintStream printStream, Registry<T> registry, String section)
	{
		printStream.println("######################################################################");
		printStream.println("######      " + section + " RESOURCE LOCATIONS (IDs)        ######");
		printStream.println();

		String previousNamespace = "minecraft";
		for (ResourceLocation id : registry.keySet().stream().sorted().toList()) {
			if (!id.getNamespace().isEmpty() && !previousNamespace.equals(id.getNamespace())) {
				printStream.println();
				previousNamespace = id.getNamespace();
			}
			printStream.println(id);
		}
	}
}
