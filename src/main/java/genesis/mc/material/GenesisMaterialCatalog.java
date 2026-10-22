package genesis.mc.material;

import genesis.api.material.MaterialFamily;
import genesis.api.material.MaterialProfile;
import genesis.api.material.MaterialRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Binds the material profiles to the blocks and items that ship with Minecraft.
 *
 * <p>Vanilla blocks and items are given a profile so that they behave consistently with the blocks
 * the mod family adds. A stone brick and a Genesis stone brick then share the same physics,
 * chemistry and magic, which is what makes the unified template usable across the whole ecosystem.
 *
 * <p>The whole vanilla world is covered: every block and every item of the game gets a profile, so
 * there is no such thing as a vanilla thing whose material the family does not know.
 */
public final class GenesisMaterialCatalog {

    private GenesisMaterialCatalog() {
    }

    /** Registers the materials of every vanilla block and every vanilla item. */
    public static void registerVanillaProfiles() {
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            MaterialRegistry.register(key.toString(), VanillaMaterials.of(key.getPath(), VanillaMaterials.BLOCK));
        }
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            MaterialRegistry.register(key.toString(), materialOf(item, key));
        }
    }

    /**
     * The material of an item.
     *
     * <p>The identifier of the item is the best evidence: it says what the thing is. A ball of
     * string is called {@code minecraft:string} and it is string, whatever block it happens to
     * place.
     *
     * <p>Only when the name of the item says nothing does the block it places get a say. A portion
     * of food holds a portion of its material rather than a piece of it.
     */
    private static MaterialProfile materialOf(Item item, Identifier key) {
        MaterialProfile own = VanillaMaterials.of(key.getPath());
        if (!isUnknown(own)) {
            return own;
        }
        if (item instanceof BlockItem blockItem) {
            MaterialProfile block = profileOf(blockItem.getBlock());
            if (block != null && !isUnknown(block)) {
                return block.withVolume(VanillaMaterials.BLOCK);
            }
            return own.withVolume(VanillaMaterials.BLOCK);
        }
        if (VanillaMaterials.isFood(key.getPath())) {
            // Food is living matter, and a portion of it.
            return MaterialFamily.organic(key.toString()).withVolume(VanillaMaterials.FOOD);
        }
        return own;
    }

    /** Whether a material is the one of a thing the family does not know. */
    private static boolean isUnknown(MaterialProfile profile) {
        return profile == null
                || profile.magic().manaCapacity() == MaterialFamily.GENERIC_MANA_CAPACITY;
    }

    /** The material profile of a block, or {@code null} when it has none. */
    public static MaterialProfile profileOf(Block block) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        return MaterialRegistry.get(key.toString()).orElse(null);
    }

    /** The material profile of an item, or {@code null} when it has none. */
    public static MaterialProfile profileOf(Item item) {
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        return MaterialRegistry.get(key.toString()).orElse(null);
    }
}
