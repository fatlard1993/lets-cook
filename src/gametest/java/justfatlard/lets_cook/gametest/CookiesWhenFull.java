package justfatlard.lets_cook.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.Items;

/**
 * A cookie is eaten on a full stomach, held to use as anyone would eat one. Bread, held the same
 * way at the same hunger, is not: that is what shows the player was full.
 */
public final class CookiesWhenFull implements FabricClientGameTest {

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();
			server.runCommand("gamemode survival @a");

			check(eaten(context, server, connection, "bread") == 0, "bread was eaten on a full stomach: the player was not full");
			check(eaten(context, server, connection, "cookie") == 1, "a cookie was not eaten on a full stomach");
		}
	}

	/** How many of this food a full player eats holding use for two seconds. */
	private static int eaten(ClientGameTestContext context, TestServerContext server, TestServerConnection connection, String food) {
		server.runOnServer(s -> {
			var player = connection.getServerPlayer();
			player.getInventory().clearContent();
			player.getFoodData().setFoodLevel(20);
			player.getFoodData().setSaturation(5);
			player.getInventory().setSelectedSlot(0);
		});
		server.runCommand("give @a minecraft:" + food + " 2");
		context.waitTicks(5);
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(40);
		context.getInput().releaseKey(options -> options.keyUse);
		context.waitTicks(5);
		var item = food.equals("cookie") ? Items.COOKIE : Items.BREAD;
		return 2 - server.computeOnServer(s -> connection.getServerPlayer().getInventory().countItem(item));
	}

	private static void check(boolean ok, String complaint) {
		if (!ok) throw new AssertionError(complaint);
	}
}
