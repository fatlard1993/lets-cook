package justfatlard.lets_cook;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;

/**
 * Food you carry, that is not meat and does not come in a bowl.
 *
 * <p>The egg is here because vanilla has no way to eat one. An egg is a projectile and a crafting
 * ingredient and never a meal, which is odd for the most obvious food an animal makes - boiling it
 * is the whole feature.
 *
 * <p>The juice is here because a cactus is a plant full of water that the game gives you no way to
 * drink; see {@link CactusTap}.
 */
public final class Snacks {
	private Snacks() {}

	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	private static final Map<String, Integer> STACKS = new LinkedHashMap<>();

	public static Map<String, Item> items() { return ITEMS; }

	/**
	 * How deep this snack stacks.
	 *
	 * <p>Recorded here rather than read back off the item, for the same reason {@link Dishes}
	 * records it: components are not bound while mods initialise, so asking a just-registered item
	 * its stack size throws.
	 */
	public static int stackOf(String name) { return STACKS.getOrDefault(name, 64); }

	public static Item CACTUS_JUICE;

	public static void register() {
		// An egg you can finally eat. Nothing clever: being food at all is the point.
		add("hardboiled_egg", 64, new Item.Properties()
			.food(new FoodProperties.Builder()
				.nutrition(4)
				.saturationModifier(0.4F)
				.build()));

		// A packed lunch: no effect, just a real meal you can carry. The effects live on the bowl
		// dishes, which cost a station and a bowl - a sandwich should not be the answer to that.
		add("meat_sandwich", 64, new Item.Properties()
			.food(new FoodProperties.Builder()
				.nutrition(8)
				.saturationModifier(0.8F)
				.build()));

		add("sushi", 64, new Item.Properties()
			.food(new FoodProperties.Builder()
				.nutrition(6)
				.saturationModifier(0.6F)
				.build()));

		// A pudding, so it follows the pudding rule: there is always room for one.
		add("caramel_apple", 64, new Item.Properties()
			.food(new FoodProperties.Builder()
				.nutrition(6)
				.saturationModifier(0.5F)
				.alwaysEdible()
				.build()));

		// Drunk, not eaten - so the drinking animation, the drinking sound, and the bottle back.
		// Always drinkable, because thirst is not hunger and a full stomach never stopped anyone
		// having a drink.
		CACTUS_JUICE = add("cactus_juice", 16, new Item.Properties()
			.usingConvertsTo(Items.GLASS_BOTTLE)
			.food(new FoodProperties.Builder()
					.nutrition(2)
					.saturationModifier(0.2F)
					.alwaysEdible()
					.build(),
				Consumables.defaultDrink().build()));
	}

	private static Item add(String name, int stack, Item.Properties properties) {
		Identifier id = Identifier.fromNamespaceAndPath(Main.MOD_ID, name);
		Item item = new Item(properties.setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(stack));
		Registry.register(BuiltInRegistries.ITEM, id, item);
		ITEMS.put(name, item);
		STACKS.put(name, stack);
		return item;
	}
}
