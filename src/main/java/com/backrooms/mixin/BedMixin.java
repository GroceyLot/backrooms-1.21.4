package com.backrooms.mixin;

import com.backrooms.TeleportManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;

import java.util.Objects;

@Mixin(BedBlock.class)
public class BedMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    public void onTrySleep(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        // Check if the player is in the Backrooms dimension (replace "YOUR_BACKROOMS_DIMENSION" with your dimension key)
        if (world.getRegistryKey().equals(RegistryKey.of(RegistryKey.ofRegistry(Identifier.of("minecraft", "dimension")), Identifier.of("backrooms", "backrooms")))) {
            // Cancel the sleep and teleport the player
            cir.setReturnValue(ActionResult.FAIL); // Cancel vanilla sleep behavior

            ServerWorld overworld = Objects.requireNonNull(world.getServer()).getWorld(World.OVERWORLD);
            if (overworld != null) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    TeleportManager.teleportBackToOverworld(serverPlayer);
                }
            }
            cir.cancel();
        }
    }
}
