/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2020, MasterKenth
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.dragz;

//region Imports
import com.dragz.models.ChatMessageData;
import com.dragz.models.ItemData;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import com.dragz.discord.Author;
import com.dragz.discord.Embed;
import com.dragz.discord.Field;
import com.dragz.discord.Image;
import com.dragz.discord.Webhook;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.inject.Inject;

import com.google.inject.spi.Message;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.*;
import org.json.JSONObject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;

//endregion

@Slf4j
@PluginDescriptor(
	name = "Bingo Party by Dragz",
	description = "Sends a detailed notification, with a screenshot, via Discord webhooks whenever you get a pre-determined item for your bingo.",
	tags = {"discord", "notification", "bingo", "screenshot"}
)
public class BingoPartyPlugin extends Plugin
{

//region Injections
	@Inject
	private Notifier notifier;

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClientUI clientUI;

	@Inject
	private  ClientThread clientThread;

	@Inject
	private BingoPartyConfig config;

	@Inject
	private BingoPartyPanel panel;

	@Inject
	private ItemManager itemManager;

	@Inject
	private DrawManager drawManager;

	@Inject
	private ConfigManager configManager;

//endregion

//region Private Variables
	private static final String PET_MESSAGE_DUPLICATE = "You have a funny feeling like you would have been followed";
	private static final ImmutableList<String> PET_MESSAGES = ImmutableList.of(
			"You have a funny feeling like you're being followed", "You feel something weird sneaking into your backpack",
			"You have a funny feeling like you would have been followed", PET_MESSAGE_DUPLICATE);

	private final RarityChecker rarityChecker = new RarityChecker();

	private CompletableFuture<java.awt.Image> queuedScreenshot = null;

	@Getter
    public final List<String> ItemsList = new ArrayList<>();
	private final List<ChatMessageData> queuedMessages = new ArrayList<>();
	private final String MessageHeader = "Bingo Party: ";

