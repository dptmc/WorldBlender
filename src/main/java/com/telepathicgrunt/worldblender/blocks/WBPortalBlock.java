package com.telepathicgrunt.worldblender.blocks;

import com.telepathicgrunt.worldblender.WBIdentifiers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.levelgen.Heightmap;


public class WBPortalBlock extends Block implements EntityBlock
{
	protected static final VoxelShape COLLISION_BOX = Block.box(2.0D, 2.0D, 2.0D, 14.0D, 14.0D, 14.0D);

	protected WBPortalBlock()
	{
		super(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_BLACK)
				.noCollission()
				.lightLevel((blockState) -> 6)
				.strength(-1.0F, 3600000.0F)
				.noLootTable()
				.noOcclusion()
				.pushReaction(PushReaction.BLOCK));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WBPortalBlockEntity(pos, state);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (type != WBBlocks.WORLD_BLENDER_PORTAL_BE.get()) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<WBPortalBlockEntity>) (lvl, pos, st, be) -> be.tick();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return COLLISION_BOX;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@SuppressWarnings("resource")
	@Override
	public void entityInside(BlockState blockState, Level world, BlockPos position, Entity entity)
	{
		BlockEntity blockEntityOriginal = world.getBlockEntity(position);
		if (blockEntityOriginal instanceof WBPortalBlockEntity wbBlockEntity)
		{
			if (!world.isClientSide() &&
					!wbBlockEntity.isCoolingDown() &&
					!entity.isPassenger() &&
					!entity.isVehicle() &&
					entity.canChangeDimensions() &&
					Shapes.joinIsNotEmpty(
							Shapes.create(entity.getBoundingBox().move(
									(-position.getX()),
									(-position.getY()),
									(-position.getZ()))),
							COLLISION_BOX,
							BooleanOp.AND))
			{
				//gets the world in the destination dimension
				MinecraftServer minecraftServer = entity.getServer(); // the server itself

				if (minecraftServer == null) return;
				ServerLevel destinationWorld = minecraftServer.getLevel(world.dimension().equals(WBIdentifiers.WB_WORLD_KEY) ? Level.OVERWORLD : WBIdentifiers.WB_WORLD_KEY);
				ServerLevel originalWorld = minecraftServer.getLevel(entity.level().dimension());

				if (destinationWorld == null) return;
				BlockPos destPos = null;

				//looks for portal blocks in other dimension
				//within a 9x256x9 area
				boolean portalOrChestFound = false;
				for (BlockPos blockpos : BlockPos.betweenClosed(position.offset(-4, -position.getY(), -4), position.offset(4, 255 - position.getY(), 4)))
				{
					Block blockNearTeleport = destinationWorld.getBlockState(blockpos).getBlock();

					if (blockNearTeleport == WBBlocks.WORLD_BLENDER_PORTAL.get())
					{
						//gets portal block closest to players original xz coordinate
						if (destPos == null || (Math.abs(blockpos.getX() - position.getX()) < Math.abs(destPos.getX() - position.getX()) && Math.abs(blockpos.getZ() - position.getZ()) < Math.abs(destPos.getZ() - position.getZ())))
							destPos = blockpos.immutable();

						portalOrChestFound = true;

						//make portals have a cooldown after being teleported to
						BlockEntity blockEntity = destinationWorld.getBlockEntity(blockpos);
						if(blockEntity instanceof WBPortalBlockEntity portalBe){
							portalBe.triggerCooldown();
						}

						continue;
					}


					// We check if the block entity class itself has 'chest' in the name.
					// Cache the result and only count the block entity if it is a chest.
					BlockEntity blockEntity = destinationWorld.getBlockEntity(blockpos);
					if(blockEntity == null) continue;

					if (WBPortalSpawning.VALID_CHEST_BLOCKS_ENTITY_TYPES.getOrDefault(blockEntity.getType(), false))
					{
						//only set position to chest if no portal block is found
						if (destPos == null)
							destPos = blockpos.immutable();
						portalOrChestFound = true;
					}
				}

				//no portal or chest was found around destination. just teleport to top land
				if (!portalOrChestFound)
				{
					BlockPos motionBlockPosition = destinationWorld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, position);
					BlockPos worldSurfacePosition = destinationWorld.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, position);
					destPos = motionBlockPosition.getY() > worldSurfacePosition.getY() ? motionBlockPosition : worldSurfacePosition;

					//places a portal block in World Blender so player can escape if
					//there is no portal block and then makes it be in cooldown
					if (destinationWorld.dimension().equals(WBIdentifiers.WB_WORLD_KEY))
					{
						// prevents portal over void killing player
						if(destPos.getY() <= destinationWorld.getMinBuildHeight()){
							destinationWorld.setBlockAndUpdate(destPos, Blocks.STONE.defaultBlockState());
							destPos = destPos.above();
						}

						destinationWorld.setBlockAndUpdate(destPos, Blocks.AIR.defaultBlockState());
						destinationWorld.setBlockAndUpdate(destPos.above(), Blocks.AIR.defaultBlockState());

						destinationWorld.setBlockAndUpdate(destPos, WBBlocks.WORLD_BLENDER_PORTAL.get().defaultBlockState());
						BlockEntity blockEntity = destinationWorld.getBlockEntity(destPos);
						if(blockEntity instanceof WBPortalBlockEntity portalBe){
							portalBe.triggerCooldown();
						}
					}
				}

				wbBlockEntity.teleportEntity(entity, destPos, destinationWorld, originalWorld);
			}
		}
	}


	/**
	 * Turns this portal block to air when right clicked while crouching
	 */
	@Override
	public InteractionResult use(BlockState blockState, Level world, BlockPos blockPos, Player playerEntity, InteractionHand hand, BlockHitResult rayTrace) {
		BlockEntity blockEntity = world.getBlockEntity(blockPos);
		if(playerEntity.isShiftKeyDown() &&
				blockEntity instanceof WBPortalBlockEntity portalBe &&
				portalBe.isRemoveable())
		{
			if (world.isClientSide()) {
				//show lots of particles when portal is removed on client
				createLotsOfParticles(blockState, world, blockPos, world.random);
			}
			else {
				//remove this portal on server side
				world.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.FAIL;
	}


	/**
	 * Shows particles around this block
	 */
	@Override
	// CLIENT-SIDED
	public void animateTick(BlockState blockState, Level world, BlockPos position, RandomSource random) {
		BlockEntity tileEntity = world.getBlockEntity(position);
		if (tileEntity instanceof WBPortalBlockEntity) {
			if (random.nextFloat() < 0.09f) {
				spawnParticle(world, position, random);
			}
		}
	}

	// CLIENT-SIDED
	public void createLotsOfParticles(BlockState blockState, Level world, BlockPos position, RandomSource random) {
		BlockEntity tileEntity = world.getBlockEntity(position);
		if (tileEntity instanceof WBPortalBlockEntity) {
			for(int i = 0; i < 50; i++) {
				spawnParticle(world, position, random);
			}
		}
	}

	private void spawnParticle(Level world, BlockPos position, RandomSource random) {
		double xPos = (double) position.getX() + (double) random.nextFloat();
		double yPos = (double) position.getY() + (double) random.nextFloat();
		double zPos = (double) position.getZ() + (double) random.nextFloat();
		double xVelocity = (random.nextFloat() - 0.5D) * 0.08D;
		double yVelocity = (random.nextFloat() - 0.5D) * 0.13D;
		double zVelocity = (random.nextFloat() - 0.5D) * 0.08D;

		world.addParticle(ParticleTypes.END_ROD, xPos, yPos, zPos, xVelocity, yVelocity, zVelocity);
	}

	@Override
	public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
		return ItemStack.EMPTY;
	}
}
