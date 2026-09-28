package justfatlard.lets_cook.gametest;

import justfatlard.lets_cook.SmokeWood;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Every wood burns for its own time, and the ones whose names contain another's still do.
 *
 * <p>The species are matched on the item's path, which is the only thing a log carries that says
 * what tree it came from. That makes three names traps: "dark_oak_log" and "pale_oak_log" both
 * contain "oak", so an oak test that runs first swallows them and two whole species quietly take
 * oak's number. Nothing throws, nothing looks wrong, and the scale the mod is built on has a hole
 * in it that only shows up if somebody counts.
 */
public final class SmokeWoods implements FabricClientGameTest {

	/** What vanilla gives every log, and the floor this whole idea has to clear. */
	private static final int VANILLA = 300;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			int oak = ticks("oak_log");
			int darkOak = ticks("dark_oak_log");
			int paleOak = ticks("pale_oak_log");
			int spruce = ticks("spruce_log");
			int cherry = ticks("cherry_log");
			int mangrove = ticks("mangrove_log");
			int birch = ticks("birch_log");
			int bamboo = ticks("bamboo_block");

			// The trap: both of these contain "oak".
			if (darkOak == oak) throw new AssertionError("dark oak was read as oak (" + oak + ")");
			if (paleOak == oak) throw new AssertionError("pale oak was read as oak (" + oak + ")");

            // Every wood beats vanilla, or "logs last longer" is not true of the one you happen to have.
			// Spruce is not in this list because it is not fuel at all any more; it has its own
			// check below.
			for (String wood : new String[] {"oak_log", "dark_oak_log", "pale_oak_log",
					"cherry_log", "mangrove_log", "birch_log", "jungle_log", "acacia_log",
					"poplar_log", "bamboo_block", "crimson_stem", "warped_stem"}) {
				int t = ticks(wood);
				if (t <= VANILLA) {
					throw new AssertionError(wood + " burns " + t + " ticks, no better than vanilla's " + VANILLA);
				}
			}

			// The shape of the scale: the resinous conifer is the worst wood, the dense hardwood
			// the best, and the fruitwood beats the plain hardwood because that is the whole point.
			if (spruce != 0) throw new AssertionError("spruce still burns for " + spruce + " ticks");
			if (ticks("poplar_log") >= birch) throw new AssertionError("poplar is not the poorest wood that burns");
			if (mangrove <= oak) throw new AssertionError("mangrove does not outlast oak");
			if (cherry <= birch) throw new AssertionError("cherry does not beat birch");
			if (bamboo >= oak) throw new AssertionError("bamboo, a grass, matches a hardwood");

			// A stripped log, and the six-sided wood block, are the same tree.
			for (String same : new String[] {"stripped_oak_log", "oak_wood", "stripped_oak_wood"}) {
				if (ticks(same) != oak) {
					throw new AssertionError(same + " burns differently from oak_log");
				}
			}

			// Spruce is refused outright, in every form it comes in. Resinous smoke is the one
			// thing here a cook would call a rule, so the box will not hold it at all.
			for (String resin : new String[] {"spruce_log", "stripped_spruce_log", "spruce_wood",
					"spruce_planks", "spruce_stairs", "spruce_slab", "spruce_fence", "spruce_door",
					"spruce_sign", "spruce_boat", "spruce_button"}) {
				if (burns(resin)) {
					throw new AssertionError(resin + " was accepted; spruce is refused in every form");
				}
			}

			// Offcuts burn, and are worth what they cost in wood.
			int planks = ticks("oak_planks");
			int stairs = ticks("oak_stairs");
			int slab = ticks("oak_slab");
			int sign = ticks("oak_sign");
			if (planks <= 0 || stairs <= 0 || slab <= 0 || sign <= 0) {
				throw new AssertionError("an offcut was refused: planks=" + planks + " stairs="
					+ stairs + " slab=" + slab + " sign=" + sign);
			}
			if (planks >= oak) throw new AssertionError("a plank is worth as much as the log it came from");
			if (slab >= planks) throw new AssertionError("a slab is worth as much as a whole plank");

			// A plank of the better wood still beats a plank of the worse, or the species scale
			// stops meaning anything the moment somebody burns offcuts.
			if (ticks("mangrove_planks") <= ticks("birch_planks")) {
				throw new AssertionError("species stopped mattering once the wood was cut up");
			}

			// Wooden blocks the vanilla tags do not cover. These stand in for the suite's own -
			// a floor from Wood Floor, a post from Fence Posts, a door from Moredoor - which are
			// wood, cut from a log, and in none of #planks, #wooden_slabs or the rest. A smoker
			// that takes a plank and refuses a floorboard is answering the tag, not the wood.
			for (String wooden : new String[] {"crafting_table", "bookshelf", "barrel", "ladder",
					"chest", "note_block", "beehive", "bamboo_mosaic"}) {
				if (!burns(wooden)) {
					throw new AssertionError(wooden + " is wood and was refused");
				}
			}

			// And the smoker still refuses what is not wood at all.
			for (String not : new String[] {"coal", "charcoal", "lava_bucket", "blaze_rod", "stick",
					"stone", "iron_block", "wool", "dirt"}) {
				if (burns(not)) throw new AssertionError(not + " was accepted as smoker fuel");
			}
		});
	}

	private static boolean burns(String path) {
		var item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
		return SmokeWood.burns(new ItemStack(item));
	}

	private static int ticks(String path) {
		var item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
		return SmokeWood.burnTicks(new ItemStack(item));
	}
}
