package genesis.mc.world;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import genesis.api.planet.PlanetDefinition;
import genesis.core.ecology.BiomeKind;
import genesis.core.ecology.EcologyModel;
import genesis.core.math.Noise;
import genesis.core.terrain.HydrologyModel;
import genesis.core.terrain.TerrainModel;
import genesis.core.terrain.TerrainSample;
import genesis.mc.planet.GenesisPlanets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * The chunk generator of a Genesis planet.
 *
 * <p>Unlike the vanilla generator, the height of the world is not produced by a three dimensional
 * density function. It is produced by the Genesis terrain model, which is evaluated on the surface
 * of the planet, and then corrected by the hydrology model, which carves the river beds and the
 * lake basins into it. The result is a world that is seamless across the world boundary and whose
 * water behaves the way water behaves on a real planet.
 */
public class GenesisChunkGenerator extends ChunkGenerator {

    public static final MapCodec<GenesisChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            com.mojang.serialization.Codec.STRING.fieldOf("planet").forGetter(generator -> generator.planet.id())
    ).apply(instance, GenesisChunkGenerator::new));

    /** The lowest block of the world. */
    public static final int MIN_Y = -64;
    /** How many blocks above the surface are generated. */
    public static final int GEN_DEPTH = 160;
    /** The key under which the seed of the world is derived from the random state. */
    private static final net.minecraft.resources.Identifier SEED_KEY =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("genesis", "world_seed");

    private final PlanetDefinition planet;
    private volatile Models models;

    public GenesisChunkGenerator(BiomeSource biomeSource, String planetId) {
        super(biomeSource);
        this.planet = GenesisPlanets.getOrThrow(planetId);
        if (biomeSource instanceof GenesisBiomeSource source) {
            // The biome source needs the same models as the generator, so it asks the generator
            // instead of keeping a copy of its own.
            source.attach(this);
        }
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public PlanetDefinition planet() {
        return planet;
    }

    // ---------------------------------------------------------------------
    // models
    // ---------------------------------------------------------------------

    /**
     * The models of this world, created when they are needed.
     *
     * <p>The seed of the world is taken from the random state of the generator, which is derived
     * from the seed of the world. That way the models are correct in every stage of the generation
     * pipeline, no matter which of them happens to run first.
     */
    private Models models(RandomState randomState) {
        long seed = seedOf(randomState);
        Models local = models;
        if (local == null || local.seed != seed) {
            synchronized (this) {
                local = models;
                if (local == null || local.seed != seed) {
                    local = new Models(seed, planet);
                    models = local;
                    GenesisModels.publish(planet.id(), local);
                }
            }
        }
        return local;
    }

    /**
     * The models that are currently in use.
     *
     * <p>They are {@code null} only before the world started generating, which never happens while
     * a chunk is being built.
     */
    public Models currentModels() {
        return models;
    }

    /**
     * The distribution of the magic over this world.
     *
     * <p>The distribution follows from the seed of the world, so it is the same in every dimension
     * that shares the seed and it never has to be stored. The influences that were applied to it
     * do have to be stored, and they live in the magic data of the dimension.
     *
     * @param randomState the random state of the world, which carries its seed
     */
    public genesis.core.magic.MagicFieldModel magicModel(RandomState randomState) {
        return models(randomState).magicModel();
    }

    /** Derives the seed of the world from the random state of the generator. */
    private static long seedOf(RandomState randomState) {
        if (randomState == null) {
            return 0L;
        }
        try {
            return randomState.getOrCreateRandomFactory(SEED_KEY).at(0, 0, 0).nextLong();
        } catch (RuntimeException exception) {
            // The random factory is not available, the aquirer is used instead.
            return randomState.aquiferRandom().at(0, 0, 0).nextLong();
        }
    }

    /** The bundled terrain, hydrology and ecology models. */
    public static final class Models {
        final long seed;
        final TerrainModel terrain;
        final HydrologyModel hydrology;
        final EcologyModel ecology;
        final genesis.core.magic.MagicFieldModel magicModel;
        final Noise caveNoise;
        final Noise tunnelNoise;

        Models(long seed, PlanetDefinition planet) {
            this.seed = seed;
            this.terrain = new TerrainModel(planet, seed);
            this.hydrology = new HydrologyModel(this.terrain, planet);
            this.ecology = new EcologyModel(this.terrain, planet);
            // The magic of the world is initialised while the world is generated, because that is
            // the moment the seed of the world is known.
            // The distribution of the magic follows from the seed, so it is the same everywhere and
            // does not have to be stored. The influences that were applied to it do have to be
            // stored, and they live in the magic data of the dimension.
            this.magicModel = new genesis.core.magic.MagicFieldModel(planet, seed);
            this.caveNoise = new Noise(seed).fork("caves");
            this.tunnelNoise = new Noise(seed).fork("tunnels");
        }

        /** The distribution of the magic over this world. */
        public genesis.core.magic.MagicFieldModel magicModel() {
            return magicModel;
        }

        /** The temperature of the ground at a position. */
        public genesis.api.physical.Temperature temperatureAt(int x, int z) {
            TerrainSample sample = terrain.sample(x, z);
            return ecology.climate(x, z, sample.height(), sample.continentalness(), 0.0).temperature();
        }

        /** How deep the water stands at a position. */
        public double waterDepthAt(int x, int z) {
            return hydrology.waterDepthAt(x, z);
        }

        /** Whether the water at a position is frozen over. */
        public boolean isFrozen(int x, int z) {
            TerrainSample sample = terrain.sample(x, z);
            return ecology.climate(x, z, sample.height(), sample.continentalness(), 0.0).isFreezing();
        }
    }

    // ---------------------------------------------------------------------
    // height
    // ---------------------------------------------------------------------

    /** The height of the ground at a position, after rivers and lakes have been applied. */
    public int heightAt(int worldX, int worldZ, LevelHeightAccessor accessor) {
        Models local = currentModels();
        if (local == null) {
            local = models(null);
        }
        TerrainSample sample = local.terrain.sample(worldX, worldZ);
        double height = sample.height();

        double waterDepth = local.hydrology.waterDepthAt(worldX, worldZ);
        if (waterDepth > 0.3) {
            // A basin: the ground drops to the floor of the lake.
            height = Math.min(height, local.hydrology.terrainFloor(worldX, worldZ));
        }
        double riverCut = local.hydrology.riverDepth(worldX, worldZ);
        if (riverCut > 0.0) {
            height -= riverCut;
        }
        int result = (int) Math.floor(height);
        return Math.max(MIN_Y + 1, Math.min(accessor.getMaxY() - 2, result));
    }

    /** The level of the water surface at a position, or {@link Integer#MIN_VALUE} when dry. */
    public int waterLevelAt(int worldX, int worldZ, LevelHeightAccessor accessor) {
        Models local = currentModels();
        if (local == null) {
            local = models(null);
        }
        int seaLevel = getSeaLevel();
        int height = heightAt(worldX, worldZ, accessor);
        double depth = local.hydrology.waterDepthAt(worldX, worldZ);
        if (depth > 0.3) {
            return (int) Math.floor(local.hydrology.waterSurfaceAt(worldX, worldZ));
        }
        if (height < seaLevel) {
            return seaLevel;
        }
        if (local.hydrology.isRiver(worldX, worldZ)) {
            return (int) Math.floor(local.hydrology.waterSurfaceAt(worldX, worldZ));
        }
        return Integer.MIN_VALUE;
    }

    // ---------------------------------------------------------------------
    // ChunkGenerator implementation
    // ---------------------------------------------------------------------

    @Override
    public int getSeaLevel() {
        return (int) Math.round(planet.terrain().seaLevel());
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        models(randomState);
        return heightAt(x, z, heightAccessor) + 1;
    }

    @Override
    public net.minecraft.world.level.NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor,
            RandomState randomState) {
        models(randomState);
        int minY = heightAccessor.getMinY();
        int height = heightAccessor.getHeight();
        BlockState[] states = new BlockState[height];
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        java.util.Arrays.fill(states, air);
        int top = Math.max(minY, Math.min(minY + height - 1, heightAt(x, z, heightAccessor)));
        for (int y = minY; y <= top; y++) {
            states[y - minY] = stone;
        }
        return new net.minecraft.world.level.NoiseColumn(minY, states);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
            StructureManager structureManager, ChunkAccess chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinY();
        int maxY = minY + chunk.getHeight() - 1;
        LevelHeightAccessor accessor = chunk.getHeightAccessorForGeneration();
        Models local = models(randomState);
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;
                int ground = heightAt(worldX, worldZ, accessor);
                int waterLevel = waterLevelAt(worldX, worldZ, accessor);
                for (int y = minY; y < ground; y++) {
                    BlockState state = y < 0 ? deepslate : stone;
                    set(chunk, localX, y, localZ, state);
                }
                if (waterLevel > ground) {
                    for (int y = ground; y <= Math.min(maxY, waterLevel - 1); y++) {
                        set(chunk, localX, y, localZ, water);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState,
            ChunkAccess chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        LevelHeightAccessor accessor = chunk.getHeightAccessorForGeneration();
        Models local = models(randomState);

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;
                int ground = heightAt(worldX, worldZ, accessor);
                int waterLevel = waterLevelAt(worldX, worldZ, accessor);
                double depth = waterLevel == Integer.MIN_VALUE ? 0.0 : Math.max(0, waterLevel - ground);
                boolean frozen = local.ecology.climate(worldX, worldZ, ground, 0.0, depth).isFreezing();
                BiomeKind kind = local.ecology.classifyAt(worldX, worldZ, depth, frozen);

                BlockState surface = surfaceBlock(kind, depth, frozen);
                BlockState subsurface = subsurfaceBlock(kind, depth);
                set(chunk, localX, ground, localZ, surface);
                if (ground - 1 >= chunk.getMinY()) {
                    set(chunk, localX, ground - 1, localZ, subsurface);
                }
            }
        }
    }

    private static BlockState surfaceBlock(BiomeKind kind, double depth, boolean frozen) {
        if (depth > 0.0) {
            if (frozen) {
                return Blocks.GRAVEL.defaultBlockState();
            }
            return depth < 5.0 ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
        }
        return switch (kind) {
            case DESERT, BEACH -> Blocks.SAND.defaultBlockState();
            case SAVANNA -> Blocks.COARSE_DIRT.defaultBlockState();
            case SNOWY_TUNDRA, SNOWY_PEAKS, FROZEN_RIVER, FROZEN_OCEAN -> Blocks.SNOW_BLOCK.defaultBlockState();
            case MOUNTAIN -> Blocks.STONE.defaultBlockState();
            case NETHER_WASTES -> Blocks.NETHERRACK.defaultBlockState();
            default -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    private static BlockState subsurfaceBlock(BiomeKind kind, double depth) {
        if (depth > 0.0) {
            return Blocks.SAND.defaultBlockState();
        }
        return switch (kind) {
            case DESERT, BEACH -> Blocks.SAND.defaultBlockState();
            case SAVANNA -> Blocks.DIRT.defaultBlockState();
            case SNOWY_TUNDRA, SNOWY_PEAKS -> Blocks.DIRT.defaultBlockState();
            case NETHER_WASTES -> Blocks.NETHERRACK.defaultBlockState();
            default -> Blocks.DIRT.defaultBlockState();
        };
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
            StructureManager structureManager, ChunkAccess chunk) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinY();
        int maxY = minY + chunk.getHeight() - 1;
        LevelHeightAccessor accessor = chunk.getHeightAccessorForGeneration();
        Models local = models(randomState);
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;
                int ground = heightAt(worldX, worldZ, accessor);
                for (int y = minY; y < Math.min(ground, maxY); y++) {
                    double cheese = local.caveNoise.perlin3(worldX / 48.0, y / 34.0, worldZ / 48.0);
                    if (cheese > 0.58) {
                        set(chunk, localX, y, localZ, air);
                        continue;
                    }
                    double tunnelA = local.tunnelNoise.perlin3(worldX / 22.0, y / 16.0, worldZ / 22.0);
                    double tunnelB = local.tunnelNoise.perlin3(worldX / 22.0 + 91.3, y / 16.0 + 47.1,
                            worldZ / 22.0 - 13.7);
                    if (Math.abs(tunnelA) < 0.055 && Math.abs(tunnelB) < 0.055) {
                        set(chunk, localX, y, localZ, air);
                    }
                }
            }
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState randomState, Blender blender,
            StructureManager structureManager, ChunkAccess chunk) {
        // The models are created before the biomes are chosen, so that the biome source always
        // works with the terrain of the world it belongs to.
        models(randomState);
        return super.createBiomes(randomState, blender, structureManager, chunk);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Genesis relies on the biome spawn rules, no extra mobs are injected.
    }

    @Override
    public void applyBiomeDecoration(net.minecraft.world.level.WorldGenLevel level, ChunkAccess chunk,
            net.minecraft.world.level.StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        // The structures of Genesis are placed after the vanilla decoration, so that the roads and
        // the buildings always end up on top of the finished ground.
        GenesisStructures.place(level, chunk);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos pos) {
        list.add("Genesis planet: " + planet.displayName() + " (" + planet.id() + ")");
        list.add("Radius: " + planet.radius() + " blocks");
        list.add("Gravity: " + planet.gravity());
        Models local = currentModels();
        if (local != null) {
            list.add("Temperature: " + local.temperatureAt(pos.getX(), pos.getZ()).format());
            genesis.api.magic.MagicConcentration concentration = local.magicModel().concentration(pos.getX(), pos.getZ());
            list.add("Magic concentration: " + String.format("%.3f", concentration.value())
                    + " (" + concentration.tier() + ")");
        }
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static void set(ChunkAccess chunk, int localX, int y, int localZ, BlockState state) {
        int minY = chunk.getMinY();
        int sectionIndex = (y - minY) >> 4;
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
            return;
        }
        int localY = (y - minY) & 15;
        chunk.getSection(sectionIndex).setBlockState(localX, localY, localZ, state, false);
    }

    /** The biome source of this generator, typed to the Genesis implementation when possible. */
    public GenesisBiomeSource genesisBiomeSource() {
        return biomeSource instanceof GenesisBiomeSource source ? source : null;
    }
}

