package genesis.core.ecology;

import genesis.api.physical.Temperature;
import genesis.api.planet.EcologyProfile;
import genesis.api.planet.PlanetDefinition;
import genesis.core.math.MathUtil;
import genesis.core.math.Noise;
import genesis.core.terrain.TerrainModel;
import genesis.core.terrain.TerrainSample;

/**
 * The ecology model of a planet.
 *
 * <p>Climate is derived from three independent drivers, in the same spirit as real climatology:
 * <ul>
 *   <li><strong>Latitude</strong> decides the baseline temperature, giving cold poles and a warm
 *       equator.</li>
 *   <li><strong>Altitude</strong> cools the air further, which is what puts snow on the peaks.</li>
 *   <li><strong>Moisture</strong> comes from a large scale noise pattern, boosted near the coast
 *       and next to running water.</li>
 * </ul>
 *
 * <p>Those three values are then mapped onto a {@link BiomeKind}.
 */
public final class EcologyModel {

    /** Above this temperature the peaks keep their snow. */
    private static final Temperature SNOWY_PEAKS_LIMIT = Temperature.ofKelvin(275.15);

    /** Below this temperature the ground stays frozen all year. */
    private static final Temperature SNOWY_TUNDRA_LIMIT = Temperature.ofKelvin(271.15);

    /** From this temperature on a dry place becomes a desert. */
    private static final Temperature DESERT_LIMIT = Temperature.ofKelvin(299.15);

    /** From this temperature on a wet place becomes a jungle. */
    private static final Temperature JUNGLE_LIMIT = Temperature.ofKelvin(298.15);

    /** From this temperature on a dry place becomes a savanna. */
    private static final Temperature SAVANNA_LIMIT = Temperature.ofKelvin(293.15);

    /** Below this temperature the land grows conifers instead of broad leaves. */
    private static final Temperature TAIGA_LIMIT = Temperature.ofKelvin(281.15);

    private final EcologyProfile profile;
    private final TerrainModel terrain;
    private final Noise moistureNoise;
    private final Noise weirdnessNoise;
    private final Noise detailNoise;

    public EcologyModel(TerrainModel terrain, PlanetDefinition planet) {
        this(terrain, planet.ecology());
    }

    public EcologyModel(TerrainModel terrain, EcologyProfile profile) {
        this.terrain = terrain;
        this.profile = profile;
        long seed = 0L;
        this.moistureNoise = new Noise(seed).fork("moisture");
        this.weirdnessNoise = new Noise(seed).fork("weirdness");
        this.detailNoise = new Noise(seed).fork("ecology-detail");
    }

    public EcologyProfile profile() {
        return profile;
    }

    /** Computes the climate at a position. */
    public Climate climate(double x, double z, double height, double continentalness, double riverDistance) {
        double latitude = Math.abs(terrain.latitudeOf(z)) / (Math.PI * 0.5);
        Temperature temperature = profile.equatorialTemperature()
                .minusKelvin(latitude * profile.polarTemperatureDrop())
                .minusKelvin(Math.max(0.0, height - terrain.seaLevel()) * profile.altitudeLapseRate());

        // The moisture pattern is sampled on the surface of the planet, so that it stays seamless
        // across the world boundary.
        genesis.core.topology.SurfacePoint point = terrain.surface().toSurface(x, z);
        double wavelength = Math.max(1.0, profile.moistureNoiseScale());
        double moistureNoiseValue = moistureNoise.fbm3(
                point.x() / wavelength, point.y() / wavelength, point.z() / wavelength, 4, 2.0, 0.5);
        double moisture = 0.5 + 0.5 * moistureNoiseValue;
        moisture += profile.coastlineMoisture() * Math.max(0.0, 1.0 - Math.abs(height - terrain.seaLevel()) / 24.0);
        moisture += profile.riverMoisture() * Math.max(0.0, 1.0 - riverDistance / 48.0);
        moisture = MathUtil.clamp(moisture, 0.0, 1.0);

        double weirdness = detailNoise.fbm3(point.x() / 320.0, point.y() / 320.0, point.z() / 320.0, 3, 2.0, 0.5);

        return new Climate(temperature, moisture, continentalness, weirdness);
    }

    /**
     * Classifies a position into a biome family.
     *
     * @param climate        the climate at the position
     * @param height         the terrain height
     * @param waterDepth     how deep the water is, {@code 0} on dry land
     * @param frozen         whether the water surface is frozen over
     */
    public BiomeKind classify(Climate climate, double height, double waterDepth, boolean frozen) {
        double seaLevel = terrain.seaLevel();

        if (waterDepth > 0.4) {
            if (frozen) {
                return BiomeKind.FROZEN_OCEAN;
            }
            return BiomeKind.OCEAN;
        }

        if (height <= seaLevel + 1.0) {
            // The shoreline.
            if (frozen) {
                return BiomeKind.FROZEN_RIVER;
            }
            return BiomeKind.RIVER;
        }

        double aboveSea = height - seaLevel;

        if (aboveSea > 96.0) {
            return climate.temperature().isAtMost(SNOWY_PEAKS_LIMIT) ? BiomeKind.SNOWY_PEAKS : BiomeKind.MOUNTAIN;
        }

        if (climate.temperature().isAtMost(SNOWY_TUNDRA_LIMIT)) {
            return BiomeKind.SNOWY_TUNDRA;
        }

        if (climate.temperature().isAtLeast(DESERT_LIMIT) && climate.moisture() < 0.25) {
            return BiomeKind.DESERT;
        }

        if (climate.temperature().isAtLeast(JUNGLE_LIMIT) && climate.moisture() > 0.7) {
            return BiomeKind.JUNGLE;
        }

        if (climate.temperature().isAtLeast(SAVANNA_LIMIT) && climate.moisture() < 0.4) {
            return BiomeKind.SAVANNA;
        }

        if (climate.moisture() > 0.78 && aboveSea < 12.0) {
            return BiomeKind.SWAMP;
        }

        if (climate.temperature().isBelow(TAIGA_LIMIT)) {
            return BiomeKind.TAIGA;
        }

        if (climate.moisture() > 0.48) {
            return BiomeKind.FOREST;
        }

        return BiomeKind.PLAINS;
    }

    /** Convenience helper that samples the terrain and classifies it in one go. */
    public BiomeKind classifyAt(double x, double z, double waterDepth, boolean frozen) {
        TerrainSample sample = terrain.sample(x, z);
        Climate climate = climate(x, z, sample.height(), sample.continentalness(), waterDepth);
        return classify(climate, sample.height(), waterDepth, frozen);
    }

    /** How much vegetation grows at a position, {@code 0} to {@code 1}. */
    public double vegetationDensity(BiomeKind biome, Climate climate) {
        double base = switch (biome) {
            case JUNGLE -> 1.6;
            case FOREST -> 1.2;
            case SWAMP -> 1.1;
            case PLAINS -> 0.55;
            case TAIGA -> 0.8;
            case SAVANNA -> 0.4;
            case DESERT -> 0.08;
            case SNOWY_TUNDRA -> 0.15;
            case MOUNTAIN -> 0.3;
            case SNOWY_PEAKS -> 0.02;
            case BEACH -> 0.03;
            case OCEAN, FROZEN_OCEAN, RIVER, FROZEN_RIVER, MUSHROOM_FIELDS, NETHER_WASTES -> 0.0;
        };
        return MathUtil.clamp(base * profile.treeDensity() * (0.6 + 0.4 * climate.moisture()), 0.0, 2.0);
    }
}
