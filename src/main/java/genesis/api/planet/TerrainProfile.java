package genesis.api.planet;

import genesis.core.math.Spline;

/**
 * The terrain shaping parameters of a planet.
 *
 * <p>Everything the terrain generator needs is expressed here, so that a new planet can be created
 * by filling in a record instead of writing generator code.
 *
 * @param seaLevel            the block height of the global sea level
 * @param continentalAmplitude how strongly the large scale landmass shape influences the height
 * @param continentalFrequency the wavelength of the landmass shape, in blocks
 * @param erosionAmplitude    how strongly erosion flattens the terrain
 * @param ridgeAmplitude      how strongly mountain ridges are raised
 * @param ridgeSharpness      how sharp the ridges are, {@code 1} is round, higher is sharper
 * @param detailAmplitude     the amplitude of the small scale detail noise
 * @param detailFrequency     the wavelength of the small scale detail noise, in blocks
 * @param heightVariance      the overall vertical scale of the terrain
 * @param plateauFlatness     how flat plateaus are, {@code 1} means perfectly flat
 */
public record TerrainProfile(
        double seaLevel,
        double continentalAmplitude,
        double continentalFrequency,
        double erosionAmplitude,
        double ridgeAmplitude,
        double ridgeSharpness,
        double detailAmplitude,
        double detailFrequency,
        double heightVariance,
        double plateauFlatness) {

    public TerrainProfile {
        if (seaLevel < 0) {
            throw new IllegalArgumentException("seaLevel must not be negative");
        }
    }

    /** The default Earth like profile used by the Genesis home world. */
    public static TerrainProfile earthLike() {
        return new TerrainProfile(
                63.0,
                180.0,
                2600.0,
                0.45,
                120.0,
                2.4,
                6.0,
                48.0,
                1.0,
                0.55);
    }

    /** The shape curve that maps raw continentalness noise onto a height contribution. */
    public Spline continentalSpline() {
        return Spline.of(
                -1.0, -0.35,
                -0.45, -0.15,
                -0.12, 0.02,
                0.08, 0.22,
                0.35, 0.55,
                0.7, 0.9,
                1.0, 1.15);
    }

    /** The shape curve that maps raw erosion noise onto a flattening factor. */
    public Spline erosionSpline() {
        return Spline.of(
                -1.0, 0.15,
                0.0, 0.45,
                0.4, 0.75,
                0.8, 0.92,
                1.0, 1.0);
    }

    /** The shape curve that maps raw ridge noise onto a mountain height contribution. */
    public Spline ridgeSpline() {
        return Spline.of(
                -1.0, 0.0,
                -0.2, 0.02,
                0.2, 0.12,
                0.55, 0.45,
                0.8, 0.8,
                1.0, 1.0);
    }
}
