package genesis.api.planet;

/**
 * The hydrology parameters of a planet: how water behaves, how deep it gets and how wide the
 * rivers become.
 *
 * @param cellSize           the size of a hydrology cell in blocks, larger cells are cheaper but
 *                           produce coarser rivers
 * @param rainfall           the amount of water falling per cell, drives the flow accumulation
 * @param seaLevelTolerance  how much a cell may sit below the sea level before it floods
 * @param riverThreshold     the flow accumulation required before a channel becomes a river
 * @param riverWidthScale    how strongly the flow accumulation widens a river
 * @param riverDepthScale    how strongly the flow accumulation deepens a river
 * @param lakeFillIterations how many depression filling passes are run, higher values remove
 *                           smaller ponds but cost more time
 * @param tributaryDecay     how quickly flow dissipates when it leaves the main channel
 */
public record HydrologyProfile(
        int cellSize,
        double rainfall,
        double seaLevelTolerance,
        double riverThreshold,
        double riverWidthScale,
        double riverDepthScale,
        int lakeFillIterations,
        double tributaryDecay) {

    public HydrologyProfile {
        if (cellSize < 1) {
            throw new IllegalArgumentException("cellSize must be at least one block");
        }
        if (lakeFillIterations < 1) {
            lakeFillIterations = 1;
        }
    }

    /** The default profile used by the Genesis home world. */
    public static HydrologyProfile earthLike() {
        return new HydrologyProfile(4, 1.0, 1.5, 320.0, 0.9, 0.55, 2, 0.92);
    }

    /** A profile for a dry planet with rare but violent flash floods. */
    public static HydrologyProfile arid() {
        return new HydrologyProfile(4, 0.35, 1.0, 700.0, 1.2, 0.8, 2, 0.96);
    }

    /** A profile for a frozen world where most water is locked into ice. */
    public static HydrologyProfile frozen() {
        return new HydrologyProfile(4, 0.6, 1.0, 480.0, 0.7, 0.35, 2, 0.9);
    }
}
