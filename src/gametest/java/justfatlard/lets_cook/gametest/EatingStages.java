package justfatlard.lets_cook.gametest;

import java.util.ArrayList;
import java.util.List;
import justfatlard.lets_cook.Main;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Everything you can eat or drink is drawn being eaten, and nothing else is.
 *
 * <p>The stages are a condition in the item's definition, so an edible thing whose definition lost
 * it still looks fine in the hand and only stops animating - nothing would say so. The frames are
 * for looking at: build/run/clientGameTest/screenshots.
 */
public final class EatingStages implements FabricClientGameTest {

	private static final String[] PICTURED = {"meat_sandwich", "beer", "berry_ice_cream", "smoked_beef"};

	@Override
	public void runTest(ClientGameTestContext context) {
		List<String> wrong = context.computeOnClient(client -> {
			List<String> found = new ArrayList<>();
			BuiltInRegistries.ITEM.forEach(item -> {
				var id = BuiltInRegistries.ITEM.getKey(item);
				if (!id.getNamespace().equals(Main.MOD_ID)) return;
				ItemStack stack = item.getDefaultInstance();
				boolean edible = stack.has(DataComponents.CONSUMABLE);
				boolean animated = client.getModelManager().getItemModel(stack.get(DataComponents.ITEM_MODEL))
					instanceof ConditionalItemModel;
				if (edible != animated) found.add(id.getPath() + (edible ? " is not animated" : " is animated"));
			});
			return found;
		});
		if (!wrong.isEmpty()) throw new AssertionError("eating stages: " + wrong);

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.runCommand("gamemode survival @a");
			// Showcase hides the GUI, which hides the hand, and the client keeps it hidden for us.
			context.getInput().pressKey(options -> options.keyToggleGui);

			for (String food : PICTURED) {
				server.runOnServer(s -> {
					var player = connection.getServerPlayer();
					player.getInventory().clearContent();
					player.getFoodData().setFoodLevel(10);
					player.getInventory().setSelectedSlot(0);
				});
				server.runCommand("give @a " + Main.MOD_ID + ":" + food);
				context.waitTicks(5);

				// Each frame lands a few ticks after a stage takes over, and before the next.
				context.getInput().holdKey(options -> options.keyUse);
				int held = 0;
				for (int at : new int[] {4, 14, 23, 29}) {
					context.waitTicks(at - held);
					held = at;
					context.takeScreenshot(TestScreenshotOptions.of("eating_" + food + "_" + at)
						.disableCounterPrefix());
				}
				context.getInput().releaseKey(options -> options.keyUse);
				context.waitTicks(10);
			}
		}
	}
}
