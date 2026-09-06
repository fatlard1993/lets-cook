package justfatlard.lets_cook;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Milk left in a dark barrel until it is cheese.
 *
 * <p>The whole mechanic is one rule - a barrel, underground, in the dark, for a day - and the
 * hardest thing about it is that every part of that rule is invisible. A barrel one light level
 * too bright looks exactly like a barrel that is working, and a player who gets it wrong has no
 * way to tell whether they are waiting or wasting their time.
 *
 * <p>So the milk says so, through the same labelling every slow thing here uses: see
 * {@link Labels}. The barrel work itself is {@link Ferments} - cheese was the first of these but
 * it was never the only one, and beer and wine want the identical rule.
 */
public final class Cheese {
	private Cheese() {}

	/** How long milk takes, in ticks: one full day. */
	private static final int AGEING = 24000;

	public static final CheeseWheelBlock WHEEL = wheel("cheese_block", 3, 0.3F);
	public static final CheeseWheelBlock SMOKED_WHEEL = wheel("smoked_cheese_block", 4, 0.4F);

	public static Item BUCKET;
	public static Item SMOKED_BUCKET;

	private static CheeseWheelBlock wheel(String name, int nutrition, float saturation) {
		return new CheeseWheelBlock(BlockBehaviour.Properties.of()
			.strength(0.5F)
			.sound(SoundType.WOOL)
			.noOcclusion()
			.setId(ResourceKey.create(Registries.BLOCK,
				Identifier.fromNamespaceAndPath(Main.MOD_ID, name))),
			nutrition, saturation);
	}

	public static void register() {
		BUCKET = put("cheese_block", WHEEL, "cheese_bucket");
		SMOKED_BUCKET = put("smoked_cheese_block", SMOKED_WHEEL, "smoked_cheese_bucket");

		// Milk in a barrel is still milk, so it wears the stage on its own name rather than on
		// the cheese's - unlike a wine starter, which already is the wine.
		//
		// Cheese left far past its minimum comes out sharper. The tier rides on the bucket and is
		// spent when the wheel is placed - see CheeseWheelBlock - because the reward is a better
		// wheel to cut from, not a better bucket to carry.
		Ferments.add(Items.MILK_BUCKET, BUCKET, AGEING, "ageing",
			Component.translatable(Items.MILK_BUCKET.getDescriptionId()), Ferments.poured(),
			(made, tier) -> {
				Vintage.stamp(made, tier);
				made.set(DataComponents.CUSTOM_NAME, Labels.suffixed(
					Component.translatable(made.getItem().getDescriptionId()),
					Vintage.wordKey(tier)));
			});
	}

	/** One wheel and the bucket that pours it. */
	private static Item put(String blockName, Block wheel, String itemName) {
		Registry.register(BuiltInRegistries.BLOCK,
			Identifier.fromNamespaceAndPath(Main.MOD_ID, blockName), wheel);

		// Stack of one, like every other full bucket: what is in it is a whole wheel of cheese.
		BucketedFoodItem bucket = new BucketedFoodItem(wheel, new Item.Properties()
			.stacksTo(1)
			.setId(ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(Main.MOD_ID, itemName))));
		Registry.register(BuiltInRegistries.ITEM,
			Identifier.fromNamespaceAndPath(Main.MOD_ID, itemName), bucket);
		// See Pies.item: the block-to-item map vanilla fills for its own block items
		bucket.registerBlocks(Item.BY_BLOCK, bucket);
		return bucket;
	}

}
