package genesis.mc;

import java.util.function.Consumer;

import genesis.Genesis;
import genesis.api.item.ItemCategory;
import genesis.api.item.ItemRarity;
import genesis.api.item.ItemTemplate;
import genesis.api.magic.MagicInput;
import genesis.api.material.MaterialFamily;
import genesis.api.material.MaterialProfile;
import genesis.api.material.MaterialRegistry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The items of Genesis.
 *
 * <p>Every item is registered through an {@link ItemTemplate}. The template carries the
 * registration information of Minecraft (stack size, rarity, creative category) and the material
 * profile behind it, which carries the physics, the chemistry and the magic.
 *
 * <p>The four items that ship with Genesis cover the four kinds of magic input there are:
 * a 魔力材料 that never was alive, a harvested 魔力器官, solidified magic and a solidified way of
 * channeling magic.
 */
public final class GenesisItems {

    /** The registry that holds every Genesis item. */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Genesis.MODID);

    // ---------------------------------------------------------------------
    // templates
    // ---------------------------------------------------------------------

    /** A shard of magic crystal: a 魔力材料, the everyday ingredient of alchemy. */
    public static final ItemTemplate CRYSTAL_SHARD = ItemTemplate.builder("crystal_shard")
            .profile(MaterialFamily.magicMaterial("genesis:crystal_shard", 0.0002))
            .maxStackSize(64)
            .rarity(ItemRarity.UNCOMMON)
            .category(ItemCategory.MATERIAL)
            .magicInput(MagicInput.MATERIAL)
            .build();

    /** Ground arcane crystal: the same material in a form that mixes into anything. */
    public static final ItemTemplate ARCANE_DUST = ItemTemplate.builder("arcane_dust")
            .profile(MaterialFamily.magicMaterial("genesis:arcane_dust", 0.00005))
            .maxStackSize(64)
            .rarity(ItemRarity.COMMON)
            .category(ItemCategory.REAGENT)
            .magicInput(MagicInput.MATERIAL)
            .build();

    /** A heart of a creature that carried magic: a 魔力器官, harvested and still alive in a way. */
    public static final ItemTemplate HEART_CORE = ItemTemplate.builder("heart_core")
            .profile(MaterialFamily.organTissue("genesis:heart_core", 0.0004))
            .maxStackSize(16)
            .rarity(ItemRarity.RARE)
            .category(ItemCategory.REAGENT)
            .magicInput(MagicInput.ORGAN)
            .build();

    /** A flask of solidified magic: what a potion is before it is drunk. */
    public static final ItemTemplate SPIRIT_VIAL = ItemTemplate.builder("spirit_vial")
            .profile(MaterialFamily.solidifiedMagic("genesis:spirit_vial", 0.0005))
            .maxStackSize(16)
            .rarity(ItemRarity.RARE)
            .category(ItemCategory.BREWING)
            .magicInput(MagicInput.ESSENCE)
            .build();

    /** A tablet with a way of channeling magic written on it: alchemy or an enchantment. */
    public static final ItemTemplate RUNE_TABLET = ItemTemplate.builder("rune_tablet")
            .profile(MaterialFamily.channelMedium("genesis:rune_tablet", 0.0006))
            .maxStackSize(16)
            .rarity(ItemRarity.UNCOMMON)
            .category(ItemCategory.ARCANE)
            .magicInput(MagicInput.CHANNEL)
            .glint(true)
            .build();

    // ---------------------------------------------------------------------
    // registration
    // ---------------------------------------------------------------------

    public static final DeferredItem<Item> CRYSTAL_SHARD_ITEM =
            ITEMS.registerSimpleItem("crystal_shard", properties(CRYSTAL_SHARD));
    public static final DeferredItem<Item> ARCANE_DUST_ITEM =
            ITEMS.registerSimpleItem("arcane_dust", properties(ARCANE_DUST));
    public static final DeferredItem<Item> HEART_CORE_ITEM =
            ITEMS.registerSimpleItem("heart_core", properties(HEART_CORE));
    public static final DeferredItem<Item> SPIRIT_VIAL_ITEM =
            ITEMS.registerSimpleItem("spirit_vial", properties(SPIRIT_VIAL));
    public static final DeferredItem<Item> RUNE_TABLET_ITEM =
            ITEMS.registerSimpleItem("rune_tablet", properties(RUNE_TABLET));

    private GenesisItems() {
    }

    /**
     * Translates a template into the item properties of Minecraft.
     *
     * <p>Everything that can be derived from the material profile is derived here, so that an item
     * of the mod family behaves like the item of Minecraft it replaces.
     */
    public static Consumer<Item.Properties> properties(ItemTemplate template) {
        return properties -> {
            properties.stacksTo(template.maxStackSize());
            properties.rarity(rarityOf(template.rarity()));
        };
    }

    /** The Minecraft rarity a rarity of the mod family maps to. */
    public static Rarity rarityOf(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> Rarity.COMMON;
            case UNCOMMON -> Rarity.UNCOMMON;
            case RARE -> Rarity.RARE;
            case EPIC -> Rarity.EPIC;
        };
    }

    /** Registers the material profiles of the Genesis items. */
    public static void registerProfiles() {
        register(CRYSTAL_SHARD);
        register(ARCANE_DUST);
        register(HEART_CORE);
        register(SPIRIT_VIAL);
        register(RUNE_TABLET);
    }

    private static void register(ItemTemplate template) {
        MaterialProfile profile = template.profile();
        MaterialRegistry.register(profile);
    }

    /** Adds the Genesis items to the creative tab of the mod. */
    public static void fillTab(BuildCreativeModeTabContentsEvent event) {
        event.accept(CRYSTAL_SHARD_ITEM.get());
        event.accept(ARCANE_DUST_ITEM.get());
        event.accept(HEART_CORE_ITEM.get());
        event.accept(SPIRIT_VIAL_ITEM.get());
        event.accept(RUNE_TABLET_ITEM.get());
    }
}
