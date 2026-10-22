package genesis.mc.dev;

import genesis.api.material.MaterialProfile;
import genesis.api.material.MaterialRegistry;
import genesis.mc.material.GenesisMaterialCatalog;
import genesis.mc.material.VanillaMaterials;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The self test of the materials of the vanilla world.
 *
 * <p>Minecraft has more than eight hundred blocks and more than eleven hundred items. This test
 * checks that every one of them has a material, that a block and the item that places it are the
 * same material, and that the materials that have a real answer really have it: an iron ingot
 * weighs what an iron ingot weighs.
 */
public final class GenesisMaterialCatalogTest {

    private static int checks;
    private static int failures;

    private GenesisMaterialCatalogTest() {
    }

    public static void main(String[] args) {
        System.out.println("== The materials of the vanilla world ==");
        // The registries of the game only exist once the game has bootstrapped itself. In the game
        // the loader does this before any mod is set up; here we have to do it ourselves: first the
        // version of the game has to be found, then the registries can be frozen.
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        GenesisMaterialCatalog.registerVanillaProfiles();
        testCoverage();
        testBlockItemsShareTheirBlock();
        testRealMaterials();
        testTheLookupFindsThem();

        System.out.println("checks  : " + checks);
        System.out.println("failures: " + failures);
        if (failures > 0) {
            System.out.println("MATERIAL TEST FAILED");
            System.exit(1);
        }
        System.out.println("MATERIAL TEST PASSED");
    }

    /** Nothing in the vanilla world may be left without a material. */
    private static void testCoverage() {
        int missingBlocks = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            if (MaterialRegistry.get(key.toString()).isEmpty()) {
                missingBlocks++;
            }
        }
        check("every vanilla block has a material (" + BuiltInRegistries.BLOCK.size() + " blocks)",
                missingBlocks == 0);

        int missingItems = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            if (MaterialRegistry.get(key.toString()).isEmpty()) {
                missingItems++;
            }
        }
        check("every vanilla item has a material (" + BuiltInRegistries.ITEM.size() + " items)",
                missingItems == 0);
    }

    /** A block and the item that places it are the same material. */
    private static void testBlockItemsShareTheirBlock() {
        int mismatched = 0;
        int compared = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BlockItem blockItem)) {
                continue;
            }
            MaterialProfile blockProfile = GenesisMaterialCatalog.profileOf(blockItem.getBlock());
            MaterialProfile itemProfile = GenesisMaterialCatalog.profileOf(item);
            if (blockProfile == null || itemProfile == null) {
                mismatched++;
                continue;
            }
            compared++;
            if (Math.abs(blockProfile.density() - itemProfile.density()) > 1.0e-9
                    || !blockProfile.formula().equals(itemProfile.formula())) {
                mismatched++;
            }
        }
        check("a block and the item that places it are the same material (" + compared + " block items)",
                mismatched == 0);
    }

    /** The materials that have a real answer really have it. */
    private static void testRealMaterials() {
        MaterialProfile ingot = profile(Items.IRON_INGOT);
        check("an iron ingot is as dense as iron", ingot != null && Math.abs(ingot.density() - 7874.0) < 1.0e-6);
        check("an iron ingot weighs about a kilogramme",
                ingot != null && Math.abs(ingot.mass() - 1.024) < 0.01);

        MaterialProfile gold = profile(Items.GOLD_BLOCK);
        check("a block of gold is as dense as gold", gold != null && Math.abs(gold.density() - 19300.0) < 1.0e-6);
        check("a block of gold weighs 19.3 tonnes", gold != null && Math.abs(gold.mass() - 19300.0) < 0.1);

        MaterialProfile diamond = profile(Items.DIAMOND);
        check("a diamond is carbon", diamond != null && "C".equals(diamond.formula()));
        check("a diamond is as dense as carbon", diamond != null && Math.abs(diamond.density() - 3515.0) < 1.0e-6);

        MaterialProfile stone = profile(Blocks.STONE);
        check("stone is silicon dioxide", stone != null && "O2SI".equals(stone.formula()));
        check("a block of stone weighs 2.6 tonnes", stone != null && Math.abs(stone.mass() - 2600.0) < 0.1);

        MaterialProfile planks = profile(Blocks.OAK_PLANKS);
        check("wood is cellulose", planks != null && planks.formula().startsWith("C6H10O5"));
        check("wood is light", planks != null && Math.abs(planks.density() - 700.0) < 1.0e-6);

        MaterialProfile potion = profile(Items.POTION);
        check("a potion is solidified magic",
                potion != null && potion.isMagicMaterial() && "?".equals(potion.formula()));
        check("a bottle of potion weighs half a kilogramme",
                potion != null && Math.abs(potion.mass() - 0.6) < 0.01);

        MaterialProfile glass = profile(Blocks.GLASS);
        check("glass is silicon dioxide", glass != null && "O2SI".equals(glass.formula()));
        check("glass is not a magic material", glass != null && !glass.isMagicMaterial());
    }

    /**
     * The lookup has to find them again.
     *
     * <p>This is a regression test: the lookup once hard coded the namespace of Genesis, so a
     * vanilla block was never found and the material of a block of the family was looked up
     * under the wrong name.
     */
    private static void testTheLookupFindsThem() {
        check("the material of a vanilla block is found",
                GenesisMaterialCatalog.profileOf(Blocks.STONE) != null);
        check("the material of a vanilla item is found",
                GenesisMaterialCatalog.profileOf(Items.IRON_INGOT) != null);
        check("a block and the item that places it are looked up under their own names",
                MaterialRegistry.get("minecraft:stone").isPresent()
                        && MaterialRegistry.get("minecraft:iron_ingot").isPresent());
    }

    private static MaterialProfile profile(Item item) {
        return GenesisMaterialCatalog.profileOf(item);
    }

    private static MaterialProfile profile(Block block) {
        return GenesisMaterialCatalog.profileOf(block);
    }

    private static void check(String name, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("  [ok]   " + name);
        } else {
            failures++;
            System.out.println("  [FAIL] " + name);
        }
    }
}
