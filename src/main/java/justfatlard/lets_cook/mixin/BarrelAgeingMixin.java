package justfatlard.lets_cook.mixin;

import justfatlard.lets_cook.Ferments;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Works whatever is fermenting in a barrel when a player opens or closes it: closing is when
 * something has just gone in and the player wants to know it took, opening is when they have come
 * back to look. {@code BarrelChangedMixin} covers everything else that fills one. Ticking the
 * barrel would buy nothing those do not already, and would charge every barrel in the world for it.
 */
@Mixin(BarrelBlockEntity.class)
public abstract class BarrelAgeingMixin {

	@Shadow
	protected abstract NonNullList<ItemStack> getItems();

	@Inject(method = "startOpen", at = @At("TAIL"))
	private void letsCook$ripenOnOpen(ContainerUser user, CallbackInfo info) {
		letsCook$tend();
	}

	@Inject(method = "stopOpen", at = @At("TAIL"))
	private void letsCook$ageOnClose(ContainerUser user, CallbackInfo info) {
		// The one moment a barrel and a person are in the same place: whoever shut the lid is who
		// gets told about what was inside.
		letsCook$tend(user instanceof net.minecraft.server.level.ServerPlayer player ? player : null);
	}

	private void letsCook$tend() {
		letsCook$tend(null);
	}

	private void letsCook$tend(net.minecraft.server.level.ServerPlayer opener) {
		BlockEntity self = (BlockEntity) (Object) this;
		Level level = self.getLevel();
		if (level == null || level.isClientSide()) return;

		if (Ferments.tend(level, self.getBlockPos(), getItems(), opener)) {
			self.setChanged();
		}
	}
}
