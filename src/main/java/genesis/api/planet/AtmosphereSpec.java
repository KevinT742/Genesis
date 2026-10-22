package genesis.api.planet;

/**
 * The atmosphere of a planet.
 *
 * <p>The atmosphere drives the sky colour, the fog distance, the ambient light and the way
 * precipitation behaves.
 *
 * @param density          the relative density of the atmosphere, {@code 1} is Earth like
 * @param skyTopColor      the packed RGB colour of the zenith
 * @param skyHorizonColor  the packed RGB colour of the horizon
 * @param fogColor         the packed RGB colour of the fog
 * @param fogDistance      the distance at which the fog fully obscures the world
 * @param cloudColor       the packed RGB colour of the clouds
 * @param cloudCoverage    how much of the sky is covered by clouds, {@code 0} to {@code 1}
 * @param windStrength     how strong the surface wind is
 * @param precipitation    how much precipitation falls, {@code 0} means a desert
 */
public record AtmosphereSpec(
        double density,
        int skyTopColor,
        int skyHorizonColor,
        int fogColor,
        double fogDistance,
        int cloudColor,
        double cloudCoverage,
        double windStrength,
        double precipitation) {

    /** The default Earth like atmosphere. */
    public static AtmosphereSpec earthLike() {
        return new AtmosphereSpec(1.0, 0x78A7FF, 0xC6D6FF, 0xC6D6FF, 16.0, 0xFFFFFF, 0.4, 1.0, 1.0);
    }

    /** A thin atmosphere with a reddish sky, typical of a desert world. */
    public static AtmosphereSpec thinDesert() {
        return new AtmosphereSpec(0.45, 0x8FB6E8, 0xE8D5B0, 0xE8D5B0, 22.0, 0xFFF4E0, 0.15, 0.6, 0.1);
    }

    /** A dense atmosphere with a heavy purple sky. */
    public static AtmosphereSpec densePurple() {
        return new AtmosphereSpec(1.8, 0x5B3A7A, 0x9A6FB5, 0x8A5FA0, 10.0, 0xD8C0E8, 0.7, 1.4, 1.6);
    }
}
