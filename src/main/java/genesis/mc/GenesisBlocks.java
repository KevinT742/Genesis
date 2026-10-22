package genesis.mc;

import java.util.function.UnaryOperator;

import genesis.Genesis;
import genesis.api.material.BlockTemplate;
import genesis.api.material.MaterialFamily;
import genesis.api.material.MaterialProfile;
import genesis.api.material.MaterialRegistry;
import genesis.mc.material.GenesisMaterialCatalog;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The blocks of Genesis.
 *
 * <p>Every block is registered through a {@link BlockTemplate}. The template holds the material
 * profile, and the profile holds the physics, the chemistry and the magic. The Minecraft specific
 * traits are derived from the same template, so a block added by another member of the mod family
 * behaves exactly like the blocks that ship with Genesis.
 */
public final class GenesisBlocks {

    /** The registry that holds every Genesis block. */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Genesis.MODID);
    /** The registry that holds every Genesis item. */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Genesis.MODID);

    // ---------------------------------------------------------------------
    // templates
    // ---------------------------------------------------------------------

    /** A cut stone that is as hard as the stone it is made from. */
    public static final BlockTemplate ANDESITE_BRICK = BlockTemplate.builder("andesite_brick")
            .profile(MaterialFamily.rock("genesis:andesite_brick"))
            .toolTier(BlockTemplate.ToolTier.STONE)
            .sound(BlockTemplate.SoundKind.STONE)
            .build();

    /** A cut deep stone, the toughest building material of the family. */
    public static final BlockTemplate DEEPSLATE_BRICK = BlockTemplate.builder("deepslate_brick")
            .profile(MaterialFamily.deepStone("genesis:deepslate_brick"))
            .toolTier(BlockTemplate.ToolTier.STONE)
            .sound(BlockTemplate.SoundKind.STONE)
            .build();

    /** Packed earth, the surface of the country roads. */
    public static final BlockTemplate PACKED_EARTH = BlockTemplate.builder("packed_earth")
            .profile(MaterialFamily.granular("genesis:packed_earth"))
            .toolTier(BlockTemplate.ToolTier.NONE)
            .sound(BlockTemplate.SoundKind.GRAVEL)
            .friction(0.8)
            .build();

    /** Gravel, the surface of the roads in the cold regions. */
    public static final BlockTemplate GRAVEL_PATH = BlockTemplate.builder("gravel_path")
            .profile(MaterialFamily.granular("genesis:gravel_path"))
            .toolTier(BlockTemplate.ToolTier.NONE)
            .sound(BlockTemplate.SoundKind.GRAVEL)
            .friction(0.8)
            .build();

    /** Cobblestone, the surface of the roads around the great towns. */
    public static final BlockTemplate COBBLE_PATH = BlockTemplate.builder("cobble_path")
            .profile(MaterialFamily.rock("genesis:cobble_path"))
            .toolTier(BlockTemplate.ToolTier.STONE)
            .sound(BlockTemplate.SoundKind.STONE)
            .friction(0.8)
            .build();

    /** An arcane brick, attuned to the element of the void. */
    public static final BlockTemplate ARCANE_BRICK = BlockTemplate.builder("arcane_brick")
            .profile(MaterialFamily.arcane("genesis:arcane_brick"))
            .toolTier(BlockTemplate.ToolTier.DIAMOND)
            .sound(BlockTemplate.SoundKind.STONE)
            .build();

    /** A resonant crystal that glows faintly. */
    public static final BlockTemplate CRYSTAL_BLOCK = BlockTemplate.builder("crystal_block")
            .profile(MaterialFamily.crystal("genesis:crystal_block"))
            .toolTier(BlockTemplate.ToolTier.IRON)
            .sound(BlockTemplate.SoundKind.GLASS)
            .lightLevel(4)
            .build();

    // ---------------------------------------------------------------------
    // registration
    // ---------------------------------------------------------------------

    public static final DeferredBlock<Block> ANDESITE_BRICK_BLOCK =
            BLOCKS.registerSimpleBlock("andesite_brick", properties(ANDESITE_BRICK));
    public static final DeferredBlock<Block> DEEPSLATE_BRICK_BLOCK =
            BLOCKS.registerSimpleBlock("deepslate_brick", properties(DEEPSLATE_BRICK));
    public static final DeferredBlock<Block> PACKED_EARTH_BLOCK =
            BLOCKS.registerSimpleBlock("packed_earth", properties(PACKED_EARTH));
    public static final DeferredBlock<Block> GRAVEL_PATH_BLOCK =
            BLOCKS.registerSimpleBlock("gravel_path", properties(GRAVEL_PATH));
    public static final DeferredBlock<Block> COBBLE_PATH_BLOCK =
            BLOCKS.registerSimpleBlock("cobble_path", properties(COBBLE_PATH));
    public static final DeferredBlock<Block> ARCANE_BRICK_BLOCK =
            BLOCKS.registerSimpleBlock("arcane_brick", properties(ARCANE_BRICK));
    public static final DeferredBlock<Block> CRYSTAL_BLOCK_BLOCK =
            BLOCKS.registerSimpleBlock("crystal_block", properties(CRYSTAL_BLOCK));

    public static final DeferredItem<BlockItem> ANDESITE_BRICK_ITEM =
            ITEMS.registerSimpleBlockItem("andesite_brick", ANDESITE_BRICK_BLOCK);
    public static final DeferredItem<BlockItem> DEEPSLATE_BRICK_ITEM =
            ITEMS.registerSimpleBlockItem("deepslate_brick", DEEPSLATE_BRICK_BLOCK);
    public static final DeferredItem<BlockItem> PACKED_EARTH_ITEM =
            ITEMS.registerSimpleBlockItem("packed_earth", PACKED_EARTH_BLOCK);
    public static final DeferredItem<BlockItem> GRAVEL_PATH_ITEM =
            ITEMS.registerSimpleBlockItem("gravel_path", GRAVEL_PATH_BLOCK);
    public static final DeferredItem<BlockItem> COBBLE_PATH_ITEM =
            ITEMS.registerSimpleBlockItem("cobble_path", COBBLE_PATH_BLOCK);
    public static final DeferredItem<BlockItem> ARCANE_BRICK_ITEM =
            ITEMS.registerSimpleBlockItem("arcane_brick", ARCANE_BRICK_BLOCK);
    public static final DeferredItem<BlockItem> CRYSTAL_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("crystal_block", CRYSTAL_BLOCK_BLOCK);

    private GenesisBlocks() {
    }

    /**
     * Translates a template into the block properties of Minecraft.
     *
     * <p>Every value that can be derived from the material profile is derived here, which is what
     * keeps the behaviour of a block consistent with its material.
     */
    public static UnaryOperator<BlockBehaviour.Properties> properties(BlockTemplate template) {
        return properties -> {
            BlockBehaviour.Properties result = BlockBehaviour.Properties.of()
                    .strength(template.miningHardness(), template.explosionResistance())
                    .sound(soundOf(template.sound()))
                    .friction((float) template.friction());
            if (template.lightLevel() > 0) {
                result = result.lightLevel(state -> template.lightLevel());
            }
            if (template.requiresCorrectTool()) {
                result = result.requiresCorrectToolForDrops();
            }
            if (template.translucent()) {
                result = result.noOcclusion();
            }
            return result;
        };
    }

    /** The Minecraft sound type a template sound maps to. */
    public static SoundType soundOf(BlockTemplate.SoundKind sound) {
        return switch (sound) {
            case WOOD -> SoundType.WOOD;
            case GRAVEL -> SoundType.GRAVEL;
            case SAND -> SoundType.SAND;
            case GLASS -> SoundType.GLASS;
            case METAL -> SoundType.METAL;
            case SNOW -> SoundType.SNOW;
            case GRASS -> SoundType.GRASS;
            case WOOL -> SoundType.WOOL;
            case PLANT -> SoundType.GRASS;
            case AMETHYST -> SoundType.GLASS;
            case SCULK -> SoundType.STONE;
            case STONE -> SoundType.STONE;
        };
    }

    /** Registers the material profiles of the Genesis blocks. */
    public static void registerProfiles() {
        GenesisMaterialCatalog.registerVanillaProfiles();
        MaterialRegistry.register(ANDESITE_BRICK.profile());
        MaterialRegistry.register(DEEPSLATE_BRICK.profile());
        MaterialRegistry.register(PACKED_EARTH.profile());
        MaterialRegistry.register(GRAVEL_PATH.profile());
        MaterialRegistry.register(COBBLE_PATH.profile());
        MaterialRegistry.register(ARCANE_BRICK.profile());
        MaterialRegistry.register(CRYSTAL_BLOCK.profile());
    }

    /**
     * The creative tab of the mod.
     *
     * <p>Minecraft 26.2 reworked the creative tabs: they are now addressed by a row and a column
     * instead of by a key, and the callback that fills a tab receives a type that is not visible
     * outside of the game. The tab is therefore created here and filled through the NeoForge
     * event, which works the same way for mod tabs and for vanilla tabs.
     */
    public static CreativeModeTab tab() {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 7)
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.genesis"))
                .icon(() -> ANDESITE_BRICK_ITEM.get().getDefaultInstance())
                .build();
    }

    /** Adds the Genesis blocks to the creative tab of the mod. */
    public static void fillTab(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        event.accept(ANDESITE_BRICK_ITEM.get());
        event.accept(DEEPSLATE_BRICK_ITEM.get());
        event.accept(PACKED_EARTH_ITEM.get());
        event.accept(GRAVEL_PATH_ITEM.get());
        event.accept(COBBLE_PATH_ITEM.get());
        event.accept(ARCANE_BRICK_ITEM.get());
        event.accept(CRYSTAL_BLOCK_ITEM.get());
    }

    /** The material profile of a Genesis block, or {@code null} when it has none. */
    public static MaterialProfile profileOf(Block block) {
        return MaterialRegistry.get("genesis:" + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath())
                .orElse(null);
    }
}
