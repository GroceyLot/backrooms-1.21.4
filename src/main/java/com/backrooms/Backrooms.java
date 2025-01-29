package com.backrooms;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class Backrooms implements ModInitializer {
	public static final Identifier CUSTOM_GENERATOR_ID =  Identifier.of("backrooms", "custom_generator");
	@Override
	public void onInitialize() {
		Registry.register(Registries.CHUNK_GENERATOR, CUSTOM_GENERATOR_ID, CustomChunkGenerator.CODEC);
    }
}
