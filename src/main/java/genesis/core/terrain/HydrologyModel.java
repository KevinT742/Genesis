package genesis.core.terrain;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;

import genesis.api.planet.HydrologyProfile;
import genesis.api.planet.PlanetDefinition;
import genesis.core.math.MathUtil;

/**
 * A realistic hydrology model.
 *
 * <p>Water is not sprinkled onto the terrain, it is routed across it. The model works in three
 * stages:
 * <ol>
 *   <li><strong>Elevation sampling.</strong> The terrain model is evaluated on a coarse grid.</li>
 *   <li><strong>Depression filling.</strong> A Priority-Flood pass raises every hollow up to its
 *       spill level. Holes that survive are lakes, exactly like on a real planet.</li>
 *   <li><strong>Flow accumulation.</strong> A D8 pass pushes the accumulated rainfall downhill
 *       along the steepest gradient. Cells that collect more water than the river threshold
 *       become rivers; the widest of them become trunk rivers.</li>
 * </ol>
 *
 * <p>The grid is periodic along the longitude axis, so rivers run around the planet without ever
 * breaking at the world boundary.
 */
public final class HydrologyModel {

    /** Number of cells per region along the longitude axis. */
    private static final int REGION_CELLS = 250;
    /** Number of cells of context computed around a region, so upstream water is captured. */
    private static final int MARGIN_CELLS = 64;
    /** Number of regions kept in memory. */
    private static final int CACHED_REGIONS = 48;

