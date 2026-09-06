package justfatlard.lets_cook.mixin;

import justfatlard.lets_cook.Crumbs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla cake eats the way this mod's cake, pies and cheese do: a sound and crumbs.
 *
 * <p>{@code eat} runs on both sides, the client's call being a prediction, and reports through
 * its result whether a slice actually came off; only a server-side success gets the bite.
 */
@Mixin(CakeBlock.class)
public abstract class CakeCrumbsMixin {
	@Inject(method = "eat", at = @At("RETURN"))
	private static void letsCook$crumbs(LevelAccessor level, BlockPos pos, BlockState state, Player player,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (!cir.getReturnValue().consumesAction()) return;
		if (level instanceof ServerLevel server) {
			Crumbs.bite(server, pos, state.getValue(CakeBlock.BITES) + 1, Items.CAKE);
		}
	}
}
