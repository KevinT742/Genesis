package genesis.api.item;

/**
 * The rarity of an item.
 *
 * <p>Mirrors the rarities of Minecraft so that the two can be mapped onto each other without a
 * table, but lives in the mod family so that the api layer stays free of Minecraft.
 */
public enum ItemRarity {
    /** The everyday item. */
    COMMON,
    /** A little out of the ordinary. */
    UNCOMMON,
    /** Hard to come by. */
    RARE,
    /** One of a kind. */
    EPIC;

    /** The translation key of the colour of this rarity. */
    public String colourKey() {
        return switch (this) {
            case COMMON -> "gray";
            case UNCOMMON -> "yellow";
            case RARE -> "aqua";
            case EPIC -> "light_purple";
        };
    }
}
