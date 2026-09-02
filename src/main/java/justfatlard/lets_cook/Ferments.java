package justfatlard.lets_cook;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Everything a barrel turns into something else, given time and a dark room.
 *
 * <p>One rule for all of it - underground, dark, and long enough - so cheese, beer and wine are
 * the same mechanic with different tables rather than three implementations that drift.
 *
 * <p>Time is counted from a stamp on the stack rather than by ticking the barrel. Nothing has to
 * stay loaded: come back in a week and it finished a week ago. A barrel with nothing fermenting in
 * it costs nothing at all, which a ticker would not.
 */
public final class Ferments {
	private Ferments() {}

	/** The brightest a cellar can be and still work. */
	private static final int DARK_ENOUGH = 7;

	/**
	 * @param result what it becomes
	 * @param ticks  how long that takes
	 * @param stage  the word the name wears while it works
	 * @param base   the name to wear the stage on, chosen per ferment: a working wine already
	 *               reads as the wine it is becoming, while milk in a barrel is still milk
	 * @param done   what it sounds like when it finishes
	 */
	private record Ferment(Item result, int ticks, String stage, Component base, SoundEvent done,
			Finisher finisher) {}

	/**
	 * What a food does with the tier it earned.
	 *
	 * <p>Kept out here because "better" means something different to a bottle than to a bucket: a
	 * wine lengthens its effect, a cheese sharpens the wheel it will become. The barrel only knows
	 * how long the thing sat.
	 */
	@FunctionalInterface
	public interface Finisher {
		void apply(ItemStack made, int tier);
	}

	private static final Map<Item, Ferment> BY_INPUT = new LinkedHashMap<>();

	public static void add(Item input, Item result, int ticks, String stage, Component base,
			SoundEvent done, Finisher finisher) {
		BY_INPUT.put(input, new Ferment(result, ticks, stage, base, done, finisher));
	}

	public static int count() { return BY_INPUT.size(); }

	/**
	 * Look at what is in a barrel and move it along.
	 *
	 * <p>Called when a barrel is opened and when it is closed, which between them cover every
	 * moment a player could care: closing is when something has just gone in, opening is when they
	 * have come back to see.
	 */
	public static boolean tend(Level level, BlockPos pos, List<ItemStack> items) {
		boolean cellar = isCellar(level, pos);
		SoundEvent finished = null;

		for (int slot = 0; slot < items.size(); slot++) {
			ItemStack stack = items.get(slot);
			Ferment ferment = BY_INPUT.get(stack.getItem());
			if (ferment == null) continue;

			if (!cellar) {
				Labels.clear(stack);
				continue;
			}

			long started = Labels.startedAt(stack);
			if (started == Long.MIN_VALUE) {
				Labels.begin(stack, level.getGameTime(), ferment.base(), ferment.stage());
				continue;
			}

			long elapsed = level.getGameTime() - started;
			if (elapsed < ferment.ticks()) continue;

			// Whole stack at once: they all went in together and they all had the same week.
			ItemStack made = new ItemStack(ferment.result(), stack.getCount());

			// Nothing collects a finished barrel for you, so the wait past the minimum is real
			// and worth paying for.
			int tier = Vintage.of(elapsed, ferment.ticks());
			if (tier > 0 && ferment.finisher() != null) {
				ferment.finisher().apply(made, tier);
			}

			items.set(slot, made);
			finished = ferment.done();
		}

		if (finished != null) {
			level.playSound(null, pos, finished, SoundSource.BLOCKS, 0.6F, 0.9F);
		}
		return finished != null;
	}

	/**
	 * Dark, and with no sky above it.
	 *
	 * <p>Sky light rather than the combined brightness, because the combined figure falls at dusk:
	 * a barrel in an open field would qualify every night and stop again at dawn, which is not a
	 * cellar, and would make the rule look random to anyone who found it by accident.
	 */
	private static boolean isCellar(Level level, BlockPos pos) {
		return level.getBrightness(LightLayer.SKY, pos) == 0
			&& level.getBrightness(LightLayer.BLOCK, pos) <= DARK_ENOUGH;
	}

	/** The sound of something being poured out of a barrel that worked. */
	public static SoundEvent poured() { return SoundEvents.BUCKET_EMPTY; }

	/** The sound of a cork. */
	public static SoundEvent corked() { return SoundEvents.BOTTLE_FILL; }
}
