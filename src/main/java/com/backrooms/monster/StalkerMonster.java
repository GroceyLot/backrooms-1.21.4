package com.backrooms.monster;

import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class StalkerMonster {
    private final UUID playerUUID;
    private final UUID skeletonUuid;
    private Vec3d position;
    private final WitherSkeletonEntity skeleton;
    private boolean active = true;
    private static final double SPEED = 0.25; // Slightly slower than sprint-jumping
    private static final double SPAWN_DISTANCE = 20.0;
    private int closeRangeTicks = 0;  // Counter for when player is within 2 blocks
    private final Random random;

    public StalkerMonster(ServerPlayerEntity player) {
        this.playerUUID = player.getUuid();
        ServerWorld world = player.getServerWorld();
        this.position = getSpawnPosition(player);
        this.random = new Random();

        this.skeleton = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, world) {
            @Override
            public void onDeath(DamageSource source) {
                super.onDeath(source);
                if (source.getAttacker() instanceof ServerPlayerEntity) {
                    dropSpecialLoot(this.getBlockPos(), this.getWorld());
                }
            }
        };

        skeleton.setAiDisabled(true);
        skeleton.setSilent(true);
        skeleton.setCanPickUpLoot(false);
        skeleton.setPersistent();
        Objects.requireNonNull(skeleton.getAttributeInstance(EntityAttributes.MAX_HEALTH)).setBaseValue(50);
        skeleton.setHealth(50);
        skeleton.setCustomName(Text.literal("The Bacteria").setStyle(Style.EMPTY.withColor(0xFF0000).withBold(true)));
        skeleton.refreshPositionAndAngles(position.getX(), position.getY(), position.getZ(), 0, 0);
        this.skeletonUuid = skeleton.getUuid();
        world.spawnEntity(skeleton);

        player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 100, 0));
        player.networkHandler.sendPacket(new TitleS2CPacket(
                Text.literal("RUN").setStyle(Style.EMPTY.withColor(0xFF0000).withBold(true))
        ));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(
                Text.literal("It's coming for you...").setStyle(Style.EMPTY.withColor(0xFF0000).withBold(true))
        ));
    }

    private Vec3d getSpawnPosition(ServerPlayerEntity player) {
        Random random = new Random();
        double angle = random.nextDouble() * 2 * Math.PI; // Random angle in radians

        double offsetX = Math.cos(angle) * SPAWN_DISTANCE;
        double offsetZ = Math.sin(angle) * SPAWN_DISTANCE;
        BlockPos spawnPos = player.getBlockPos().add((int) offsetX, 0, (int) offsetZ);

        return new Vec3d(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
    }

    // Array of possible sounds
    SoundEvent[] sounds = {
            SoundEvents.ENTITY_WARDEN_AMBIENT,
            SoundEvents.ENTITY_ENDERMAN_AMBIENT,
            SoundEvents.ENTITY_WARDEN_ANGRY,
            SoundEvents.ENTITY_ENDERMAN_SCREAM,
            SoundEvents.ENTITY_PHANTOM_AMBIENT,
            SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
            SoundEvents.ENTITY_BLAZE_AMBIENT,
            SoundEvents.ENTITY_VEX_AMBIENT,
            SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK,
            SoundEvents.AMBIENT_CAVE.value()
    };

    public boolean tick(MinecraftServer server) {
        if (!active) return true;

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUUID);

        if (!this.skeleton.isAlive() || this.skeleton == null) {
            despawn();
            return true;
        }

        // If the player is offline or disconnected, despawn immediately.
        if (player == null || player.isDisconnected()) {
            despawn();
            return true;
        }

        Vec3d playerPos = player.getPos();
        double distance = playerPos.distanceTo(position);

        if (distance > 30) {
            despawn();
            return true;
        }

        SoundEvent chosenSound = sounds[random.nextInt(sounds.length)];

        player.getWorld().playSound(
                null,
                skeleton.getBlockPos(),
                chosenSound,
                SoundCategory.PLAYERS,
                5.0F,
                1.0F
        );

        if (distance < 10) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 1));

            if (distance < 2) {
                closeRangeTicks++;

                if (closeRangeTicks >= 10) { // 0.5 seconds
                    Optional<Registry<DamageType>> optionalDamageTypeRegistry = player.getWorld().getRegistryManager().getOptional(RegistryKeys.DAMAGE_TYPE);

                    if (optionalDamageTypeRegistry.isEmpty()) {
                        return false;
                    }

                    Registry<DamageType> damageTypeRegistry = optionalDamageTypeRegistry.get();

                    player.damage(player.getServerWorld(), new DamageSource(damageTypeRegistry.getEntry(damageTypeRegistry.get(DamageTypes.MOB_ATTACK)), skeleton), 10);
                    if (!player.isAlive()) {
                        despawn();
                        return true;
                    }
                    closeRangeTicks = 0;
                    position = getSpawnPosition(player);
                }
            } else {
                closeRangeTicks = 0;
            }
        }

        Vec3d direction = playerPos.subtract(position).normalize();
        position = position.add(direction.multiply(SPEED));

        skeleton.refreshPositionAndAngles(position.getX(), position.getY(), position.getZ(), 0, 0);
        skeleton.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, playerPos);

        return false;
    }

    private void despawn() {
        if (skeleton != null) {
            skeleton.discard();
            skeleton.remove(Entity.RemovalReason.DISCARDED);
        }
        active = false;
    }

    private void dropSpecialLoot(BlockPos pos, World world) {
        Item[] ironArmorPieces = {
                Items.IRON_HELMET,
                Items.IRON_CHESTPLATE,
                Items.IRON_LEGGINGS,
                Items.IRON_BOOTS
        };

        ItemStack specialItem = new ItemStack(ironArmorPieces[random.nextInt(ironArmorPieces.length)]);

        specialItem.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.of("Rubber"))));
        world.spawnEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), specialItem));
    }
}
