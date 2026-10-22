package genesis.core.ecology;

/**
 * The biome families Genesis can classify a position into.
 *
 * <p>The mapping from climate to biome happens in {@link EcologyModel}; the Minecraft layer then
 * translates the family into a concrete vanilla biome.
 */
public enum BiomeKind {
    /** Open grassland. */
    PLAINS,
    /** Temperate forest. */
    FOREST,
    /** Cold coniferous forest. */
    TAIGA,
    /** Frozen tundra. */
    SNOWY_TUNDRA,
    /** Hot and dry. */
    DESERT,
    /** Warm grassland with sparse trees. */
    SAVANNA,
    /** Dense tropical forest. */
    JUNGLE,
    /** Wet lowland. */
    SWAMP,
    /** Rocky highland. */
    MOUNTAIN,
    /** Snow covered peaks. */
    SNOWY_PEAKS,
    /** Sandy coast. */
    BEACH,
    /** Open ocean. */
    OCEAN,
    /** A river or a small inland water body. */
    RIVER,
    /** Frozen river. */
    FROZEN_RIVER,
    /** Frozen ocean. */
    FROZEN_OCEAN,
    /** Rare mushroom island. */
    MUSHROOM_FIELDS,
    /** The surface of a lava world. */
    NETHER_WASTES;

    /** Whether the biome is covered by water at the surface. */
    public boolean isWater() {
        return this == OCEAN || this == RIVER || this == FROZEN_OCEAN || this == FROZEN_RIVER;
    }

    /** Whether the biome is cold enough to freeze over. */
    public boolean isFrozen() {
        return this == SNOWY_TUNDRA || this == SNOWY_PEAKS || this == FROZEN_OCEAN || this == FROZEN_RIVER;
    }

    /** Whether the biome is dry enough to support sand rather than soil. */
    public boolean isDry() {
        return this == DESERT || this == SAVANNA || this == NETHER_WASTES;
    }

    /** Whether the biome is mountainous. */
    public boolean isMountain() {
        return this == MOUNTAIN || this == SNOWY_PEAKS;
    }
}
