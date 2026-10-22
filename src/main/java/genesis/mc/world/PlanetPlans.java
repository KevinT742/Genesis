package genesis.mc.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import genesis.api.planet.PlanetDefinition;
import genesis.core.settlement.SettlementPlanner;
import genesis.core.settlement.WorldPlan;
import genesis.core.terrain.TerrainModel;
import genesis.mc.planet.GenesisPlanets;

/**
 * Caches the structural plan of every generated planet.
 *
 * <p>The plan is fully determined by the world seed and the planet definition, so it only has to be
 * computed once per world. Keeping it in memory rather than on disk keeps the planner free of
 * serialisation concerns while still guaranteeing that a settlement looks the same no matter in
 * which order the chunks around it are generated.
 */
public final class PlanetPlans {

    private static final Map<Long, WorldPlan> CACHE = new ConcurrentHashMap<>();

    private PlanetPlans() {
    }

    /** The plan of the planet the given level belongs to. */
    public static WorldPlan of(net.minecraft.server.level.ServerLevel level, PlanetDefinition planet) {
        long seed = level.getSeed();
        long key = mix(seed, planet.id().hashCode());
        return CACHE.computeIfAbsent(key, ignored -> {
            TerrainModel terrain = new TerrainModel(planet, seed);
            return new SettlementPlanner(seed, planet, terrain).plan();
        });
    }

    /** Drops every cached plan, used when a server shuts down. */
    public static void clear() {
        CACHE.clear();
    }

    private static long mix(long seed, int extra) {
        long h = seed * 0x9E3779B97F4A7C15L + extra;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }
}
