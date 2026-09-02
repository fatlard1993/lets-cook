package justfatlard.lets_cook;

import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;

/**
 * Something carried in a bucket that becomes a block when you tip it out.
 *
 * <p>A wheel of cheese and a pie in its tin both work this way: the bucket is how you carry the
 * thing, so putting the thing down hands the bucket back. That is not a reward for cooking, it is
 * just what happens when you empty a bucket.
 */
public class BucketedFoodItem extends BlockItem {

	public BucketedFoodItem(Block block, Properties properties) {
		super(block, properties);
	}

	/**
	 * Carries the bucket's vintage onto the block it becomes.
	 *
	 * <p>A tier written on a bucket is worth nothing until it is a wheel you can cut, so this is
	 * where it is spent. Blocks that have no vintage simply never match the property.
	 */
	@Override
	protected BlockState getPlacementState(BlockPlaceContext context) {
		BlockState state = super.getPlacementState(context);
		if (state == null || !state.hasProperty(CheeseWheelBlock.VINTAGE)) return state;
		return state.setValue(CheeseWheelBlock.VINTAGE,
			Math.min(Vintage.BEST, Vintage.on(context.getItemInHand())));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		InteractionResult result = super.useOn(context);

		Player player = context.getPlayer();
		if (!result.consumesAction() || player == null || player.hasInfiniteMaterials()) {
			return result;
		}

		ItemStack bucket = new ItemStack(Items.BUCKET);
		if (!player.getInventory().add(bucket)) {
			player.drop(bucket, false, Prediction.SERVER_ONLY);
		}
		return result;
	}
}
