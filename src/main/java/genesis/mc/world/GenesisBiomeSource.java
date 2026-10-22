package genesis.mc.world;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import genesis.core.ecology.BiomeKind;
import genesis.core.terrain.TerrainModel;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

/**
 * The biome source of a Genesis world.
 *
 * <p>Instead of the vanilla multi noise spline, Genesis classifies every position with its own
 * ecology model: the temperature comes from the latitude and the altitude, the moisture from a
 * large scale pattern boosted near the coast and next to running water, and the resulting pair is
 * mapped onto a biome family.
 *
 * <p>The biome families are then bound to concrete biomes, which by default are the vanilla ones
 * but can be replaced per family by other mods.
 */
public class GenesisBiomeSource extends BiomeSource {

    public static final MapCodec<GenesisBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("planet", "genesis:terran")
                    .forGetter(source -> source.planetId)
    ).apply(instance, GenesisBiomeSource::new));

    private final HolderGetter<Biome> biomeGetter;
    private final String planetId;
    private volatile Map<BiomeKind, Holder<Biome>> resolved;
    private volatile GenesisChunkGenerator generator;

    public GenesisBiomeSource(HolderGetter<Biome> biomeGetter, String planetId) {
        this.biomeGetter = biomeGetter;
        this.planetId = planetId;
    }

    /**
     * Connects the source to the generator that owns it.
     *
     * <p>The generator holds the generation models of the world, so the source asks the generator
     * for them instead of keeping a second copy that could drift apart.
     */
    public void attach(GenesisChunkGenerator generator) {
        this.generator = generator;
    }

    private Map<BiomeKind, Holder<Biome>> biomes() {
        Map<BiomeKind, Holder<Biome>> local = resolved;
        if (local == null) {
            synchronized (this) {
                local = resolved;
                if (local == null) {
                    local = new EnumMap<>(BiomeKind.class);
                    for (BiomeKind kind : BiomeKind.values()) {
                        local.put(kind, biomeGetter.getOrThrow(defaultKey(kind)));
                    }
                    resolved = local;
                }
            }
        }
        return local;
    }

    /** The vanilla biome a biome family is bound to. */
    public static ResourceKey<Biome> defaultKey(BiomeKind kind) {
        return switch (kind) {
            case PLAINS -> Biomes.PLAINS;
            case FOREST -> Biomes.FOREST;
            case TAIGA -> Biomes.TAIGA;
            case SNOWY_TUNDRA -> Biomes.SNOWY_PLAINS;
            case DESERT -> Biomes.DESERT;
            case SAVANNA -> Biomes.SAVANNA;
            case JUNGLE -> Biomes.JUNGLE;
            case SWAMP -> Biomes.SWAMP;
            case MOUNTAIN -> Biomes.MEADOW;
            case SNOWY_PEAKS -> Biomes.FROZEN_PEAKS;
            case BEACH -> Biomes.BEACH;
            case OCEAN -> Biomes.OCEAN;
            case RIVER -> Biomes.RIVER;
            case FROZEN_RIVER -> Biomes.FROZEN_RIVER;
            case FROZEN_OCEAN -> Biomes.FROZEN_OCEAN;
            case MUSHROOM_FIELDS -> Biomes.MUSHROOM_FIELDS;
            case NETHER_WASTES -> Biomes.NETHER_WASTES;
        };
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes().values().stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        BiomeKind kind = classify(x, y, z);
        return biomes().getOrDefault(kind, biomes().get(BiomeKind.PLAINS));
    }

    /** Classifies a position into a biome family using the ecology model of the planet. */
    public BiomeKind classify(int x, int y, int z) {
        GenesisChunkGenerator.Models models = models();
        if (models == null) {
            return BiomeKind.PLAINS;
        }
        double depth = models.waterDepthAt(x, z);
        boolean frozen = models.isFrozen(x, z);
        return models.ecology.classifyAt(x, z, depth, frozen);
    }

    /** The models of the world this source belongs to. */
    private GenesisChunkGenerator.Models models() {
        GenesisChunkGenerator local = generator;
        if (local != null) {
            GenesisChunkGenerator.Models models = local.currentModels();
            if (models != null) {
                return models;
            }
        }
        return GenesisModels.get(planetId);
    }

    /** The id of the planet this source belongs to. */
    public String planetId() {
        return planetId;
    }

    /** The terrain model of the planet, may be {@code null} before the world seed is known. */
    public TerrainModel terrain() {
        GenesisChunkGenerator.Models models = GenesisModels.get(planetId);
        return models == null ? null : models.terrain;
    }
}
