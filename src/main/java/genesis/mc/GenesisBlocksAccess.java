package genesis.mc;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Resolves the block states of the Genesis blocks.
 *
 * <p>The deferred holders of a mod cannot be touched during registration, so the world generation
 * code goes through this small accessor instead.
 */
public final class GenesisBlocksAccess {

    private GenesisBlocksAccess() {
    }

    /** The packed earth used on the country roads. */
    public static BlockState packedEarth() {
        return GenesisBlocks.PACKED_EARTH_BLOCK.get().defaultBlockState();
    }

    /** The gravel used on the roads of the cold regions. */
    public static BlockState gravelPath() {
        return GenesisBlocks.GRAVEL_PATH_BLOCK.get().defaultBlockState();
    }

    /** The cobblestone used on the roads of the great towns. */
    public static BlockState cobblePath() {
        return GenesisBlocks.COBBLE_PATH_BLOCK.get().defaultBlockState();
    }

    /** The arcane brick. */
    public static BlockState arcaneBrick() {
        return GenesisBlocks.ARCANE_BRICK_BLOCK.get().defaultBlockState();
    }

    /** The resonant crystal. */
    public static BlockState crystal() {
        return GenesisBlocks.CRYSTAL_BLOCK_BLOCK.get().defaultBlockState();
    }

    /** The planks used for the walls of a settlement. */
    public static BlockState planks() {
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    /** The floor of a settlement. */
    public static BlockState floor() {
        return Blocks.COBBLESTONE.defaultBlockState();
    }
}
