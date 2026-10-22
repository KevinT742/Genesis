package genesis.mc;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

import genesis.api.item.ItemTemplate;
import genesis.api.magic.MagicConcentration;
import genesis.api.magic.MagicTier;
import genesis.api.material.BlockTemplate;
import genesis.api.material.MaterialProfile;
import genesis.api.planet.PlanetDefinition;
import genesis.core.magic.MagicField;
import genesis.mc.magic.GenesisMagicData;
import genesis.mc.planet.GenesisPlanets;
import genesis.mc.world.GenesisChunkGenerator;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * The way into Genesis for the other members of the mod family.
 *
 * <p>Everything a sibling mod needs is reachable from here, so that it never has to reach into the
 * internals of Genesis: the world of a dimension, the magic that lives in it, the planet it is on
 * and the material of a block.
 *
 * <p>The magic of a dimension is <strong>stored on disk</strong>. A ritual that changed the magic
 * of a region is still there after the server has been restarted, and the magic of two dimensions
 * never mixes.
 */
public final class GenesisApi {

    private GenesisApi() {
    }

    // ---------------------------------------------------------------------
    // the magic of a world
    // ---------------------------------------------------------------------

    /**
     * The magic of a dimension.
     *
     * <p>The returned field is the living one: changing it changes the world, and the change is
     * written to disk when the world is saved.
     *
     * @return the magic of the dimension, or nothing when the dimension is not a Genesis world
     */
    public static Optional<MagicField> magicField(ServerLevel level) {
        GenesisChunkGenerator generator = generatorOf(level);
        if (generator == null) {
            return Optional.empty();
        }
        GenesisMagicData data = level.getDataStorage().computeIfAbsent(GenesisMagicData.TYPE);
        return Optional.of(data.field(generator.planet(), generator.magicModel(level.getChunkSource().randomState())));
    }

    /**
     * The concentration of the ground at a position.
     *
     * @return the concentration, or nothing when the dimension is not a Genesis world
     */
    public static Optional<MagicConcentration> concentration(ServerLevel level, double x, double z) {
        return magicField(level).map(field -> field.concentration(x, z));
    }

    /**
     * The strength of the magic of a region.
     *
     * @return the strength, or nothing when the dimension is not a Genesis world
     */
    public static Optional<MagicTier> tierAt(ServerLevel level, double x, double z) {
        return magicField(level).map(field -> field.tierAt(x, z));
    }

    // ---------------------------------------------------------------------
    // the world of a dimension
    // ---------------------------------------------------------------------

    /**
     * The planet a dimension is on.
     *
     * @return the planet, or nothing when the dimension is not a Genesis world
     */
    public static Optional<PlanetDefinition> planetOf(ServerLevel level) {
        return Optional.ofNullable(generatorOf(level)).map(GenesisChunkGenerator::planet);
    }

    /** Every planet that is known to the game. */
    public static Collection<PlanetDefinition> planets() {
        return GenesisPlanets.all();
    }

    // ---------------------------------------------------------------------
    // the materials of the world
    // ---------------------------------------------------------------------

    /**
     * The material profile of a block.
     *
     * <p>Every block of the game carries one, whether it was registered by Genesis or by another
     * member of the family.
     *
     * @return the profile, or nothing when the block has no material
     */
    public static Optional<MaterialProfile> materialOf(Block block) {
        return Optional.ofNullable(GenesisBlocks.profileOf(block));
    }

    /**
     * Turns a block template into the block properties of Minecraft.
     *
     * <p>Everything that can be derived from the material profile is derived here, so a block of
     * the family behaves the way the block of Minecraft it replaces behaves. A member that adds a
     * block of its own hands the template to this method and gets the same treatment Genesis
     * gives its own blocks.
     *
     * @param template the template of the block
     * @return the properties the block has to be registered with
     */
    public static UnaryOperator<BlockBehaviour.Properties> blockProperties(BlockTemplate template) {
        return GenesisBlocks.properties(template);
    }

    /**
     * Turns an item template into the item properties of Minecraft.
     *
     * @param template the template of the item
     * @return the properties the item has to be registered with
     */
    public static UnaryOperator<Item.Properties> itemProperties(ItemTemplate template) {
        return GenesisItems.properties(template);
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    /** The Genesis generator of a dimension, or {@code null} when the dimension is a vanilla one. */
    private static GenesisChunkGenerator generatorOf(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return generator instanceof GenesisChunkGenerator genesis ? genesis : null;
    }
}
