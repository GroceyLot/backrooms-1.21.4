package com.backrooms;

import com.backrooms.monster.StalkerMonsterManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Random;

public class Backrooms implements ModInitializer {
	public static final Identifier CUSTOM_GENERATOR_ID =  Identifier.of("backrooms", "custom_generator");
	private Random random;

    @Override
	public void onInitialize() {
		Registry.register(Registries.CHUNK_GENERATOR, CUSTOM_GENERATOR_ID, CustomChunkGenerator.CODEC);
		ServerTickEvents.START_SERVER_TICK.register(this::onTick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("spawnmonsters")
					.requires(source -> source.hasPermissionLevel(2)) // Requires operator level 2
					.executes(context -> {
						MinecraftServer server = context.getSource().getServer();
						for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
							StalkerMonsterManager.spawnMonster(player);
						}
						context.getSource().sendFeedback(() -> Text.of("Spawned monsters for all players."), true);
						return 1;
					})
			);
		});
		this.random = new Random();
	}

	private int tickCounter = 0;

	private void onTick(MinecraftServer server) {
		tickCounter++;
		if (tickCounter >= 1200) { // Every minute (20 ticks * 60)
			tickCounter = 0;
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (random.nextInt(10) == 0) {
					StalkerMonsterManager.spawnMonster(player);
				}
			}
		}

		StalkerMonsterManager.updateMonsters();
	}
}
