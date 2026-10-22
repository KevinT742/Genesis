package genesis.api.item;

/**
 * The creative tab an item belongs to.
 *
 * <p>The categories of the mod family are a little more precise than the ones of Minecraft: they
 * keep the ingredients of alchemy apart from the ingredients of cooking, because the mod family
 * needs to know which recipe table an item belongs to.
 */
public enum ItemCategory {
    /** Raw material: stone, ingots, dust. */
    MATERIAL,
    /** A tool. */
    TOOL,
    /** A weapon. */
    WEAPON,
    /** A piece of armour. */
    ARMOR,
    /** Something to eat. */
    FOOD,
    /** A potion or an elixir. */
    BREWING,
    /** An ingredient of alchemy. */
    REAGENT,
    /** An arcane object: a focus, a rune, a tome. */
    ARCANE,
    /** A part of a bigger thing. */
    COMPONENT,
    /** Everything else. */
    MISC;

    /** The translation key of the name of this category. */
    public String translationKey() {
        return "itemGroup.genesis." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
