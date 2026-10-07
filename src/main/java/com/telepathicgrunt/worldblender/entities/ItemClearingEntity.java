package com.telepathicgrunt.worldblender.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

/**
 * Invisible, server-only ticking entity spawned while a chunk is generated. A moment after the chunk is
 * ready it walks the chunk and removes blocks that cannot survive (floating grass, rails, plants, ...)
 * which worldgen feature blending tends to leave behind, and finally deletes stray dropped items.
 *
 * The original 1.16.5 version cast to ServerLevel unconditionally and scanned the entire build height of
 * every column. On 1.20.1 worlds are 384 tall instead of 256 and the entity can be sent to clients, so
 * that cast would throw on the client and the full-height scan was needlessly expensive. This version is
 * strictly server-side and only visits non-empty sections.
 */
public class ItemClearingEntity extends Entity {
   private int ticksTillDetonation;
   private final int tickCountdownStart = 50;

   public ItemClearingEntity(EntityType<? extends ItemClearingEntity> type, Level worldIn) {
      super(type, worldIn);
      ticksTillDetonation = tickCountdownStart;
   }

   @Override
   protected void defineSynchedData() {}

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this);
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return false;
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag compound) {
      compound.putInt("ticksTillDetonation", this.ticksTillDetonation);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag compound) {
      this.ticksTillDetonation = compound.getInt("ticksTillDetonation");
      if(this.ticksTillDetonation == 0) this.ticksTillDetonation = tickCountdownStart;
   }

   @Override
   public void tick() {
      // This entity is purely a server-side worldgen cleanup helper. If it ever ends up on a client
      // (e.g. a chunk with the entity is synced), drop it immediately instead of doing any work.
      if (this.level().isClientSide()) {
         this.discard();
         return;
      }

      if(ticksTillDetonation > 0){
         // Force blocks to update themselves and tick so they break
         if(ticksTillDetonation == tickCountdownStart - 2){
            removeInvalidBlocks();
         }

         // count down
         ticksTillDetonation--;
      }

      // NUKE ALL THE ITEMS NOW
      else{
         clearDroppedItems();
         this.discard(); // remove self as task is done
      }
   }

   private void removeInvalidBlocks() {
      LevelChunk chunk = this.level().getChunk(this.chunkPosition().x, this.chunkPosition().z);
      int chunkOriginX = chunk.getPos().getMinBlockX();
      int chunkOriginZ = chunk.getPos().getMinBlockZ();
      int minBuildHeight = this.level().getMinBuildHeight();
      BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
      LevelChunkSection[] sections = chunk.getSections();

      for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
         LevelChunkSection section = sections[sectionIndex];
         // Skip empty sections (most of the sky and underground) entirely.
         if (section.hasOnlyAir()) continue;

         int sectionBottomY = minBuildHeight + (sectionIndex << 4);

         for(int x = 0; x < 16; x++){
            for(int z = 0; z < 16; z++){
               for(int y = 0; y < 16; y++){
                  mutable.set(chunkOriginX + x, sectionBottomY + y, chunkOriginZ + z);
                  BlockState currentState = chunk.getBlockState(mutable);

                  // Skip air, liquids, and full solid cubes as those do not break themselves.
                  if(!currentState.isAir() &&
                    currentState.getFluidState().isEmpty() &&
                    !currentState.isSolidRender(this.level(), mutable) &&
                    !currentState.canSurvive(this.level(), mutable))
                  {
                     // removes all invalid placed blocks like floating grass or rails
                     this.level().removeBlock(mutable, false);
                  }
               }
            }
         }
      }
   }

   private void clearDroppedItems() {
      LevelChunk chunk = this.level().getChunk(this.chunkPosition().x, this.chunkPosition().z);
      AABB chunkBox = new AABB(
              chunk.getPos().getMinBlockX(), this.level().getMinBuildHeight(), chunk.getPos().getMinBlockZ(),
              chunk.getPos().getMaxBlockX() + 1, this.level().getMaxBuildHeight(), chunk.getPos().getMaxBlockZ() + 1);

      // Clear the chunk of all ItemEntities
      for (net.minecraft.world.entity.item.ItemEntity itemEntity : this.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, chunkBox)) {
         itemEntity.discard(); // Will be removed automatically on next world tick
      }
   }
}
