package genesis.api.item;

import genesis.api.magic.MagicInput;
import genesis.api.magic.MagicMaterial;
import genesis.api.material.MaterialProfile;

/**
 * The information an item is registered with.
 *
 * <p>Every item of the mod family is described by one of these templates. It bundles the four
 * groups of information that the game needs and that the family wants to keep consistent:
 *
 * <ul>
 *   <li><strong>原版注册信息</strong> &mdash; the identifier, the stack size, the rarity and the
 *       creative category. These are the values Minecraft itself knows.</li>
 *   <li><strong>物理信息</strong> &mdash; the mass, the volume, the density, the melting and boiling
 *       points. These follow from the material profile.</li>
 *   <li><strong>化学信息</strong> &mdash; the composition, the reactivity, the flammability. These
 *       follow from the material profile as well.</li>
 *   <li><strong>魔法信息</strong> &mdash; how well the item adapts to magic and what kind of magic
 *       input it is: a magic material, a magic organ, solidified magic or a solidified way of
 *       channeling magic.</li>
 * </ul>
 *
 * <p>Because the physics, the chemistry and the magic all come from one profile, an item can never
 * weigh more than its material allows or hold more magic than it can keep.
 *
 * @param name          the identifier of the item inside its namespace
 * @param profile       the material the item is made of
 * @param maxStackSize  how many of the item fit into one slot
 * @param rarity        how rare the item is
 * @param category      the creative category the item is filed under
 * @param magicInput    what the item is, as far as magic is concerned
 * @param burnSeconds   how long one item burns in a furnace, in seconds; {@code 0} for items that do not burn
 * @param glint         whether the item shines without being enchanted
 */
public record ItemTemplate(
        String name,
        MaterialProfile profile,
        int maxStackSize,
        ItemRarity rarity,
        ItemCategory category,
        MagicInput magicInput,
        int burnSeconds,
        boolean glint) {

    public ItemTemplate {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("An item template needs a name");
        }
        if (profile == null) {
            throw new IllegalArgumentException("An item template needs a material profile");
        }
        if (rarity == null) {
            rarity = ItemRarity.COMMON;
        }
        if (category == null) {
            category = ItemCategory.MISC;
        }
        if (magicInput == null) {
            magicInput = MagicInput.NONE;
        }
        maxStackSize = Math.max(1, Math.min(99, maxStackSize));
        burnSeconds = Math.max(0, burnSeconds);
    }

    /** Creates a template with the default registration information. */
    public static ItemTemplate of(String name, MaterialProfile profile) {
        return builder(name).profile(profile).build();
    }

    /** The mass of one item, in kilograms. */
    public double mass() {
        return profile.physical().mass();
    }

    /** The volume of one item, in cubic metres. */
    public double volume() {
        return profile.physical().volume();
    }

    /** The density of the material the item is made of, in kilograms per cubic metre. */
    public double density() {
        return profile.physical().density();
    }

    /** Whether the item is a magic material that can be used as an ingredient. */
    public boolean isMagicMaterial() {
        return magicInput == MagicInput.MATERIAL && profile.magic().isMagicMaterial();
    }

    /** Whether the item can be used as an ingredient of a potion or an elixir. */
    public boolean isBrewable() {
        return magicInput.isBrewable();
    }

    /** The magic material this item stands for, or {@code null} when it is not one. */
    public MagicMaterial asMagicMaterial() {
        if (magicInput != MagicInput.MATERIAL) {
            return null;
        }
        return MagicMaterial.of(name, profile, 0.0);
    }

    /** Creates a builder for a template. */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** The fluent builder of {@link ItemTemplate}. */
    public static final class Builder {

        private final String name;
        private MaterialProfile profile;
        private int maxStackSize = 64;
        private ItemRarity rarity = ItemRarity.COMMON;
        private ItemCategory category = ItemCategory.MISC;
        private MagicInput magicInput = MagicInput.NONE;
        private int burnSeconds;
        private boolean glint;

        private Builder(String name) {
            this.name = name;
        }

        public Builder profile(MaterialProfile value) {
            this.profile = value;
            return this;
        }

        public Builder maxStackSize(int value) {
            this.maxStackSize = value;
            return this;
        }

        public Builder rarity(ItemRarity value) {
            this.rarity = value;
            return this;
        }

        public Builder category(ItemCategory value) {
            this.category = value;
            return this;
        }

        public Builder magicInput(MagicInput value) {
            this.magicInput = value;
            return this;
        }

        public Builder burnSeconds(int value) {
            this.burnSeconds = value;
            return this;
        }

        public Builder glint(boolean value) {
            this.glint = value;
            return this;
        }

        public ItemTemplate build() {
            if (profile == null) {
                throw new IllegalStateException("An item template needs a material profile");
            }
            return new ItemTemplate(name, profile, maxStackSize, rarity, category, magicInput, burnSeconds, glint);
        }
    }
}
