package justfatlard.lets_cook.gametest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import justfatlard.lets_cook.Ferments;
import justfatlard.lets_cook.Labels;
import justfatlard.lets_cook.RockCandy;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Rock candy comes out of its bottle, and the bottle stays in the barrel: onto the bottles already
 * there first, then into a free slot. A batch left three times its minimum comes out at the top
 * vintage and still shimmers.
 */
public final class RockCandyBarrel implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			String wrong = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				// Under the surface layer, with solid ground on every side: no sky, no light.
				BlockPos buried = new BlockPos(0, level.getMinY() + 1, 0);
				long now = level.getGameTime();

				ItemStack starters = new ItemStack(RockCandy.STARTER, 12);
				long started = now - 36000L * 3 - 1;
				Labels.begin(starters, started, Component.literal("candy"), "crystallising");
				Labels.note(starters, started, true);

				List<ItemStack> barrel = new ArrayList<>(Collections.nCopies(27, ItemStack.EMPTY));
				barrel.set(0, starters);
				barrel.set(5, new ItemStack(Items.GLASS_BOTTLE, 60));

				if (!Ferments.tend(level, buried, barrel)) return "nothing finished";
				ItemStack candy = barrel.get(0);
				if (!candy.is(RockCandy.CANDY) || candy.getCount() != 12) return "slot 0 holds " + candy;
				Component name = candy.get(DataComponents.CUSTOM_NAME);
				if (name == null || !name.getString().toLowerCase().contains("aged")) return "not aged: " + name;
				if (!candy.getOrDefault(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false)) return "no glint";
				if (barrel.get(5).getCount() != 64) return "existing bottles not topped up: " + barrel.get(5);
				ItemStack spill = barrel.get(1);
				if (!spill.is(Items.GLASS_BOTTLE) || spill.getCount() != 8) return "the rest of the bottles: " + spill;
				return null;
			});
			if (wrong != null) throw new AssertionError("rock candy: " + wrong);
		}
	}
}