    private boolean buttonAttached;
	private NavigationButton navButton;

//endregion

//region Lifecycle Overrides
    @Override
	protected void startUp() throws Exception
	{
		super.startUp();

		try
		{
//			BingoPartyPanel panel = injector.getInstance(BingoPartyPanel.class);
			panel.init();

			final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/util/refresh_icon.png");

			navButton = NavigationButton.builder()
					.tooltip("Bingo Party Items Refresh")
					.icon(icon)
					.priority(1)
					.panel(panel)
					.build();

//			clientToolbar.addNavigation(navButton);

//			if (!config.itemsListCode().isEmpty()) // load new data from api
//				ItemsList.addAll(LoadItemsList(config.itemsListCode()));
//
//			if (!ItemsList.isEmpty())
//				SendLoadedMessages(MessageHeader + "Items list has been loaded! (See chat box for more info.)", true);
		}
		catch (Exception ex) {
			ThrowException(ex);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		buttonAttached = false;
		try
		{
			ItemsList.clear();
//			panel.ClearList();
			clientToolbar.removeNavigation(navButton);
		}
		catch (Exception ex) {
			ThrowException(ex);
		}
		super.shutDown();
	}
//endregion

//region Event Subscriptions

	@Provides
	BingoPartyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BingoPartyConfig.class);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		boolean shouldShow = client.getGameState() == GameState.LOGGED_IN;
		if (shouldShow != buttonAttached)
		{
			SwingUtilities.invokeLater(() ->
			{
				if (shouldShow)
					clientToolbar.addNavigation(navButton);
				else
					clientToolbar.removeNavigation(navButton);
			});
			buttonAttached = shouldShow;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		try {

			switch (gameStateChanged.getGameState()) {
				case LOGGED_IN:
					// resend any queued messages
					if (!queuedMessages.isEmpty())
						SendMessages(queuedMessages, true);

					// alert user if a list is loaded upon login
					if (!ItemsList.isEmpty()) {
						SendLoadedMessages(MessageHeader + "Items list has been loaded! (See chat for more details)", true);
					}
					break;

				case LOGIN_SCREEN:
					SwingUtilities.invokeLater(() ->
					{
						clientToolbar.removeNavigation(navButton);
						buttonAttached = false;
					});
					break;

				default:
					break;
			}
		}
		catch (Exception ex) {
			ThrowException(ex);
		}
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived npcLootReceived)
	{
		NPC npc = npcLootReceived.getNpc();
		Collection<ItemStack> items = npcLootReceived.getItems();

		List<CompletableFuture<Boolean>> futures = new ArrayList<>();
		for (ItemStack item : items)
		{
			CompletableFuture<ItemData>[] wrapper = new CompletableFuture[1];

			Supplier<CompletableFuture<ItemData>> itemDataSupplier = () -> {
				wrapper[0] = getNPCLootReceivedItemData(npc.getName(), item.getId(), item.getQuantity());
				return wrapper[0];
			};

			shouldUpload(item.getId(), itemDataSupplier).thenAccept(doUpload -> {
				if (doUpload)
				{
					if(wrapper[0] == null){
						log.debug("We're setting the wrapper value");
						itemDataSupplier.get();
					}

					wrapper[0].thenAccept(itemData -> {
						futures.add(processNpcNotification(npc, item.getId(), item.getQuantity(), itemData.Rarity));
					});
				}
			});
		}

		if (!futures.isEmpty())
		{
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
				.thenAccept(_v -> performScreenshotUpload());
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
			.thenAccept(_v -> performScreenshotUpload()).exceptionally(e ->
		{
			log.error(String.format("onNpcLootReceived error: %s", e.getMessage()), e);
			log.error(String.format("npc %d items %s", npcLootReceived.getNpc().getId(),
				npcLootReceived.getItems().stream().map(i -> "" + i.getId()).reduce("", (p, c) -> p + ", " + c)));
			return null;
		});
	}

	@Subscribe
	public void onLootReceived(LootReceived lootReceived)
	{
		// Only process EVENTS such as Barrows, CoX etc. and PICKPOCKET
		// For NPCs onNpcLootReceived receives more information and is used instead.
		if (lootReceived.getType() == LootRecordType.NPC) { return; }

		Collection<ItemStack> items = lootReceived.getItems();
		List<CompletableFuture<Boolean>> futures = new ArrayList<>();

		for (ItemStack item : items)
		{
			shouldUpload(
				item.getId(),
				() -> getLootReceivedItemData(lootReceived.getName(), lootReceived.getType(), item.getId())
			).thenAccept(doUpload -> {
				if (doUpload)
					futures.add(processEventNotification(lootReceived.getType(), lootReceived.getName(), item.getId(), item.getQuantity()));
			});
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
			.thenAccept(_v -> performScreenshotUpload()).exceptionally(e ->
		{
			log.error(String.format("onLootReceived error: %s", e.getMessage()), e);
			log.error(String.format("event %s items %s", lootReceived.getName(),
				lootReceived.getItems().stream().map(i -> "" + i.getId()).reduce("", (p, c) -> p + ", " + c)));
			return null;
		});

		if (!futures.isEmpty())
		{
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
				.thenAccept(_v -> performScreenshotUpload());
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		try
		{
			if (Objects.equals(configChanged.getKey(), "itemsListCode"))
			{
				if (Objects.requireNonNull(configChanged.getNewValue()).isEmpty())
				{
					ItemsList.clear();
					panel.ClearList();

					String msg = "Items list has been cleared.";
					SendMessage(ChatMessageData.NewBroadcastMessage(MessageHeader + msg));
//					notifier.notify(msg);
				}
				else
				{
					String msg = "List code was changed. Go to the panel to load the new list.";
					SendMessage(ChatMessageData.NewBroadcastMessage(MessageHeader + msg));
//					notifier.notify(msg);
				}
            }
		}
		catch (Exception ex) {
			ThrowException(ex);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String chatMessage = event.getMessage();

		if (event.getType() != ChatMessageType.GAMEMESSAGE &&
			event.getType() != ChatMessageType.SPAM &&
			event.getType() != ChatMessageType.TRADE &&
			event.getType() != ChatMessageType.FRIENDSCHATNOTIFICATION)
				return;

		if (PET_MESSAGES.stream().anyMatch(chatMessage::contains))
		{
			boolean isDuplicate = chatMessage.contains(PET_MESSAGE_DUPLICATE);
			log.info(String.format("Possible pet: duplicate=%b (%s, %s) %s", isDuplicate, event.getSender(), event.getName(), event.getMessage()));

			CompletableFuture<java.awt.Image> screenshotFuture = config.sendScreenshot() ? getScreenshot()
				: CompletableFuture.completedFuture(null);

			screenshotFuture
				// Waiting for screenshot before checking pet allows us to wait one frame,
				// in case pet data is not available yet
				// TODO: Figure out how to get pet info
				.thenApply(screenshot -> queuePetNotification(getPlayerName(), getPlayerIconUrl(), null, -1, isDuplicate)
					.thenCompose(_v -> screenshot != null ? sendScreenshot(getWebhookUrls(), screenshot)
						: CompletableFuture.completedFuture(null)))
				.exceptionally(e ->
				{
					log.error(String.format("onChatMessage (pet) error: %s", e.getMessage()), e);
					log.error(event.toString());
					return null;
				});
		}
	}

//endregion

//region Private Methods
	@SneakyThrows
    public List<String> RefreshList()
	{
		try {
			log.info("Refreshing the list... (the list has also been cleared at this point)");
			ItemsList.clear();

			if(config.itemsListCode().isEmpty())
			{
				throw new Exception("You need to enter a pastebin code!");
			}

			ItemsList.addAll(LoadItemsList(config.itemsListCode()));

			if (!ItemsList.isEmpty())
			{
				SendLoadedMessages(MessageHeader + "Items list has been loaded! (see chat box for list)", true);
//			notifier.notify("Items list has been loaded!");
				return ItemsList;
			}
		}
		catch (Exception ex) {
			ThrowException(ex);
		}

		return new ArrayList<>();
	}

	private void SendLoadedMessages(String broadcastMessage, Boolean sendGameMessage) {
		List<ChatMessageData> messagesToSend = new ArrayList<>();

		if (!broadcastMessage.isEmpty())
			messagesToSend.add(ChatMessageData.NewBroadcastMessage(broadcastMessage));

		if (sendGameMessage)
		{
			messagesToSend.add(ChatMessageData.NewGameMessage(String.format(MessageHeader + "The bingo items list has been successfully loaded!")));
			messagesToSend.add(ChatMessageData.NewGameMessage(String.format(MessageHeader + "Items loaded: %s", String.join(", ", ItemsList))));
		}
		SendMessages(messagesToSend, false);
	}

	private void SendMessage(ChatMessageData msg)
	{
		clientThread.invoke(() -> client.addChatMessage(msg.messageType, msg.name, msg.message, null));
	}

	private void SendMessages(List<ChatMessageData> messagesList, Boolean areQueuedMessages)
	{
		if (!messagesList.isEmpty())
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				log.info("User is logged in, sending messages");
				for (ChatMessageData msg: messagesList)
					SendMessage(msg);

				// remove messages from queue once they have been sent
				if (areQueuedMessages)
					queuedMessages.clear();
			}
			else
			{
				log.info("User is not logged in; queueing messages");
				// do not re-queue messages if they already came from the queue
				if (!areQueuedMessages)
					queuedMessages.addAll(messagesList);
			}
		}
	}

    private List<String> LoadItemsList(String code) throws Exception
	{
		log.info("Updating item list...");
		SendMessage(ChatMessageData.NewGameMessage("Loading items list for code \"" + config.itemsListCode() + "\"..."));

		List<String> itemsList = ApiTool.getInstance().loadItemsList(code);

		if (!itemsList.isEmpty())
		{
			log.info("Items list has been successfully loaded! - {}", String.join(", ", itemsList));
			return itemsList;
		}
		else
		{
			throw new Exception("An error occurred when trying to load the items list. Check your code and try again.");
		}
	}

	private void ThrowException(Exception ex)
	{
		log.error(ex.getMessage());
		log.error(Arrays.toString(ex.getStackTrace()));
		notifier.notify(ex.getMessage(), TrayIcon.MessageType.ERROR);
	}

//endregion

//region Functionality methods

	private CompletableFuture<ItemData> getLootReceivedItemData(String eventName, LootRecordType lootRecordType, int itemId){
		CompletableFuture<ItemData> result = new CompletableFuture<>();

		ItemData itemData = lootRecordType == LootRecordType.PICKPOCKET ?
			rarityChecker.CheckRarityPickpocket(eventName, EnrichItem(itemId), itemManager) :
			rarityChecker.CheckRarityEvent(eventName, EnrichItem(itemId), itemManager);

		result.complete(itemData);
		return result;
	}

	private CompletableFuture<ItemData> getNPCLootReceivedItemData(String npcName, int itemId, int quantity)
	{
		ItemData incomplete = EnrichItem(itemId);
		return rarityChecker.CheckRarityNPC(npcName, incomplete, itemManager, quantity);
	}

	private CompletableFuture<Boolean> shouldUpload(int itemId, Supplier<CompletableFuture<ItemData>> itemDataSupplier)
	{
		CompletableFuture<Boolean> result = new CompletableFuture<>();
		ItemComposition comp = itemManager.getItemComposition(itemId);
		String lowerName = comp.getName().toLowerCase();

		if(log.isDebugEnabled()) { log.info(String.format("Determining if %s should be uploaded...", lowerName)); }
		if(ItemsList.stream().anyMatch(lowerName::equals)){
			// Item is on the list
			if(log.isDebugEnabled()) { log.info("Item is on the list!"); }
			result.complete(true);
			return result;
		}

		if(log.isDebugEnabled())  { log.info("We're not in any item list. We need to continue our check."); }
		return itemDataSupplier.get().thenCompose(itemData -> {
			result.complete(false);
			return result;
		});
	}

	private CompletableFuture<Boolean> processEventNotification(LootRecordType lootRecordType, String eventName, int itemId, int quantity)
	{
		ItemData itemData = lootRecordType == LootRecordType.PICKPOCKET ? rarityChecker.CheckRarityPickpocket(eventName, EnrichItem(itemId), itemManager) : rarityChecker.CheckRarityEvent(eventName, EnrichItem(itemId), itemManager);

		queueScreenshot();
		clientThread.invokeLater(() -> {
			queueLootNotification(getPlayerName(), getPlayerIconUrl(), itemId, quantity, itemData.Rarity, -1, -1, null,
				eventName, config.webhookUrl()).thenApply(_v -> true);
		});

		return CompletableFuture.completedFuture(false);
	}

	private ItemData EnrichItem(int itemId)
	{
		ItemData r = new ItemData();
		r.ItemId = itemId;
		r.GePrice = itemManager.getItemPrice(itemId);
		r.HaPrice = itemManager.getItemComposition(itemId).getHaPrice();

		if(log.isDebugEnabled()){
			log.info(MessageFormat.format("Item {0} prices HA{1}, GE{2}", itemId, r.HaPrice, r.GePrice));
		}

		return r;
	}

	private CompletableFuture<Boolean> processNpcNotification(NPC npc, int itemId, int quantity, float rarity)
	{
		int npcId = npc.getId();
		int npcCombatLevel = npc.getCombatLevel();
		String npcName = npc.getName();

		CompletableFuture<Boolean> f = new CompletableFuture<>();
		queueScreenshot();
		clientThread.invokeLater(() -> {
			queueLootNotification(getPlayerName(), getPlayerIconUrl(), itemId, quantity, rarity, npcId, npcCombatLevel,
				npcName, null, config.webhookUrl()).handle((_v, e) ->
			{
				if (e != null)
				{
					f.completeExceptionally(e);
				}
				else
				{
					f.complete(true);
				}
				return null;
			});
		});

		return f;
	}

	private void queueScreenshot()
	{
		if (queuedScreenshot == null && config.sendScreenshot())
		{
			queuedScreenshot = getScreenshot();
		}
	}

	private void performScreenshotUpload()
	{
		if (queuedScreenshot != null && config.sendScreenshot())
		{
			CompletableFuture<java.awt.Image> copy = queuedScreenshot;
			queuedScreenshot = null;
			copy.thenAccept(screenshot -> sendScreenshot(getWebhookUrls(), screenshot)).handle((v, e) ->
			{
				if (e != null)
				{
					log.error(String.format("sendScreenshotIfSupposedTo error: %s", e.getMessage()), e);
				}
				queuedScreenshot = null;
				return null;
			});
		}
	}

	private CompletableFuture<Void> queueLootNotification(String playerName, String playerIconUrl, int itemId,
														  int quantity, float rarity, int npcId, int npcCombatLevel, String npcName, String eventName, String webhookUrl)
	{
		Webhook webhookData = new Webhook();

		Author author = new Author();
		author.setName(playerName);

		if (playerIconUrl != null) author.setIcon_url(playerIconUrl);

		Embed embed = new Embed();
		embed.setAuthor(author);

		if(config.sendDropRateAndValue()) {
			Field rarityField = new Field();
			rarityField.setName("Rarity");
			rarityField.setValue(getRarityString(rarity));
			rarityField.setInline(true);

			Field haValueField = new Field();
			haValueField.setName("HA Value");
			haValueField.setValue(getGPValueString(itemManager.getItemComposition(itemId).getHaPrice() * quantity));
			haValueField.setInline(true);

			Field geValueField = new Field();
			geValueField.setName("GE Value");
			geValueField.setValue(getGPValueString(itemManager.getItemPrice(itemId) * quantity));
			geValueField.setInline(true);

			embed.setFields(new Field[]{rarityField, haValueField, geValueField});
		}

		Image thumbnail = new Image();
		String iconUrl = ApiTool.getInstance().getIconUrl(itemId);
		thumbnail.setUrl(iconUrl);
		embed.setThumbnail(thumbnail);

		CompletableFuture<String> descFuture = getLootNotificationDescription(itemId, quantity, npcId, npcCombatLevel,
			npcName, eventName, false).handle((notifyDesc, e) ->
		{
			if (e != null) log.error(String.format("queueLootNotification (desc %d) error: %s", itemId, e.getMessage()), e);
			embed.setDescription(notifyDesc);
			return null;
		});

		return CompletableFuture.allOf(descFuture).thenCompose(_v ->
		{
			webhookData.setEmbeds(new Embed[]{embed});
			return sendWebhookData(getWebhookUrls(), webhookData);
		});
	}

	private CompletableFuture<Void> queuePetNotification(String playerName, String playerIconUrl, String petName,
														 int rarity, boolean isDuplicate)
	{
		Author author = new Author();
		author.setName(playerName);

		if (playerIconUrl != null)
		{
			author.setIcon_url(playerIconUrl);
		}

		/*
		 * Field rarityField = new Field(); rarityField.setName("Rarity");
		 * rarityField.setValue(getRarityString(rarity)); rarityField.setInline(true);
		 */

		Embed embed = new Embed();
		embed.setAuthor(author);
		embed.setFields(new Field[]{ /* rarityField */});
		embed.setDescription(getPetNotificationDescription(isDuplicate));

		/*
		 * Image thumbnail = new Image(); CompletableFuture<Void> iconFuture =
		 * ApiTool.getInstance().getIconUrl("pet", -1, petName).thenAccept(iconUrl -> {
		 * thumbnail.setUrl(iconUrl); embed.setThumbnail(thumbnail); });
		 */

		return CompletableFuture.allOf().thenCompose(_v ->
		{
			Webhook webhookData = new Webhook();
			webhookData.setEmbeds(new Embed[]{embed});
			return sendWebhookData(getWebhookUrls(), webhookData);
		});
	}

	private CompletableFuture<java.awt.Image> getScreenshot()
	{
		CompletableFuture<java.awt.Image> f = new CompletableFuture<>();
		drawManager.requestNextFrameListener(screenshotImage ->
		{
			f.complete(screenshotImage);
		});
		return f;
	}

	private CompletableFuture<Void> sendWebhookData(List<String> webhookUrls, Webhook webhookData)
	{
		JSONObject json = new JSONObject(webhookData);
		String jsonStr = json.toString();

		List<Throwable> exceptions = new ArrayList<>();
		List<CompletableFuture<Void>> sends = webhookUrls.stream()
			.map(url -> ApiTool.getInstance().postRaw(url, jsonStr, "application/json").handle((_v, e) ->
			{
				if (e != null)
				{
					exceptions.add(e);
				}
				return null;
			}).thenAccept(_v ->
			{
			})).collect(Collectors.toList());

		return CompletableFuture.allOf(sends.toArray(new CompletableFuture[sends.size()])).thenCompose(_v ->
		{
			if (exceptions.size() > 0)
			{
				log.error(String.format("sendWebhookData got %d error(s)", exceptions.size()));
				exceptions.forEach(t -> log.error(t.getMessage()));
				CompletableFuture<Void> f = new CompletableFuture<>();
				f.completeExceptionally(exceptions.get(0));
				return f;
			}
			return CompletableFuture.completedFuture(null);
		});
	}

	private CompletableFuture<Void> sendScreenshot(List<String> webhookUrls, java.awt.Image screenshot)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write((BufferedImage) screenshot, "png", baos);
			byte[] imageBytes = baos.toByteArray();

			List<Throwable> exceptions = new ArrayList<>();
			List<CompletableFuture<Void>> sends = webhookUrls.stream()
				.map(url -> ApiTool.getInstance().postFormImage(url, imageBytes, "image/png").handle((_v, e) ->
				{
					if (e != null)
					{
						exceptions.add(e);
					}
					return null;
				}).thenAccept(_v ->
				{
				})).collect(Collectors.toList());

			return CompletableFuture.allOf(sends.toArray(new CompletableFuture[sends.size()])).thenCompose(_v ->
			{
				if (exceptions.size() > 0)
				{
					log.error(String.format("sendScreenshot got %d error(s)", exceptions.size()));
					exceptions.forEach(t -> log.error(t.getMessage()));
					CompletableFuture<Void> f = new CompletableFuture<>();
					f.completeExceptionally(exceptions.get(0));
					return f;
				}
				return CompletableFuture.completedFuture(null);
			});
		}
		catch (Exception e)
		{
			log.error("Unable to send screenshot", e);
			return CompletableFuture.completedFuture(null);
		}
	}

