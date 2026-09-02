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

/**
 * Smoking, as a craft of its own rather than a faster furnace.
 *
 * <p>Vanilla's smoker is a stopwatch: it cooks the same food in half the time, so choosing it is
 * never a decision about the food. Here it makes something else of anything that was an animal.
 * Smoked meat carries more than cooked, and - because smoking is how you keep meat rather than how
 * you serve it - it can be eaten when you are already full, which cooked meat cannot.
 *
 * <p>That second part is the point. A bigger number only shifts which food everyone carries; being
 * edible when full is a different thing to reach for, and gives the smoker a reason to exist beside
 * the furnace rather than instead of it.
 *
 * <p>Only meat, and later cheese. A stew or a pie put in a smoker comes out exactly as the furnace
 * would have made it - smoking is something you do to an ingredient, not to a dish.
 */
public final class Foods {
	private Foods() {}

	/**
	 * What smoking adds, over the same food cooked.
	 *
	 * <p>Deliberately small. Two points of nutrition is a bite, not a tier; the reason to smoke is
	 * meant to be what it lets you do rather than how far the bar moves.
	 */
	private static final int SMOKING_BONUS = 2;
	private static final float SMOKING_SATURATION_BONUS = 0.2F;

	/** Vanilla's own numbers for the cooked forms, written down because they live in code. */
	private record Cooked(String vanilla, int nutrition, float saturation) {}

	private static final Map<String, Cooked> SMOKED = new LinkedHashMap<>();

	static {
		smoke("smoked_beef", "cooked_beef", 8, 0.8F);
		smoke("smoked_porkchop", "cooked_porkchop", 8, 0.8F);
		smoke("smoked_mutton", "cooked_mutton", 6, 0.8F);
		smoke("smoked_chicken", "cooked_chicken", 6, 0.6F);
		smoke("smoked_rabbit", "cooked_rabbit", 5, 0.6F);
		smoke("smoked_cod", "cooked_cod", 5, 0.6F);
		smoke("smoked_salmon", "cooked_salmon", 6, 0.8F);
	}

	private static void smoke(String name, String vanilla, int nutrition, float saturation) {
		SMOKED.put(name, new Cooked(vanilla, nutrition, saturation));
	}

	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	public static Map<String, Item> items() { return ITEMS; }

	/** The vanilla cooked item each smoked one wears the look of. */
	public static String appearanceOf(String name) {
		Cooked cooked = SMOKED.get(name);
		return cooked == null ? null : cooked.vanilla();
	}

	public static void register() {
		SMOKED.forEach((name, cooked) -> {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(Main.MOD_ID, name));

			Item item = new Item(new Item.Properties()
				.setId(key)
				.food(new FoodProperties.Builder()
					.nutrition(cooked.nutrition() + SMOKING_BONUS)
					.saturationModifier(cooked.saturation() + SMOKING_SATURATION_BONUS)
					// Preserved food keeps; a full stomach is no reason not to pack some.
					.alwaysEdible()
					.build()));

			Registry.register(BuiltInRegistries.ITEM,
				Identifier.fromNamespaceAndPath(Main.MOD_ID, name), item);
			ITEMS.put(name, item);
		});
	}
}
