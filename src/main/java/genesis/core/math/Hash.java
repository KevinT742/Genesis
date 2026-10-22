package genesis.core.math;

/**
 * Deterministic integer hashing used as the foundation of all procedural generation in Genesis.
 *
 * <p>Everything here is stateless and reproducible: the same seed always yields the same world, on
 * any machine, in any order of evaluation. This is a hard requirement for world generation, where
 * chunks may be generated in arbitrary order or in parallel.
 */
public final class Hash {

    private Hash() {
    }

    /** The golden ratio constant used by splitmix64. */
    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    /** Mixes a single 64 bit value. */
    public static long mix64(long z) {
        z += GOLDEN;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Mixes a seed with a 2D lattice coordinate. */
    public static long mix2(long seed, int x, int y) {
        long h = mix64(seed ^ (x * GOLDEN));
        return mix64(h ^ (y * 0xD6E8FEB86659FD93L));
    }

    /** Mixes a seed with a 3D lattice coordinate. */
    public static long mix3(long seed, int x, int y, int z) {
        long h = mix2(seed, x, y);
        return mix64(h ^ (z * 0xC2B2AE3D27D4EB4FL));
    }

    /** Mixes a seed with an arbitrary object hash. */
    public static long mixObj(long seed, Object value) {
        return mix64(seed ^ value.hashCode());
    }

    /** Derives an independent sub-seed from a parent seed. */
    public static long derive(long seed, String channel) {
        long h = mix64(seed);
        for (int i = 0; i < channel.length(); i++) {
            h = mix64(h ^ channel.charAt(i));
        }
        return h;
    }

    /** Converts a 64 bit hash into a double in {@code [0, 1)}. */
    public static double toUnit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    /** Converts a 64 bit hash into a double in {@code [-1, 1)}. */
    public static double toSignedUnit(long hash) {
        return toUnit(hash) * 2.0 - 1.0;
    }

    /** Picks a value uniformly from an array using the supplied hash. */
    public static <T> T pick(T[] values, long hash) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Cannot pick from an empty array");
        }
        int index = (int) Math.floorMod(hash, values.length);
        return values[index];
    }
}
