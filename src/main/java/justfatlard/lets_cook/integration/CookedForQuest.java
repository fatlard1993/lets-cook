package justfatlard.lets_cook.integration;

import java.util.UUID;
import justfatlard.village_quests.quest.DialogueQuest;
import justfatlard.village_quests.util.InventoryHelper;
import justfatlard.village_quests.util.VillagerVoice;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One villager asks the player to cook something for another. The asker knows
 * the recipe and says it; the one who eats it finds out what it does, out loud,
 * in front of the player. That second half is the lesson: nobody reads a table.
 *
 * <p>A delivery in Village Quests' own sense, so the recipient offers the
 * handover and the asker wants to hear how it went. What differs is that
 * nothing is handed over at the start: the player makes the thing, and any one
 * of it will do.
 */
public class CookedForQuest extends DialogueQuest {
	private final CookingErrands.Errand errand;
	private final Item dish;
	private final String dishName;

	public CookedForQuest(String requesterName, UUID requesterUuid, String targetName, UUID targetUuid,
			CookingErrands.Errand errand, Item dish) {
		super(requesterName, requesterUuid, DialogueType.DELIVER_ITEM, targetName, targetUuid,
			dish.getName(new ItemStack(dish)).getString(), errand.ask(), errand.reward());
		this.errand = errand;
		this.dish = dish;
		this.dishName = dish.getName(new ItemStack(dish)).getString();
	}

	@Override
	public String getDescription() {
		return requesterName + ": \"" + errand.ask().replace("{target}", getTargetVillagerName()) + "\"";
	}

	@Override
	public String getObjective() {
		if (isMessageDelivered()) return super.getObjective();

		String hint = getTargetProfessionHint();
		return "make " + errand.article() + " " + dishName + " and take it to " + getTargetVillagerName()
			+ (hint != null ? ", " + hint : "");
	}

	/** Nothing to hand over at the start: making it is the job. */
	@Override
	public void onAccept(ServerPlayer player) {
	}

	@Override
	public Item getGiveItem() {
		return null;
	}

	@Override
	public Item getCarriedItem() {
		return dish;
	}

	/** On the offer screen this reads as "Bring: Vegetable Soup"; once it is eaten there is nothing to bring. */
	@Override
	public Item getSubmissionItem() {
		return isMessageDelivered() ? null : dish;
	}

	@Override
	public boolean hasDeliveryItem(ServerPlayer player) {
		return InventoryHelper.hasMatch(player.getInventory(), stack -> stack.is(dish));
	}

	@Override
	public void consumeDeliveryItem(ServerPlayer player) {
		InventoryHelper.decrementFirst(player.getInventory(), stack -> stack.is(dish), 1);
	}

	@Override
	public String getDeliveryLabel() {
		return "Hand over the " + dishName + ".";
	}

	@Override
	public String getDeliveryReaction() {
		return isMessageDelivered() ? errand.reaction() : null;
	}

	/** They ate it; there is no reply to carry back but the telling. */
	@Override
	public ItemStack replyToken() {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean checkCompletion(ServerPlayer player) {
		return isMessageDelivered();
	}

	@Override
	public void onComplete(ServerPlayer player) {
		VillagerVoice.queue(player, villagerUuid, requesterName, errand.closing().replace("{target}", getTargetVillagerName()));
		this.completed = true;
	}
}
