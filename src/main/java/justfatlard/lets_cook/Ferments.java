package justfatlard.lets_cook;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
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
	 * Stacks of one starter begun this close together are one batch. A hopper hands a barrel one
	 * item at a time, each is stamped as it lands, and a stamp is part of the stack: without this,
	 * sixteen starters fed in by hopper filled sixteen slots.
	 */
	private static final long ONE_BATCH = 600;

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
	 * <p>Called when a barrel is opened, closed or changed, and when somebody looks at it: between
	 * them, every moment something goes in or anyone could care how it is getting on.
	 */
	public static boolean tend(Level level, BlockPos pos, List<ItemStack> items) {
		boolean cellar = isCellar(level, pos);
		SoundEvent finished = null;

		for (int slot = 0; slot < items.size(); slot++) {
			ItemStack stack = items.get(slot);
			Ferment ferment = BY_INPUT.get(stack.getItem());
			if (ferment == null) continue;

			long now = level.getGameTime();
			long started = Labels.startedAt(stack);
			if (started == Long.MIN_VALUE) {
				// A batch still needs a cellar to begin in: that is the rule, and it is the one
				// part of it a player can see the moment they get it wrong, because nothing
				// starts.
				if (cellar) Labels.begin(stack, now, ferment.base(), ferment.stage());
				Labels.note(stack, now, cellar);
				continue;
			}

			long counted = counting(stack, started, now);
			if (counted != started) {
				started = counted;
				Labels.restamp(stack, started);
			}
			Labels.note(stack, now, cellar);

			long elapsed = now - started;
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

		if (cellar) gatherBatches(items);

		if (finished != null) {
			level.playSound(null, pos, finished, SoundSource.BLOCKS, 0.6F, 0.9F);
		}
		return finished != null;
	}

	/**
	 * Puts stacks of the same starter begun within {@link #ONE_BATCH} of each other back together,
	 * on the later stamp: the earlier ones wait a few seconds more, and nothing gets a head start.
	 */
	private static void gatherBatches(List<ItemStack> items) {
		for (int into = 0; into < items.size(); into++) {
			ItemStack batch = items.get(into);
			if (batch.isEmpty() || !BY_INPUT.containsKey(batch.getItem())) continue;
			long batchStarted = Labels.startedAt(batch);
			if (batchStarted == Long.MIN_VALUE) continue;

			for (int from = into + 1; from < items.size() && batch.getCount() < batch.getMaxStackSize(); from++) {
				ItemStack other = items.get(from);
				if (other.isEmpty() || other.getItem() != batch.getItem()) continue;
				long otherStarted = Labels.startedAt(other);
				if (otherStarted == Long.MIN_VALUE || Math.abs(otherStarted - batchStarted) > ONE_BATCH) continue;
				if (batch.getCount() + other.getCount() > batch.getMaxStackSize()) continue;

				if (otherStarted > batchStarted) {
					Labels.restamp(batch, otherStarted);
					batchStarted = otherStarted;
				}
				batch.grow(other.getCount());
				items.set(from, ItemStack.EMPTY);
			}
		}
	}

	/** {@link #tend(Level, BlockPos, List)} for a container, through its own slots. */
	public static boolean tend(Level level, BlockPos pos, Container barrel) {
		List<ItemStack> items = new java.util.ArrayList<>(barrel.getContainerSize());
		for (int slot = 0; slot < barrel.getContainerSize(); slot++) items.add(barrel.getItem(slot));
		boolean finished = tend(level, pos, items);
		for (int slot = 0; slot < items.size(); slot++) {
			if (items.get(slot) != barrel.getItem(slot)) barrel.setItem(slot, items.get(slot));
		}
		return finished;
	}

	/**
	 * The clock this stack is really on: its stamp, moved on past any stretch that began in the
	 * light.
	 *
	 * <p>Light does not undo the work; it only fails to count. The stretch since the last look is
	 * credited to the room that look found, so opening the cellar door costs nothing - the dark
	 * stretch behind it is already banked - while a room left lit hands its time back at the next
	 * look and the batch stands still instead of dying. It used to throw the stamp away outright,
	 * which meant checking on a cheese destroyed it.
	 *
	 * <p>Both the tending and the tip read it here. They did not, once, and the tip said "Ready:
	 * open it" over a barrel that then refused to be done: the reader was counting a lit stretch
	 * the writer was about to hand back. Two answers to one question is one answer too many.
	 */
	private static long counting(ItemStack stack, long started, long now) {
		long seen = Labels.seenAt(stack);
		if (seen == Long.MIN_VALUE || Labels.seenDark(stack)) return started;
		return started + (now - seen);
	}

	/** How a barrel's first ferment is getting on, for somebody looking at the barrel. */
	public enum State { WORKING, READY, TOO_BRIGHT }

	/**
	 * @param input    what went in
	 * @param result   what it is becoming
	 * @param fraction how far along, nought to one; only meaningful while WORKING
	 * @param stage    the translation key of the word it wears while it works
	 */
	public record Status(State state, Item input, Item result, float fraction, String stage) {}

	/**
	 * The first thing working in this barrel and how far along it is, or null when nothing in it
	 * ferments. Read off the stamps and the room, moving nothing; a stack in a dark barrel that has
	 * not been stamped yet reads as just begun, which is what the next {@link #tend} makes it.
	 */
	public static Status status(Level level, BlockPos pos, Container barrel) {
		boolean cellar = isCellar(level, pos);
		for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
			ItemStack stack = barrel.getItem(slot);
			Ferment ferment = BY_INPUT.get(stack.getItem());
			if (ferment == null) continue;

			String stage = "stage.lets-cook-justfatlard." + ferment.stage();
			long started = Labels.startedAt(stack);

			// Nothing started, in a room that cannot start it: the one case where too bright is
			// the whole story.
			if (started == Long.MIN_VALUE) {
				return new Status(cellar ? State.WORKING : State.TOO_BRIGHT, stack.getItem(),
					ferment.result(), 0F, stage);
			}

			// A lit room stops the clock rather than emptying it, so a batch caught in one has a
			// figure worth showing: what it had banked before the light arrived.
			long now = level.getGameTime();
			float fraction = Math.min((now - counting(stack, started, now)) / (float) ferment.ticks(), 1F);
			if (!cellar) return new Status(State.TOO_BRIGHT, stack.getItem(), ferment.result(), fraction, stage);

			return new Status(fraction >= 1F ? State.READY : State.WORKING, stack.getItem(), ferment.result(),
				fraction, stage);
		}
		return null;
	}

	/**
	 * Dark, and with no sky above it, in the room the barrel stands in.
	 *
	 * <p>Read beside the barrel rather than at it: a barrel is a solid block, and the light kept
	 * inside a solid block is always nought, which made every barrel in the world a cellar. The
	 * brightest open side is the room's.
	 *
	 * <p>Sky light rather than the combined brightness, because the combined figure falls at dusk:
	 * a barrel in an open field would qualify every night and stop again at dawn, which is not a
	 * cellar, and would make the rule look random to anyone who found it by accident.
	 */
	private static boolean isCellar(Level level, BlockPos pos) {
		int sky = 0;
		int block = 0;
		for (Direction side : Direction.values()) {
			BlockPos beside = pos.relative(side);
			sky = Math.max(sky, level.getBrightness(LightLayer.SKY, beside));
			block = Math.max(block, level.getBrightness(LightLayer.BLOCK, beside));
		}
		return sky == 0 && block <= DARK_ENOUGH;
	}

	/** The sound of something being poured out of a barrel that worked. */
	public static SoundEvent poured() { return SoundEvents.BUCKET_EMPTY; }

	/** The sound of a cork. */
	public static SoundEvent corked() { return SoundEvents.BOTTLE_FILL; }
}
