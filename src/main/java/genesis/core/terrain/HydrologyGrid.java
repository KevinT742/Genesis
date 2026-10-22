package genesis.core.terrain;

/**
 * A window of the hydrology grid.
 *
 * <p>The grid stores three parallel fields per cell:
 * <ul>
 *   <li>{@code elevation} &mdash; the raw terrain height of the cell.</li>
 *   <li>{@code waterSurface} &mdash; the elevation of the water surface. In depressions this is
 *       raised to the spill level, which is what turns a hollow into a lake.</li>
 *   <li>{@code flow} &mdash; the accumulated amount of water running through the cell.</li>
 * </ul>
 */
public final class HydrologyGrid {

    private final int originCellX;
    private final int originCellZ;
    private final int size;
    private final int cellSize;
    private final float[] elevation;
    private final float[] waterSurface;
    private final float[] flow;

    public HydrologyGrid(int originCellX, int originCellZ, int size, int cellSize,
            float[] elevation, float[] waterSurface, float[] flow) {
        this.originCellX = originCellX;
        this.originCellZ = originCellZ;
        this.size = size;
        this.cellSize = cellSize;
        this.elevation = elevation;
        this.waterSurface = waterSurface;
        this.flow = flow;
    }

    public int originCellX() {
        return originCellX;
    }

    public int originCellZ() {
        return originCellZ;
    }

    public int size() {
        return size;
    }

    public int cellSize() {
        return cellSize;
    }

    /** Index of a local cell position inside the backing arrays. */
    public int index(int localX, int localZ) {
        return localZ * size + localX;
    }

    /** Whether the given absolute cell coordinate is inside this window. */
    public boolean contains(int cellX, int cellZ) {
        return cellX >= originCellX && cellX < originCellX + size
                && cellZ >= originCellZ && cellZ < originCellZ + size;
    }

    /** The raw terrain height of a cell. */
    public double elevationAt(int cellX, int cellZ) {
        if (!contains(cellX, cellZ)) {
            return Double.NaN;
        }
        return elevation[index(cellX - originCellX, cellZ - originCellZ)];
    }

    /** The water surface height of a cell, raised to the spill level inside depressions. */
    public double waterSurfaceAt(int cellX, int cellZ) {
        if (!contains(cellX, cellZ)) {
            return Double.NaN;
        }
        return waterSurface[index(cellX - originCellX, cellZ - originCellZ)];
    }

    /** The accumulated water flow of a cell. */
    public double flowAt(int cellX, int cellZ) {
        if (!contains(cellX, cellZ)) {
            return 0.0;
        }
        return flow[index(cellX - originCellX, cellZ - originCellZ)];
    }

    /** How deep the water stands in a cell, {@code 0} when the cell is dry. */
    public double waterDepthAt(int cellX, int cellZ) {
        double surface = waterSurfaceAt(cellX, cellZ);
        if (Double.isNaN(surface)) {
            return 0.0;
        }
        return Math.max(0.0, surface - elevationAt(cellX, cellZ));
    }
}
