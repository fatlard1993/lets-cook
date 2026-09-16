package justfatlard.lets_cook.integration;

import justfatlard.block_tip.api.BlockTipApi;
import justfatlard.lets_cook.Ferments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

/**
 * What a barrel is making, on the block tip, so a closed barrel in a cellar can be read without
 * opening it: milk to cheese, and how far along, or why it is not working.
 *
 * <p>Only loaded when block-tip is here: this class imports its API, and a class that mentions a
 * missing one cannot be loaded.
 */
public final class BarrelTips {
	private BarrelTips() {}

	public static void register() {
		BlockTipApi.illustrate((level, pos, state, player) -> {
			if (!state.is(Blocks.BARREL) || !(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) return null;
			// Whatever is waiting to start starts now, so the percentage shown is one that moves.
			// A barrel changed while its room was lit, and lit no longer, is the case nothing
			// else catches.
			Ferments.Status status = Ferments.status(level, pos, barrel);
			if (status != null && status.state() == Ferments.State.WORKING && status.fraction() == 0F) {
				Ferments.tend(level, pos, barrel);
				// A stamp is a change to a stack, which nothing else tells the chunk to save.
				barrel.setChanged();
			}
			if (status == null) return null;

			String line = switch (status.state()) {
				case WORKING -> word(status.stage()) + " " + (int) (status.fraction() * 100) + "%";
				case READY -> "Ready: open it";
				case TOO_BRIGHT -> "Too bright to age";
			};
			return new BlockTipApi.Tip(line, id(status.input()), id(status.result()));
		});
	}

	/** The stage in English words; the key's last part where the server has no line for it. */
	private static String word(String key) {
		String said = Component.translatable(key).getString();
		if (!said.equals(key)) return said;
		String last = key.substring(key.lastIndexOf('.') + 1);
		return Character.toUpperCase(last.charAt(0)) + last.substring(1);
	}

	private static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}
}
