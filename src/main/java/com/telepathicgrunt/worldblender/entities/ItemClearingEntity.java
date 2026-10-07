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
import net.minecraft.server.level.ServerLevel;

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
      if(ticksTillDetonation > 0){
         // Force blocks to update themselves and tick so they break
         if(ticksTillDetonation == tickCountdownStart - 2){
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            LevelChunk chunk = this.level().getChunk(this.chunkPosition().x, this.chunkPosition().z);
            int chunkOriginX = chunk.getPos().getMinBlockX();
            int chunkOriginZ = chunk.getPos().getMinBlockZ();

            for(int x = 0; x < 16; x++){
               for(int z = 0; z < 16; z++){
                  for(int y = this.level().getMinBuildHeight(); y < this.level().getMaxBuildHeight(); y++){
                     mutable.set(chunkOriginX + x, y, chunkOriginZ + z);
                     BlockState currentState = chunk.getBlockState(mutable);

                     // Skip air, full solid cubes, and liquid blocks as those typically do not break themselves.
                     if(!currentState.isAir() &&
                       currentState.getFluidState().isEmpty() &&
                       !currentState.isSolidRender(this.level(), mutable))
                     {
                        if(!currentState.canSurvive(this.level(), mutable)){
                           // removes all invalid placed blocks like floating grass or rails
                           this.level().removeBlock(mutable, false);
                        }
                        else{
                           // forces blocks like leaves or twisting vines to self-destruct
                           currentState.tick((ServerLevel) this.level(), mutable, this.level().random);
                        }
                     }
                  }
               }
            }
         }

         // count down
         ticksTillDetonation--;
      }

      // NUKE ALL THE ITEMS NOW
      else{
         LevelChunk chunk = this.level().getChunk(this.chunkPosition().x, this.chunkPosition().z);
         net.minecraft.world.phys.AABB chunkBox = new net.minecraft.world.phys.AABB(
                 chunk.getPos().getMinBlockX(), this.level().getMinBuildHeight(), chunk.getPos().getMinBlockZ(),
                 chunk.getPos().getMaxBlockX() + 1, this.level().getMaxBuildHeight(), chunk.getPos().getMaxBlockZ() + 1);

         // Clear the chunk of all ItemEntities
         for (net.minecraft.world.entity.item.ItemEntity itemEntity : this.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, chunkBox)) {
            itemEntity.discard(); // Will be removed automatically on next world tick
         }

         this.discard(); // remove self as task is done
      }
   }
}
