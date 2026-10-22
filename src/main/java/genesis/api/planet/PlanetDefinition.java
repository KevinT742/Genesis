package genesis.api.planet;

/**
 * The complete definition of a planet.
 *
 * <p>A planet bundles everything Genesis needs to know about a world: its physical size, its
 * gravity, its atmosphere and the three generation profiles that shape terrain, water and life.
 * Adding a new planet to the mod family therefore means writing one record, not a generator.
 *
 * @param id           a stable identifier, usually {@code namespace:name}
 * @param displayName  a human readable name shown in the interface
 * @param radius       the physical radius of the planet in blocks
 * @param gravity      the gravity factor, {@code 1} is Earth like
 * @param halfSpan     half the extent of the world along the latitude axis, in blocks
 * @param samplingMode how terrain noise is projected onto the surface
 * @param wrapLatitude whether the latitude axis also wraps, turning the world into a torus
 * @param atmosphere   the atmosphere of the planet
 * @param terrain      the terrain shaping parameters
 * @param hydrology    the water system parameters
 * @param ecology      the climate and vegetation parameters
 */
public record PlanetDefinition(
        String id,
        String displayName,
        double radius,
        double gravity,
        int halfSpan,
        SamplingMode samplingMode,
        boolean wrapLatitude,
        AtmosphereSpec atmosphere,
        TerrainProfile terrain,
        HydrologyProfile hydrology,
        EcologyProfile ecology) {

    public PlanetDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Planet id must not be blank");
        }
        if (halfSpan < 1024) {
            throw new IllegalArgumentException("A planet needs a half span of at least 1024 blocks");
        }
        if (radius <= 0.0) {
            throw new IllegalArgumentException("Planet radius must be positive");
        }
        displayName = displayName == null ? id : displayName;
        atmosphere = atmosphere == null ? AtmosphereSpec.earthLike() : atmosphere;
        terrain = terrain == null ? TerrainProfile.earthLike() : terrain;
        hydrology = hydrology == null ? HydrologyProfile.earthLike() : hydrology;
        ecology = ecology == null ? EcologyProfile.earthLike() : ecology;
        samplingMode = samplingMode == null ? SamplingMode.CYLINDER : samplingMode;
    }

    /** The Genesis home world: an Earth like planet with a comfortable day cycle. */
    public static PlanetDefinition homeWorld() {
        return new PlanetDefinition(
                "genesis:terran",
                "Terran",
                12732.0,
                1.0,
                40000,
                SamplingMode.CYLINDER,
                false,
                AtmosphereSpec.earthLike(),
                TerrainProfile.earthLike(),
                HydrologyProfile.earthLike(),
                EcologyProfile.earthLike());
    }

    /** A second planet, used to prove the multi planet architecture works. */
    public static PlanetDefinition ember() {
        return new PlanetDefinition(
                "genesis:ember",
                "Ember",
                9000.0,
                0.72,
                40000,
                SamplingMode.SPHERE,
                false,
                AtmosphereSpec.thinDesert(),
                new TerrainProfile(58.0, 150.0, 2100.0, 0.55, 165.0, 3.1, 8.0, 40.0, 1.1, 0.4),
                HydrologyProfile.arid(),
                new EcologyProfile(genesis.api.physical.Temperature.ofKelvin(319.15), 34.0, 0.5, 1000.0, 0.15, 0.2, 0.4, 0.9));
    }
}
