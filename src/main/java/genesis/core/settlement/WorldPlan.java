package genesis.core.settlement;

import java.util.Collections;
import java.util.List;

/**
 * The complete structural layout of a planet.
 *
 * <p>The plan is computed once from the world seed and then reused, so a settlement looks the same
 * no matter in which order the chunks around it are generated.
 *
 * @param settlements the inhabited places
 * @param roads       the roads connecting the settlements
 * @param ruins       the abandoned places
 * @param hidden      the concealed places
 */
public record WorldPlan(List<StructureSite> settlements, List<RoadPath> roads, List<StructureSite> ruins,
        List<StructureSite> hidden) {

    public WorldPlan {
        settlements = List.copyOf(settlements);
        roads = List.copyOf(roads);
        ruins = List.copyOf(ruins);
        hidden = List.copyOf(hidden);
    }

    /** An empty plan, used for dimensions that do not generate structures. */
    public static WorldPlan empty() {
        return new WorldPlan(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList());
    }

    /** All sites of every family, in one list. */
    public List<StructureSite> allSites() {
        List<StructureSite> all = new java.util.ArrayList<>(
                settlements.size() + ruins.size() + hidden.size());
        all.addAll(settlements);
        all.addAll(ruins);
        all.addAll(hidden);
        return all;
    }

    /** The settlement closest to a position, or {@code null} when there is none. */
    public StructureSite nearestSettlement(int x, int z) {
        StructureSite best = null;
        long bestDistance = Long.MAX_VALUE;
        for (StructureSite site : settlements) {
            long dx = site.x() - x;
            long dz = site.z() - z;
            long distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = site;
            }
        }
        return best;
    }
}
