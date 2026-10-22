package genesis.api.material;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The registry of material profiles.
 *
 * <p>Every block known to Genesis has an entry here. Vanilla blocks are seeded from the
 * {@link MaterialFamily} templates when the game starts, and members of the mod family add their
 * own entries during their own setup phase.
 *
 * <p>All methods are thread safe: blocks are registered from the mod loading thread while the
 * server thread may be reading the registry while chunks are generated.
 */
public final class MaterialRegistry {

    private static final Map<String, MaterialProfile> PROFILES = new ConcurrentHashMap<>();

    private MaterialRegistry() {
    }

    /** Registers a material profile, replacing any previous entry with the same id. */
    public static MaterialProfile register(MaterialProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Cannot register a null material profile");
        }
        PROFILES.put(profile.id(), profile);
        return profile;
    }

    /** Registers a profile under a different id than the one it carries. */
    public static MaterialProfile register(String id, MaterialProfile profile) {
        return register(profile.withId(id));
    }

    /** Looks a profile up by id. */
    public static Optional<MaterialProfile> get(String id) {
        return Optional.ofNullable(PROFILES.get(id));
    }

    /** Whether a profile is registered under the given id. */
    public static boolean contains(String id) {
        return PROFILES.containsKey(id);
    }

    /** All registered profiles. */
    public static Collection<MaterialProfile> all() {
        return Collections.unmodifiableCollection(new LinkedHashMap<>(PROFILES).values());
    }

    /** The number of registered profiles. */
    public static int size() {
        return PROFILES.size();
    }

    /** Removes every entry, used when a world is unloaded in a test environment. */
    public static void clear() {
        PROFILES.clear();
    }
}