    private static final int[] NEIGHBOUR_X = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] NEIGHBOUR_Z = {0, 1, 1, 1, 0, -1, -1, -1};

    private final TerrainModel terrain;
    private final HydrologyProfile profile;
    private final int cellSize;
    private final int cellCountX;
    private final int latitudeCellCount;
    private final int regionCountX;
    private final int regionCountZ;
    private final double halfSpan;
    private final double seaLevel;
    private final Map<Long, HydrologyGrid> cache;

    public HydrologyModel(TerrainModel terrain, HydrologyProfile profile) {
        this.terrain = terrain;
        this.profile = profile;
        this.cellSize = profile.cellSize();
        this.halfSpan = terrain.surface().halfSpan();
        this.seaLevel = profile.seaLevelTolerance() + terrain.seaLevel();

        this.cellCountX = Math.max(16, (int) Math.round(terrain.surface().circumference() / cellSize));
        this.latitudeCellCount = Math.max(16, (int) Math.round((halfSpan * 2.0) / cellSize));

        int regionsAround = Math.max(1, cellCountX / REGION_CELLS);
        this.regionCountX = regionsAround;
        this.regionCountZ = Math.max(1, latitudeCellCount / REGION_CELLS);

        this.cache = new LinkedHashMap<>(CACHED_REGIONS * 2, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, HydrologyGrid> eldest) {
                return size() > CACHED_REGIONS;
            }
        };
    }

    public HydrologyModel(TerrainModel terrain, PlanetDefinition planet) {
        this(terrain, planet.hydrology());
    }

    public HydrologyProfile profile() {
        return profile;
    }

    public double seaLevel() {
        return seaLevel;
    }

    /** The grid that covers the given block position. */
    public synchronized HydrologyGrid gridAt(double blockX, double blockZ) {
        int cellX = Math.floorDiv((int) Math.floor(blockX), cellSize);
        int cellZ = Math.floorDiv((int) Math.floor(blockZ), cellSize);
        int regionX = Math.floorMod(cellX / REGION_CELLS, regionCountX);
        int regionZ = Math.floorMod(cellZ / REGION_CELLS, regionCountZ);
        long key = pack(regionX, regionZ);
        HydrologyGrid grid = cache.get(key);
        if (grid == null) {
            grid = build(regionX, regionZ);
            cache.put(key, grid);
        }
        return grid;
    }

    /** The accumulated water flow at a block position. */
    public double flowAt(double blockX, double blockZ) {
        int cellX = Math.floorDiv((int) Math.floor(blockX), cellSize);
        int cellZ = Math.floorDiv((int) Math.floor(blockZ), cellSize);
        HydrologyGrid grid = gridAt(blockX, blockZ);
        return grid.flowAt(cellX, cellZ);
    }

    /** The water surface height at a block position. */
    public double waterSurfaceAt(double blockX, double blockZ) {
        int cellX = Math.floorDiv((int) Math.floor(blockX), cellSize);
        int cellZ = Math.floorDiv((int) Math.floor(blockZ), cellSize);
        HydrologyGrid grid = gridAt(blockX, blockZ);
        double surface = grid.waterSurfaceAt(cellX, cellZ);
        return Double.isNaN(surface) ? seaLevel : surface;
    }

    /** How deep the water stands at a block position. */
    public double waterDepthAt(double blockX, double blockZ) {
        int cellX = Math.floorDiv((int) Math.floor(blockX), cellSize);
        int cellZ = Math.floorDiv((int) Math.floor(blockZ), cellSize);
        HydrologyGrid grid = gridAt(blockX, blockZ);
        double depth = grid.waterDepthAt(cellX, cellZ);
        return Double.isNaN(depth) ? 0.0 : depth;
    }

    /**
     * The floor of the water body at a block position.
     *
     * <p>This is the raw ground height of the cell, which is the level the water actually stands
     * on inside a lake or a river bed.
     */
    public double terrainFloor(double blockX, double blockZ) {
        int cellX = Math.floorDiv((int) Math.floor(blockX), cellSize);
        int cellZ = Math.floorDiv((int) Math.floor(blockZ), cellSize);
        HydrologyGrid grid = gridAt(blockX, blockZ);
        double floor = grid.elevationAt(cellX, cellZ);
        return Double.isNaN(floor) ? seaLevel : floor;
    }

    /** Whether the position carries a river rather than just scattered rainfall. */
    public boolean isRiver(double blockX, double blockZ) {
        return flowAt(blockX, blockZ) >= profile.riverThreshold();
    }

    /** The width of the river channel at a block position, in blocks. */
    public double riverWidth(double blockX, double blockZ) {
        double flow = flowAt(blockX, blockZ);
        double threshold = profile.riverThreshold();
        if (flow < threshold) {
            return 0.0;
        }
        double excess = flow / threshold - 1.0;
        return 1.0 + profile.riverWidthScale() * Math.sqrt(excess);
    }

    /** How deep the river bed is cut into the terrain, in blocks. */
    public double riverDepth(double blockX, double blockZ) {
        double flow = flowAt(blockX, blockZ);
        double threshold = profile.riverThreshold();
        if (flow < threshold) {
            return 0.0;
        }
        double excess = flow / threshold - 1.0;
        return profile.riverDepthScale() * (1.0 + 0.6 * Math.sqrt(excess));
    }

    // ---------------------------------------------------------------------
    // grid construction
    // ---------------------------------------------------------------------

    private HydrologyGrid build(int regionX, int regionZ) {
        int originCellX = regionX * REGION_CELLS - MARGIN_CELLS;
        int originCellZ = regionZ * REGION_CELLS - MARGIN_CELLS;
        int size = REGION_CELLS + MARGIN_CELLS * 2;

        float[] elevation = new float[size * size];
        float[] waterSurface = new float[size * size];

        for (int lz = 0; lz < size; lz++) {
            int cellZ = originCellZ + lz;
            double blockZ = cellToBlockZ(cellZ);
            for (int lx = 0; lx < size; lx++) {
                int cellX = originCellX + lx;
                double blockX = cellToBlockX(cellX);
                elevation[lz * size + lx] = (float) terrain.coarseElevation((int) blockX, (int) blockZ);
            }
        }

        float[] surface = Arrays.copyOf(elevation, elevation.length);
        int[] visitOrder = fillDepressions(elevation, size, surface);
        float[] flow = accumulateFlow(elevation, surface, size, visitOrder);

        return new HydrologyGrid(originCellX, originCellZ, size, cellSize, elevation, surface, flow);
    }
    /** Converts a longitude cell index into the matching flat world coordinate. */
    private double cellToBlockX(int cellX) {
        return MathUtil.wrap(cellX * (double) cellSize, halfSpan);
    }

    /** Converts a latitude cell index into the matching flat world coordinate. */
    private double cellToBlockZ(int cellZ) {
        return cellZ * (double) cellSize - halfSpan;
    }

    /**
     * Priority-Flood depression filling.
     *
     * <p>Every cell on the window border is treated as an outlet. Working from the lowest border
     * cell upwards, the water surface of a neighbour is raised to at least the level of the cell it
     * drains into. Depressions end up with a flat water surface at their spill level.
     *
     * <p>The order in which the cells are visited is returned as well. The flood visits the cells
     * from the lowest water surface upwards, so the reversed order is the order in which the water
     * has to be accumulated.
     */
    private int[] fillDepressions(float[] elevation, int size, float[] surface) {
        boolean[] visited = new boolean[elevation.length];
        int[] visitOrder = new int[elevation.length];
        int visitedCount = 0;

        PriorityQueue<Integer> queue = new PriorityQueue<>((a, b) -> {
            int cmp = Float.compare(surface[a], surface[b]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });

        for (int x = 0; x < size; x++) {
            pushBorder(queue, visited, surface, x, 0, size);
            pushBorder(queue, visited, surface, x, size - 1, size);
        }
        for (int z = 0; z < size; z++) {
            pushBorder(queue, visited, surface, 0, z, size);
            pushBorder(queue, visited, surface, size - 1, z, size);
        }

        while (!queue.isEmpty()) {
            int current = queue.poll();
            visitOrder[visitedCount++] = current;
            int cx = current % size;
            int cz = current / size;
            float currentSurface = surface[current];
            for (int n = 0; n < 8; n++) {
                int nx = cx + NEIGHBOUR_X[n];
                int nz = cz + NEIGHBOUR_Z[n];
                if (nx < 0 || nz < 0 || nx >= size || nz >= size) {
                    continue;
                }
                int ni = nz * size + nx;
                if (visited[ni]) {
                    continue;
                }
                visited[ni] = true;
                if (surface[ni] < currentSurface) {
                    surface[ni] = currentSurface;
                }
                queue.add(ni);
            }
        }
        return visitOrder;
    }

    private void pushBorder(PriorityQueue<Integer> queue, boolean[] visited, float[] surface, int x, int z, int size) {
        int index = z * size + x;
        if (visited[index]) {
            return;
        }
        visited[index] = true;
        queue.add(index);
    }

    /**
     * D8 flow accumulation.
     *
     * <p>Cells are processed from the highest water surface downwards. Because every contributor to
     * a cell sits higher than the cell itself, the flow of a cell is complete by the time it is
     * processed and can be handed to its steepest downhill neighbour.
     *
     * <p>The order comes straight from the depression filling: the flood visits the cells from the
     * lowest water surface upwards, so reversing it gives exactly the order that is needed here.
     * That saves sorting a hundred thousand cells for every region.
     */
    private float[] accumulateFlow(float[] elevation, float[] surface, int size, int[] visitOrder) {
        float[] flow = new float[elevation.length];

        for (int i = visitOrder.length - 1; i >= 0; i--) {
            int index = visitOrder[i];
            if (elevation[index] <= seaLevel) {
                // The cell is already part of the sea, it does not collect any more water.
                continue;
            }
            int cx = index % size;
            int cz = index / size;
            int best = -1;
            double bestSurface = surface[index];
            double bestElevation = elevation[index];

            for (int n = 0; n < 8; n++) {
                int nx = cx + NEIGHBOUR_X[n];
                int nz = cz + NEIGHBOUR_Z[n];
                if (nx < 0 || nz < 0 || nx >= size || nz >= size) {
                    continue;
                }
                int ni = nz * size + nx;
                if (surface[ni] > surface[index]) {
                    continue;
                }
                boolean lowerSurface = surface[ni] < bestSurface;
                boolean sameSurfaceLowerGround = surface[ni] == bestSurface && elevation[ni] < bestElevation;
                if (lowerSurface || sameSurfaceLowerGround) {
                    best = ni;
                    bestSurface = surface[ni];
                    bestElevation = elevation[ni];
                }
            }

            flow[index] += (float) profile.rainfall();
            if (best >= 0) {
                flow[best] += flow[index];
            }
        }
        return flow;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
