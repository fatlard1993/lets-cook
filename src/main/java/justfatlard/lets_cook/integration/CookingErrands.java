package justfatlard.lets_cook.integration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import justfatlard.lets_cook.Main;
import justfatlard.village_quests.api.QuestRegistry;
import justfatlard.village_quests.quest.VillagerQuest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

/**
 * Villagers asking the player to cook for each other.
 *
 * <p>The butcher's lessons teach the rules; these are where the rest of the
 * menu gets met. Each errand belongs to the trade that would know the dish,
 * and the asker says the recipe in the asking. The one who eats it says what it
 * did to them, which is the effect or the twist the dish carries, said by
 * somebody it just happened to. None of them repeat a lesson.
 *
 * <p>This class must only be touched behind a mod-loaded check. It refers to
 * Village Quests types directly, so loading it without that mod present throws.
 */
public final class CookingErrands {
	private CookingErrands() {}

	/** The same odds the sibling mods' profession quests use. */
	private static final float OFFER_CHANCE = 0.12F;
	private static final double TARGET_RANGE = 48.0;

	/**
	 * {@code ask} and {@code closing} may name the recipient as {target}. The
	 * reaction is the recipient's own line, so it never needs to.
	 */
	public record Errand(String dish, String article, int minReputation, int reward,
			String ask, String reaction, String closing) {}

	private static final Errand VEGETABLE_SOUP = new Errand("vegetable_soup", "a", 0, 6,
		"{target} has been doing the late watch and walking into things. Make them a vegetable soup? A carrot, a potato "
			+ "and a beetroot in a bowl. Nothing clever.",
		"*a minute later, squinting past you* ...Why can I see the back of the barn. It is dark in there.",
		"They could see in the dark after? That is the carrot. The soup only does what the carrot always said it would.");

	private static final Errand PUMPKIN_PIE = new Errand("whole_pumpkin_pie", "a", 0, 8,
		"{target} has family coming and is pretending not to be worried about it. A pumpkin pie would do more than anything "
			+ "I could say. A bucket, a pumpkin, sugar and an egg make the filling. It is not food until the oven has had it, "
			+ "so do not let anyone taste it raw.",
		"*sets it on the table* Seven slices if nobody is greedy. And the bucket comes back when it goes down, which is "
			+ "more than my mother's pies ever managed.",
		"Seven slices. They will count them, too. Good.");

	private static final Errand FISH_CHOWDER = new Errand("fish_chowder", "a", 0, 8,
		"{target} keeps going in after whatever they drop off the jetty. I cannot stop them, so I would rather they did not "
			+ "drown doing it. Fish chowder: a cooked fish, a baked potato, a bucket of milk and a bowl.",
		"Thick. *eats* ...Huh. I could stay under a good while on this. I am going to go and get my hook back.",
		"They went in after the hook, then. And came out. Fish and milk does that: most of a minute of not "
			+ "needing air, which is about what that hook deserves.");

	private static final Errand SUSHI = new Errand("sushi", "some", 0, 6,
		"{target} says raw fish is for cats. Prove them wrong. A raw cod or salmon, dried kelp and wheat. Do not cook the "
			+ "fish. That is the whole of it.",
		"*eats one, slowly* ...It is raw. I am eating raw fish. *eats another* The kelp is doing a lot of the work.",
		"They ate two? They told me cats. I will be bringing that up at supper.");

	private static final Errand MEAT_SANDWICH = new Errand("meat_sandwich", "a", 0, 5,
		"{target} works straight through the midday meal and it is making them short with people. A sandwich: bread, a "
			+ "cooked meat and a bit of kelp. Something they can eat with one hand on a ladder.",
		"A sandwich. Good. Nothing clever in it, which is what you want at the top of a ladder.",
		"Nothing clever in it, they said? That is the idea. A sandwich is for carrying. If you want a stew's worth of "
			+ "anything, you pay for the bowl.");

	private static final Errand MELON_ICE_CREAM = new Errand("melon_ice_cream", "a", 0, 8,
		"{target} is getting over something and will not eat. They say they are full. Nobody is too full for ice cream. "
			+ "Two snowballs, a bucket of milk, sugar and a bowl, and stir a melon slice into it after.",
		"I am not hungry. *eats it anyway* ...There is always room for that, it turns out. And I feel better. Properly "
			+ "better, not just full.",
		"Always room. That is the sweet ones: they go down on a full stomach. The melon did the mending. I only chose "
			+ "how it got into them.");

	private static final Errand POTATO_CHOWDER = new Errand("potato_chowder", "a", 0, 7,
		"{target} fell off the wall last week and has decided the answer is to go back up it. If they must, they can go "
			+ "up with something heavy in them. Potato chowder: two baked potatoes, a bucket of milk and a bowl.",
		"Heavy. *eats* ...I feel like if I walked into a door, the door would apologise.",
		"The door would apologise. Yes. It is the heaviest thing you can put in a bowl, and it sits on you like armour for "
			+ "most of a minute. Not long enough to fall off anything slowly.");

	private static final Errand GLOW_BERRY_PIE = new Errand("whole_glow_berry_pie", "a", 0, 9,
		"{target} reads until the candle dies and then goes on reading in the dark. I cannot give them more candle. I can "
			+ "give them a glow berry pie: a bucket, glow berries, sugar and an egg, and the oven after.",
		"*puts it down, and the table lights up* It glows. The pie glows. I am going to read by a pie.",
		"Read by a pie. The berries glow, so the pie does, right up until the last slice. Which, knowing them, will be "
			+ "about dawn.");

