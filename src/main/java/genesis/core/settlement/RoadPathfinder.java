package genesis.core.settlement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import genesis.core.math.MathUtil;
import genesis.core.terrain.TerrainModel;

/**
 * Finds a plausible route between two places on the planet.
 *
 * <p>The search runs on a coarse grid that follows the planet: the longitude axis wraps around, the
 * latitude axis does not. The cost function prefers gentle ground and treats water as expensive
 * rather than impassable, which produces the characteristic result of a road that hugs the
 * coastline and bridges the occasional river instead of refusing to cross it.
 */
public final class RoadPathfinder {

    /**
     * The size of a routing cell in blocks.
     *
     * <p>The routing grid is deliberately coarse. A road is a long, smooth ribbon, so the exact
     * shape of the ground it crosses matters far less than the fact that it reaches its
     * destination, and a coarse grid turns the search from seconds into milliseconds.
     */
    private static final int CELL_SIZE = 64;
    /** The maximum number of cells the search is allowed to expand. */
    private static final int MAX_EXPANSIONS = 500_000;

    private static final int[] NEIGHBOUR_X = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] NEIGHBOUR_Z = {0, 1, 1, 1, 0, -1, -1, -1};

    private final TerrainModel terrain;
    private final double halfSpan;
    private final int longitudeCells;
    private final int latitudeCells;
    private final double seaLevel;
    /** The elevations of the cells that were already looked up. */
    private final Map<Long, Double> elevationCache = new HashMap<>();

    public RoadPathfinder(TerrainModel terrain) {
        this.terrain = terrain;
        this.halfSpan = terrain.surface().halfSpan();
        this.seaLevel = terrain.seaLevel();
        this.longitudeCells = Math.max(8, (int) Math.round(terrain.surface().circumference() / CELL_SIZE));
        this.latitudeCells = Math.max(8, (int) Math.round((halfSpan * 2.0) / CELL_SIZE));
    }

    public int cellSize() {
        return CELL_SIZE;
    }

    /** The great circle distance between two world positions, in blocks. */
    public double distance(double x1, double z1, double x2, double z2) {
        double lon1 = longitudeOf(x1);
        double lon2 = longitudeOf(x2);
        double lat1 = latitudeOf(z1);
        double lat2 = latitudeOf(z2);
        double deltaLon = Math.abs(MathUtil.wrap(lon1 - lon2, Math.PI));
        double meanLat = (lat1 + lat2) * 0.5;
        double dx = Math.cos(meanLat) * deltaLon * terrain.surface().radius();
        double dy = (lat2 - lat1) * terrain.surface().radius();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double longitudeOf(double x) {
        return (2.0 * Math.PI) * (terrain.surface().wrapLongitude(x) / terrain.surface().circumference());
    }

    private double latitudeOf(double z) {
        return (Math.PI * 0.5) * (terrain.surface().foldLatitude(z) / halfSpan);
    }

    /** The block coordinate of a longitude cell. */
    public double cellToBlockX(int longitudeCell) {
        return MathUtil.wrap(longitudeCell * (double) CELL_SIZE, halfSpan);
    }

    /** The block coordinate of a latitude cell. */
    public double cellToBlockZ(int latitudeCell) {
        return latitudeCell * (double) CELL_SIZE - halfSpan;
    }

    private int longitudeCellOf(double x) {
        int cell = (int) Math.floor(x / CELL_SIZE);
        return Math.floorMod(cell, longitudeCells);
    }

    private int latitudeCellOf(double z) {
        int cell = (int) Math.floor((z + halfSpan) / CELL_SIZE);
        return MathUtil.clamp(cell, 0, latitudeCells - 1);
    }

    /**
     * Finds a route between two world positions.
     *
     * @return the centre line of the route, or an empty list when no route could be found
     */
    public List<int[]> route(double fromX, double fromZ, double toX, double toZ) {
        int startLon = longitudeCellOf(fromX);
        int startLat = latitudeCellOf(fromZ);
        int goalLon = longitudeCellOf(toX);
        int goalLat = latitudeCellOf(toZ);

        if (startLon == goalLon && startLat == goalLat) {
            return List.of(new int[] {(int) Math.round(fromX), (int) Math.round(fromZ)});
        }

        Map<Long, Double> gScore = new HashMap<>();
        Map<Long, Long> cameFrom = new HashMap<>();
        // The tie breaker prefers the node that is already closer to the goal. Without it the search
        // drowns in the plateau of equal scores that the expensive water crossings create.
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>((a, b) -> {
            int compared = Double.compare(a.f, b.f);
            return compared != 0 ? compared : Double.compare(b.g, a.g);
        });

        long startKey = key(startLon, startLat);
        long goalKey = key(goalLon, goalLat);
        gScore.put(startKey, 0.0);
        queue.add(new QueueEntry(startKey, heuristic(startLon, startLat, goalLon, goalLat), 0.0));

        int expansions = 0;
        while (!queue.isEmpty() && expansions < MAX_EXPANSIONS) {
            QueueEntry entry = queue.poll();
            long currentKey = entry.key;
            if (currentKey == goalKey) {
                return reconstruct(cameFrom, goalKey, startKey);
            }
            int lon = longitudeOfKey(currentKey);
            int lat = latitudeOfKey(currentKey);
            double currentScore = gScore.getOrDefault(currentKey, Double.MAX_VALUE);
            double currentHeight = heightAt(lon, lat);

            for (int n = 0; n < 8; n++) {
                int nextLon = Math.floorMod(lon + NEIGHBOUR_X[n], longitudeCells);
                int nextLat = lat + NEIGHBOUR_Z[n];
                if (nextLat < 0 || nextLat >= latitudeCells) {
                    continue;
                }
                long nextKey = key(nextLon, nextLat);
                double nextHeight = heightAt(nextLon, nextLat);
                double step = stepCost(currentHeight, nextHeight, nextHeight < seaLevel);
                double tentative = currentScore + step;
                double known = gScore.getOrDefault(nextKey, Double.MAX_VALUE);
                if (tentative + 1.0e-6 < known) {
                    gScore.put(nextKey, tentative);
                    cameFrom.put(nextKey, currentKey);
                    queue.add(new QueueEntry(nextKey, tentative + heuristic(nextLon, nextLat, goalLon, goalLat),
                            tentative));
                }
            }
            expansions++;
        }
        // The search did not find a way through. The direct route along the great circle is used
        // instead, so that a road always connects the two places.
        return greatCircle(fromX, fromZ, toX, toZ);
    }

    /** The direct route between two places, following the great circle between them. */
    public List<int[]> greatCircle(double fromX, double fromZ, double toX, double toZ) {
        List<int[]> points = new ArrayList<>();
        double longitude1 = longitudeOf(fromX);
        double longitude2 = longitudeOf(toX);
        double latitude1 = latitudeOf(fromZ);
        double latitude2 = latitudeOf(toZ);
        double deltaLongitude = longitude2 - longitude1;
        // Take the short way around the planet.
        if (deltaLongitude > Math.PI) {
            deltaLongitude -= 2.0 * Math.PI;
        } else if (deltaLongitude < -Math.PI) {
            deltaLongitude += 2.0 * Math.PI;
        }
        int steps = Math.max(2, (int) Math.ceil(Math.abs(deltaLongitude) * 64.0
                + Math.abs(latitude2 - latitude1) * 64.0));
        for (int step = 0; step <= steps; step++) {
            double t = (double) step / steps;
            double longitude = longitude1 + deltaLongitude * t;
            double latitude = latitude1 + (latitude2 - latitude1) * t;
            double x = MathUtil.wrap(longitude / (2.0 * Math.PI) * terrain.surface().circumference(), halfSpan);
            double z = latitude / (Math.PI * 0.5) * halfSpan;
            points.add(new int[] {(int) Math.round(x), (int) Math.round(z)});
        }
        return points;
    }

    private double stepCost(double fromHeight, double toHeight, boolean underwater) {
        double slope = Math.abs(toHeight - fromHeight);
        double cost = 1.0 + slope * 0.35;
        if (underwater) {
            // Roads bridge water instead of avoiding it entirely.
            cost += 16.0;
        }
        if (slope > 6.0) {
            // Climbing steep ground is expensive.
            cost += 6.0;
        }
        return cost;
    }

    private double heightAt(int longitudeCell, int latitudeCell) {
        long key = key(longitudeCell, latitudeCell);
        Double cached = elevationCache.get(key);
        if (cached != null) {
            return cached;
        }
        double height = terrain.coarseElevation((int) Math.round(cellToBlockX(longitudeCell)),
                (int) Math.round(cellToBlockZ(latitudeCell)));
        if (elevationCache.size() < 400_000) {
            elevationCache.put(key, height);
        }
        return height;
    }

    private double heuristic(int fromLon, int fromLat, int toLon, int toLat) {
        double dx = Math.floorMod(toLon - fromLon, longitudeCells);
        dx = Math.min(dx, longitudeCells - dx);
        double dz = toLat - fromLat;
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }

    private List<int[]> reconstruct(Map<Long, Long> cameFrom, long goalKey, long startKey) {
        List<int[]> reversed = new ArrayList<>();
        long current = goalKey;
        int guard = 0;
        while (true) {
            int lon = longitudeOfKey(current);
            int lat = latitudeOfKey(current);
            reversed.add(new int[] {(int) Math.round(cellToBlockX(lon)), (int) Math.round(cellToBlockZ(lat))});
            if (current == startKey) {
                break;
            }
            Long previous = cameFrom.get(current);
            if (previous == null) {
                break;
            }
            current = previous;
            if (++guard > MAX_EXPANSIONS) {
                break;
            }
        }
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    private static long key(int longitudeCell, int latitudeCell) {
        return ((long) longitudeCell << 32) ^ (latitudeCell & 0xFFFFFFFFL);
    }

    /** An entry of the A* priority queue. */
    private record QueueEntry(long key, double f, double g) {
    }

    private static int longitudeOfKey(long key) {
        return (int) (key >> 32);
    }

    private static int latitudeOfKey(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }
}
