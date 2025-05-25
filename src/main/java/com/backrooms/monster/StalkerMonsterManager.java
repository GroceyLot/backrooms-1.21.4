package com.backrooms.monster;

import com.backrooms.TeleportManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;

public class StalkerMonsterManager {
    private static final HashMap<String, StalkerMonster> activeMonsters = new HashMap<>();

    public static void spawnMonster(ServerPlayerEntity player) {
        if (TeleportManager.isInBackrooms(player) // Only in backrooms
                && !player.getAbilities().invulnerable
                && !monsterExists(player.getUuidAsString())
        ) {
            activeMonsters.put(player.getUuidAsString(), new StalkerMonster(player));
        }
    }

    public static void updateMonsters(MinecraftServer server) {
        activeMonsters.entrySet().removeIf(entry -> entry.getValue().tick(server));
    }

    public static boolean monsterExists(String uuid) {
        return activeMonsters.containsKey(uuid);
    }
}
