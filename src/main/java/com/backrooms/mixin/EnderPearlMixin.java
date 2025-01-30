package com.backrooms.mixin;

import com.backrooms.TeleportManager;
import com.backrooms.monster.StalkerMonsterManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderPearlEntity.class)
public abstract class EnderPearlMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    public void onEnderPearlCollision(net.minecraft.util.hit.HitResult hitResult, CallbackInfo ci) {
        EnderPearlEntity pearlEntity = (EnderPearlEntity) (Object) this;
        World world = pearlEntity.getWorld();
        if (pearlEntity.getOwner() instanceof ServerPlayerEntity player) {
            if (StalkerMonsterManager.monsterExists(player.getUuid())) {
                pearlEntity.remove(Entity.RemovalReason.KILLED);
                ci.cancel();
                return;
            }

            if (!world.isClient && hitResult.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                BlockPos blockPos = ((net.minecraft.util.hit.BlockHitResult) hitResult).getBlockPos();

                // Check if the block at the position is yellow carpet
                if (world.getBlockState(blockPos).isOf(Blocks.YELLOW_CARPET)) {
                    if (TeleportManager.isInBackrooms(player)) {
                        // Call your custom teleport function with the player as an argument
                        TeleportManager.teleportBackToOverworld(player, true);
                    } else {
                        TeleportManager.teleportToBackrooms(player);
                    }
                    pearlEntity.remove(Entity.RemovalReason.KILLED);
                    ci.cancel();
                }
            }
        }
    }
}
