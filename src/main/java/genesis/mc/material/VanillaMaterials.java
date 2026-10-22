package genesis.mc.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import genesis.api.material.MaterialFamily;
import genesis.api.material.MaterialProfile;

/**
 * The materials of the vanilla world.
 *
 * <p>Minecraft has more than eight hundred blocks and more than eleven hundred items, and every
 * one of them is made of something. This class decides what.
 *
 * <p>It does not list them one by one. The identifiers of the vanilla world are systematic: an
 * iron ingot, an iron ore and an iron sword all carry the same word, and the words say what the
 * thing is made of. So the materials are written down as <em>rules</em> over the identifier, and
 * every block and every item of the game is resolved through them.
 *
 * <p>Two things make the result consistent:
 *
 * <ul>
 *   <li>the same identifier always gives the same material, so a block and the item that places
 *       it are the same thing;</li>
 *   <li>the <em>amount</em> of the material comes from what the thing is: a block is a cubic
 *       metre, an ingot is a fraction of one, a sword is a little more than an ingot.</li>
 * </ul>
 *
 * <p>Nothing is left out. An item the rules do not recognise gets the profile of
 * {@link MaterialFamily#generic(String)}, which says what is true of nearly everything rather
 * than pretending to know more than the family does.
 */
public final class VanillaMaterials {

    // ---------------------------------------------------------------------
    // how much of a material one thing holds
    // ---------------------------------------------------------------------

    /** A whole block: one cubic metre. */
    public static final double BLOCK = 1.0;
    /** A lump of raw ore. */
    public static final double ORE = 0.0006;
    /** An ingot. */
    public static final double INGOT = 0.00013;
    /** A nugget. */
    public static final double NUGGET = 0.00002;
    /** A gem. */
    public static final double GEM = 0.00001;
    /** A shard, a dust or a powder. */
    public static final double DUST = 0.00005;
    /** A tool. */
    public static final double TOOL = 0.0004;
    /** A piece of armour. */
    public static final double ARMOR = 0.002;
    /** A bottle. */
    public static final double VIAL = 0.0005;
    /** A portion of food. */
    public static final double FOOD = 0.00025;
    /** A sheet, a length of thread, a small thing. */
    public static final double ITEM = 0.0002;

    // ---------------------------------------------------------------------
    // the rules
    // ---------------------------------------------------------------------

    /** The materials of the items that do not follow a pattern. */
    private static final Map<String, MaterialProfile> EXACT = new HashMap<>();

    /** The rules, in the order they are tried. */
    private static final List<Rule> RULES = new ArrayList<>();

    /**
     * The things a player can eat.
     *
     * <p>Food is a portion of a material, not a piece of one, so it holds a different amount.
     * The game says which items are food through a data component, but that component is only
     * bound once the game has fully started, so the list is written down here instead.
     */
    private static final java.util.Set<String> FOOD_IDS = java.util.Set.of(
            "apple", "baked_potato", "beetroot", "beetroot_soup", "bread", "cake", "carrot",
            "cooked_beef", "cooked_chicken", "cooked_cod", "cooked_mutton", "cooked_porkchop",
            "cooked_rabbit", "cooked_salmon", "cookie", "dried_kelp", "enchanted_golden_apple",
            "golden_apple", "golden_carrot", "glow_berries", "honey_bottle", "melon_slice",
            "mushroom_stew", "mutton", "beef", "chicken", "chorus_fruit", "cod", "fermented_spider_eye",
            "milk_bucket", "porkchop", "potato", "poisonous_potato", "pufferfish",
            "pumpkin_pie", "rabbit", "rabbit_stew", "rotten_flesh", "salmon", "spider_eye",
            "suspicious_stew", "sweet_berries", "tropical_fish");

    private VanillaMaterials() {
    }

