package com.backrooms;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class CustomChunkGenerator extends ChunkGenerator {
    public static final MapCodec<CustomChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(CustomChunkGenerator::getBiomeSource)
            ).apply(instance, CustomChunkGenerator::new)
    );

    private final BiomeSource biomeSource;
    private final Random random;
    private final Integer seed;

    public CustomChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
        this.biomeSource = biomeSource;
        this.random = new Random();
        this.seed = random.nextInt();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int chunkX = chunk.getPos().x * 16;
        int chunkZ = chunk.getPos().z * 16;

        for (int sectionX = 0; sectionX < 16; sectionX++) {
            for (int sectionZ = 0; sectionZ < 16; sectionZ++) {
                pos.set(chunkX + sectionX, 51, chunkZ + sectionZ);
                // Only place a barrel if it's an open space
                if (random.nextFloat() < 0.0025 && chunk.getBlockState(pos).isAir()) { // 0.25% chance per block
                    chunk.setBlockState(pos, Blocks.BARREL.getDefaultState(), false); // Place the block
                    BarrelBlockEntity barrelBlockEntity = new BarrelBlockEntity(pos, Blocks.BARREL.getDefaultState());
                    chunk.setBlockEntity(barrelBlockEntity);
                    List<Loot.LootEntry> loot;
                    if (random.nextFloat() > 0.99) { // 1% chance of ultra loot
                        loot = Loot.ultraLoot;
                    } else if (random.nextFloat() > 0.95) { // 5% chance of rare loot
                        loot = Loot.rareLoot;
                    } else {
                        loot = Loot.barrelLoot;
                    }
                    Loot.addRandomLoot(barrelBlockEntity, world.toServerWorld(), loot, random);
                }
            }
        }
    }

    @Override
    public StructurePlacementCalculator createStructurePlacementCalculator(RegistryWrapper<StructureSet> structureSetRegistry, NoiseConfig noiseConfig, long seed) {
        return StructurePlacementCalculator.create(noiseConfig, seed, getBiomeSource(), Stream.<RegistryEntry<StructureSet>>builder().build());
    }

    private boolean isSpiderChunk(double chunkX, double chunkZ) {
        return Math.abs(OpenSimplex2S.noise2(seed + 1, chunkX, chunkZ)) > 0.6;
    }

    private boolean isWaterChunk(double chunkX, double chunkZ) {
        return Math.abs(OpenSimplex2S.noise2(seed + 2, chunkX, chunkZ)) > 0.9;
    }

    private boolean isSculkChunk(double chunkX, double chunkZ) {
        return Math.abs(OpenSimplex2S.noise2(seed + 3, chunkX, chunkZ)) > 0.9;
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int chunkX = chunk.getPos().x * 16;
        int chunkZ = chunk.getPos().z * 16;
        double noiseX = (double) chunk.getPos().x / 16;
        double noiseY = (double) chunk.getPos().z / 16;

        String biomeKey = determineBiomeFromNoise(noiseX, noiseY);
        Block[] palette = BIOME_BLOCK_PALETTES.getOrDefault(biomeKey, BIOME_BLOCK_PALETTES.get("Level 0"));

        boolean spiderChunk = isSpiderChunk(noiseX, noiseY);
        boolean waterChunk = isWaterChunk(noiseX, noiseY);
        boolean sculkChunk = isSculkChunk(noiseX, noiseY);

        // Build surface using the biome's block palette
        for (int sectionX = 0; sectionX < 16; sectionX++) {
            for (int sectionZ = 0; sectionZ < 16; sectionZ++) {
                BlockPos newPos = new BlockPos(chunkX + sectionX, 2, chunkZ + sectionZ);
                chunk.setBlockState(newPos, Blocks.BEDROCK.getDefaultState(), false);
                for (int y = 0; y < 5; y++) {
                    pos.set(chunkX + sectionX, y + 50, chunkZ + sectionZ);
                    if (chunk.getBlockState(pos).isOf(Blocks.STONE_BRICKS)) {
                        chunk.setBlockState(pos, palette[2].getDefaultState(), false);
                    } else if (chunk.getBlockState(pos).isOf(Blocks.LIGHT)) {
                        if (random.nextFloat() > 0.5) {
                            chunk.setBlockState(pos, palette[2].getDefaultState(), false);
                        } else {
                            if (palette[3] == Blocks.REDSTONE_LAMP && !sculkChunk) {
                                chunk.setBlockState(pos, Blocks.REDSTONE_LAMP.getDefaultState().with(Properties.LIT, true), false); // Ensure powered
                                chunk.setBlockState(pos.add(0, 1, 0), Blocks.LEVER.getDefaultState().with(Properties.POWERED, true).with(Properties.HORIZONTAL_FACING, Direction.NORTH).with(Properties.BLOCK_FACE, BlockFace.FLOOR), false); // Place powered lever above
                            } else {
                                chunk.setBlockState(pos, !sculkChunk ? palette[3].getDefaultState() : Blocks.REDSTONE_LAMP.getDefaultState(), false);
                            }
                        }
                    } else if (chunk.getBlockState(pos).isOf(Blocks.COBBLESTONE)) {
                        chunk.setBlockState(pos, palette[0].getDefaultState(), false);
                    } else if (chunk.getBlockState(pos).isOf(Blocks.GRAVEL)) {
                        chunk.setBlockState(pos, palette[1].getDefaultState(), false);
                    } else if (chunk.getBlockState(pos).isOf(Blocks.STONE)) {
                        if (waterChunk) {
                            chunk.setBlockState(pos, Blocks.WATER.getDefaultState(), false); // Water
                        } else if (spiderChunk && random.nextFloat() > 0.75) {
                            chunk.setBlockState(pos, Blocks.COBWEB.getDefaultState(), false); // Cobweb
                        } else {
                            chunk.setBlockState(pos, Blocks.AIR.getDefaultState(), false); // Air
                        }
                    }
                }
                if (sculkChunk) {
                    pos.set(chunkX + sectionX, 55, chunkZ + sectionZ);
                    int block = SculkChunk.sculkChunk[sectionX][sectionZ];
                    if (block != 0) {
                        if (block == 1) {
                            chunk.setBlockState(pos, Blocks.REDSTONE_WIRE.getDefaultState(), false);
                        } else if (block == 2) {
                            chunk.setBlockState(pos, Blocks.SCULK_SENSOR.getDefaultState(), false);
                        }
                    }
                }
            }
        }
    }

    private String determineBiomeFromNoise(double x, double z) {
        float noiseValue = OpenSimplex2S.noise2(this.seed, x, z);
        // Map noise value to different backrooms levels
        if (noiseValue < -0.5) {
            return "Level 0";  // Default office-like level
        } else if (noiseValue < 0) {
            return "Level Red"; // Red-themed variation
        } else if (noiseValue < 0.5) {
            return "Level 1";   // Dark, abandoned office
        } else {
            return "Level 2";   // Weird, distorted desert-like level
        }
    }

    // Define block palettes per biome
    private static final HashMap<String, Block[]> BIOME_BLOCK_PALETTES = new HashMap<>() {{
        put("Level 0", new Block[]{
                Blocks.STRIPPED_BIRCH_LOG,
                Blocks.SANDSTONE,
                Blocks.POLISHED_DIORITE,
                Blocks.OCHRE_FROGLIGHT
        });
        put("Level Red", new Block[]{
                Blocks.STRIPPED_ACACIA_LOG,
                Blocks.SANDSTONE,
                Blocks.POLISHED_DIORITE,
                Blocks.OCHRE_FROGLIGHT
        });
        put("Level 1", new Block[]{
                Blocks.STONE,
                Blocks.COBBLESTONE,
                Blocks.SMOOTH_STONE,
                Blocks.SEA_LANTERN
        });
        put("Level 2", new Block[]{
                Blocks.OAK_LOG,
                Blocks.OAK_PLANKS,
                Blocks.OAK_PLANKS,
                Blocks.REDSTONE_LAMP
        });
    }};

    @Override
    public CompletableFuture<Chunk> populateBiomes(NoiseConfig noiseConfig, Blender blender, StructureAccessor structureAccessor, Chunk chunk) {
        BiomeSource biomeSource = this.getBiomeSource();
        chunk.populateBiomes(biomeSource, noiseConfig.getMultiNoiseSampler());
        return CompletableFuture.completedFuture(chunk);
    }
    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk) {

    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int chunkX = chunk.getPos().x * 16;
        int chunkZ = chunk.getPos().z * 16;

        boolean use16x16 = random.nextFloat() > 0.95;
        if (use16x16) {
            // 1) Pick the "16x16" pattern (which is actually a 4x4 array of indexes).
            int[][] patternOfIndexes = patterns16x16.get(random.nextInt(patterns16x16.size()));

            // We'll treat each cell (x,z) in patternOfIndexes as an index into the `patterns` list.
            // That sub-pattern is then 4x4, so placing it expands to 16x16 in total.
            for (int bigX = 0; bigX < 4; bigX++) {
                for (int bigZ = 0; bigZ < 4; bigZ++) {
                    // 2) This index points to which 4x4 pattern to use.
                    int index = patternOfIndexes[bigX][bigZ];

                    // Safety check (optional) to avoid out-of-range errors:
                    if (index < 0 || index >= patterns.size()) {
                        // If it's out of range, you can clamp or skip:
                        continue;
                    }

                    int[][] subPattern = patterns.get(index);

                    // 3) Now place that subPattern in the 4×4 area:
                    //    from [bigX*4..bigX*4+3] to [bigZ*4..bigZ*4+3].
                    for (int subX = 0; subX < 4; subX++) {
                        for (int subZ = 0; subZ < 4; subZ++) {
                            // Check if the sub-pattern says "block" (1) or "air" (0).
                            boolean isBlock = subPattern[subX][subZ] == 1;
                            for (int y = 0; y < 5; y++) {
                                // Convert to actual world coordinates.
                                pos.set(
                                        chunkX + (bigX * 4) + subX,
                                        y + 50,
                                        chunkZ + (bigZ * 4) + subZ
                                );

                                if (y == 4) {
                                    // “Ceiling” layer: center 2×2 uses LIGHT, else STONE_BRICKS
                                    if (subX > 0 && subX < 3 && subZ > 0 && subZ < 3) {
                                        chunk.setBlockState(pos, Blocks.LIGHT.getDefaultState(), false);
                                    } else {
                                        chunk.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState(), false);
                                    }
                                } else if (y == 0) {
                                    // “Floor” layer
                                    chunk.setBlockState(pos, Blocks.GRAVEL.getDefaultState(), false);
                                } else {
                                    // “Walls” or “Air” placeholders
                                    if (isBlock) {
                                        chunk.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState(), false);
                                    } else {
                                        chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // The original 4×4 pattern logic remains exactly the same.
            for (int sectionX = 0; sectionX < 16; sectionX += 4) {
                for (int sectionZ = 0; sectionZ < 16; sectionZ += 4) {
                    int[][] pattern = patterns.get(random.nextInt(patterns.size()));

                    for (int x = 0; x < 4; x++) {
                        for (int z = 0; z < 4; z++) {
                            boolean isBlock = pattern[x][z] == 1;
                            for (int y = 0; y < 5; y++) {
                                pos.set(chunkX + sectionX + x, y + 50, chunkZ + sectionZ + z);
                                if (y == 4) {
                                    if (x > 0 && x < 3 && z > 0 && z < 3) {
                                        chunk.setBlockState(pos, Blocks.LIGHT.getDefaultState(), false);
                                    } else {
                                        chunk.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState(), false);
                                    }
                                } else if (y == 0) {
                                    chunk.setBlockState(pos, Blocks.GRAVEL.getDefaultState(), false);
                                } else if (isBlock) {
                                    chunk.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState(), false);
                                } else {
                                    chunk.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                                }
                            }
                        }
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // No entities
    }

    @Override
    public int getWorldHeight() {
        return 128;
    }

    @Override
    public int getMinimumY() {
        return 0;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return 0;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        return new VerticalBlockSample(0, new net.minecraft.block.BlockState[]{
                Blocks.BEDROCK.getDefaultState(),
                Blocks.STONE.getDefaultState()
        });
    }

    @Override
    public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
    }

    @Override
    public BiomeSource getBiomeSource() {
        return this.biomeSource;
    }

    // Define patterns
    private static final List<int[][]> patterns = List.of(
            new int[][]{
                    {0, 0, 0, 0},
                    {0, 0, 0, 0},
                    {0, 0, 0, 0},
                    {0, 0, 0, 0}
            },
            new int[][]{
                    {0, 0, 0, 0},
                    {0, 1, 1, 0},
                    {0, 1, 1, 0},
                    {0, 0, 0, 0}
            },
            new int[][]{
                    {1, 1, 1, 1},
                    {1, 1, 1, 1},
                    {1, 1, 1, 1},
                    {1, 1, 1, 1}
            },
            new int[][]{
                    {0, 0, 0, 0},
                    {0, 0, 0, 0},
                    {0, 0, 0, 0},
                    {1, 1, 1, 1}
            },
            new int[][]{
                    {0, 0, 0, 1},
                    {0, 0, 0, 1},
                    {0, 0, 0, 1},
                    {0, 0, 0, 1}
            },
            new int[][]{
                    {1, 0, 0, 0},
                    {1, 0, 0, 0},
                    {1, 0, 0, 0},
                    {1, 0, 0, 0}
            },
            new int[][]{
                    {1, 1, 1, 1},
                    {0, 0, 0, 0},
                    {0, 0, 0, 0},
                    {0, 0, 0, 0}
            }
    );

    private static final List<int[][]> patterns16x16 = List.of(
            new int[][]{
                    {0, 0, 0, 0},
                    {0, 0, 0, 0},
                    {0, 0, 0, 0},
                    {0, 0, 0, 0}
            },
            new int[][]{
                    {1, 1, 1, 1},
                    {1, 1, 1, 1},
                    {1, 1, 1, 1},
                    {1, 1, 1, 1}
            },
            new int[][]{
                    {2, 2, 2, 2},
                    {2, 2, 2, 2},
                    {2, 2, 2, 2},
                    {2, 2, 2, 2}
            }
    );
}
