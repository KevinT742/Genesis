package genesis;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import genesis.mc.GenesisBlocks;
import genesis.mc.GenesisEvents;
import genesis.mc.GenesisItems;
import genesis.mc.planet.GenesisPlanets;

/**
 * The entry point of the Genesis mod.
 *
 * <p>Genesis is the foundation of a family of mods. It provides:
 * <ul>
 *   <li>a unified material template that describes every block by its physics, its chemistry and
 *       its magic,</li>
 *   <li>a world generator that produces continuous planets with realistic terrain, water systems
 *       and ecosystems,</li>
 *   <li>a structural system with settlements, ruins and hidden sites that are connected by
 *       roads,</li>
 *   <li>and the architecture for adding further planets.</li>
 * </ul>
 */
@Mod(Genesis.MODID)
public class Genesis {

    /** The unique identifier of the mod. */
    public static final String MODID = "genesis";

    /** The registry that holds the creative tabs of the mod. */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** The registry that holds the chunk generators of the mod. */
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MODID);

    /** The registry that holds the biome sources of the mod. */
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, MODID);

    /** The codec of the Genesis chunk generator, referenced by the dimension definitions. */
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<? extends ChunkGenerator>>
            CHUNK_GENERATOR_CODEC = CHUNK_GENERATORS.register("genesis_chunk_generator",
            () -> genesis.mc.world.GenesisChunkGenerator.CODEC);

    /** The codec of the Genesis biome source, referenced by the dimension definitions. */
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<? extends BiomeSource>>
            BIOME_SOURCE_CODEC = BIOME_SOURCES.register("genesis_biome_source",
            () -> genesis.mc.world.GenesisBiomeSource.CODEC);

    /** The creative tab of the mod. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("genesis", GenesisBlocks::tab);

    public Genesis(IEventBus modEventBus, ModContainer modContainer) {
        GenesisBlocks.BLOCKS.register(modEventBus);
        GenesisBlocks.ITEMS.register(modEventBus);
        GenesisItems.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        CHUNK_GENERATORS.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, GenesisConfig.SPEC);

        NeoForge.EVENT_BUS.register(new GenesisEvents());
    }

    /** Fills the creative tab of the mod with the Genesis blocks and items. */
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (TAB.getKey() != null && TAB.getKey().equals(event.getTabKey())) {
            GenesisBlocks.fillTab(event);
            GenesisItems.fillTab(event);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        GenesisPlanets.registerBuiltIns();
        GenesisBlocks.registerProfiles();
        GenesisItems.registerProfiles();
    }
}
