package justfatlard.lets_cook;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/**
 * Cooking that puts something in a bowl.
 *
 * <p>Vanilla has four stews and no reason to make any of them: they cost more than the meat they
 * replace and restore about as much. These cost ingredients you were already growing and give back
 * the bowl.
 *
 * <p>What each dish is <em>for</em> splits two ways, and the split is the point - a savoury dish and
 * a pudding that both just restored more hunger would be the same item twice.
 *
 * <ul>
 *   <li><b>Savoury dishes carry an effect.</b> Short, always level one, and always something the
 *       ingredients would have told you: the carrot in the soup is what vanilla brews night vision
 *       from, the loaf is made entirely of the nether and answers its fire.
 *   <li><b>Puddings are always edible.</b> There is always room for pudding. That is worth more
 *       than a number when your hunger bar is full and you want to top off before a fight, and it
 *       is the same reason smoked meat keeps - see {@link Foods}.
 * </ul>
 *
 * <p>Two of them are not really dishes. The nether chili is a mistake you can make: it is edible,
 * and eating it is a bad idea. Cooking it is the point, and what comes out is the loaf - the only
 * thing here that leaves the bowl behind, because the bowl went in the furnace too.
 */
public final class Dishes {
	private Dishes() {}

	/**
	 * How many of a dish fit in one slot.
	 *
	 * <p>Vanilla stops its stews at one, which makes cooking a meal rather than a supply: you carry
	 * a single bowl and go back to eating meat. Sixteen is honey's number - the game's own answer
	 * for a food that comes in a container - and it is the whole advantage these dishes get. Their
	 * nutrition stays inside vanilla's stew range on purpose, so what cooking buys is the trip home
	 * you do not have to make, not a bigger number than the food it competes with.
	 */
	private static final int STACK = 16;

	/** An effect a dish grants, in seconds, always at level one. */
	private record Buff(Holder<MobEffect> effect, int seconds) {
		MobEffectInstance instance() {
			return new MobEffectInstance(effect, seconds * 20, 0);
		}
	}

	private record Dish(int nutrition, float saturation, boolean pudding, Buff buff) {}

	private static final Map<String, Dish> BOWLS = new LinkedHashMap<>();

	static {
		savoury("vegetable_soup", 6, 0.6F, MobEffects.NIGHT_VISION, 30);
		savoury("meat_stew", 10, 0.8F, MobEffects.STRENGTH, 45);
		savoury("potato_chowder", 8, 0.7F, MobEffects.RESISTANCE, 45);
		savoury("fish_chowder", 8, 0.8F, MobEffects.WATER_BREATHING, 45);

		pudding("bread_pudding", 8, 0.6F);
		pudding("fruit_salad", 6, 0.5F);

		// The plain churn is the base the others are made from, so it is the plain one in every
		// sense: always edible, and nothing else.
		pudding("ice_cream", 4, 0.3F);
		pudding("berry_ice_cream", 5, 0.3F, MobEffects.SPEED, 30);
		pudding("melon_ice_cream", 5, 0.3F, MobEffects.REGENERATION, 8);
		pudding("glow_ice_cream", 5, 0.3F, MobEffects.GLOWING, 30);
		pudding("cookie_ice_cream", 6, 0.4F, MobEffects.HASTE, 30);
	}

	private static void savoury(String name, int nutrition, float saturation,
			Holder<MobEffect> effect, int seconds) {
		BOWLS.put(name, new Dish(nutrition, saturation, false, new Buff(effect, seconds)));
	}

	private static void pudding(String name, int nutrition, float saturation) {
		BOWLS.put(name, new Dish(nutrition, saturation, true, null));
	}

	private static void pudding(String name, int nutrition, float saturation,
			Holder<MobEffect> effect, int seconds) {
		BOWLS.put(name, new Dish(nutrition, saturation, true, new Buff(effect, seconds)));
	}

	public static final String CHILI = "nether_chili";
	public static final String LOAF = "nether_loaf";

	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	private static final Map<String, Integer> STACKS = new LinkedHashMap<>();

	public static Map<String, Item> items() { return ITEMS; }

	/**
	 * How deep this dish stacks.
	 *
	 * <p>Recorded as the item is built rather than read back off it: components are not bound
	 * during mod initialisation, so asking a freshly registered item its stack size throws. The
	 * client needs the number too - see {@code Main.share} - and this is the one place it is
	 * written, so the two ends cannot disagree.
	 */
	public static int stackOf(String name) { return STACKS.getOrDefault(name, 64); }

	public static void register() {
		BOWLS.forEach((name, dish) ->
			add(name, STACK, properties(dish).usingConvertsTo(Items.BOWL)));

		// Raw fungus and wart in a bowl. Edible, in the sense that you can put it in your mouth.
		add(CHILI, STACK, properties(new Dish(3, 0.2F, false, new Buff(MobEffects.POISON, 15)))
			.usingConvertsTo(Items.BOWL));

		// What the chili was for. No bowl comes back: it was in the furnace with everything else.
		add(LOAF, 64, properties(new Dish(10, 0.9F, false, new Buff(MobEffects.FIRE_RESISTANCE, 45))));
	}

	/** Nutrition, whether it goes down on a full stomach, and whatever it does on the way. */
	private static Item.Properties properties(Dish dish) {
		FoodProperties.Builder food = new FoodProperties.Builder()
			.nutrition(dish.nutrition())
			.saturationModifier(dish.saturation());
		if (dish.pudding()) {
			food.alwaysEdible();
		}

		Item.Properties properties = new Item.Properties();
		if (dish.buff() == null) {
			return properties.food(food.build());
		}

		// Built from defaultFood rather than a bare builder so the dish is eaten the way food is:
		// same one-and-a-half seconds, same animation, same sound, same crumbs.
		Consumable eating = Consumables.defaultFood()
			.onConsume(new ApplyStatusEffectsConsumeEffect(dish.buff().instance()))
			.build();
		return properties.food(food.build(), eating);
	}

	private static ResourceKey<Item> key(String name) {
		return ResourceKey.create(Registries.ITEM,
			Identifier.fromNamespaceAndPath(Main.MOD_ID, name));
	}

	/** Stamps on the two things every dish needs - its id and its depth - and records the depth. */
	private static void add(String name, int stack, Item.Properties properties) {
		Item item = new Item(properties.setId(key(name)).stacksTo(stack));
		Registry.register(BuiltInRegistries.ITEM,
			Identifier.fromNamespaceAndPath(Main.MOD_ID, name), item);
		ITEMS.put(name, item);
		STACKS.put(name, stack);
	}
}
