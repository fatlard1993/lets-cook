package justfatlard.lets_cook.integration;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import justfatlard.lets_cook.Main;
import justfatlard.village_quests.api.LessonApi;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A butcher teaching what the smoker is actually for.
 *
 * <p>Registered with Village Quests when that mod is present. Every lesson is
 * something this mod changed that the game will never mention: that the smoker
 * has stopped taking coal, that smoked meat can be eaten on a full stomach,
 * that a bowl of stew carries the effect its ingredients would have implied,
 * that a barrel in a dark cellar is a cheese press, and that the one dish which
 * poisons you is meant to be cooked twice.
 *
 * <p>Every number below was read off this mod's own source and recipe files
 * rather than its README. The load-bearing one is the second: smoking adds two
 * points of nutrition, which is a bite and not a tier, and the reason to do it
 * is the other half of what it adds.
 *
 * <p>Items are looked up by id at the moment a predicate runs, never while the
 * lesson list is being built. This craft registers from {@code Main} and the
 * cost of being wrong about that ordering is a lesson nobody can ever complete.
 *
 * <p>This class must only be touched behind a mod-loaded check. It refers to
 * Village Quests types directly, so loading it without that mod present throws.
 */
public final class CookingLessons {
	private CookingLessons() {}

	/** Ours, resolved late. Null when something was renamed, which fails a lesson shut rather than loudly. */
	private static Item ours(String path) {
		return BuiltInRegistries.ITEM
			.getOptional(Identifier.fromNamespaceAndPath(Main.MOD_ID, path))
			.orElse(null);
	}

	private static boolean is(ItemStack stack, String path) {
		Item item = ours(path);
		return item != null && stack.is(item);
	}

