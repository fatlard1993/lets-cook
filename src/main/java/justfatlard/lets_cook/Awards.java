package justfatlard.lets_cook;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * The advancements this mod hands out.
 *
 * <p>They are declared with the impossible trigger, which is the game's way of saying "only code
 * grants this". A barrel finishing is not a thing any vanilla criterion can see: it happens on a
 * block, from a stamp written a week ago, to whoever happens to open the lid.
 */
public final class Awards {
	private Awards() {}

	/** Something came out of a barrel at the top vintage. */
	public static void aged(@Nullable ServerPlayer opener) {
		award(opener, "aged");
	}

	private static void award(@Nullable ServerPlayer player, String path) {
		if (player == null || player.level().getServer() == null) return;
		AdvancementHolder holder = player.level().getServer().getAdvancements()
			.get(Identifier.fromNamespaceAndPath(Main.MOD_ID, path));
		if (holder == null) return;

		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
		if (progress.isDone()) return;
		for (String criterion : progress.getRemainingCriteria()) {
			player.getAdvancements().award(holder, criterion);
		}
	}
}