    /**
     * The material of a vanilla identifier.
     *
     * @param path the path of the identifier, without the namespace
     * @return the material, never {@code null}
     */
    public static MaterialProfile of(String path) {
        MaterialProfile exact = EXACT.get(path);
        if (exact != null) {
            return exact;
        }
        for (Rule rule : RULES) {
            if (rule.match().test(path)) {
                return rule.profile().apply(path);
            }
        }
        return MaterialFamily.generic("minecraft:" + path);
    }

    /**
     * The material of a vanilla identifier, in a given amount.
     *
     * @param path   the path of the identifier, without the namespace
     * @param volume the volume of one unit, in cubic metres
     * @return the material, never {@code null}
     */
    public static MaterialProfile of(String path, double volume) {
        return of(path).withVolume(volume);
    }

    // ---------------------------------------------------------------------
    // building the rules
    // ---------------------------------------------------------------------

    /** One rule: what an identifier has to look like, and what it is made of. */
    private record Rule(Predicate<String> match, Function<String, MaterialProfile> profile) {
    }

    private static void prefix(String start, Function<String, MaterialProfile> profile) {
        RULES.add(new Rule(path -> path.startsWith(start), profile));
    }

    private static void suffix(String end, Function<String, MaterialProfile> profile) {
        RULES.add(new Rule(path -> path.endsWith(end), profile));
    }

    private static void word(String part, Function<String, MaterialProfile> profile) {
        RULES.add(new Rule(path -> path.contains(part), profile));
    }

    /** A rule that matches when the identifier carries the word anywhere in it. */
    private static void containsWord(String part, Function<String, MaterialProfile> profile) {
        RULES.add(new Rule(path -> path.contains(part), profile));
    }

    private static void exact(String path, MaterialProfile profile) {
        EXACT.put(path, profile);
    }

    /** A group of identifiers that are all made of the same kind of material. */
    private static void exactFamily(java.util.Set<String> paths, Function<String, MaterialProfile> builder) {
        for (String path : paths) {
            EXACT.put(path, builder.apply("minecraft:" + path));
        }
    }

    /**
     * Whether a thing is a portion of food rather than a piece of material.
     *
     * @param path the path of the identifier, without the namespace
     */
    public static boolean isFood(String path) {
        return FOOD_IDS.contains(path);
    }

    /**
     * How much of the material one thing holds.
     *
     * <p>The word at the end of an identifier says what kind of thing it is, and every kind of
     * thing holds a different amount of the material.
     */
    public static double volumeOf(String path) {
        if (path.endsWith("_block") || path.equals("block") || path.equals("beacon")) {
            return BLOCK;
        }
        if (path.endsWith("_ore") || path.endsWith("_scrap") || path.endsWith("_debris")) {
            return ORE;
        }
        if (path.endsWith("_ingot")) {
            return INGOT;
        }
        if (path.endsWith("_nugget")) {
            return NUGGET;
        }
        if (path.endsWith("_shard") || path.endsWith("_dust") || path.endsWith("_powder")
                || path.endsWith("_fragment") || path.endsWith("_nugget")) {
            return DUST;
        }
        if (path.endsWith("_sword") || path.endsWith("_pickaxe") || path.endsWith("_axe")
                || path.endsWith("_shovel") || path.endsWith("_hoe") || path.endsWith("_brush")) {
            return TOOL;
        }
        if (path.endsWith("_helmet") || path.endsWith("_chestplate") || path.endsWith("_leggings")
                || path.endsWith("_boots") || path.endsWith("_shield")) {
            return ARMOR;
        }
        if (path.endsWith("_bottle") || path.endsWith("_potion")) {
            return VIAL;
        }
        if (path.endsWith("_lazuli") || path.endsWith("_gem")) {
            return GEM;
        }
        return ITEM;
    }

    /** A metal, from its element. */
    private static Function<String, MaterialProfile> metal(String element, double hardness, boolean noble) {
        return path -> {
            String id = "minecraft:" + path;
            MaterialProfile profile = noble
                    ? MaterialFamily.nobleMetal(id, element, hardness)
                    : MaterialFamily.metal(id, element, hardness);
            return profile.withVolume(volumeOf(path));
        };
    }