	/** Anything that came out of a smoker as itself rather than as the cooked form. */
	private static boolean isSmoked(ItemStack stack) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && id.getNamespace().equals(Main.MOD_ID) && id.getPath().startsWith("smoked_");
	}

	/** The always-edible half of the menu, by name, since that is what the rule is written on. */
	private static final Set<String> PUDDINGS = Set.of(
		"bread_pudding", "fruit_salad", "ice_cream", "berry_ice_cream",
		"melon_ice_cream", "glow_ice_cream", "cookie_ice_cream", "caramel_apple");

	private static boolean isPudding(ItemStack stack) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return id != null && id.getNamespace().equals(Main.MOD_ID) && PUDDINGS.contains(id.getPath());
	}

	private static Predicate<ItemStack> atLeast(Predicate<ItemStack> what, int count) {
		return stack -> what.test(stack) && stack.getCount() >= count;
	}

	public static void register() {
		LessonApi.register(new LessonApi.Craft(
			"lets-cook:kitchen",
			"butcher",
			LessonApi.Policy.standard(),
			lessons(),
			new LessonApi.Openings(
				LessonApi.lines(
					"{former} is gone. They had you part-way through and a half-taught cook is worse fed than an untaught one. ",
					"You were learning off {former}, weren't you. I'd not have said a word while they were at the block. Since they aren't: ",
					"*at the block* {former}'s student. I know about where they had got you. "),
				LessonApi.lines(
					"There's another when you want it. Nothing here spoils waiting.",
					"*nods at the smoker* Next one whenever. It'll keep, which is rather the subject.",
					"One more when you've time. No rush. Everything I teach takes the time it takes."),
				LessonApi.lines(
					"{former} is gone. Their smoker is still out the back and nobody has lit it.",
					"You'll have heard about {former}. They were teaching you what the smoker is for, weren't they."),
				LessonApi.lines(
					"*sees what you're carrying* You keep smoked and cooked in separate stacks. Somebody taught you that.",
					"You've not once asked me why your coal won't light the smoker. That means you know.",
					"{mentor} taught you, didn't they. They always start people on the firebox.")),
			new LessonApi.Hooks() {
				/**
				 * A butcher hands things over. The barrel lesson is the one that
				 * needs it: a day is a long time to be told about and no time at
				 * all to be handed the end of.
				 */
				@Override
				public void onLesson(ServerPlayer player, ServerLevel world, int beat, LessonApi.Teacher teacher) {
					// As the fourth lesson closes, because the fifth opens "Here is a barrel".
					if (beat == 4) teacher.give(new ItemStack(Items.BARREL));
				}

				@Override
				public void onGraduate(ServerPlayer player, ServerLevel world, LessonApi.Teacher teacher) {
					teacher.give(new ItemStack(Items.SMOKER));
					teacher.says("Take a smoker of your own. Mine has been going since before you could reach the block.");
					teacher.laterInTheVillage("There is woodsmoke coming off somebody's back garden at an odd hour, "
						+ "and it does not smell like anyone's dinner. It smells like next week's.", 0);
				}
			}));
	}

	private static List<LessonApi.Lesson> lessons() {
		return List.of(
			new LessonApi.Lesson(
				"You have been using the smoker as a fast furnace, which is what everyone does and what it is not. "
					+ "Bring me something smoked. Anything that was an animal. And when your coal will not light it, do not come "
					+ "and tell me it is broken.",
				"bring {name} something smoked",
				"Wood. Only wood. Coal will not light a smoker any more.",
				"*takes it* Good. Now the thing you found out the hard way: that firebox takes wood and nothing else. "
					+ "Not coal, not charcoal, not planks. Logs.",
				"It goes by what a log is rather than by a list, so somebody else's tree works the day they add it. "
					+ "And it is not an inconvenience I invented. Smoke is the whole trick, and you do not get smoke off a rock.",
				Items.SMOKER, CookingLessons::isSmoked, 6),

			new LessonApi.Lesson(
				"Now four more, and eat one before you bring me the rest. On a full stomach. I want you to try it and find "
					+ "out you can, because nobody believes me when I say it first.",
				"bring {name} four smoked things, and eat one on a full stomach",
				"Smoked meat goes down on a full stomach. Cooked does not.",
				"*counts them* You ate one full, then. That is the whole of why the smoker is worth walking to. "
					+ "Smoking puts two more on the bar than cooking the same thing, which is a bite, not a fortune.",
				"The two points are not the point. Anything can be a bigger number. Being able to eat when you are already "
					+ "full is a different thing to reach for, and it is the only food here that will let you.",
				null, atLeast(CookingLessons::isSmoked, 4), 6),

			new LessonApi.Lesson(
				"A bowl now. Meat stew: a cooked or smoked meat, a carrot, a potato, in a bowl. Make it, do not eat it, "
					+ "and pay attention to yourself for the minute after you do.",
				"bring {name} a meat stew",
				"Strength, forty-five seconds. The meat in it told you that.",
				"*sets it aside* Strength. Three quarters of a minute of it. And you could have guessed, which is the design: "
					+ "the effect is always the one the ingredients would have implied.",
				"Carrot, potato and beetroot gives you night vision, because of the carrot. Two baked potatoes and milk gives "
					+ "resistance, because it is the heaviest thing in a bowl. Fish and milk gives you water breathing. "
					+ "Nobody has to memorise a table. You already know it.",
				null, stack -> is(stack, "meat_stew"), 8),

			new LessonApi.Lesson(
				"Something sweet. A pudding, any of them. And you are going to eat this one full as well, and this time you "
					+ "will not be surprised.",
				"bring {name} a pudding",
				"There is always room for pudding. That is a rule here, not a joke.",
				"There is always room for pudding. Every sweet thing on my board goes down on a full bar, "
					+ "the same as the smoked meat does.",
				"Which is worth more than it sounds. You are full, you are about to go somewhere unpleasant, and a full bar "
					+ "is not a topped-up bar. A pudding is how you go in with the saturation you actually wanted.",
				Items.BOWL, CookingLessons::isPudding, 8),

			new LessonApi.Lesson(
				"Here is a barrel. Put a bucket of milk in it, put the barrel somewhere underground and properly dark, "
					+ "and leave it a full day. Bring me what you find.",
				"bring {name} a bucket of cheese",
				"Dark, underground, one full day. The barrel does the rest.",
				"*takes the bucket* A day, in the dark. Light of seven or less, which is darker than most people's idea of a "
					+ "cellar. Get that wrong and you come back to milk and blame the barrel.",
				"And it is not ticking away in there. It writes the day down on the bucket and works out the answer when you "
					+ "next open the lid, so nothing has to stay loaded and you can leave it a season. Same rule makes beer and "
					+ "wine. One cellar, three trades.",
				Items.MILK_BUCKET, stack -> is(stack, "cheese_bucket"), 10),

			new LessonApi.Lesson(
				"Last one, and it is the strangest thing I know. Crimson fungus, warped fungus and a nether wart in a bowl. "
					+ "That is the chili. Do not eat it. Put the whole bowl in a furnace and bring me what comes out.",
				"bring {name} a nether loaf",
				"The chili poisons you. Cooking it is the point of it.",
				"*looks the loaf over* There. The chili is a dish that makes you ill, which sounds like a mistake until you "
					+ "notice it is the only dish here worth cooking twice. That is the joke and the recipe at once.",
				"And look what you are holding. No bowl. The bowl went in the furnace with it, so this is the one thing off "
					+ "my board that hands you nothing back and stacks to sixty-four for it. Forty-five seconds of not minding "
					+ "fire, out of a thing made entirely of the place that is on fire.",
				null, stack -> is(stack, "nether_loaf"), 12));
	}
}
