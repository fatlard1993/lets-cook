package justfatlard.lets_cook;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * What you get for forgetting about a barrel.
 *
 * <p>A ferment is finished the moment its minimum has passed, and nothing makes you collect it.
 * Leaving it longer is the only decision in the whole mechanic that costs nothing but patience, so
 * it is the one worth paying for: come back much later and what comes out is better.
 *
 * <p>Three tiers, and the gaps between them are large on purpose. Doubling the wait for a small
 * gain would turn a cellar into a chore to optimise; eight times the wait for something you will
 * genuinely notice makes it a thing you set up and walk away from, which is what a cellar is for.
 */
public final class Vintage {
	private Vintage() {}

	/** How many minimums must pass for each tier. */
	private static final int[] MULTIPLES = {1, 3};

	/** The word each tier wears. Tier zero wears nothing - it is just the thing. */
	private static final String[] WORDS = {null, "aged"};

	public static final int BEST = MULTIPLES.length - 1;

	/** Which tier a ferment reached, given how long it actually sat and what its minimum was. */
	public static int of(long elapsed, int minimum) {
		int tier = 0;
		for (int step = 1; step <= BEST; step++) {
			if (elapsed >= (long) minimum * MULTIPLES[step]) tier = step;
		}
		return tier;
	}

	/**
	 * How much longer a tier makes an effect last.
	 *
	 * <p>Duration and not level. An aged wine granting Strength II would be a combat item you
	 * happen to brew; twice as long is the same drink, and what you waited for is having to drink
	 * fewer of them.
	 */
	private static final int[] STRENGTH = {1, 2};

	public static int strength(int tier) {
		return tier <= 0 || tier >= STRENGTH.length ? 1 : STRENGTH[tier];
	}

	/** Where a tier rides on something carried rather than placed. */
	private static final String TIER = "vintage";

	public static void stamp(ItemStack stack, int tier) {
		if (tier <= 0) return;
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TIER, tier));
	}

	/** The tier stamped on a stack, or zero for the ordinary sort. */
	public static int on(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) return 0;
		CompoundTag tag = data.copyTag();
		return tag.contains(TIER) ? tag.getIntOr(TIER, 0) : 0;
	}

	/** The translation key for a tier's word, or null for the plain one. */
	public static String wordKey(int tier) {
		return tier <= 0 || tier >= WORDS.length ? null
			: "vintage." + Main.MOD_ID + "." + WORDS[tier];
	}
}