    /** A family that is the same whatever the amount of it. */
    private static Function<String, MaterialProfile> family(Function<String, MaterialProfile> builder) {
        return path -> builder.apply("minecraft:" + path).withVolume(volumeOf(path));
    }

    static {
        buildBare();
        buildMetals();
        buildWood();
        buildStone();
        buildSoil();
        buildGlassAndFibre();
        buildMagic();
        buildOrganic();
    }

    // ---------------------------------------------------------------------
    // the materials that are called by one word
    // ---------------------------------------------------------------------

    /**
     * The materials whose identifier is a single word.
     *
     * <p>Most of the rules below match a prefix, which does not match the word itself: the rule
     * for {@code stone_} finds the stone bricks, but not the stone. These are the bare ones.
     */
    private static void buildBare() {
        exactFamily(java.util.Set.of(
                        "stone", "cobblestone", "andesite", "diorite", "granite", "tuff", "calcite",
                        "netherrack", "basalt", "blackstone", "brick", "bedrock", "monument"),
                id -> MaterialFamily.rock(id));
        exactFamily(java.util.Set.of("dirt", "sand", "gravel", "clay", "sponge", "soul_sand"),
                id -> MaterialFamily.granular(id));
        exactFamily(java.util.Set.of("ice", "snow"), id -> MaterialFamily.frozen(id));
        exactFamily(java.util.Set.of("water"), id -> MaterialFamily.liquid(id));
        exactFamily(java.util.Set.of("obsidian"), id -> MaterialFamily.obsidian(id));
        exactFamily(java.util.Set.of("stick", "bowl", "basket"), id -> MaterialFamily.wood(id));
        exactFamily(java.util.Set.of("coal"), id -> MaterialFamily.carbon(id));
        exactFamily(java.util.Set.of("glass", "glass_bottle"), id -> MaterialFamily.glass(id));
        exactFamily(java.util.Set.of("paper", "book", "string", "wool", "canvas"),
                id -> MaterialFamily.fibre(id));
        exactFamily(java.util.Set.of(
                        "poppy", "dandelion", "cornflower", "allium", "azure_bluet", "red_tulip",
                        "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "lily_of_the_valley",
                        "wither_rose", "sunflower", "lilac", "rose_bush", "peony", "tall_grass",
                        "large_fern", "grass", "fern", "dead_bush", "sea_pickle", "blue_orchid",
                        "cactus_flower", "torchflower"),
                id -> MaterialFamily.foliage(id));
    }

    // ---------------------------------------------------------------------
    // the metals
    // ---------------------------------------------------------------------

