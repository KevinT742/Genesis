package genesis.mc.planet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import genesis.api.planet.PlanetDefinition;

/**
 * The registry of planets known to the game.
 *
 * <p>Genesis ships with two planets. Members of the mod family add their own during the common
 * setup phase, and any dimension whose chunk generator names a registered planet picks up its
 * terrain, climate and gravity automatically.
 */
public final class GenesisPlanets {

    private static final Map<String, PlanetDefinition> PLANETS = new ConcurrentHashMap<>();
    private static final List<PlanetDefinition> ORDERED = new java.util.ArrayList<>();

    private GenesisPlanets() {
    }

    /** Registers a planet, replacing any previous planet with the same id. */
    public static PlanetDefinition register(PlanetDefinition planet) {
        if (planet == null) {
            throw new IllegalArgumentException("Cannot register a null planet");
        }
        PLANETS.put(planet.id(), planet);
        synchronized (ORDERED) {
            ORDERED.removeIf(existing -> existing.id().equals(planet.id()));
            ORDERED.add(planet);
        }
        return planet;
    }

    /** Looks a planet up by id. */
    public static PlanetDefinition get(String id) {
        return PLANETS.get(id);
    }

    /** Looks a planet up by id, failing loudly when it is unknown. */
    public static PlanetDefinition getOrThrow(String id) {
        PlanetDefinition planet = PLANETS.get(id);
        if (planet == null) {
            throw new IllegalArgumentException("Unknown Genesis planet '" + id + "'. Known planets: "
                    + PLANETS.keySet());
        }
        return planet;
    }

    /** All registered planets, in registration order. */
    public static Collection<PlanetDefinition> all() {
        synchronized (ORDERED) {
            return Collections.unmodifiableList(new java.util.ArrayList<>(ORDERED));
        }
    }

    /** Registers the planets that ship with Genesis. */
    public static void registerBuiltIns() {
        register(PlanetDefinition.homeWorld());
        register(PlanetDefinition.ember());
    }
}
