package com.telepathicgrunt.worldblender.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


public class WBPortalBlockEntity extends BlockEntity {
    private float teleportCooldown = 300;
    private boolean removeable = true;

    public WBPortalBlockEntity(BlockPos pos, BlockState state) {
        super(WBBlocks.WORLD_BLENDER_PORTAL_BE.get(), pos, state);
    }

    public void tick() {
        boolean isCoolingDown = this.isCoolingDown();
        if (isCoolingDown) {
            --this.teleportCooldown;
        }

        if (isCoolingDown != this.isCoolingDown()) {
            this.setChanged();
        }
    }

    public void teleportEntity(Entity entity, BlockPos destPos, ServerLevel destinationWorld, ServerLevel originalWorld) {
        this.triggerCooldown();

        // makes sure chunk is made
        destinationWorld.getChunkAt(destPos);

        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(destinationWorld,
                    destPos.getX() + 0.5D,
                    destPos.getY() + 1D,
                    destPos.getZ() + 0.5D,
                    entity.getYRot(),
                    entity.getXRot());
        }
        else {
            Entity entity2 = entity.getType().create(destinationWorld);
            if (entity2 != null) {
                entity2.restoreFrom(entity);
                entity2.moveTo(destPos.getX() + 0.5D, destPos.getY(), destPos.getZ() + 0.5D, entity.getYRot(), entity.getXRot());
                entity2.setDeltaMovement(entity.getDeltaMovement());
                destinationWorld.addDuringTeleport(entity2);
            }
            entity.discard();
            originalWorld.resetEmptyTime();
            destinationWorld.resetEmptyTime();
        }
    }

    public boolean isCoolingDown() {
        return this.teleportCooldown > 0;
    }

    public float getCoolDown() {
        return this.teleportCooldown;
    }

    public void setCoolDown(float cooldown) {
        this.teleportCooldown = cooldown;
    }

    public void triggerCooldown() {
        if (this.level == null || this.level.isClientSide) return;

        this.teleportCooldown = 300;
        this.setChanged();
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    public boolean isRemoveable() {
        return this.removeable;
    }

    public void makeNotRemoveable() {
        this.removeable = false;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putFloat("Cooldown", this.teleportCooldown);
        data.putBoolean("Removeable", this.removeable);
    }

    @Override
    public void load(CompoundTag data) {
        super.load(data);
        if (data.contains("Cooldown")) {
            this.teleportCooldown = data.getFloat("Cooldown");
        }
        else {
            this.teleportCooldown = 300;
        }

        this.removeable = data.getBoolean("Removeable");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }
}