	// TODO: Add Pet notification
	private CompletableFuture<String> getLootNotificationDescription(int itemId, int quantity, int npcId,
																	 int npcCombatLevel, String npcName, String eventName, boolean plainText)
	{
		ItemComposition itemComp = itemManager.getItemComposition(itemId);
		String itemUrl = getWikiUrl(itemComp.getName());
		String baseMsg = (plainText) ?
			"Just got **" + (quantity > 1 ? quantity + "x " : "") + itemComp.getName() + "**" :
			"Just got " + (quantity > 1 ? quantity + "x " : "") + "[" + itemComp.getName() + "](" + itemUrl + ")";

		if (npcId >= 0)
		{
			String npcUrl = getWikiUrl(npcName);
			String fullMsg = (plainText) ?
				baseMsg + " from lvl " + npcCombatLevel + " **" + npcName + "**" :
				baseMsg + " from lvl " + npcCombatLevel + " [" + npcName + "](" + npcUrl + ")";

			return CompletableFuture.completedFuture(fullMsg);
		}
		else if (eventName != null)
		{
			String eventUrl = getWikiUrl(eventName);
			String fullMsg = (plainText) ?
				baseMsg + " from **" + eventName + "**" :
				baseMsg + " from [" + eventName + "](" + eventUrl + ")";
			return CompletableFuture.completedFuture(fullMsg);
		}
		else
		{
			return CompletableFuture.completedFuture(baseMsg + " from something");
		}
	}

