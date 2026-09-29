package justfatlard.lets_cook.mixin;

import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The smoker's fuel slot, which is the door a player actually pushes on; see {@link SmokerFuelMixin}.
 *
 * <p>It asks {@link justfatlard.lets_cook.SmokeWood} the same question the other two doors ask,
 * rather than holding a rule of its own. It held one for a while - anything that was not
 * {@code #minecraft:logs} was refused here - and went on holding it after the rule elsewhere grew
 * to take offcuts, so a smoker that would happily burn a stair could not be handed one. The
 * gametest never saw it, because it asks SmokeWood directly and no test opens the screen.
 *
 * <p>Answered for the smoker alone. A furnace and a blast furnace keep vanilla's rule: nothing
 * about either is a question of what the smoke tastes of.
 */
@Mixin(AbstractFurnaceMenu.class)
public abstract class SmokerMenuFuelMixin {
	@Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
	private void letsCook$slotTakesWhatTheFireboxWill(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof SmokerMenu)) return;
		cir.setReturnValue(justfatlard.lets_cook.SmokeWood.burns(stack));
	}
}
