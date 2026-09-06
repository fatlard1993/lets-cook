package justfatlard.lets_cook;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * A bite taken out of a block, heard and seen.
 *
 * <p>Vanilla cake is eaten in silence and leaves nothing behind, which is the one way it is
 * unlike every other food in the game: an item going down makes the eating sound and throws
 * crumbs of its own texture. A slice cut off a block gets the same two things here, and the
 * crumbs come off the face that was just cut rather than the player's mouth, because the
 * block is the thing that lost a piece.
 */
public final class Crumbs {
	private Crumbs() {}

	/** Crumbs per bite. Vanilla throws five a tick for the length of a chew; one bite is one burst. */
	private static final int COUNT = 8;

	/**
	 * @param bitesAfter the block's bite count once this bite is taken; the cut face is the
	 *                   west edge, which the shapes move east two pixels per bite
	 * @param crumb      the item whose texture the crumbs are cut from
	 */
	public static void bite(ServerLevel level, BlockPos pos, int bitesAfter, Item crumb) {
		RandomSource random = level.getRandom();
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;
		// The same pitch spread LivingEntity gives an eaten item, so a slice sounds like one
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS,
			1.0F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.4F);

		if (crumb == Items.AIR) return;
		double edge = pos.getX() + (1 + bitesAfter * 2) / 16.0;
		level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, crumb),
			edge, pos.getY() + 0.3, z, COUNT, 0.05, 0.1, 0.2, 0.05);
	}

	/** The block's own item, which is what the crumbs should look like. */
	public static Item crumbOf(Block block) {
		return block.asItem();
	}
}
