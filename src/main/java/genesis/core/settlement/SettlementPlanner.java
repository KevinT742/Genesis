package genesis.core.settlement;

import java.util.ArrayList;
import java.util.List;

import genesis.api.planet.PlanetDefinition;
import genesis.core.math.Hash;
import genesis.core.math.MathUtil;
import genesis.core.math.Noise;
import genesis.core.terrain.TerrainModel;

/**
 * Plans where the structures of a planet are and how they are connected.
 *
 * <p>The planner works in three independent passes:
 * <ol>
 *   <li><strong>Settlements.</strong> Candidate positions are spread over the planet with a golden
 *       angle spiral, which gives an even but natural looking distribution, and are then filtered
 *       by whether the ground could actually hold a village.</li>
 *   <li><strong>Roads.</strong> The settlements are connected with a minimum spanning tree, which
 *       guarantees that every settlement can be reached, plus a few extra links that create the
 *       loops real road networks have.</li>
 *   <li><strong>Ruins and hidden sites.</strong> These use their own, sparser distributions and
 *       deliberately avoid the settlements.</li>
 * </ol>
 *
 * <p>Everything is derived from the world seed, so the layout is identical on every server and
 * independent of the order in which chunks are generated.
 */
public final class SettlementPlanner {

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private final long seed;
    private final PlanetDefinition planet;
    private final TerrainModel terrain;
    private final RoadPathfinder pathfinder;
    private final Noise hiddennessNoise;
    private final double halfSpan;
    private final double seaLevel;

    public SettlementPlanner(long seed, PlanetDefinition planet, TerrainModel terrain) {
        this.seed = seed;
        this.planet = planet;
        this.terrain = terrain;
        this.pathfinder = new RoadPathfinder(terrain);
        this.hiddennessNoise = new Noise(seed).fork("hiddenness");
        this.halfSpan = terrain.surface().halfSpan();
        this.seaLevel = terrain.seaLevel();
    }

    /** Builds the complete plan. */
    public WorldPlan plan() {
        List<StructureSite> settlements = planSettlements();
        List<RoadPath> roads = planRoads(settlements);
        List<StructureSite> ruins = planRuins(settlements);
        List<StructureSite> hidden = planHidden(settlements);
        return new WorldPlan(settlements, roads, ruins, hidden);
    }

    // ---------------------------------------------------------------------
    // settlements
    // ---------------------------------------------------------------------

    private List<StructureSite> planSettlements() {
        int target = settlementTarget();
        List<StructureSite> sites = new ArrayList<>();
        int candidates = target * 14;
        double circumference = terrain.surface().circumference();

        for (int i = 0; i < candidates && sites.size() < target; i++) {
            long hash = Hash.mix2(seed, i, 0x51A7);
            double latitudeUnit = 1.0 - 2.0 * ((i + 0.5) / candidates);
            double latitude = Math.asin(MathUtil.clamp(latitudeUnit, -1.0, 1.0));
            double longitude = i * GOLDEN_ANGLE + Hash.toSignedUnit(hash) * 0.6;

            double z = (latitude / (Math.PI * 0.5)) * halfSpan;
            double x = MathUtil.wrap((longitude / (2.0 * Math.PI)) * circumference, halfSpan);

            double height = terrain.coarseElevation((int) Math.round(x), (int) Math.round(z));
            if (height < seaLevel + 2.0 || height > seaLevel + 78.0) {
                continue;
            }
            if (!farEnough(sites, x, z, 900.0)) {
                continue;
            }

            double suitability = suitability(height);
            long siteSeed = Hash.mix2(seed, i, 0x2B17);
            double roll = Hash.toUnit(siteSeed);
            // Better ground is more likely to host a larger settlement.
            roll = MathUtil.clamp(roll * (1.6 - suitability), 0.0, 0.999);
            SettlementSize size = SettlementSize.fromRoll(roll);
            float rotation = (float) (Hash.toUnit(Hash.mix2(siteSeed, 7, 11)) * Math.PI * 2.0);

            sites.add(StructureSite.settlement((int) Math.round(x), (int) Math.round(z), (int) Math.round(height),
                    size, siteSeed, rotation, sites.size()));
        }
        return sites;
    }

    private int settlementTarget() {
        double area = terrain.surface().circumference() * halfSpan * 2.0;
        return MathUtil.clamp((int) Math.round(area / 90_000_000.0), 12, 140);
    }

