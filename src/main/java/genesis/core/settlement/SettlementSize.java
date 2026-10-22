package genesis.core.settlement;

/**
 * How large a settlement is.
 *
 * <p>The size drives how many buildings are placed, how wide the roads are and how much traffic the
 * settlement is expected to see.
 */
public enum SettlementSize {
    /** A handful of houses. */
    HAMLET(6, 0.55),
    /** A small village. */
    VILLAGE(18, 0.75),
    /** A town with a market. */
    TOWN(42, 1.0),
    /** A large town. */
    CITY(96, 1.3);

    private final int buildingCount;
    private final double roadWidthScale;

    SettlementSize(int buildingCount, double roadWidthScale) {
        this.buildingCount = buildingCount;
        this.roadWidthScale = roadWidthScale;
    }

    /** How many buildings the settlement contains. */
    public int buildingCount() {
        return buildingCount;
    }

    /** How wide the roads around the settlement are. */
    public double roadWidthScale() {
        return roadWidthScale;
    }

    /** The radius of the settlement in blocks. */
    public int radius() {
        return switch (this) {
            case HAMLET -> 22;
            case VILLAGE -> 40;
            case TOWN -> 64;
            case CITY -> 96;
        };
    }

    /** Picks a size from a unit random value. */
    public static SettlementSize fromRoll(double roll) {
        if (roll < 0.55) {
            return HAMLET;
        }
        if (roll < 0.85) {
            return VILLAGE;
        }
        if (roll < 0.98) {
            return TOWN;
        }
        return CITY;
    }
}
