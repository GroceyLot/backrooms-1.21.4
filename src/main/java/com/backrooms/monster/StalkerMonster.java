package com.backrooms.monster;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;
import java.util.Random;

public class StalkerMonster {
    private final ServerPlayerEntity player;
    private Vec3d position;
    private final WitherSkeletonEntity skeleton;
    private boolean active = true;
    private static final double SPEED = 0.25; // Slightly slower than sprint-jumping
    private static final double SPAWN_DISTANCE = 20.0;

    private int closeRangeTicks = 0;  // Counter for when player is within 2 blocks

    public StalkerMonster(ServerPlayerEntity player) {
        this.player = player;

        ServerWorld world = player.getServerWorld();
        this.position = getSpawnPosition(player);

        this.skeleton = EntityType.WITHER_SKELETON.create(world, SpawnReason.EVENT);
        if (this.skeleton != null) {
            skeleton.setAiDisabled(true);
            skeleton.setInvulnerable(true);
            skeleton.setSilent(true);
            skeleton.refreshPositionAndAngles(position.getX(), position.getY(), position.getZ(), 0, 0);
            world.spawnEntity(skeleton);
        }
    }

    private Vec3d getSpawnPosition(ServerPlayerEntity player) {
        Random random = new Random();
        double angle = random.nextDouble() * 2 * Math.PI; // Random angle in radians

        double offsetX = Math.cos(angle) * SPAWN_DISTANCE;
        double offsetZ = Math.sin(angle) * SPAWN_DISTANCE;
        BlockPos spawnPos = player.getBlockPos().add((int) offsetX, 0, (int) offsetZ);

        return new Vec3d(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
    }

    public boolean tick() {
        if (player.isDisconnected() || !active) return true;

        Vec3d playerPos = player.getPos();
        double distance = playerPos.distanceTo(position);

        if (distance > 35) {
            despawn();
            return true;
        }

        System.out.println("Playing sound for " + player.getName().getString());
        player.playSound(SoundEvent.of(Identifier.of("minecraft", "entity.warden.hurt")));

        if (distance < 10) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 1));

            // If within 2 blocks, increase the countdown
            if (distance < 2) {
                closeRangeTicks++;

                // Delay before killing
                if (closeRangeTicks >= 10) { // 0.5 seconds
                    player.kill(player.getServerWorld());
                    despawn();
                    return true;
                }
            } else {
                closeRangeTicks = 0; // Reset if they escape
            }
        }

        // Move at controlled speed
        Vec3d direction = playerPos.subtract(position).normalize();
        position = position.add(direction.multiply(SPEED));

        if (skeleton != null) {
            float yaw = (float) Math.toDegrees(Math.atan2(player.getZ() - position.getZ(), player.getX() - position.getX())) - 90;
            skeleton.refreshPositionAndAngles(position.getX(), position.getY(), position.getZ(), yaw, 0);
        }

        return false;
    }

    private void despawn() {
        if (skeleton != null) {
            skeleton.discard();
        }
        active = false;
    }
}
