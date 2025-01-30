package com.backrooms;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class Loot {
    public static final List<LootEntry> barrelLoot = List.of(
            new LootEntry(Items.ROTTEN_FLESH, 8, 2, 5),
            new LootEntry(Items.BREAD, 5),
            new LootEntry(Items.COOKIE, 4, 2, 4),
            new LootEntry(Items.COOKED_COD, 4),
            new LootEntry(Items.COOKED_SALMON, 4),
            new LootEntry(Items.POTATO, 6, 1, 3),
            new LootEntry(Items.BEETROOT, 5, 1, 3),
            new LootEntry(Items.DEAD_BUSH, 10),
            new LootEntry(Items.WHEAT_SEEDS, 8),
            new LootEntry(Items.FEATHER, 6),
            new LootEntry(Items.PAPER, 6),
            new LootEntry(Items.BONE, 5, 1, 3),
            new LootEntry(Items.STRING, 5, 1, 3),
            new LootEntry(Items.LEATHER, 5),
            new LootEntry(Items.IRON_NUGGET, 4, 2, 6),
            new LootEntry(Items.COAL, 4, 1, 3),
            new LootEntry(Items.FLINT, 4),
            new LootEntry(Items.WOODEN_SWORD, 6),
            new LootEntry(Items.STONE_PICKAXE, 4),
            new LootEntry(Items.STONE_AXE, 3),
            new LootEntry(Items.WOODEN_SHOVEL, 4),
            new LootEntry(Items.SHEARS, 2),
            new LootEntry(Items.LEATHER_HELMET, 3),
            new LootEntry(Items.LEATHER_BOOTS, 3),
            new LootEntry(Items.GLASS_BOTTLE, 5),
            new LootEntry(Items.BOOK, 4),
            new LootEntry(Items.MAP, 2),
            new LootEntry(Items.ARROW, 6, 2, 8),
            new LootEntry(Items.GUNPOWDER, 3),
            new LootEntry(Items.OAK_PLANKS, 6, 1, 3),
            new LootEntry(Items.STONE, 5, 1, 4),
            new LootEntry(Items.IRON_BARS, 4),
            new LootEntry(Items.COBBLESTONE, 5, 1, 3),
            new LootEntry(Items.FLOWER_POT, 2),
            new LootEntry(Items.PAINTING, 1)
    );

    public static final List<LootEntry> rareLoot = List.of(
            new LootEntry(Items.IRON_SWORD, 6),
            new LootEntry(Items.IRON_PICKAXE, 5),
            new LootEntry(Items.BOW, 4),
            new LootEntry(Items.ARROW, 5, 3, 10),
            new LootEntry(Items.IRON_CHESTPLATE, 3),
            new LootEntry(Items.IRON_LEGGINGS, 2),
            new LootEntry(Items.GOLD_INGOT, 4, 1, 3),
            new LootEntry(Items.GOLDEN_CARROT, 3),
            new LootEntry(Items.GOLDEN_APPLE, 2),
            new LootEntry(Items.ENDER_PEARL, 3),
            new LootEntry(Items.BUCKET, 4),
            new LootEntry(Items.PAINTING, 2),
            new LootEntry(Items.NAME_TAG, 1),
            new LootEntry(Items.STICK, 1)
    );

    public static final List<LootEntry> ultraLoot = List.of(
            new LootEntry(Items.ENCHANTED_BOOK, 1),
            new LootEntry(Items.DIAMOND, 10),
            new LootEntry(Items.NETHERITE_INGOT, 2),
            new LootEntry(Items.NETHERITE_SCRAP, 6)
    );


    public static void addRandomLoot(BarrelBlockEntity barrelBlockEntity, ServerWorld world, List<LootEntry> lootTable, Random random) {
        int rolls = random.nextInt(5) + 4; // Rolls between 4 and 8

        for (int i = 0; i < rolls; i++) {
            ItemStack lootItem = getRandomWeightedLoot(lootTable, world, random);
            barrelBlockEntity.setStack(random.nextInt(27), lootItem);
        }
        barrelBlockEntity.markDirty();
    }

    private static ItemStack getRandomWeightedLoot(List<LootEntry> lootTable, ServerWorld world, Random random) {
        int totalWeight = lootTable.stream().mapToInt(e -> e.weight).sum();
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (LootEntry entry : lootTable) {
            currentWeight += entry.weight;
            if (randomWeight < currentWeight) {
                int count = entry.minCount + random.nextInt(entry.maxCount - entry.minCount + 1);
                ItemStack itemStack = new ItemStack(entry.item, count);

                if (entry.item == Items.ENCHANTED_BOOK) {
                    itemStack = getOverleveledEnchantedBook(world, random);
                }

                if (entry.item == Items.STICK) {
                    itemStack = getKnockbackStick(world
                    );
                }

                return itemStack;
            }
        }

        return new ItemStack(Items.AIR); // Fallback
    }

    public static ItemStack getOverleveledEnchantedBook(ServerWorld world, Random random) {

        // Create an enchanted book
        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);

        // Define possible enchantments
        List<RegistryKey<Enchantment>> possibleEnchantments = List.of(
                Enchantments.SWIFT_SNEAK,
                Enchantments.FEATHER_FALLING,
                Enchantments.RESPIRATION
        );

        // Select a random enchantment
        RegistryKey<Enchantment> selectedEnchantment = possibleEnchantments.get(random.nextInt(possibleEnchantments.size()));
        Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantment = world.getRegistryManager().getOptionalEntry(selectedEnchantment);

        // Check if the enchantment exists
        if (optionalEnchantment.isEmpty()) {
            return enchantedBook;
        }

        Optional<Registry<Enchantment>> optionalEnchantmentRegistry = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);

        // Check if the enchantment exists
        if (optionalEnchantmentRegistry.isEmpty()) {
            return enchantedBook;
        }

        Registry<Enchantment> enchantmentRegistry = optionalEnchantmentRegistry.get();

        // Get the enchantment value
        Enchantment enchantment = optionalEnchantment.get().value();
        // Get an over-leveled enchantment level
        int overleveledLevel = enchantment.getMaxLevel() + 1;

        // Add the enchantment properly using EnchantedBookItem
        enchantedBook.addEnchantment(enchantmentRegistry.getEntry(enchantment), overleveledLevel);

        return enchantedBook;
    }

    public static ItemStack getKnockbackStick(ServerWorld world) {

        // Create an enchanted book
        ItemStack stick = new ItemStack(Items.ENCHANTED_BOOK);
        // Select a random enchantment
        RegistryKey<Enchantment> selectedEnchantment = Enchantments.KNOCKBACK;
        Optional<RegistryEntry.Reference<Enchantment>> optionalEnchantment = world.getRegistryManager().getOptionalEntry(selectedEnchantment);

        // Check if the enchantment exists
        if (optionalEnchantment.isEmpty()) {
            return stick;
        }

        Optional<Registry<Enchantment>> optionalEnchantmentRegistry = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);

        // Check if the enchantment exists
        if (optionalEnchantmentRegistry.isEmpty()) {
            return stick;
        }

        Registry<Enchantment> enchantmentRegistry = optionalEnchantmentRegistry.get();

        // Get the enchantment value
        Enchantment enchantment = optionalEnchantment.get().value();

        // Add the enchantment properly using EnchantedBookItem
        stick.addEnchantment(enchantmentRegistry.getEntry(enchantment), 4);

        return stick;
    }


    public static class LootEntry {
        public final Item item;
        public final int weight;
        public final int minCount;
        public final int maxCount;

        public LootEntry(Item item, int weight) {
            this(item, weight, 1, 1);
        }

        public LootEntry(Item item, int weight, int minCount, int maxCount) {
            this.item = item;
            this.weight = weight;
            this.minCount = minCount;
            this.maxCount = maxCount;
        }
    }
}