    private static void buildMetals() {
        // The raw ores are the metal before it was refined, so they are the same metal.
        exact("raw_iron", MaterialFamily.metal("minecraft:raw_iron", "Fe", 5.0).withVolume(ORE));
        exact("raw_copper", MaterialFamily.metal("minecraft:raw_copper", "Cu", 3.5).withVolume(ORE));
        exact("raw_gold", MaterialFamily.nobleMetal("minecraft:raw_gold", "Au", 3.0).withVolume(ORE));

        prefix("netherite_", family(id -> MaterialFamily.netherite(id)));
        exact("ancient_debris", MaterialFamily.netherite("minecraft:ancient_debris").withVolume(ORE));
        exact("netherite_scrap", MaterialFamily.netherite("minecraft:netherite_scrap").withVolume(DUST));

        prefix("iron_", metal("Fe", 5.0, false));
        prefix("copper_", metal("Cu", 3.5, false));
        prefix("gold_", metal("Au", 3.0, true));
        prefix("chainmail_", metal("Fe", 5.0, false));

        // The whole copper family: the blocks that have weathered, oxidised or been waxed are
        // still copper, whatever the game calls them.
        containsWord("copper", metal("Cu", 3.5, false));

        // The raw blocks are the metal before it was refined.
        prefix("raw_iron", metal("Fe", 5.0, false));
        prefix("raw_copper", metal("Cu", 3.5, false));
        prefix("raw_gold", metal("Au", 3.0, true));

        // An ore is the metal inside the rock, so it carries the metal and not the stone it
        // was found in. This has to come before the rule for deep slate.
        containsWord("iron_ore", metal("Fe", 5.0, false));
        containsWord("copper_ore", metal("Cu", 3.5, false));
        containsWord("gold_ore", metal("Au", 3.0, true));
        containsWord("diamond_ore", family(id -> MaterialFamily.gem(id, "C", 3515.0, 10.0)));
        containsWord("emerald_ore", family(id -> MaterialFamily.gem(id, "Be3Al2Si6O18", 2700.0, 7.5)));
        containsWord("lapis_ore", family(id -> MaterialFamily.gem(id, "Na8(AlSi3O8)8(SO4)2", 2400.0, 5.5)));
        containsWord("redstone_ore", family(id -> MaterialFamily.gem(id, "SiO2", 2400.0, 2.0)));
        containsWord("coal_ore", family(id -> MaterialFamily.carbon(id)));
        containsWord("quartz_ore", family(id -> MaterialFamily.gem(id, "SiO2", 2650.0, 7.0)));

        // A golden apple is a fruit that happens to be gold coloured.
        prefix("golden_", family(id -> MaterialFamily.organic(id)));

        // The gems and the powders that are not metals.
        exact("diamond", MaterialFamily.gem("minecraft:diamond", "C", 3515.0, 10.0).withVolume(GEM));
        prefix("diamond_", family(id -> MaterialFamily.gem(id, "C", 3515.0, 10.0)));
        exact("emerald", MaterialFamily.gem("minecraft:emerald", "Be3Al2Si6O18", 2700.0, 7.5).withVolume(GEM));
        prefix("emerald_", family(id -> MaterialFamily.gem(id, "Be3Al2Si6O18", 2700.0, 7.5)));
        exact("lapis_lazuli", MaterialFamily.gem("minecraft:lapis_lazuli", "Na8(AlSi3O8)8(SO4)2", 2400.0, 5.5).withVolume(GEM));
        prefix("lapis_", family(id -> MaterialFamily.gem(id, "Na8(AlSi3O8)8(SO4)2", 2400.0, 5.5)));
        exact("redstone", MaterialFamily.gem("minecraft:redstone", "SiO2", 2400.0, 2.0).withVolume(DUST));
        prefix("redstone_", family(id -> MaterialFamily.gem(id, "SiO2", 2400.0, 2.0)));
        exact("quartz", MaterialFamily.gem("minecraft:quartz", "SiO2", 2650.0, 7.0).withVolume(GEM));
        prefix("quartz_", family(id -> MaterialFamily.gem(id, "SiO2", 2650.0, 7.0)));
        prefix("nether_quartz", family(id -> MaterialFamily.gem(id, "SiO2", 2650.0, 7.0)));

        // Coal and the things that are nearly carbon.
        prefix("coal_", family(id -> MaterialFamily.carbon(id)));
        exact("charcoal", MaterialFamily.carbon("minecraft:charcoal").withVolume(NUGGET));
        exact("gunpowder", MaterialFamily.carbon("minecraft:gunpowder").withVolume(DUST));
        prefix("sulfur", family(id -> MaterialFamily.carbon(id)));
    }

    // ---------------------------------------------------------------------
    // the wood
    // ---------------------------------------------------------------------

