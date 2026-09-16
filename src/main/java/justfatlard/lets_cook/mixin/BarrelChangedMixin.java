package justfatlard.lets_cook.mixin;

import justfatlard.lets_cook.Ferments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Works a barrel whenever what is in it changes, not only when a player opens and closes it: a
 * hopper filling a cellar barrel starts it, and a hopper draining one finishes what is ready on
 * the way out.
 *
 * <p>On {@code BlockEntity} because the barrel inherits the method rather than declaring it.
 */
@Mixin(BlockEntity.class)
public abstract class BarrelChangedMixin {
	@Unique
	private static boolean letsCook$tending;

	@Inject(method = "setChanged()V", at = @At("TAIL"))
	private void letsCook$tendOnChange(CallbackInfo info) {
		if (!((Object) this instanceof BarrelBlockEntity barrel) || letsCook$tending) return;
		Level level = barrel.getLevel();
		if (level == null || level.isClientSide()) return;

		letsCook$tending = true;
		try {
			if (Ferments.tend(level, barrel.getBlockPos(), barrel)) barrel.setChanged();
		} finally {
			letsCook$tending = false;
		}
	}
}
