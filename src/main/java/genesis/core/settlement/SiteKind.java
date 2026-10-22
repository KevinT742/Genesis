package genesis.core.settlement;

/**
 * The three structural families Genesis generates.
 *
 * <p>They are deliberately distinct in how they are found and how they behave:
 * <ul>
 *   <li>{@link #SETTLEMENT} is visible, inhabited and connected by roads.</li>
 *   <li>{@link #RUIN} is visible, abandoned and overgrown.</li>
 *   <li>{@link #HIDDEN} is concealed, rare and rewards exploration.</li>
 * </ul>
 */
public enum SiteKind {
    /** An inhabited place. Settlements are linked together by the road network. */
    SETTLEMENT,
    /** The remains of a lost settlement. */
    RUIN,
    /** A concealed site that is hard to find on purpose. */
    HIDDEN
}
