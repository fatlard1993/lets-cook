package justfatlard.lets_cook;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Pies, and a cake the game forgot to make chocolate.
 *
 * <p>A pie is made in its tin and baked in it, so it is carried the way cheese is: a bucket goes
 * into the recipe and comes back when you put the pie down. Raw pie is not food - it is a bucket
 * of wet filling, and the oven is the point of it.
 *
 * <p>The glow berry pie gives off light, because the berries did. Nothing else in this mod earns
 * its ingredient quite so directly.
 */
public final class Pies {
	private Pies() {}

	/** Nutrition and saturation of one slice - modest, because there are seven of them. */
	private static final int SLICE = 3;
	private static final float SLICE_SATURATION = 0.3F;

	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	private static final List<String> BLOCKS = new java.util.ArrayList<>();

	public static Map<String, Item> items() { return ITEMS; }

	public static List<String> blocks() { return BLOCKS; }

	/** Every pie stacks to one: it is a whole pie in a tin, not a slice. */
	public static int stackOf(String name) { return name.startsWith("chocolate") ? 1 : 1; }

	public static void register() {
		pie("pumpkin", 0);
		pie("sweet_berry", 0);
		pie("glow_berry", 7);

		// Cake, but chocolate. Its own block rather than a state of vanilla's, because a cake is
		// a block with seven models and adding a flavour to it would double all of them.
		Block cake = block("chocolate_cake", 0);
		item("chocolate_cake", new BlockItem(cake, properties("chocolate_cake").stacksTo(1)));
	}

	private static void pie(String fruit, int light) {
		Block baked = block(fruit + "_pie", light);

		// Wet filling in a tin. Not food: an oven turns it into something, and until then it is
		// an ingredient that happens to be shaped like a meal.
		item("raw_" + fruit + "_pie", new Item(properties("raw_" + fruit + "_pie").stacksTo(1)));

		item("whole_" + fruit + "_pie",
			new BucketedFoodItem(baked, properties("whole_" + fruit + "_pie").stacksTo(1)));
	}

	private static Block block(String name, int light) {
		BlockBehaviour.Properties settings = BlockBehaviour.Properties.of()
			.strength(0.5F)
			.sound(SoundType.WOOL)
			.noOcclusion()
			.setId(ResourceKey.create(Registries.BLOCK,
				Identifier.fromNamespaceAndPath(Main.MOD_ID, name)));
		if (light > 0) {
			settings.lightLevel(state -> light);
		}

		Block baked = new SliceableBlock(settings, SLICE, SLICE_SATURATION);
		Registry.register(BuiltInRegistries.BLOCK,
			Identifier.fromNamespaceAndPath(Main.MOD_ID, name), baked);
		BLOCKS.add(name);
		return baked;
	}

	private static Item.Properties properties(String name) {
		return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
			Identifier.fromNamespaceAndPath(Main.MOD_ID, name)));
	}

	private static void item(String name, Item item) {
		Registry.register(BuiltInRegistries.ITEM,
			Identifier.fromNamespaceAndPath(Main.MOD_ID, name), item);
		// What vanilla's own registration does for a block item and Registry.register does
		// not: without this the block answers asItem() with air, so pick-block gives nothing
		// and a bite throws no crumbs.
		if (item instanceof BlockItem blockItem) {
			blockItem.registerBlocks(Item.BY_BLOCK, item);
		}
		ITEMS.put(name, item);
	}
}
