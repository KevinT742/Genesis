package genesis.mc.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes the generation models of a planet so that the biome source can reach them.
 *
 * <p>The chunk generator owns the models because it is the component that learns the world seed.
 * The biome source is created earlier, during the loading of the dimension type, and therefore
 * cannot be handed the models directly. Publishing them through this small registry bridges the
 * two without coupling them.
 */
public final class GenesisModels {

    private static final Map<String, GenesisChunkGenerator.Models> MODELS = new ConcurrentHashMap<>();

    private GenesisModels() {
    }

    /** Publishes the models of a planet. */
    public static void publish(String planetId, GenesisChunkGenerator.Models models) {
        MODELS.put(planetId, models);
    }

    /** The models of a planet, or {@code null} when the world has not started generating yet. */
    public static GenesisChunkGenerator.Models get(String planetId) {
        return MODELS.get(planetId);
    }

    /** Drops every published model, used when a server shuts down. */
    public static void clear() {
        MODELS.clear();
    }
}
