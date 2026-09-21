package justfatlard.lets_cook;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;

/**
 * Beer, a wine for every fruit worth pressing, mead, and one spirit.
 *
 * <p>Each one is made twice. Crafting gets you a starter - a bottle of sugared water and fruit,
 * which is not a drink and does not pretend to be: it has no food value at all, and its name says
 * {@code (Starter)}. A dark cellar and enough time turns it into the real thing. See
 * {@link Ferments} for the barrel and {@link Labels} for how a bottle tells you which of the three
 * states it is in.
 *
 * <p>Mead is the one that takes no sugar, and the one that makes two. Every other starter needs
 * something for the yeast to work on that the fruit alone will not give it; honey already is that,
 * and asking for sugar beside it was asking twice for the same thing. Honey is also thick enough to
 * let down, so a honey bottle and two waters come out as two starters and the honey's empty glass -
 * the same three bottles that went in.
 *
 * <p>Beer is quick and wine is slow, which is the only difference between them worth having: they
 * cost the same bottle and the same sugar, so what you are really choosing is how long you are
 * prepared to wait for a better effect.
 *
 * <p>They share two things beyond that. There is always room for a drink, so a full stomach is no
 * reason not to have one - and every one of them takes the edge off; see {@link #MALAISE}.
 */
public final class Drinks {
	private Drinks() {}

	/**
	 * What a drink takes the edge off.
	 *
	 * <p>Every one of these clears the same three, and the choice of which three is the whole
	 * point. Milk already exists and strips <em>everything</em> - your own buffs with it - so a
	 * drink that did the same would just be milk you waited a day for. These are the malaise
	 * effects: the ones that make you slow and useless rather than the ones that hurt you.
	 *
	 * <p>Poison and wither are deliberately not here. Being able to drink off real damage is a
	 * different power, and a bottle you can carry sixteen of should not have it.
	 */
	private static final HolderSet<MobEffect> MALAISE = HolderSet.direct(
		MobEffects.NAUSEA, MobEffects.WEAKNESS, MobEffects.MINING_FATIGUE);

	/** Beer is ready in half a day. */
	private static final int BREWING = 12000;

	/** Wine takes a day and a half, and is worth more for it. */
	private static final int FERMENTING = 36000;

	/**
	 * Vodka takes three days, which is twice any wine.
	 *
	 * <p>It is the one thing here that is not simply left alone to turn: a spirit is the drink you
	 * meant to make rather than the one that happened, and the wait is the only way a barrel can
	 * say so.
	 */
	private static final int DISTILLING = 72000;

	/**
	 * What each brew is made from lives in its recipe and only there. It used to be a field here
	 * as well, read by nothing, which is one more place for the answer to drift away from the one
	 * the game actually uses.
	 *
	 * @param stage the word it wears while the barrel has it
	 */
	private record Brew(String name, int ticks, String stage,
			Holder<MobEffect> effect, int seconds) {}

	private static final List<Brew> BREWS = List.of(
		new Brew("beer", BREWING, "brewing", MobEffects.STRENGTH, 45),
		new Brew("apple_wine", FERMENTING, "fermenting", MobEffects.REGENERATION, 6),
		new Brew("berry_wine", FERMENTING, "fermenting", MobEffects.SPEED, 45),
		new Brew("glow_berry_wine", FERMENTING, "fermenting", MobEffects.NIGHT_VISION, 45),
		new Brew("melon_wine", FERMENTING, "fermenting", MobEffects.ABSORPTION, 60),
		new Brew("chorus_wine", FERMENTING, "fermenting", MobEffects.SLOW_FALLING, 45),
		new Brew("mead", FERMENTING, "fermenting", MobEffects.RESISTANCE, 45),
		// Fire resistance because it burns going down, and because it is the one effect no other
		// drink here carries - a spirit that did what a beer does would not be worth three days.
		new Brew("beetroot_vodka", DISTILLING, "distilling", MobEffects.FIRE_RESISTANCE, 45));

	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	public static Map<String, Item> items() { return ITEMS; }

	/** Every bottle stacks sixteen deep, the way honey does. */
	public static int stackOf(String name) { return 16; }

	public static void register() {
		for (Brew brew : BREWS) {
			String key = "item." + Main.MOD_ID + "." + brew.name();

			// Sugared water with fruit in it, or watered honey. No food value: an unfinished brew
			// is not a drink, and giving it one would make the barrel optional.
			Item starter = add(brew.name() + "_starter", new Item.Properties()
				.stacksTo(16)
				.component(DataComponents.CUSTOM_NAME, Labels.starter(key)));

			Item finished = add(brew.name(), new Item.Properties()
				.stacksTo(16)
				.usingConvertsTo(Items.GLASS_BOTTLE)
				.food(new FoodProperties.Builder()
						.nutrition(2)
						.saturationModifier(0.2F)
						// A drink, and there is always room for one.
						.alwaysEdible()
						.build(),
					Consumables.defaultDrink()
						.onConsume(new ApplyStatusEffectsConsumeEffect(
							new MobEffectInstance(brew.effect(), brew.seconds() * 20, 0)))
						.onConsume(new RemoveStatusEffectsConsumeEffect(MALAISE))
						.build()));

			// The stage rides on the finished drink's name, because a starter already is that
			// drink - it is just not ready. "Apple Wine (Fermenting)" says exactly where it is.
			//
			// A wine left far past its minimum comes out as a vintage: the same drink, lasting
			// longer, and saying so. Written onto the bottle rather than registered as six more
			// items, because a Fine and a Reserve differ by one number.
			Ferments.add(starter, finished, brew.ticks(), brew.stage(),
				Component.translatable(key), Ferments.corked(),
				(made, tier) -> {
					made.set(DataComponents.CONSUMABLE, Consumables.defaultDrink()
						.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(
							brew.effect(), brew.seconds() * 20 * Vintage.strength(tier), 0)))
						.onConsume(new RemoveStatusEffectsConsumeEffect(MALAISE))
						.build());
					made.set(DataComponents.CUSTOM_NAME, Labels.suffixed(
						Component.translatable(key), Vintage.wordKey(tier)));
				});
		}
	}

	private static Item add(String name, Item.Properties properties) {
		Identifier id = Identifier.fromNamespaceAndPath(Main.MOD_ID, name);
		Item item = new Item(properties.setId(ResourceKey.create(Registries.ITEM, id)));
		Registry.register(BuiltInRegistries.ITEM, id, item);
		ITEMS.put(name, item);
		return item;
	}
}