    /** How suitable a height is for a settlement, {@code 0} to {@code 1}. */
    private double suitability(double height) {
        double above = height - seaLevel;
        double ideal = MathUtil.clamp(1.0 - Math.abs(above - 16.0) / 62.0, 0.0, 1.0);
        double flat = MathUtil.clamp(1.0 - (above / 90.0), 0.05, 1.0);
        return ideal * 0.7 + flat * 0.3;
    }

    private boolean farEnough(List<StructureSite> sites, double x, double z, double minimumDistance) {
        for (StructureSite site : sites) {
            if (pathfinder.distance(site.x(), site.z(), x, z) < minimumDistance) {
                return false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------------
    // roads
    // ---------------------------------------------------------------------

    private List<RoadPath> planRoads(List<StructureSite> settlements) {
        List<RoadPath> roads = new ArrayList<>();
        if (settlements.size() < 2) {
            return roads;
        }

        boolean[] connected = new boolean[settlements.size()];
        int[] best = new int[settlements.size()];
        double[] bestDistance = new double[settlements.size()];
        java.util.Arrays.fill(bestDistance, Double.MAX_VALUE);
        java.util.Arrays.fill(best, -1);
        best[0] = 0;
        bestDistance[0] = 0.0;

        List<int[]> edges = new ArrayList<>();
        for (int iteration = 0; iteration < settlements.size(); iteration++) {
            int current = -1;
            double currentDistance = Double.MAX_VALUE;
            for (int i = 0; i < settlements.size(); i++) {
                if (!connected[i] && bestDistance[i] < currentDistance) {
                    currentDistance = bestDistance[i];
                    current = i;
                }
            }
            if (current < 0) {
                break;
            }
            connected[current] = true;
            if (best[current] >= 0) {
                edges.add(new int[] {best[current], current});
            }
            for (int i = 0; i < settlements.size(); i++) {
                if (connected[i]) {
                    continue;
                }
                double distance = pathfinder.distance(settlements.get(current).x(), settlements.get(current).z(),
                        settlements.get(i).x(), settlements.get(i).z());
                if (distance < bestDistance[i]) {
                    bestDistance[i] = distance;
                    best[i] = current;
                }
            }
        }

        // Add a few extra links so the network has loops instead of being a pure tree.
        double average = 0.0;
        for (int[] edge : edges) {
            average += pathfinder.distance(settlements.get(edge[0]).x(), settlements.get(edge[0]).z(),
                    settlements.get(edge[1]).x(), settlements.get(edge[1]).z());
        }
        average = edges.isEmpty() ? 0.0 : average / edges.size();
        double limit = average * 1.9;

        for (int i = 0; i < settlements.size(); i++) {
            int nearest = -1;
            double nearestDistance = Double.MAX_VALUE;
            for (int j = 0; j < settlements.size(); j++) {
                if (i == j || hasEdge(edges, i, j)) {
                    continue;
                }
                double distance = pathfinder.distance(settlements.get(i).x(), settlements.get(i).z(),
                        settlements.get(j).x(), settlements.get(j).z());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = j;
                }
            }
            if (nearest >= 0 && nearestDistance < limit) {
                long roll = Hash.mix2(seed, i, 0x2F01);
                if (Hash.toUnit(roll) < 0.55) {
                    edges.add(new int[] {i, nearest});
                }
            }
        }

        for (int[] edge : edges) {
            StructureSite from = settlements.get(edge[0]);
            StructureSite to = settlements.get(edge[1]);
            List<int[]> points = pathfinder.route(from.x(), from.z(), to.x(), to.z());
            if (points.size() < 2) {
                continue;
            }
            // The route runs on a coarse grid, so the exact position of the settlements is added
            // back in to make sure that the road really starts and ends at the right place.
            List<int[]> centreLine = new ArrayList<>(points.size() + 2);
            centreLine.add(new int[] {from.x(), from.z()});
            centreLine.addAll(points);
            centreLine.add(new int[] {to.x(), to.z()});
            float width = (float) Math.max(from.size().roadWidthScale(), to.size().roadWidthScale());
            RoadPath.RoadSurface surface = surfaceFor(from, to);
            roads.add(new RoadPath(edge[0], edge[1], centreLine, width, surface));
        }
        return roads;
    }

    private boolean hasEdge(List<int[]> edges, int a, int b) {
        for (int[] edge : edges) {
            if ((edge[0] == a && edge[1] == b) || (edge[0] == b && edge[1] == a)) {
                return true;
            }
        }
        return false;
    }

    private RoadPath.RoadSurface surfaceFor(StructureSite from, StructureSite to) {
        if (from.size() == SettlementSize.CITY || to.size() == SettlementSize.CITY) {
            return RoadPath.RoadSurface.COBBLESTONE;
        }
        long hash = Hash.mix2(seed, from.x(), to.z());
        double roll = Hash.toUnit(hash);
        if (roll < 0.45) {
            return RoadPath.RoadSurface.DIRT;
        }
        if (roll < 0.85) {
            return RoadPath.RoadSurface.GRAVEL;
        }
        return RoadPath.RoadSurface.COBBLESTONE;
    }

    // ---------------------------------------------------------------------
    // ruins
    // ---------------------------------------------------------------------

    private List<StructureSite> planRuins(List<StructureSite> settlements) {
        List<StructureSite> ruins = new ArrayList<>();
        int target = Math.max(6, settlementTarget() / 3);
        int candidates = target * 10;
        double circumference = terrain.surface().circumference();

        for (int i = 0; i < candidates && ruins.size() < target; i++) {
            long hash = Hash.mix2(seed, i, 0x71C5);
            double latitudeUnit = 1.0 - 2.0 * ((i + 0.5) / candidates);
            double latitude = Math.asin(MathUtil.clamp(latitudeUnit, -1.0, 1.0));
            double longitude = (i + 0.37) * GOLDEN_ANGLE + Hash.toSignedUnit(hash) * 0.9;

            double z = (latitude / (Math.PI * 0.5)) * halfSpan;
            double x = MathUtil.wrap((longitude / (2.0 * Math.PI)) * circumference, halfSpan);

            double height = terrain.coarseElevation((int) Math.round(x), (int) Math.round(z));
            if (height < seaLevel + 0.5) {
                continue;
            }
            if (!farEnough(settlements, x, z, 260.0)) {
                continue;
            }
            long siteSeed = Hash.mix2(seed, i, 0x71C5);
            float rotation = (float) (Hash.toUnit(Hash.mix2(siteSeed, 3, 5)) * Math.PI * 2.0);
            ruins.add(StructureSite.ruin((int) Math.round(x), (int) Math.round(z), (int) Math.round(height),
                    siteSeed, rotation));
        }
        return ruins;
    }

    // ---------------------------------------------------------------------
    // hidden sites
    // ---------------------------------------------------------------------

    private List<StructureSite> planHidden(List<StructureSite> settlements) {
        List<StructureSite> hidden = new ArrayList<>();
        int target = Math.max(4, settlementTarget() / 7);
        int candidates = target * 24;
        double circumference = terrain.surface().circumference();

        for (int i = 0; i < candidates && hidden.size() < target; i++) {
            long hash = Hash.mix2(seed, i, 0x0DD5);
            double latitudeUnit = 1.0 - 2.0 * ((i + 0.5) / candidates);
            double latitude = Math.asin(MathUtil.clamp(latitudeUnit, -1.0, 1.0));
            double longitude = (i + 0.71) * GOLDEN_ANGLE + Hash.toSignedUnit(hash) * 1.4;

            double z = (latitude / (Math.PI * 0.5)) * halfSpan;
            double x = MathUtil.wrap((longitude / (2.0 * Math.PI)) * circumference, halfSpan);

            double height = terrain.coarseElevation((int) Math.round(x), (int) Math.round(z));
            if (height < seaLevel + 0.5 || height > seaLevel + 70.0) {
                continue;
            }
            if (!farEnough(settlements, x, z, 700.0)) {
                continue;
            }
            // Hidden sites favour places that already look unusual.
            double hiddenness = hiddennessNoise.fbm3(x / 180.0, 4.2, z / 180.0, 3, 2.0, 0.5);
            if (hiddenness < 0.18) {
                continue;
            }
            long siteSeed = Hash.mix2(seed, i, 0x0DD5);
            float rotation = (float) (Hash.toUnit(Hash.mix2(siteSeed, 9, 13)) * Math.PI * 2.0);
            hidden.add(StructureSite.hidden((int) Math.round(x), (int) Math.round(z), (int) Math.round(height),
                    siteSeed, rotation));
        }
        return hidden;
    }

    public PlanetDefinition planet() {
        return planet;
    }
}
