package justfatlard.lets_cook;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Tapping a cactus for what is inside it.
 *
 * <p>A cactus is a plant that stores water, standing in the one biome with none, and the game lets
 * you do nothing with that but take damage. Crouch against one with an empty bottle and you get a
 * drink.
 *
 * <p>The cooldown is the cactus's own {@code age}. Nothing is stored anywhere: age is what vanilla
 * already counts up towards growing a new segment, so tapping a cactus for its water sets back the
 * growth it was saving for - which is a price the player can see in their farm, rather than a
 * hidden timer they have to learn about. It persists, survives unloading and costs no memory,
 * because it is a property the block already had.
 */
public final class CactusTap {
	private CactusTap() {}

	/**
	 * How grown a cactus must be before it has anything to give.
	 *
	 * <p>Well short of the fifteen it grows at, so tapping is a regular thing rather than a race
	 * against the plant.
	 */
	private static final int RIPE = 4;

	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand,
			BlockHitResult hit) {
		if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

		ItemStack held = player.getItemInHand(hand);
		if (!held.is(Items.GLASS_BOTTLE) || !player.isCrouching()) return InteractionResult.PASS;

		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof CactusBlock)) return InteractionResult.PASS;

		if (state.getValue(CactusBlock.AGE) < RIPE) {
			// Saying so, because a cactus that is not ready looks exactly like one that is, and a
			// player who gets nothing twice concludes the feature does not exist.
			if (player instanceof ServerPlayer serving) {
				serving.sendSystemMessage(
					Component.translatable("message.lets-cook-justfatlard.cactus_dry"), true);
			}
			level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 0.5F, 1.4F);
			return InteractionResult.SUCCESS;
		}

		held.consume(1, player);
		ItemStack juice = new ItemStack(Snacks.CACTUS_JUICE);
		if (!player.getInventory().add(juice)) {
			player.drop(juice, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}

		// Back to nothing: it spent what it had saved.
		serverLevel.setBlock(pos, state.setValue(CactusBlock.AGE, 0), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS;
	}
}
