package justfatlard.lets_cook;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a smoker will burn, for how long, and why.
 *
 * <p>Two real things decide the how long. Density: a dense hardwood holds more fire than a light
 * one, so a mangrove log outlasts a birch. And whether the wood is any good to smoke with, which
 * is not the same question - cherry is softer than oak and is still the wood people go looking
 * for, because fruitwood smoke is the point rather than the heat.
 *
 * <p>Spruce is refused outright. Conifers are resinous, and resin burns sooty and sharp: it lays
 * creosote down on the food and on the box, and it tastes of it. It is not a wood you would put
 * under something you meant to eat, so the firebox will not take it. That costs a taiga nothing -
 * every other tree in the game is a hardwood and fine - and it is the one piece of this a cook
 * would call a rule rather than a preference.
 *
 * <p>Poplar burns but poorly: a real hardwood by the botany and a very light one in the hand,
 * mild to the point of saying nothing. Bamboo is a grass and the nether stems are a fungus.
 * Neither is wood and neither has an honest answer, so they sit at the bottom.
 *
 * <p>Any wooden thing counts, not only the log, and not only the shapes vanilla ships. A stack of offcuts is exactly what somebody with a
 * smoker and a workshop actually has: planks, the stairs left from a roof, a sign nobody hung.
 * What they are worth is what they cost in wood, in three plain steps rather than by tracking
 * every recipe's own arithmetic: a whole piece of wood, a quarter of one for anything cut from a
 * plank, and half of that again for a slab.
 */
public final class SmokeWood {
	private SmokeWood() {}

	/** What an unlisted wood from another mod is worth: the middle of the range, and no opinion. */
	private static final int UNKNOWN = 1200;

	/** Everything a smoker will take, if its wood is allowed. */
	private static final TagKey<Item>[] WOODEN = new TagKey[] {
		ItemTags.LOGS, ItemTags.PLANKS, ItemTags.WOODEN_STAIRS, ItemTags.WOODEN_SLABS,
		ItemTags.WOODEN_FENCES, ItemTags.FENCE_GATES, ItemTags.WOODEN_DOORS,
		ItemTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_BUTTONS, ItemTags.WOODEN_PRESSURE_PLATES,
		ItemTags.WOODEN_SHELVES, ItemTags.SIGNS, ItemTags.HANGING_SIGNS,
		ItemTags.BOATS, ItemTags.CHEST_BOATS,
		// Its own tag: #minecraft:logs is only the trees, and a block of bamboo is not one.
		ItemTags.BAMBOO_BLOCKS,
	};

	/** Resinous, and so never fuel for something you are going to eat. */
	public static boolean refused(ItemStack fuel) {
		return path(fuel).contains("spruce");
	}

	/** The sounds a wooden block makes. A mod that builds in wood picks one of these. */
	private static final java.util.Set<net.minecraft.world.level.block.SoundType> WOODEN_SOUNDS =
		java.util.Set.of(net.minecraft.world.level.block.SoundType.WOOD,
			net.minecraft.world.level.block.SoundType.BAMBOO_WOOD,
			net.minecraft.world.level.block.SoundType.CHERRY_WOOD,
			net.minecraft.world.level.block.SoundType.NETHER_WOOD,
			net.minecraft.world.level.block.SoundType.LADDER);

	/** Whether a smoker will hold this at all. */
	public static boolean burns(ItemStack fuel) {
		return burnTicks(fuel) > 0;
	}

	/**
	 * Whether this is wood, by the tags the game ships or by the sound the block makes.
	 *
	 * <p>The tags only know vanilla's own shapes. A floor from Wood Floor, a post from Fence Posts
	 * and a door from Moredoor are all wood, all cut from a log, and in none of them - so a smoker
	 * refused the offcuts of the very mods it sits beside.
	 *
	 * <p>The sound is the check that generalises. A block built in wood is given a wooden sound by
	 * whoever built it, because it has to sound right when you walk on it, and that is a decision
	 * no mod skips.
	 */
	private static boolean wooden(ItemStack fuel) {
		for (TagKey<Item> tag : WOODEN) {
			if (fuel.is(tag)) return true;
		}
		if (fuel.getItem() instanceof net.minecraft.world.item.BlockItem block) {
			return WOODEN_SOUNDS.contains(block.getBlock().defaultBlockState().getSoundType());
		}
		return false;
	}

	/** Ticks this burns for in a smoker; 0 for anything a smoker will not take. */
	public static int burnTicks(ItemStack fuel) {
		if (refused(fuel)) return 0;
		if (!wooden(fuel)) return 0;
		String path = path(fuel);
		return Math.max(1, Math.round(species(path) * portion(path)));
	}

	/** How much of a whole piece of wood this is. */
	private static float portion(String path) {
		if (path.endsWith("_slab")) return 0.125F;
		// A log, the six-sided wood block, a nether stem and a block of bamboo are whole.
		if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_stem")
				|| path.endsWith("_hyphae") || path.equals("bamboo_block")) {
			return 1F;
		}
		return 0.25F;
	}

	/**
	 * By species, matched on the item's path so every form of a wood lands together: a log, a
	 * stripped log, a plank cut from it and a sign carved out of that are all the same tree.
	 */
	private static int species(String path) {
		// Dark oak and pale oak must be asked before plain oak, since both contain "oak". They
		// also hold numbers of their own rather than oak's, so getting that wrong shows up as a
		// wrong scale instead of hiding behind a value that happened to match.
		if (path.contains("mangrove")) return 2000;
		if (path.contains("cherry")) return 1800;
		if (path.contains("acacia")) return 1800;
		if (path.contains("dark_oak")) return 1700;
		if (path.contains("pale_oak")) return 1400;
		if (path.contains("oak")) return 1600;
		if (path.contains("jungle")) return 1500;
		if (path.contains("birch")) return 1200;
		if (path.contains("poplar")) return 900;
		if (path.contains("bamboo")) return 700;
		if (path.contains("crimson") || path.contains("warped")) return 600;
		return UNKNOWN;
	}

	private static String path(ItemStack fuel) {
		Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(fuel.getItem());
		return id == null ? "" : id.getPath();
	}
}
