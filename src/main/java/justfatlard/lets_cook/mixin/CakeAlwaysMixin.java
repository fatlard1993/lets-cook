package justfatlard.lets_cook.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CakeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla cake, like this mod's, is cut whether or not you are hungry. */
@Mixin(CakeBlock.class)
public abstract class CakeAlwaysMixin {
	@Redirect(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canEat(Z)Z"))
	private static boolean letsCook$alwaysASlice(Player player, boolean ignoreHunger) {
		return player.canEat(true);
	}
}
