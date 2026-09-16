package justfatlard.lets_cook.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;

/**
 * The pictures for the readme and the mod page: a cellar of barrels working, and the drinks that
 * come out of them lined up where they can be read.
 *
 * <p>Run it under xvfb-run; the frames land in build/run/clientGameTest/screenshots.
 */
public final class Showcase implements FabricClientGameTest {

	private static final int WIDTH = 1920;
	private static final int HEIGHT = 1080;

	/** Beer, the wines in the order the readme lists them, and mead. */
	private static final String[] DRINKS = {
		"beer", "apple_wine", "berry_wine", "glow_berry_wine", "melon_wine", "chorus_wine", "mead",
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			TestServerConnection connection = world.getConnection();
			connection.waitForChunksRender();

			context.getInput().pressKey(options -> options.keyToggleGui);
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("gamerule doWeatherCycle false");
			server.runCommand("time set noon");
			server.runCommand("gamemode spectator @a");

			BlockPos origin = server.computeOnServer(s -> connection.getServerPlayer().blockPosition());
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();

			// A cellar: a stone room cut into the ground, a lantern for the little light a cellar
			// is allowed, and barrels along the back wall with cheese and a cake to keep them company.
			server.runCommand("fill %d %d %d %d %d %d minecraft:deepslate_bricks"
				.formatted(x - 6, y - 1, z - 9, x + 6, y + 4, z + 1));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air"
				.formatted(x - 5, y, z - 8, x + 5, y + 3, z));
			server.runCommand("setblock %d %d %d minecraft:lantern[hanging=true]".formatted(x - 3, y + 3, z - 4));
			for (int i = -2; i <= 2; i++) {
				server.runCommand("setblock %d %d %d minecraft:barrel[facing=south]".formatted(x + i * 2, y, z - 7));
				server.runCommand("setblock %d %d %d minecraft:barrel[facing=south]".formatted(x + i * 2, y + 1, z - 7));
			}
			server.runCommand("setblock %d %d %d lets-cook-justfatlard:cheese_block".formatted(x + 4, y, z - 3));
			server.runCommand("setblock %d %d %d lets-cook-justfatlard:chocolate_cake".formatted(x - 4, y, z - 3));

			look(server, x + 0.5, y, z - 1.0, x + 0.5, y + 1.2, z - 7.0);
			context.waitTicks(40);
			shoot(context, "cellar");

			// And the drinks themselves, framed on the wall in the order they are made.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air"
				.formatted(x - 5, y, z - 8, x + 5, y + 3, z));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone_bricks"
				.formatted(x - 6, y, z - 9, x + 6, y + 4, z - 9));
			for (int i = 0; i < DRINKS.length; i++) {
				int at = x - DRINKS.length / 2 + i;
				server.runCommand(("summon minecraft:item_frame %d %d %d "
					+ "{Facing:3b,Invisible:1b,Fixed:1b,Item:{id:\"lets-cook-justfatlard:%s\",count:1}}")
					.formatted(at, y + 2, z - 8, DRINKS[i]));
			}
			// A dark cellar is the point of the first picture and the enemy of this one: light it.
			for (int i = -1; i <= 1; i++) {
				server.runCommand("setblock %d %d %d minecraft:lantern[hanging=true]"
					.formatted(x + i * 3, y + 3, z - 6));
			}

			look(server, x + 0.5, y, z - 4.0, x + 0.5, y + 2.5, z - 8.0);
			context.waitTicks(40);
			shoot(context, "drinks");
		}
	}

	/**
	 * Stand the camera at one place and point it at another. The camera's y is the feet, so it
	 * looks from 1.62 above where it stands.
	 */
	private void look(TestServerContext server, double x, double y, double z,
			double atX, double atY, double atZ) {
		double dx = atX - x;
		double dy = atY - (y + 1.62);
		double dz = atZ - z;
		double yaw = -Math.toDegrees(Math.atan2(dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand("tp @a %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
	}

	private void shoot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name)
			.withSize(WIDTH, HEIGHT)
			.disableCounterPrefix());
	}
}
