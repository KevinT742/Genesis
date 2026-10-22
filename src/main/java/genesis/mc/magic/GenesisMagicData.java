package genesis.mc.magic;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import genesis.Genesis;
import genesis.api.magic.MagicInfluence;
import genesis.api.planet.PlanetDefinition;
import genesis.core.magic.MagicField;
import genesis.core.magic.MagicFieldModel;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The magic of one dimension, written to disk.
 *
 * <p>The distribution of the magic over a planet follows from the seed, so it does not have to be
 * stored. What has to be stored are the <em>influences</em>: the rituals, the sacrifices and the
 * drained regions that changed the magic of the ground. They are what a player does to a world,
 * and they have to survive a restart of the server.
 *
 * <p>Every dimension has its own storage, so the magic of the home world and the magic of Ember
 * never mix.
 */
public final class GenesisMagicData extends SavedData {

    /**
     * The format the magic of a dimension is stored in.
     *
     * <p>Declared before {@link #TYPE}, because the type refers to it while the class is still
     * being initialised.
     */
    public static final Codec<GenesisMagicData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MagicInfluenceCodec.CODEC.listOf().fieldOf("influences").forGetter(GenesisMagicData::influences)
    ).apply(instance, GenesisMagicData::new));

    /**
     * The type of the stored magic.
     *
     * <p>The data fix type is {@code null}: the magic of a world is our own data, and there is no
     * vanilla data fixer that knows how to bring it forward. NeoForge supports a missing data fix
     * type and then writes and reads the data as it is.
     */
    public static final SavedDataType<GenesisMagicData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Genesis.MODID, "magic_field"),
            GenesisMagicData::new,
            CODEC,
            null);

    /** The influences that were applied to this world. */
    private final List<MagicInfluence> influences = new CopyOnWriteArrayList<>();

    /** The living field of this dimension. It is built when it is asked for. */
    private MagicField field;

    /** The planet the living field belongs to. */
    private String planetId;

    public GenesisMagicData() {
    }

    private GenesisMagicData(List<MagicInfluence> loaded) {
        this.influences.addAll(loaded);
    }

    /** Every outside influence that was applied to this world. */
    public List<MagicInfluence> influences() {
        return List.copyOf(influences);
    }

    /**
     * The living magic field of this dimension.
     *
     * <p>The field writes straight into the list of influences that is written to disk, so a
     * change to the magic is a change to the stored world. The field is rebuilt when the planet
     * of the dimension changes, which does not happen in a running world but does happen when a
     * dimension type is edited.
     *
     * @param planet the planet this dimension is on
     * @param model  the distribution of the magic over that planet
     */
    public MagicField field(PlanetDefinition planet, MagicFieldModel model) {
        if (field == null || !planet.id().equals(planetId)) {
            planetId = planet.id();
            field = new MagicField(model, influences);
            field.setChangeListener(changed -> setDirty());
        }
        return field;
    }
}
