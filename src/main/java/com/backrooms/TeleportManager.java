package com.backrooms;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.TeleportTarget;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.Objects;

public class TeleportManager {

    private static final RegistryKey<Dimension> backroomsDim = RegistryKey.of(RegistryKey.ofRegistry(Identifier.of("minecraft", "dimension")), Identifier.of("backrooms", "backrooms"));

    // Teleport a player to the Backrooms (only if in Overworld)
    public static void teleportToBackrooms(ServerPlayerEntity player) {
        // Check if the player is in the Overworld
        if (!isInOverworld(player)) {
            return; // Do nothing if not in the Overworld
        }

        // Get the custom dimension
        ServerWorld customWorld = null;
        // Loop through each world and print its registry key
        for (ServerWorld world : Objects.requireNonNull(player.getServer()).getWorlds()) {
            RegistryKey<?> worldKey = world.getRegistryKey();
            if (worldKey.equals(backroomsDim)) {
                customWorld = world;
            }
        }
        if (customWorld == null) {
            player.sendMessage(Text.of("The backrooms fail to pull you in. Contact the server admin!"));
            return; // Do nothing if the custom dimension is missing
        }

        Vec3d updatedPosition = player.getPos();
        BlockPos targetPos = new BlockPos((int) updatedPosition.getX(), 51, (int) updatedPosition.getZ());

        // Define the teleport target
        TeleportTarget.PostDimensionTransition transition = (entity) -> {};

        TeleportTarget target = new TeleportTarget(customWorld, Vec3d.ofCenter(targetPos), Vec3d.ZERO, 0, 0, transition);

        // Teleport the player
        player.teleportTo(target);
    }

    /// Teleport the player back to the Overworld (only if in Backrooms)
    public static void teleportBackToOverworld(ServerPlayerEntity player) {
        // Check if the player is in the Backrooms
        if (!isInBackrooms(player)) {
            return; // Do nothing if not in the Backrooms
        }

        Vec3d targetPosition;
        ServerWorld targetWorld = Objects.requireNonNull(player.getServer()).getOverworld();

        targetPosition = new Vec3d(player.getX(), 320, player.getZ());

        // Define the teleport target
        TeleportTarget.PostDimensionTransition transition = (entity) -> {};

        TeleportTarget target = new TeleportTarget(targetWorld, targetPosition, Vec3d.ZERO, 0, 0, transition);

        // Teleport the player
        player.teleportTo(target);
    }

    // Check if the player is in the Overworld
    private static boolean isInOverworld(ServerPlayerEntity player) {
        return player.getWorld().getRegistryKey().equals(World.OVERWORLD);
    }

    // Check if the player is in the Backrooms
    public static boolean isInBackrooms(ServerPlayerEntity player) {
        RegistryKey<?> worldKey = player.getWorld().getRegistryKey();
        return worldKey.equals(backroomsDim);
    }
}