    private static void buildWood() {
        String[] trees = {
                "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry",
                "pale_oak", "bamboo", "crimson", "warped"
        };
        for (String tree : trees) {
            prefix(tree + "_", family(id -> MaterialFamily.wood(id)));
        }
        prefix("stripped_", family(id -> MaterialFamily.wood(id)));
        prefix("wooden_", family(id -> MaterialFamily.wood(id)));
        suffix("_planks", family(id -> MaterialFamily.wood(id)));
        suffix("_sapling", family(id -> MaterialFamily.foliage(id)));
        suffix("_leaves", family(id -> MaterialFamily.foliage(id)));
    }

    // ---------------------------------------------------------------------
    // the stone
    // ---------------------------------------------------------------------

    private static void buildStone() {
        prefix("deepslate_", family(id -> MaterialFamily.deepStone(id)));
        prefix("cobbled_deepslate", family(id -> MaterialFamily.deepStone(id)));
        prefix("polished_deepslate", family(id -> MaterialFamily.deepStone(id)));
        prefix("deepslate", family(id -> MaterialFamily.deepStone(id)));
        prefix("stone_", family(id -> MaterialFamily.rock(id)));
        prefix("cobblestone", family(id -> MaterialFamily.rock(id)));
        prefix("andesite", family(id -> MaterialFamily.rock(id)));
        prefix("diorite", family(id -> MaterialFamily.rock(id)));
        prefix("granite", family(id -> MaterialFamily.rock(id)));
        // The cut, polished, cracked and mossy forms of a stone are the same stone.
        prefix("polished_", family(id -> MaterialFamily.rock(id)));
        prefix("smooth_", family(id -> MaterialFamily.rock(id)));
        prefix("cut_", family(id -> MaterialFamily.rock(id)));
        prefix("chiseled_", family(id -> MaterialFamily.rock(id)));
        prefix("cracked_", family(id -> MaterialFamily.rock(id)));
        prefix("mossy_", family(id -> MaterialFamily.rock(id)));
        prefix("infested_", family(id -> MaterialFamily.rock(id)));
        prefix("tuff", family(id -> MaterialFamily.rock(id)));
        prefix("calcite", family(id -> MaterialFamily.rock(id)));
        prefix("netherrack", family(id -> MaterialFamily.rock(id)));
        prefix("basalt", family(id -> MaterialFamily.rock(id)));
        prefix("blackstone", family(id -> MaterialFamily.rock(id)));
        prefix("end_stone", family(id -> MaterialFamily.rock(id)));
        prefix("prismarine", family(id -> MaterialFamily.rock(id)));
        prefix("purpur", family(id -> MaterialFamily.rock(id)));
        prefix("sandstone", family(id -> MaterialFamily.rock(id)));
        prefix("red_sandstone", family(id -> MaterialFamily.rock(id)));
        prefix("terracotta", family(id -> MaterialFamily.rock(id)));
        suffix("_terracotta", family(id -> MaterialFamily.rock(id)));
        word("concrete", family(id -> MaterialFamily.rock(id)));
        prefix("bricks", family(id -> MaterialFamily.rock(id)));
        prefix("nether_brick", family(id -> MaterialFamily.rock(id)));
        prefix("dripstone", family(id -> MaterialFamily.rock(id)));
        prefix("pointed_dripstone", family(id -> MaterialFamily.rock(id)));
        prefix("magma_", family(id -> MaterialFamily.rock(id)));
        prefix("obsidian", family(id -> MaterialFamily.obsidian(id)));
        prefix("crying_obsidian", family(id -> MaterialFamily.obsidian(id)));
        prefix("amethyst_", family(id -> MaterialFamily.crystal(id)));
        prefix("budding_", family(id -> MaterialFamily.crystal(id)));
        prefix("glowstone", family(id -> MaterialFamily.crystal(id)));
        prefix("sea_lantern", family(id -> MaterialFamily.crystal(id)));
        prefix("sulfur", family(id -> MaterialFamily.sulfur(id)));
        prefix("cinnabar", family(id -> MaterialFamily.gem(id, "HgS", 8200.0, 2.5)));
        prefix("lodestone", family(id -> MaterialFamily.rock(id)));
    }

