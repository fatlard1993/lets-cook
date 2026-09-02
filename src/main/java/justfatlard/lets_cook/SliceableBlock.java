package justfatlard.lets_cook;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * Food you put down and cut slices off, the way cake works.
 *
 * <p>A wheel of cheese, a pie, a cake: things too big to be one mouthful, that belong on a table
 * rather than in a backpack. That is the whole reason they are blocks - a wheel on the cellar
 * floor is a larder, and the same cheese as an item would just be another food with a better
 * number on it.
 *
 * <p>Vanilla's own {@code bites} property is reused rather than declared again, so the value range
 * cannot drift from the models, which are cake's models retextured.
 */
public class SliceableBlock extends Block {

	public static final IntegerProperty BITES = CakeBlock.BITES;
	private static final int MAX_BITES = CakeBlock.MAX_BITES;

	private final int nutrition;
	private final float saturation;

	/**
	 * Whether a full player can still cut a slice.
	 *
	 * <p>True for the smoked wheel, for the same reason smoked meat keeps: smoking is preserving,
	 * and preserved food is what you top up on before you need it.
	 */
	private final boolean keeps;

	public SliceableBlock(Properties properties, int nutrition, float saturation, boolean keeps) {
		super(properties);
		this.nutrition = nutrition;
		this.saturation = saturation;
		this.keeps = keeps;
		registerDefaultState(getStateDefinition().any().setValue(BITES, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BITES);
	}

	/**
	 * What one slice is worth.
	 *
	 * <p>Asked of the state rather than read from the field, so a block whose quality varies -
	 * a cheese wheel that spent longer in the barrel - can answer differently without
	 * reimplementing eating.
	 */
	protected int nutritionOf(BlockState state) {
		return nutrition;
	}

	protected float saturationOf(BlockState state) {
		return saturation;
	}

	/** Shrinks from the west as it is eaten, one slice - two pixels - at a time. */
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return Block.box(1 + state.getValue(BITES) * 2, 0, 1, 15, 8, 15);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!player.canEat(keeps)) {
			return InteractionResult.PASS;
		}

		player.getFoodData().eat(nutritionOf(state), saturationOf(state));
		level.gameEvent(player, GameEvent.EAT, pos);

		int bites = state.getValue(BITES);
		if (bites < MAX_BITES) {
			level.setBlock(pos, state.setValue(BITES, bites + 1), Block.UPDATE_ALL);
		} else {
			level.removeBlock(pos, false);
		}
		return InteractionResult.SUCCESS;
	}
}
