package genesis.mc.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import genesis.api.magic.MagicInfluence;

/**
 * The format the influences of a world are written in.
 *
 * <p>The influence itself is plain data and lives in the contract layer, which is free of any
 * dependency on the game. The format that carries it into the save file belongs to the Minecraft
 * layer, because it is the Minecraft layer that writes the file.
 */
public final class MagicInfluenceCodec {

    /** The format one influence is written in. */
    public static final Codec<MagicInfluence> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(MagicInfluence::x),
            Codec.DOUBLE.fieldOf("z").forGetter(MagicInfluence::z),
            Codec.DOUBLE.fieldOf("radius").forGetter(MagicInfluence::radius),
            Codec.DOUBLE.fieldOf("delta").forGetter(MagicInfluence::delta)
    ).apply(instance, MagicInfluence::new));

    private MagicInfluenceCodec() {
    }
}
