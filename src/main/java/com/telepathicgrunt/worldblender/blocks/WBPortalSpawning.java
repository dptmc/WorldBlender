package com.telepathicgrunt.worldblender.blocks;

import com.telepathicgrunt.worldblender.WorldBlender;
import com.telepathicgrunt.worldblender.configs.WBPortalConfigs;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class WBPortalSpawning
{
	protected static final Object2BooleanMap<BlockEntityType<?>> VALID_CHEST_BLOCKS_ENTITY_TYPES = new Object2BooleanArrayMap<>();
	private static final List<Block> REQUIRED_PORTAL_BLOCKS = new ArrayList<>();
	private static final List<String> INVALID_IDS = new ArrayList<>();
	private static boolean chestListGenerated = false;

	/**
	 * Takes config string and chops it up into individual entries and returns the array of the entries.
	 * Splits the incoming string on commas, trims white spaces on end, turns inside whitespace to _, and lowercases entry.
	 */
	public static void generateRequiredBlockList(Level world, String configEntry) {
		String[] entriesArray = configEntry.split(",");
		Arrays.parallelSetAll(entriesArray, (i) -> entriesArray[i].trim().toLowerCase(Locale.ROOT).replace(' ', '_'));

		REQUIRED_PORTAL_BLOCKS.clear();
		INVALID_IDS.clear();

		//test and make sure the entries exists
		//if not, add it to an invalid rl list so we can warn user later
		for(String rlString : entriesArray)
		{
			if(rlString.isEmpty()) continue;

			ResourceLocation rl = ResourceLocation.tryParse(rlString);
			if(rl != null && BuiltInRegistries.BLOCK.containsKey(rl))
			{
				REQUIRED_PORTAL_BLOCKS.add(BuiltInRegistries.BLOCK.get(rl));
			}
			else
			{
				INVALID_IDS.add(rlString);
			}
		}

		//find all entity types that are most likely chests
		for(Block block : BuiltInRegistries.BLOCK) {
			if(block instanceof EntityBlock entityBlock){
				ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

				try{
					BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());

					if(blockEntity instanceof Container) {
						ResourceLocation blockEntityId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());

						if(blockEntityId != null) {
							boolean hasChestName = blockId.getPath().contains("chest") || blockEntityId.getPath().contains("chest");
							WBPortalSpawning.VALID_CHEST_BLOCKS_ENTITY_TYPES.put(blockEntity.getType(), hasChestName);
						}
						else{
							throw new Exception();
						}
					}
				}
				catch(Throwable e){
					WorldBlender.LOGGER.log(org.apache.logging.log4j.Level.WARN, "Failed to check if "+blockId+" is a chest. If is not a chest, ignore this message. If it is, let TelepathicGrunt (World Blender dev) know this.");
				}
			}
		}
	}

	public static void BlockRightClickEvent(PlayerInteractEvent.RightClickBlock event)
	{
		ensureChestListGenerated(event.getLevel());
		InteractionResult result = blockRightClick(event.getEntity(), event.getLevel(), event.getHand(), event.getPos());
		if(!result.equals(InteractionResult.PASS)){
			event.setCanceled(true);
		}
	}

	private static void ensureChestListGenerated(Level world) {
		if(!chestListGenerated && world != null){
			generateRequiredBlockList(world, WBPortalConfigs.requiredBlocksInChests.get());
			chestListGenerated = true;
		}
	}

	public static InteractionResult blockRightClick(Player player, Level world, InteractionHand hand, BlockPos position)
	{
		if(world.isClientSide() || player.isSpectator()) return InteractionResult.PASS;

		// Checks to see if player uses right click on a chest while crouching while holding nether star
		BlockEntity blockEntity = world.getBlockEntity(position);

		if (player.isShiftKeyDown() &&
				blockEntity != null &&
				WBPortalSpawning.VALID_CHEST_BLOCKS_ENTITY_TYPES.getOrDefault(blockEntity.getType(), false))
		{
			//checks to make sure the activation item is a real item before doing the rest of the checks
			String[] activationItems = WBPortalConfigs.activationItem.get().split(",");
			Arrays.parallelSetAll(activationItems, (i) -> activationItems[i].trim().toLowerCase(Locale.ROOT).replace(' ', '_'));
			boolean validItem = false;

			for(String itemString : activationItems){
				ResourceLocation activationItem = ResourceLocation.tryParse(itemString);
				if (activationItem == null || !BuiltInRegistries.ITEM.containsKey(activationItem))
				{
					WorldBlender.LOGGER.log(org.apache.logging.log4j.Level.INFO, "World Blender: Warning, the activation item set in the config does not exist. Please make sure " + itemString + " is a valid resource location to a real item as the portal cannot be created now.");
					Component message = Component.literal(ChatFormatting.YELLOW + "World Blender: " + ChatFormatting.WHITE + "Warning, the activation item set in the config does not exist. Please make sure " + ChatFormatting.YELLOW + itemString + ChatFormatting.WHITE + " is a valid resource location to a real item as the portal cannot be created now.");
					player.displayClientMessage(message, false);
					return InteractionResult.FAIL;
				}
				else if((player.getItemInHand(InteractionHand.MAIN_HAND).getItem().equals(BuiltInRegistries.ITEM.get(activationItem)) && hand == InteractionHand.MAIN_HAND) ||
						(player.getItemInHand(InteractionHand.OFF_HAND).getItem().equals(BuiltInRegistries.ITEM.get(activationItem)) && hand == InteractionHand.OFF_HAND))
				{
					validItem = true;
					break;
				}
			}

			if(activationItems.length != 0 && !validItem){
				return InteractionResult.PASS;
			}

			BlockPos.MutableBlockPos cornerOffset = new BlockPos.MutableBlockPos(1, 1, 1);
			boolean eightChestsFound = checkForValidChests(world, position, cornerOffset);

			//8 chests found, time to check their inventory.
			if (eightChestsFound)
			{
				Set<Item> uniqueBlocksSet = new HashSet<>();
				Set<Item> invalidItemSet = new HashSet<>();
				Set<Item> duplicateBlockSlotSet = new HashSet<>();

				for (BlockPos blockpos : BlockPos.betweenClosed(position, position.offset(cornerOffset)))
				{
					BlockEntity chestTileEntity = world.getBlockEntity(blockpos);
					if(chestTileEntity instanceof Container chestContainer)
					{
						for (int index = 0; index < chestContainer.getContainerSize(); index++)
						{
							Item item = chestContainer.getItem(index).getItem();

							//if it is a valid block, it would not return air
							if (Block.byItem(item) != Blocks.AIR)
							{
								if(uniqueBlocksSet.contains(item))
								{
									duplicateBlockSlotSet.add(item); // save what block is stacked
								}

								uniqueBlocksSet.add(item);
							}
							//not a valid block item.
							else
							{
								invalidItemSet.add(item);
							}
						}
					}
				}

				if(!INVALID_IDS.isEmpty())
				{
					WorldBlender.LOGGER.log(org.apache.logging.log4j.Level.INFO, "World Blender: Warning, error reading the required blocks config entry. Please make sure the blocks specified in that config are valid resource locations and points to real blocks as the portal cannot be created now. The problematic entries are: " + String.join(", ", INVALID_IDS));
					Component message = Component.literal(ChatFormatting.YELLOW + "World Blender: " + ChatFormatting.WHITE + "Warning, error reading the required blocks config entry. Please make sure the blocks specified in that config are valid resource locations and points to real blocks as the portal cannot be created now. The problematic entries are: " + ChatFormatting.GOLD + String.join(", ", INVALID_IDS));
					player.displayClientMessage(message, false);
					return InteractionResult.FAIL;
				}

				List<Block> listOfRequireBlocksNotFound = new ArrayList<>(REQUIRED_PORTAL_BLOCKS);
				boolean isMissingRequiredBlocks = false;

				//all unique blocks in chests must be a part of the require blocks list
				if(WBPortalConfigs.uniqueBlocksNeeded.get() <= REQUIRED_PORTAL_BLOCKS.size())
				{
					for(Item blockItem : uniqueBlocksSet)
					{
						listOfRequireBlocksNotFound.remove(Block.byItem(blockItem));
					}

					if(WBPortalConfigs.uniqueBlocksNeeded.get() > REQUIRED_PORTAL_BLOCKS.size() - listOfRequireBlocksNotFound.size())
					{
						isMissingRequiredBlocks = true;
					}
				}
				//all blocks in the required blocks list must be present
				else
				{
					for(Item blockItem : uniqueBlocksSet)
					{
						listOfRequireBlocksNotFound.remove(Block.byItem(blockItem));
					}

					if(listOfRequireBlocksNotFound.size() != 0)
					{
						isMissingRequiredBlocks = true;
					}
				}

				//warn player that they do not have enough required blocks for the portal
				if(isMissingRequiredBlocks)
				{
					WorldBlender.LOGGER.log(org.apache.logging.log4j.Level.INFO, "World Blender: There are not enough required blocks in the chests. Please add the needed required blocks and then add any other unique blocks until you have "+WBPortalConfigs.uniqueBlocksNeeded.get()+" unique blocks. The require blocks specified in the config are " + REQUIRED_PORTAL_BLOCKS.stream().map(entry -> BuiltInRegistries.BLOCK.getKey(entry).toString()).collect(Collectors.joining(", ")));
					Component message = Component.literal(ChatFormatting.YELLOW + "World Blender: " + ChatFormatting.WHITE + "There are not enough required blocks in the chests. Please add the needed required blocks and then add any other unique blocks until you have " + ChatFormatting.RED+WBPortalConfigs.uniqueBlocksNeeded.get()+ChatFormatting.WHITE + " unique blocks. The require blocks specified in the config are " + ChatFormatting.GOLD + REQUIRED_PORTAL_BLOCKS.stream().map(entry -> BuiltInRegistries.BLOCK.getKey(entry).toString()).collect(Collectors.joining(", ")));
					player.displayClientMessage(message, false);
					return InteractionResult.FAIL;
				}



				invalidItemSet.remove(Items.AIR); //We don't need to list air
				if (invalidItemSet.size() == 0 &&
						uniqueBlocksSet.size() >= WBPortalConfigs.uniqueBlocksNeeded.get())
				{
					//enough unique blocks were found and no items are in chest. Make portal now
					for (BlockPos blockpos : BlockPos.betweenClosed(position, position.offset(cornerOffset)))
					{
						//consume chest and contents if config says so
						if (WBPortalConfigs.consumeChests.get())
						{
							BlockEntity chestTileEntity = world.getBlockEntity(blockpos);
							if(chestTileEntity instanceof Container chestContainer)
							{
								for (int index = chestContainer.getContainerSize() - 1; index >= 0; index--) {
									chestContainer.removeItemNoUpdate(index);
								}
							}
						}
						else
						{
							world.destroyBlock(blockpos, true, player);
						}

						//create portal but with cooldown so players can grab items before they get teleported
						world.setBlockAndUpdate(blockpos, WBBlocks.WORLD_BLENDER_PORTAL.get().defaultBlockState());
						BlockEntity wbtile = world.getBlockEntity(blockpos);

						if(wbtile instanceof WBPortalBlockEntity portalBe)
							portalBe.triggerCooldown();

						player.getItemInHand(hand).shrink(1); //consume item in hand
					}

					return InteractionResult.SUCCESS;
				}
				//throw error and list all the invalid items in the chests
				else
				{
					String msg = ChatFormatting.YELLOW + "World Blender: " + ChatFormatting.WHITE + "There are not enough unique block items in the chests. (stacks or duplicates are ignored) You need " + ChatFormatting.RED + WBPortalConfigs.uniqueBlocksNeeded.get() + ChatFormatting.WHITE + " block items to make the portal but there is only " + ChatFormatting.GREEN + uniqueBlocksSet.size() + ChatFormatting.WHITE + " unique block items right now.";

					if(invalidItemSet.size() > 0)
					{
						//collect the items names into a list of strings
						List<String> invalidItemString = new ArrayList<>();
						invalidItemSet.forEach(item -> invalidItemString.add(item.getDescription().getString()));
						msg += ChatFormatting.WHITE + "\n Also, here is a list of non-block items that were found and should be removed: " + ChatFormatting.GOLD + String.join(", ", invalidItemString);
					}

					if(duplicateBlockSlotSet.size() != 0)
					{
						//collect the items names into a list of strings
						List<String> duplicateSlotString = new ArrayList<>();
						duplicateBlockSlotSet.remove(Items.AIR); //We dont need to list air
						duplicateBlockSlotSet.forEach(blockitem -> duplicateSlotString.add(blockitem.getDescription().getString()));
						msg += ChatFormatting.WHITE + "\n There are some slots that contains the same blocks and should be removed. These blocks are: " + ChatFormatting.GOLD + String.join(", ", duplicateSlotString);
					}

					WorldBlender.LOGGER.log(org.apache.logging.log4j.Level.INFO, msg);
					player.displayClientMessage(Component.literal(msg), false);

					return InteractionResult.FAIL;
				}
			}
		}

		return InteractionResult.PASS;
	}


	/**
	 * Checks all 8 configurations that a 2x2 area of chests could be around incoming position. If 2x2 is all chests,
	 * returns true and the offset blockpos will be set to that configuration's corner.
	 */
	private static boolean checkForValidChests(Level world, BlockPos position, BlockPos.MutableBlockPos offset)
	{
		boolean eightChestsFound = true;
		for (; offset.getX() >= -1; offset.move(Direction.WEST, 2))
		{
			for (; offset.getY() >= -1; offset.move(Direction.DOWN, 2))
			{
				for (; offset.getZ() >= -1; offset.move(Direction.NORTH, 2))
				{
					//checks if this 2x2 has 8 chests
					for (BlockPos blockpos : BlockPos.betweenClosed(position, position.offset(offset)))
					{
						BlockEntity blockEntity = world.getBlockEntity(blockpos);
						if (blockEntity == null ||
								!WBPortalSpawning.VALID_CHEST_BLOCKS_ENTITY_TYPES.getOrDefault(blockEntity.getType(), false))
						{
							eightChestsFound = false;
							break;
						}
					}

					//is only true if no spot was not a chest
					if (eightChestsFound)
					{
						return true;
					}

					//reset to true for next 2x2 to be checked
					eightChestsFound = true;
				}
				offset.move(Direction.SOUTH, 4); //move back. have to do 4 because the loop's move will fire when exiting loop too
			}
			offset.move(Direction.UP, 4); //move back. have to do 4 because the loop's move will fire when exiting loop too
		}

		return false;
	}
}
