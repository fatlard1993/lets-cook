package justfatlard.lets_cook.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A smoker burns wood, and only wood.
 *
 * <p>Smoke is the whole mechanism - it is what the food is being flavoured by - so what goes in the
 * firebox matters in a way it does not for a furnace. Coal in a smoker was always slightly absurd;
 * refusing it makes the smoker a wood-fired thing and gives every forest a use it did not have.
 *
 * <p>Logs, not planks: a plank fire is a furnace fire. Anything in the log tag counts, so another
 * mod's wood works without being listed here.
 *
 * <p>Answered by burn duration rather than by a fuel test, because that is where the game decides
 * it: a duration of zero is exactly what "this is not fuel" means, and it is the one place both
 * the burning and the slot's willingness to hold something read from.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class SmokerFuelMixin {

	@Inject(method = "getBurnDuration", at = @At("HEAD"), cancellable = true)
	private void letsCook$smokerBurnsWoodOnly(ServerLevel level, ItemStack fuel,
			CallbackInfoReturnable<Integer> cir) {
		if (!((Object) this instanceof SmokerBlockEntity)) return;
		if (fuel.is(ItemTags.LOGS)) return;

		cir.setReturnValue(0);
	}
}
