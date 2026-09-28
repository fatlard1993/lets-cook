package justfatlard.lets_cook.mixin;

import net.minecraft.server.level.ServerLevel;
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
 * <p>Wood, and only wood it is worth eating the smoke of. Which woods, in which forms, and for
 * how long is {@link justfatlard.lets_cook.SmokeWood}'s business.
 *
 * <p>How long each one burns is {@link justfatlard.lets_cook.SmokeWood}'s business: vanilla gives
 * every log the same 300 ticks, which is three items out of a whole tree trunk and says nothing
 * about which tree it came from.
 *
 * <p>Three doors, because the game asks three different questions. The burn duration is what the
 * firebox reads when it lights; the slot in the screen asks the menu whether a thing is fuel
 * before it will hold it; a hopper asks the block entity. With only the first shut, coal sat in
 * the slot unburnt rather than being refused, which is not the same thing at all.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class SmokerFuelMixin {

	@Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
	private void letsCook$hopperFeedsWoodOnly(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!((Object) this instanceof SmokerBlockEntity)) return;
		if (slot == 1 && !justfatlard.lets_cook.SmokeWood.burns(stack)) cir.setReturnValue(false);
	}

	@Inject(method = "getBurnDuration", at = @At("HEAD"), cancellable = true)
	private void letsCook$smokerBurnsWoodOnly(ServerLevel level, ItemStack fuel,
			CallbackInfoReturnable<Integer> cir) {
		if (!((Object) this instanceof SmokerBlockEntity)) return;
		cir.setReturnValue(justfatlard.lets_cook.SmokeWood.burnTicks(fuel));
	}
}
