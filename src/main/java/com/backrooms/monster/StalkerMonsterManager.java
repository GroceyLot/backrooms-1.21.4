package com.backrooms.monster;

import com.backrooms.TeleportManager;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class StalkerMonsterManager {
    private static final Random random = new Random();
    private static final HashMap<UUID, StalkerMonster> activeMonsters = new HashMap<>();

    public static void spawnMonster(ServerPlayerEntity player) {
        if (TeleportManager.isInBackrooms(player) // Only in backrooms
                && !player.getAbilities().invulnerable
                && !monsterExists(player.getUuid())
        ) {
            activeMonsters.put(player.getUuid(), new StalkerMonster(player));
        }
    }

    public static void updateMonsters() {
        activeMonsters.values().removeIf(StalkerMonster::tick);
    }

    public static boolean monsterExists(UUID uuid) {
        return activeMonsters.containsKey(uuid);
    }
}