	private String getWikiUrl(String search){
		return HttpUrl.parse("https://oldschool.runescape.wiki/").newBuilder()
			.addPathSegments("w/Special:Search").addQueryParameter("search", search).build().toString();
	}

	private String getPetNotificationDescription(boolean isDuplicate)
	{
		if (isDuplicate)
		{
			return "Would've gotten a pet, but already has it.";
		}
		else
		{
			return "Just got a pet.";
		}
	}

	private String getGPValueString(int value)
	{
		return "```fix\n" + NumberFormat.getNumberInstance(Locale.US).format(value) + " GP\n```";
	}

	private String getRarityString(float rarity)
	{
		return "```glsl\n# 1/" + (1 / rarity) + " (" + (rarity * 100f) + "%)\n```";
	}

	private String getPlayerIconUrl()
	{
		switch (client.getAccountType())
		{
			case IRONMAN:
				return "https://oldschool.runescape.wiki/images/0/09/Ironman_chat_badge.png";
			case HARDCORE_IRONMAN:
				return "https://oldschool.runescape.wiki/images/b/b8/Hardcore_ironman_chat_badge.png";
			case ULTIMATE_IRONMAN:
				return "https://oldschool.runescape.wiki/images/0/02/Ultimate_ironman_chat_badge.png";
			case GROUP_IRONMAN:
				return "https://oldschool.runescape.wiki/images/Group_ironman_chat_badge.png";
			case HARDCORE_GROUP_IRONMAN:
				return "https://oldschool.runescape.wiki/images/Hardcore_group_ironman_chat_badge.png";
			default:
				return null;
		}
	}
	
	private String getPlayerName()
	{
		return client.getLocalPlayer().getName();
	}

	private List<String> getWebhookUrls()
	{
		return Arrays.asList(config.webhookUrl().split("[\\n,]")).stream().filter(u -> u.length() > 0).map(u -> u.trim())
			.collect(Collectors.toList());
	}
//endregion
}
