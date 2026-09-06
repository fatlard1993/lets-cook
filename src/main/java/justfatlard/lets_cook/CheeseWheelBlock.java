package justfatlard.lets_cook;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * A cheese wheel, which remembers how long it spent in the barrel.
 *
 * <p>Its own class rather than a flag on {@link SliceableBlock}, because a block either carries a
 * property or it does not - there is no per-state answer to that question. A pie has no vintage
 * and should not pay for the states to say so, and adding the property conditionally would mean
 * reading a field inside {@code createBlockStateDefinition}, which runs before any field is set.
 * A subclass simply always has it.
 */
public class CheeseWheelBlock extends SliceableBlock {

	public static final IntegerProperty VINTAGE = IntegerProperty.create("vintage", 0, Vintage.BEST);

	/** What each tier adds to a slice. */
	private static final int SHARPER = 1;
	private static final float RICHER = 0.15F;

	public CheeseWheelBlock(Properties properties, int nutrition, float saturation) {
		super(properties, nutrition, saturation);
		registerDefaultState(defaultBlockState().setValue(VINTAGE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(VINTAGE);
	}

	@Override
	protected int nutritionOf(BlockState state) {
		return super.nutritionOf(state) + state.getValue(VINTAGE) * SHARPER;
	}

	@Override
	protected float saturationOf(BlockState state) {
		return super.saturationOf(state) + state.getValue(VINTAGE) * RICHER;
	}
}