	private static final Errand BEER = new Errand("beer", "a", 10, 10,
		"{target} has been at the forge since dawn and their arms have stopped listening to them. A beer would take the "
			+ "ache out. A water bottle, sugar and wheat make the starter, and the starter is not beer yet. It wants half a "
			+ "day in a barrel somewhere dark and underground.",
		"*drinks* Oh. That is the ache gone, and the fog in my arms with it. The cleric has a long word for that fog. I "
			+ "have a short one.",
		"The fog went? That is the cellar, not the wheat. Anything that has been in a dark barrel takes the heaviness off "
			+ "you. Half a day in the dark for a minute of relief. Worth it, if you ask {target}.");

	private static final Errand BREAD_PUDDING = new Errand("bread_pudding", "a", 0, 6,
		"{target} throws bread out the moment it goes hard, and it breaks my heart. Make it into a bread pudding and let "
			+ "them taste what they have been wasting: bread, an egg, sugar and a bowl.",
		"*finishes it* ...That was the bread I threw out? I am not throwing out bread again.",
		"Not throwing out bread again. Good. And they ate it straight after supper. Puddings go down on a full stomach, "
			+ "which is the whole trick of an evening.");

	private static final Errand CARAMEL_APPLE = new Errand("caramel_apple", "a", 0, 5,
		"{target} has been sulking all week about something nobody else remembers. Take them a caramel apple: an apple, "
			+ "sugar and a stick. It is hard to sulk holding one.",
		"*holding it* ...It is hard to stay cross holding one of these. You knew that.",
		"Hard to stay cross. That is why I sent it.");

	private static final Errand FRUIT_SALAD = new Errand("fruit_salad", "a", 0, 6,
		"{target} has eaten nothing but bread for a week of building and it is starting to show in their temper. A fruit "
			+ "salad: sweet berries, a melon slice and an apple in a bowl.",
		"*eats it standing up* I did not know I wanted that until it was gone.",
		"Did not know they wanted it. Nobody does, until there is a bowl of it in front of them.");

	private static final Errand CHOCOLATE_CAKE = new Errand("chocolate_cake", "a", 25, 9,
		"It is {target}'s day and they have told nobody. I know because I know. A cake, with cocoa beans worked into it. "
			+ "It becomes its own cake, not a cake with chocolate on.",
		"*sees it* How did... who told you? Nobody knows. *cuts a slice* Seven of them. I will share. Probably.",
		"Who told you, they said? Nobody. That is how you know it was the right cake.");

	/** Who asks for what: the trade that would know the dish. */
	private static final Map<String, List<Errand>> MENU = Map.ofEntries(
		Map.entry("farmer", List.of(VEGETABLE_SOUP, PUMPKIN_PIE)),
		Map.entry("fisherman", List.of(FISH_CHOWDER, SUSHI)),
		Map.entry("butcher", List.of(MEAT_SANDWICH)),
		Map.entry("cleric", List.of(MELON_ICE_CREAM, POTATO_CHOWDER)),
		Map.entry("librarian", List.of(GLOW_BERRY_PIE)),
		Map.entry("toolsmith", List.of(BEER)),
		Map.entry("armorer", List.of(BEER)),
		Map.entry("weaponsmith", List.of(BEER)),
		Map.entry("shepherd", List.of(BREAD_PUDDING)),
		Map.entry("cartographer", List.of(CARAMEL_APPLE)),
		Map.entry("mason", List.of(FRUIT_SALAD)),
		Map.entry("fletcher", List.of(CARAMEL_APPLE, FRUIT_SALAD)),
		Map.entry("leatherworker", List.of(CHOCOLATE_CAKE)));

	public static void register() {
		MENU.forEach((profession, errands) -> QuestRegistry.registerProfessionQuest(profession,
			(villager, villagerName, reputation, random) -> offer(villager, villagerName, reputation, random, errands)));
	}

	private static VillagerQuest offer(Villager villager, String villagerName, int reputation, Random random, List<Errand> errands) {
		if (reputation < 0 || random.nextFloat() > OFFER_CHANCE) return null;

		List<Errand> open = errands.stream().filter(e -> reputation >= e.minReputation()).toList();
		if (open.isEmpty()) return null;

		Errand errand = open.get(random.nextInt(open.size()));
		// Resolved at offer time: a renamed dish fails the errand shut, never a quest nobody can finish.
		Item dish = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(Main.MOD_ID, errand.dish())).orElse(null);
		Villager target = someoneNearby(villager, random);
		if (dish == null || target == null) return null;

		String targetName = justfatlard.village_quests.VillageQuests.getNameManager().getName(target);
		CookedForQuest quest = new CookedForQuest(villagerName, villager.getUUID(), targetName, target.getUUID(), errand, dish);
		quest.withTargetHint(target);
		return quest;
	}

	/** One of the few nearest grown villagers, so the walk is across the village rather than to the next one. */
	private static Villager someoneNearby(Villager asker, Random random) {
		if (!(asker.level() instanceof ServerLevel world)) return null;

		List<Villager> near = world.getEntitiesOfClass(Villager.class, new AABB(asker.blockPosition()).inflate(TARGET_RANGE),
				v -> v != asker && !v.isBaby() && v.isAlive())
			.stream()
			.sorted(Comparator.comparingDouble(v -> v.distanceToSqr(asker)))
			.limit(3)
			.toList();
		return near.isEmpty() ? null : near.get(random.nextInt(near.size()));
	}
}
