package justfatlard.lets_cook;

import justfatlard.pandorical.api.BlockRegistration;
import justfatlard.pandorical.api.ItemRegistration;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The cooking the game left out.
 *
 * <p>Server-side; Pandorical carries the client's half. Vanilla's kitchen converges on whichever
 * meat has the highest saturation, and most of its stations are the same station at different
 * speeds. This gives the foods reasons to exist that are not the size of the number they restore.
 *
 * <p>The smoker makes something of its own out of anything that was an animal, and burns wood to do
 * it. A stew or a pie put in one comes out as the furnace would have made it: smoking is something
 * you do to an ingredient, not to a dish. What goes in a bowl is in {@link Dishes}.
 */
public class Main implements ModInitializer {

	public static final String MOD_ID = "lets-cook-justfatlard";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Foods.register();
		Dishes.register();
		Cheese.register();
		Snacks.register();
		Pies.register();
		Drinks.register();

		// Each smoked food wears its cooked counterpart's look: it is the same meat, kept
		// differently, and drawing it as something else would be a lie about what it is.
		Foods.items().forEach((name, item) -> share(name, 64, appearanceModel(name)));

		Dishes.items().forEach((name, item) ->
			share(name, Dishes.stackOf(name), MOD_ID + ":item/" + name));

		// A bucket of cheese and the wheel it pours out. The wheel is interactive: right-clicking
		// it eats a slice, and without saying so the client would predict a block placement for
		// that click and show one appearing where the server put nothing.
		share("cheese_bucket", 1, MOD_ID + ":item/cheese_bucket");
		share("smoked_cheese_bucket", 1, MOD_ID + ":item/smoked_cheese_bucket");
		wheel("cheese_block", "vintage");
		wheel("smoked_cheese_block", "vintage");

		Snacks.items().forEach((name, item) ->
			share(name, Snacks.stackOf(name), MOD_ID + ":item/" + name));

		Pies.items().forEach((name, item) ->
			share(name, Pies.stackOf(name), MOD_ID + ":item/" + name));

		Drinks.items().forEach((name, item) ->
			share(name, Drinks.stackOf(name), MOD_ID + ":item/" + name));
		Pies.blocks().forEach(Main::wheel);

		UseBlockCallback.EVENT.register(CactusTap::onUseBlock);

		PandoricalApi.content().registerModAssets(MOD_ID);

		// Written here rather than shipped, since which look they wear depends on Minedew Fishing.
		for (String fish : java.util.List.of("smoked_cod", "smoked_salmon")) {
			String definition = "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n    \"model\": \""
				+ appearanceModel(fish) + "\"\n  }\n}\n";
			PandoricalApi.content().registerAsset("assets/" + MOD_ID + "/items/" + fish + ".json",
				definition.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}

		LOGGER.info("Let's Cook loaded - {} smoked foods, {} dishes, {} snacks, {} baked, "
				+ "{} drinks, {} things a barrel finishes, and a smoker that burns wood",
			Foods.items().size(), Dishes.items().size(), Snacks.items().size(),
			Pies.items().size(), Drinks.items().size(), Ferments.count());

		// Guarded, and the guard is why the call sits behind its own class: naming a
		// village-quests type here would load it whether or not that mod is installed.
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("village-quests-justfatlard")) {
			justfatlard.lets_cook.integration.CookingLessons.register();
			justfatlard.lets_cook.integration.CookingErrands.register();
		}
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("block-tip")) {
			justfatlard.lets_cook.integration.BarrelTips.register();
		}
	}

	/**
	 * Hand one item to the client, stack size and all.
	 *
	 * <p>The client's stand-in for a synced item defaults to sixty-four, so a dish the server stops
	 * at sixteen would stack to a different depth at each end - showing up as items that will not
	 * merge, or that appear to and then snap back. The depth comes from {@link Dishes#stackOf},
	 * which is where it was written when the item was built.
	 */
	private static void wheel(String name, String... extra) {
		BlockRegistration registration = new BlockRegistration()
			.baseBlock("minecraft:cake")
			.property("bites")
			.interactive();
		for (String property : extra) {
			registration.property(property);
		}
		PandoricalApi.content().registerBlock(MOD_ID + ":" + name, registration);
	}

	/**
	 * The cooked counterpart's model, except that where Minedew Fishing draws cooked cod and salmon
	 * as fillets, the smoked ones are fillets too: the same cut, kept differently.
	 */
	private static String appearanceModel(String name) {
		boolean fillets = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("minedew-fishing");
		if (fillets && (name.equals("smoked_cod") || name.equals("smoked_salmon"))) {
			return MOD_ID + ":item/" + name + "_fillet";
		}
		return "minecraft:item/" + Foods.appearanceOf(name);
	}

	private static void share(String name, int stack, String model) {
		PandoricalApi.content().registerItem(MOD_ID + ":" + name,
			new ItemRegistration()
				.model(model)
				.maxStackSize(stack));
	}
}