    // ---------------------------------------------------------------------
    // the soil and the sand
    // ---------------------------------------------------------------------

    private static void buildSoil() {
        prefix("grass_block", family(id -> MaterialFamily.organic(id)));
        prefix("moss_block", family(id -> MaterialFamily.foliage(id)));
        prefix("mud", family(id -> MaterialFamily.granular(id)));
        prefix("packed_mud", family(id -> MaterialFamily.granular(id)));
        prefix("soul_sand", family(id -> MaterialFamily.granular(id)));
        prefix("soul_soil", family(id -> MaterialFamily.granular(id)));
        prefix("mycelium", family(id -> MaterialFamily.organic(id)));
        prefix("podzol", family(id -> MaterialFamily.organic(id)));
        prefix("rooted_dirt", family(id -> MaterialFamily.organic(id)));
        prefix("coarse_dirt", family(id -> MaterialFamily.granular(id)));
        prefix("dirt", family(id -> MaterialFamily.granular(id)));
        prefix("clay", family(id -> MaterialFamily.granular(id)));
        prefix("sand", family(id -> MaterialFamily.granular(id)));
        suffix("_sand", family(id -> MaterialFamily.granular(id)));
        prefix("gravel", family(id -> MaterialFamily.granular(id)));
        prefix("sponge", family(id -> MaterialFamily.granular(id)));
        prefix("snow", family(id -> MaterialFamily.frozen(id)));
        prefix("ice", family(id -> MaterialFamily.frozen(id)));
        prefix("packed_ice", family(id -> MaterialFamily.frozen(id)));
        prefix("blue_ice", family(id -> MaterialFamily.frozen(id)));
        prefix("frosted_ice", family(id -> MaterialFamily.frozen(id)));
        prefix("water", family(id -> MaterialFamily.liquid(id)));
        prefix("bubble_column", family(id -> MaterialFamily.liquid(id)));
    }

    // ---------------------------------------------------------------------
    // the glass and the fibre
    // ---------------------------------------------------------------------

    private static void buildGlassAndFibre() {
        prefix("glass", family(id -> MaterialFamily.glass(id)));
        prefix("tinted_glass", family(id -> MaterialFamily.glass(id)));
        prefix("wool", family(id -> MaterialFamily.fibre(id)));
        suffix("_wool", family(id -> MaterialFamily.fibre(id)));
        prefix("carpet", family(id -> MaterialFamily.fibre(id)));
        suffix("_carpet", family(id -> MaterialFamily.fibre(id)));
        prefix("string", family(id -> MaterialFamily.fibre(id)));
        prefix("cobweb", family(id -> MaterialFamily.fibre(id)));
        prefix("paper", family(id -> MaterialFamily.fibre(id)));
        prefix("book", family(id -> MaterialFamily.fibre(id)));
        prefix("map", family(id -> MaterialFamily.fibre(id)));
        suffix("_stained_glass", family(id -> MaterialFamily.glass(id)));

        // The coloured variants of a thing are the same thing.
        suffix("_concrete", family(id -> MaterialFamily.rock(id)));
        suffix("_concrete_powder", family(id -> MaterialFamily.granular(id)));
        suffix("_glazed_terracotta", family(id -> MaterialFamily.rock(id)));
        suffix("_candle", family(id -> MaterialFamily.organic(id)));
        suffix("_bed", family(id -> MaterialFamily.organic(id)));
        suffix("_shulker_box", family(id -> MaterialFamily.organic(id)));
        suffix("_banner", family(id -> MaterialFamily.wood(id)));
        suffix("_dye", family(id -> MaterialFamily.organic(id)));
        suffix("_coral", family(id -> MaterialFamily.rock(id)));
    }

    // ---------------------------------------------------------------------
    // the magic
    // ---------------------------------------------------------------------

