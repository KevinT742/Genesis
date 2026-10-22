package genesis.core.settlement;

/**
 * A point of interest placed on the planet.
 *
 * @param kind        the structural family this site belongs to
 * @param x           the world X coordinate
 * @param z           the world Z coordinate
 * @param y           the ground height at the site
 * @param size        the size of the settlement, meaningless for ruins and hidden sites
 * @param radius      the radius of the site in blocks
 * @param seed        an independent seed, so each site generates its own content
 * @param rotation    the rotation of the site in radians
 * @param settlement  the index of the settlement this site belongs to, or {@code -1}
 */
public record StructureSite(
        SiteKind kind,
        int x,
        int z,
        int y,
        SettlementSize size,
        int radius,
        long seed,
        float rotation,
        int settlement) {

    public static StructureSite settlement(int x, int z, int y, SettlementSize size, long seed, float rotation,
            int index) {
        return new StructureSite(SiteKind.SETTLEMENT, x, z, y, size, size.radius(), seed, rotation, index);
    }

    public static StructureSite ruin(int x, int z, int y, long seed, float rotation) {
        return new StructureSite(SiteKind.RUIN, x, z, y, SettlementSize.HAMLET, 14, seed, rotation, -1);
    }

    public static StructureSite hidden(int x, int z, int y, long seed, float rotation) {
        return new StructureSite(SiteKind.HIDDEN, x, z, y, SettlementSize.HAMLET, 9, seed, rotation, -1);
    }
}
