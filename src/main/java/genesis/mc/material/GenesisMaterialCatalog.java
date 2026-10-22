package genesis.mc.material;

import genesis.api.material.MaterialFamily;
import genesis.api.material.MaterialProfile;
import genesis.api.material.MaterialRegistry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Binds the material profiles to the blocks that ship with Minecraft.
 *
 * <p>Vanilla blocks are given a profile so that they behave consistently with the blocks the mod
 * family adds. A stone brick and a Genesis stone brick then share the same physics, chemistry and
 * magic, which is what makes the unified template usable across the whole ecosystem.
 */
public final class GenesisMaterialCatalog {

    private GenesisMaterialCatalog() {
    }

    /** Registers the profiles of the vanilla blocks. */
    public static void registerVanillaProfiles() {
        register(Blocks.STONE, MaterialFamily.rock("minecraft:stone"));
        register(Blocks.COBBLESTONE, MaterialFamily.rock("minecraft:cobblestone"));
        register(Blocks.STONE_BRICKS, MaterialFamily.rock("minecraft:stone_bricks"));
        register(Blocks.DEEPSLATE, MaterialFamily.deepStone("minecraft:deepslate"));
        register(Blocks.DIRT, MaterialFamily.granular("minecraft:dirt"));
        register(Blocks.GRASS_BLOCK, MaterialFamily.organic("minecraft:grass_block"));
        register(Blocks.COARSE_DIRT, MaterialFamily.granular("minecraft:coarse_dirt"));
        register(Blocks.SAND, MaterialFamily.granular("minecraft:sand"));
        register(Blocks.GRAVEL, MaterialFamily.granular("minecraft:gravel"));
        register(Blocks.CLAY, MaterialFamily.granular("minecraft:clay"));
        register(Blocks.SNOW_BLOCK, MaterialFamily.frozen("minecraft:snow_block"));
        register(Blocks.WATER, MaterialFamily.liquid("minecraft:water"));
        register(Blocks.NETHERRACK, MaterialFamily.rock("minecraft:netherrack"));

        register(Blocks.OAK_PLANKS, MaterialFamily.wood("minecraft:oak_planks"));
        register(Blocks.OAK_LOG, MaterialFamily.wood("minecraft:oak_log"));
        register(Blocks.OAK_LEAVES, MaterialFamily.foliage("minecraft:oak_leaves"));

        register(Blocks.IRON_BLOCK, MaterialFamily.metal("minecraft:iron_block", "Fe", 5.0));
        register(Blocks.GOLD_BLOCK, MaterialFamily.nobleMetal("minecraft:gold_block", "Au", 3.0));
        register(Blocks.DIAMOND_BLOCK, MaterialFamily.crystal("minecraft:diamond_block"));
        register(Blocks.AMETHYST_BLOCK, MaterialFamily.crystal("minecraft:amethyst_block"));
        register(Blocks.OBSIDIAN, MaterialFamily.obsidian("minecraft:obsidian"));
    }

    private static void register(Block block, MaterialProfile profile) {
        MaterialRegistry.register("minecraft:" + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath(),
                profile);
    }
}
