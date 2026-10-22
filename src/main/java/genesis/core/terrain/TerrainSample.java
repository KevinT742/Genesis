package genesis.core.terrain;

/**
 * The result of sampling the terrain model at one position.
 *
 * @param height          the surface height in blocks, before rivers and lakes are carved in
 * @param continentalness the large scale landmass value, {@code -1} to {@code 1}
 * @param erosion         the erosion value, {@code 0} means rugged, {@code 1} means worn flat
 * @param ridge           the ridge value, {@code 0} to {@code 1}
 * @param submerged       whether the position is below the sea level
 */
public record TerrainSample(
        double height,
        double continentalness,
        double erosion,
        double ridge,
        boolean submerged) {
}
