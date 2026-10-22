package genesis.api.material;

/**
 * The unified template every block in a Genesis world is registered with.
 *
 * <p>A template bundles the identity of a block with its material profile and the handful of
 * Minecraft specific traits that cannot be derived from the profile alone, such as which sound it
 * makes when it breaks or how it is rendered.
 *
 * <p>Registering a block through a template guarantees that the block behaves consistently with
 * every other block of the same material, and gives the mod family a single place to extend.
 */
public record BlockTemplate(
        String name,
        MaterialProfile profile,
        ToolTier toolTier,
        SoundKind sound,
        RenderKind render,
        int lightLevel,
        boolean translucent,
        boolean conductive,
        double friction) {

    /** The tool tier needed to mine a block efficiently. */
    public enum ToolTier {
        NONE,
        WOOD,
        STONE,
        IRON,
        DIAMOND,
        NETHERITE
    }

    /** The sound a block makes when it is broken or walked on. */
    public enum SoundKind {
        STONE,
        WOOD,
        GRAVEL,
        SAND,
        GLASS,
        METAL,
        SNOW,
        GRASS,
        WOOL,
        PLANT,
        AMETHYST,
        SCULK
    }

    /** How a block is rendered. */
    public enum RenderKind {
        /** A fully opaque cube. */
        SOLID,
        /** A cube with transparent parts, such as leaves. */
        CUTOUT,
        /** A cube that lets light through, such as ice. */
        TRANSLUCENT,
        /** Fully transparent glass. */
        GLASS,
        /** A fluid. */
        LIQUID
    }

    public BlockTemplate {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A block template needs a name");
        }
        if (profile == null) {
            throw new IllegalArgumentException("A block template needs a material profile");
        }
        if (lightLevel < 0 || lightLevel > 15) {
            throw new IllegalArgumentException("lightLevel must be between 0 and 15");
        }
        toolTier = toolTier == null ? ToolTier.NONE : toolTier;
        sound = sound == null ? SoundKind.STONE : sound;
        render = render == null ? RenderKind.SOLID : render;
    }

    /** The hardness the block is registered with. */
    public float miningHardness() {
        return profile.miningHardness();
    }

    /** The explosion resistance the block is registered with. */
    public float explosionResistance() {
        return profile.explosionResistance();
    }

    /** Whether the block needs the correct tool to drop anything. */
    public boolean requiresCorrectTool() {
        return profile.requiresCorrectTool();
    }

    /** Creates a builder for a template. */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** Creates a template that uses the default traits of a material family. */
    public static BlockTemplate of(String name, MaterialProfile profile) {
        return builder(name).profile(profile).build();
    }

    /** The fluent builder of {@link BlockTemplate}. */
    public static final class Builder {

        private final String name;
        private MaterialProfile profile;
        private ToolTier toolTier = ToolTier.NONE;
        private SoundKind sound = SoundKind.STONE;
        private RenderKind render = RenderKind.SOLID;
        private int lightLevel;
        private boolean translucent;
        private boolean conductive;
        private double friction = 0.6;

        private Builder(String name) {
            this.name = name;
        }

        public Builder profile(MaterialProfile value) {
            this.profile = value;
            return this;
        }

        public Builder toolTier(ToolTier value) {
            this.toolTier = value;
            return this;
        }

        public Builder sound(SoundKind value) {
            this.sound = value;
            return this;
        }

        public Builder render(RenderKind value) {
            this.render = value;
            return this;
        }

        public Builder lightLevel(int value) {
            this.lightLevel = Math.max(0, Math.min(15, value));
            return this;
        }

        public Builder translucent(boolean value) {
            this.translucent = value;
            return this;
        }

        public Builder conductive(boolean value) {
            this.conductive = value;
            return this;
        }

        public Builder friction(double value) {
            this.friction = value;
            return this;
        }

        /** Derives the traits from the material profile and overrides them with what was set. */
        public BlockTemplate build() {
            if (profile == null) {
                throw new IllegalStateException("A block template needs a material profile");
            }
            int emission = lightLevel > 0 ? lightLevel : profile.lightEmission();
            return new BlockTemplate(name, profile, toolTier, sound, render, emission, translucent, conductive,
                    friction);
        }
    }
}
