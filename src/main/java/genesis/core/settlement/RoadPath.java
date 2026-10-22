package genesis.core.settlement;

import java.util.List;

/**
 * A road between two settlements.
 *
 * @param from       the index of the settlement the road starts at
 * @param to         the index of the settlement the road ends at
 * @param points     the centre line of the road as a list of {@code {x, z}} block coordinates
 * @param width      how wide the road is, in blocks
 * @param surface    the kind of surface the road is built from
 */
public record RoadPath(int from, int to, List<int[]> points, float width, RoadSurface surface) {

    /** The kinds of road surface Genesis can build. */
    public enum RoadSurface {
        /** Packed earth, used on dry continents. */
        DIRT,
        /** Gravel, used in cold or wet regions. */
        GRAVEL,
        /** Cobblestone, used for the roads of large settlements. */
        COBBLESTONE
    }

    /** The length of the road in blocks, measured along the centre line. */
    public double length() {
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            int[] a = points.get(i - 1);
            int[] b = points.get(i);
            double dx = b[0] - a[0];
            double dz = b[1] - a[1];
            total += Math.sqrt(dx * dx + dz * dz);
        }
        return total;
    }
}
