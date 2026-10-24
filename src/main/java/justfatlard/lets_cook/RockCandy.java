package justfatlard.lets_cook;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/**
 * A stick stood in a bottle of sugared water, left in the dark until the sugar comes out onto it.
 *
 * <p>The one thing a barrel makes that is not a ferment: nothing works on the sugar, it simply
 * crystallises out of water that holds more than it can keep. The rule is the same all the same -
 * a still, dark room and time - so it goes through {@link Ferments} like everything else.
 *
 * <p>It comes out of the bottle it grew in: what the barrel hands back is a lump of crystal on a
 * stick, with the empty bottle beside it. It is the one thing here that shimmers, because it is the one thing here that looks enchanted -
 * the glint is how the game draws a thing that catches the light like that.
 */
public final class RockCandy {
	private RockCandy() {}

	/** As long as a wine. Crystal grows slower than yeast works, and is not hurried by warmth. */
	private static final int CRYSTALLISING = 36000;

	/**
	 * Haste, because it is pure sugar. Cookie ice cream has it for thirty seconds; this waited a day
	 * and a half in a cellar, so it lasts longer.
	 */
	private static final int SECONDS = 45;

	public static Item STARTER;
	public static Item CANDY;

	public static void register() {
		String key = "item." + Main.MOD_ID + ".rock_candy";

		STARTER = add("rock_candy_starter", new Item.Properties()
			.stacksTo(16)
			.component(DataComponents.CUSTOM_NAME, Labels.starter(key)));

		// Candy, so it follows the pudding rule: there is always room for one. Next to nothing in
		// it but sugar, so it fills you about as much as a cookie does. Out of the glass, it stacks
		// like the caramel apple it is held like.
		CANDY = add("rock_candy", new Item.Properties()
			.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
			.food(new FoodProperties.Builder()
					.nutrition(2)
					.saturationModifier(0.1F)
					.alwaysEdible()
					.build(),
				eaten(1)));

		Ferments.add(STARTER, CANDY, CRYSTALLISING, "crystallising",
			// It finishes with the sound of the crystal it looks like, not a cork.
			Component.translatable(key), SoundEvents.AMETHYST_BLOCK_CHIME,
			(made, tier) -> {
				made.set(DataComponents.CONSUMABLE, eaten(Vintage.strength(tier)));
				made.set(DataComponents.CUSTOM_NAME, Labels.suffixed(
					Component.translatable(key), Vintage.wordKey(tier)));
			},
			Items.GLASS_BOTTLE);
	}

	/** As much haste as the vintage earned. */
	private static Consumable eaten(int strength) {
		return Consumables.defaultFood()
			.onConsume(new ApplyStatusEffectsConsumeEffect(
				new MobEffectInstance(MobEffects.HASTE, SECONDS * 20 * strength, 0)))
			.build();
	}

	private static Item add(String name, Item.Properties properties) {
		Identifier id = Identifier.fromNamespaceAndPath(Main.MOD_ID, name);
		Item item = new Item(properties.setId(ResourceKey.create(Registries.ITEM, id)));
		Registry.register(BuiltInRegistries.ITEM, id, item);
		return item;
	}
}
