package com.backrooms.monster;

import com.backrooms.TeleportManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class StalkerMonsterManager {
    private static final Random random = new Random();
    private static final HashMap<UUID, StalkerMonster> activeMonsters = new HashMap<UUID, StalkerMonster>();

    public static void spawnMonster(ServerPlayerEntity player) {
        if (TeleportManager.isInBackrooms(player) // Only in backrooms
                && !player.getAbilities().invulnerable
        ) {
            System.out.println("Player doing it " + player.getName());
            activeMonsters.put(player.getUuid(), new StalkerMonster(player));
        }
    }

    public static void updateMonsters() {
        activeMonsters.values().removeIf(StalkerMonster::tick);
    }
}