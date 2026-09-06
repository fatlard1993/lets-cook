package justfatlard.lets_cook.mixin;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The smoker's fuel slot will not take anything but a log; see {@link SmokerFuelMixin}. */
@Mixin(AbstractFurnaceMenu.class)
public abstract class SmokerMenuFuelMixin {
	@Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
	private void letsCook$slotTakesWoodOnly(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof SmokerMenu)) return;
		if (!stack.is(ItemTags.LOGS)) cir.setReturnValue(false);
	}
}
