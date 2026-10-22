package genesis;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import genesis.api.physical.Temperature;
import genesis.api.planet.AtmosphereSpec;
import genesis.api.planet.EcologyProfile;
import genesis.api.planet.HydrologyProfile;
import genesis.api.planet.PlanetDefinition;
import genesis.api.planet.SamplingMode;
import genesis.api.planet.TerrainProfile;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The codecs of the Genesis planets.
 *
 * <p>A planet is fully described by a handful of numbers, which is what makes it possible to add
 * new planets to the mod family without touching the generator.
 */
public final class GenesisPlanetCodecs {

    /** The codec of a sampling mode. */
    public static final com.mojang.serialization.Codec<SamplingMode> SAMPLING_MODE =
            com.mojang.serialization.Codec.STRING.xmap(
                    name -> SamplingMode.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
                    SamplingMode::name);

    /**
     * The codec of a temperature.
     *
     * <p>The value in the data file is the absolute temperature in kelvin, like everywhere else in
     * the mod family.
     */
    public static final com.mojang.serialization.Codec<Temperature> TEMPERATURE =
            com.mojang.serialization.Codec.DOUBLE.xmap(Temperature::ofKelvin, Temperature::kelvin);

    /** The codec of a terrain profile. */
    public static final MapCodec<TerrainProfile> TERRAIN = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.DOUBLE.fieldOf("sea_level").forGetter(TerrainProfile::seaLevel),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("continental_amplitude").forGetter(TerrainProfile::continentalAmplitude),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("continental_frequency").forGetter(TerrainProfile::continentalFrequency),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("erosion_amplitude").forGetter(TerrainProfile::erosionAmplitude),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("ridge_amplitude").forGetter(TerrainProfile::ridgeAmplitude),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("ridge_sharpness").forGetter(TerrainProfile::ridgeSharpness),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("detail_amplitude").forGetter(TerrainProfile::detailAmplitude),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("detail_frequency").forGetter(TerrainProfile::detailFrequency),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("height_variance").forGetter(TerrainProfile::heightVariance),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("plateau_flatness").forGetter(TerrainProfile::plateauFlatness)
    ).apply(instance, TerrainProfile::new));

    /** The codec of a hydrology profile. */
    public static final MapCodec<HydrologyProfile> HYDROLOGY = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.INT.fieldOf("cell_size").forGetter(HydrologyProfile::cellSize),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("rainfall").forGetter(HydrologyProfile::rainfall),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("sea_level_tolerance").forGetter(HydrologyProfile::seaLevelTolerance),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("river_threshold").forGetter(HydrologyProfile::riverThreshold),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("river_width_scale").forGetter(HydrologyProfile::riverWidthScale),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("river_depth_scale").forGetter(HydrologyProfile::riverDepthScale),
            com.mojang.serialization.Codec.INT.fieldOf("lake_fill_iterations").forGetter(HydrologyProfile::lakeFillIterations),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("tributary_decay").forGetter(HydrologyProfile::tributaryDecay)
    ).apply(instance, HydrologyProfile::new));

    /** The codec of an ecology profile. */
    public static final MapCodec<EcologyProfile> ECOLOGY = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TEMPERATURE.fieldOf("equatorial_temperature").forGetter(EcologyProfile::equatorialTemperature),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("polar_temperature_drop").forGetter(EcologyProfile::polarTemperatureDrop),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("altitude_lapse_rate").forGetter(EcologyProfile::altitudeLapseRate),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("moisture_noise_scale").forGetter(EcologyProfile::moistureNoiseScale),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("coastline_moisture").forGetter(EcologyProfile::coastlineMoisture),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("river_moisture").forGetter(EcologyProfile::riverMoisture),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("tree_density").forGetter(EcologyProfile::treeDensity),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("grass_color_saturation").forGetter(EcologyProfile::grassColorSaturation)
    ).apply(instance, EcologyProfile::new));

    /** The codec of an atmosphere. */
    public static final MapCodec<AtmosphereSpec> ATMOSPHERE = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.DOUBLE.fieldOf("density").forGetter(AtmosphereSpec::density),
            com.mojang.serialization.Codec.INT.fieldOf("sky_top_color").forGetter(AtmosphereSpec::skyTopColor),
            com.mojang.serialization.Codec.INT.fieldOf("sky_horizon_color").forGetter(AtmosphereSpec::skyHorizonColor),
            com.mojang.serialization.Codec.INT.fieldOf("fog_color").forGetter(AtmosphereSpec::fogColor),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("fog_distance").forGetter(AtmosphereSpec::fogDistance),
            com.mojang.serialization.Codec.INT.fieldOf("cloud_color").forGetter(AtmosphereSpec::cloudColor),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("cloud_coverage").forGetter(AtmosphereSpec::cloudCoverage),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("wind_strength").forGetter(AtmosphereSpec::windStrength),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("precipitation").forGetter(AtmosphereSpec::precipitation)
    ).apply(instance, AtmosphereSpec::new));

    /** The codec of a whole planet. */
    public static final MapCodec<PlanetDefinition> PLANET = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.STRING.fieldOf("id").forGetter(PlanetDefinition::id),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("display_name", "").forGetter(PlanetDefinition::displayName),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("radius").forGetter(PlanetDefinition::radius),
            com.mojang.serialization.Codec.DOUBLE.fieldOf("gravity").forGetter(PlanetDefinition::gravity),
            com.mojang.serialization.Codec.INT.fieldOf("half_span").forGetter(PlanetDefinition::halfSpan),
            SAMPLING_MODE.optionalFieldOf("sampling_mode", SamplingMode.CYLINDER).forGetter(PlanetDefinition::samplingMode),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("wrap_latitude", false).forGetter(PlanetDefinition::wrapLatitude),
            ATMOSPHERE.fieldOf("atmosphere").forGetter(PlanetDefinition::atmosphere),
            TERRAIN.fieldOf("terrain").forGetter(PlanetDefinition::terrain),
            HYDROLOGY.fieldOf("hydrology").forGetter(PlanetDefinition::hydrology),
            ECOLOGY.fieldOf("ecology").forGetter(PlanetDefinition::ecology)
    ).apply(instance, PlanetDefinition::new));

    /** The key of the dimension a planet lives in. */
    public static ResourceKey<Level> dimensionKey(String namespace, String path) {
        return ResourceKey.create(Registries.DIMENSION, net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, path));
    }

    private GenesisPlanetCodecs() {
    }
}