    private static void buildMagic() {
        // A potion is magic that was solidified: it is what the magic model is for.
        prefix("potion", path -> MaterialFamily.solidifiedMagic("minecraft:" + path, VIAL));
        prefix("splash_potion", path -> MaterialFamily.solidifiedMagic("minecraft:" + path, VIAL));
        prefix("lingering_potion", path -> MaterialFamily.solidifiedMagic("minecraft:" + path, VIAL));

        // An enchanted book is a way of channeling magic that was written down.
        exact("enchanted_book", MaterialFamily.channelMedium("minecraft:enchanted_book", ITEM));

        prefix("blaze_", family(id -> MaterialFamily.arcane(id)));
        prefix("ender_", family(id -> MaterialFamily.arcane(id)));
        prefix("chorus", family(id -> MaterialFamily.arcane(id)));
        prefix("nether_star", family(id -> MaterialFamily.arcane(id)));
        prefix("dragon_breath", family(id -> MaterialFamily.arcane(id)));
        prefix("nether_wart", family(id -> MaterialFamily.arcane(id)));
        prefix("phantom_membrane", family(id -> MaterialFamily.arcane(id)));
        prefix("totem", family(id -> MaterialFamily.arcane(id)));
        prefix("ghast_tear", family(id -> MaterialFamily.arcane(id)));
        prefix("magma_cream", family(id -> MaterialFamily.arcane(id)));
        prefix("echo_shard", family(id -> MaterialFamily.crystal(id)));
        exact("heart_of_the_sea", MaterialFamily.crystal("minecraft:heart_of_the_sea").withVolume(GEM));
        prefix("sculk", family(id -> MaterialFamily.obsidian(id)));
        prefix("shriek", family(id -> MaterialFamily.arcane(id)));
    }

    // ---------------------------------------------------------------------
    // the organic
    // ---------------------------------------------------------------------

    private static void buildOrganic() {
        prefix("leather", family(id -> MaterialFamily.organic(id)));
        prefix("feather", family(id -> MaterialFamily.organic(id)));
        prefix("bone", family(id -> MaterialFamily.organic(id)));
        prefix("egg", family(id -> MaterialFamily.organic(id)));
        prefix("rotten_flesh", family(id -> MaterialFamily.organic(id)));
        prefix("spider_eye", family(id -> MaterialFamily.organic(id)));
        prefix("slime", family(id -> MaterialFamily.organic(id)));
        prefix("honey", family(id -> MaterialFamily.organic(id)));
        prefix("mushroom", family(id -> MaterialFamily.organic(id)));
        prefix("melon", family(id -> MaterialFamily.organic(id)));
        prefix("pumpkin", family(id -> MaterialFamily.organic(id)));
        prefix("sweet_berries", family(id -> MaterialFamily.organic(id)));
        prefix("glow_berries", family(id -> MaterialFamily.organic(id)));
        prefix("cactus", family(id -> MaterialFamily.organic(id)));
        prefix("nautilus_shell", family(id -> MaterialFamily.organic(id)));
        prefix("turtle_scute", family(id -> MaterialFamily.organic(id)));
        prefix("scute", family(id -> MaterialFamily.organic(id)));
        prefix("shulker_shell", family(id -> MaterialFamily.organic(id)));
        prefix("ink_sac", family(id -> MaterialFamily.organic(id)));
        prefix("glow_ink_sac", family(id -> MaterialFamily.organic(id)));
        prefix("resin", family(id -> MaterialFamily.organic(id)));
        prefix("sugar_cane", family(id -> MaterialFamily.foliage(id)));
        prefix("vine", family(id -> MaterialFamily.foliage(id)));
        prefix("lily_pad", family(id -> MaterialFamily.foliage(id)));
        prefix("kelp", family(id -> MaterialFamily.foliage(id)));
        prefix("seagrass", family(id -> MaterialFamily.foliage(id)));
        prefix("mangrove_propagule", family(id -> MaterialFamily.foliage(id)));
    }
}
