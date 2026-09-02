package justfatlard.lets_cook;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * How a half-finished thing says so.
 *
 * <p>Everything this mod makes slowly has the same problem: a barrel is opaque, and a jar of
 * something that is working looks exactly like a jar of something that is not. The answer is
 * always the same too - put it in the name - so it is written once here instead of once per food.
 *
 * <p>Three states, and the name carries all of them:
 *
 * <ul>
 *   <li><b>{@code (Starter)}</b> - made, but sitting in your pack doing nothing.
 *   <li><b>{@code (Brewing)}</b> or <b>{@code (Ageing)}</b> - in a barrel that qualifies, clock
 *       running. Applied by the barrel, removed again the moment the barrel stops qualifying.
 *   <li><b>no suffix at all</b> - done. The label is not cleared so much as left behind: what
 *       comes out of the barrel is a different item, and it never had one.
 * </ul>
 *
 * <p>The suffix is composed rather than written into the language file, so a new ferment needs one
 * line naming the finished drink and nothing else. Writing "Apple Wine (Starter)" out by hand six
 * times is how the brackets end up in the wrong place on the fourth one.
 */
public final class Labels {
	private Labels() {}

	/** Where the clock lives on a stack that is working. */
	private static final String STARTED = "started";

	/**
	 * {@code <name> (<word>)}, both halves translated.
	 *
	 * <p>The one format string every suffix in this mod goes through, whether the word is a stage
	 * something is passing through or a vintage it has earned. Two format strings would drift the
	 * moment one of them got brackets and the other got a dash.
	 */
	public static Component suffixed(Component name, String wordKey) {
		return Component.translatable("label.lets-cook-justfatlard.suffixed", name,
			Component.translatable(wordKey));
	}

	/** {@code <name> (<stage>)} - a thing partway through becoming something. */
	public static Component unfinished(Component name, String stage) {
		return suffixed(name, "stage.lets-cook-justfatlard." + stage);
	}

	/** The name a thing carries from the moment it is made until a barrel takes it. */
	public static Component starter(String finishedKey) {
		return unfinished(Component.translatable(finishedKey), "starter");
	}

	/** Start the clock, and say so in the name. */
	public static void begin(ItemStack stack, long now, Component base, String stage) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(STARTED, now));
		stack.set(DataComponents.CUSTOM_NAME, unfinished(base, stage));
	}

	/**
	 * Stop the clock and put the name back to whatever it says at rest.
	 *
	 * <p>Read off the item's own default rather than passed in, because the two cases disagree
	 * about what rest means and neither caller should have to know: a starter goes back to saying
	 * {@code (Starter)}, and a bucket of milk goes back to being a bucket of milk. Clearing the
	 * component outright would strip a starter's name along with the brewing one, since a removal
	 * on a stack hides the item's default as well as the patch over it.
	 */
	public static void clear(ItemStack stack) {
		if (startedAt(stack) == Long.MIN_VALUE) return;
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(STARTED));

		Component resting = stack.getItem().components().get(DataComponents.CUSTOM_NAME);
		if (resting == null) {
			stack.remove(DataComponents.CUSTOM_NAME);
		} else {
			stack.set(DataComponents.CUSTOM_NAME, resting);
		}
	}

	/** The tick this stack started working, or {@code Long.MIN_VALUE} if it is not. */
	public static long startedAt(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) return Long.MIN_VALUE;
		CompoundTag tag = data.copyTag();
		return tag.contains(STARTED) ? tag.getLongOr(STARTED, Long.MIN_VALUE) : Long.MIN_VALUE;
	}
}
