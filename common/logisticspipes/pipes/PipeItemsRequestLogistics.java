/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.ResourceLocation;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.GuiIDs;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCQueued;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.request.RequestHandler;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;

@CCType(name = "LogisticsPipes:Request")
public class PipeItemsRequestLogistics extends CoreRoutedPipe implements IRequestItems {

	private final LinkedList<Map<ItemIdentifier, Integer>> _history = new LinkedList<>();

	public PipeItemsRequestLogistics(Item item) {
		super(item);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_REQUESTER_TEXTURE;
	}

	@Override
	public @Nullable LogisticsModule getLogisticsModule() {
		return null;
	}

	public void openGui(EntityPlayer entityplayer) {
		entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_Normal_Orderer_ID, getWorld(), getX(), getY(), getZ());
	}

	@Override
	public boolean handleClick(EntityPlayer entityplayer, SecuritySettings settings) {
		if (MainProxy.isServer(getWorld())) {
			if (settings == null || settings.openRequest) {
				openGui(entityplayer);
			} else {
				entityplayer.sendMessage(new TextComponentString("Permission denied"));
			}
		}
		return true;
	}

	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		if (getWorld().getTotalWorldTime() % 1200 == 0) {
			_history.addLast(SimpleServiceLocator.logisticsManager.getAvailableItems(getRouter().getIRoutersByCost()));
			if (_history.size() > 20) {
				_history.removeFirst();
			}
		}
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@CCCommand(description = "Requests the given ItemIdentifier with the given amount")
	@CCQueued
	public Object[] makeRequest(Map<String, Object> item, Double amount) throws Exception {
		return makeRequest(item, amount, false);
	}

	@CCCommand(description = "Requests the given ItemIdentifier with the given amount")
	@CCQueued
	public Object[] makeRequest(Map<String, Object> item, Double amount, Boolean forceCrafting) throws Exception {
		if (forceCrafting == null) {
			forceCrafting = false;
		}
		if (item == null) {
			throw new Exception("Invalid ItemIdentifier");
		}
		//Item item, int itemUndamagableDamage, NBTTagCompound tag
		int dmg = 0;
		if(item.containsKey("damage")) {
			dmg = ((Double)item.get("damage")).intValue();
		}
		return RequestHandler.computerRequest(ItemIdentifier.get(Item.REGISTRY.getObject(new ResourceLocation((String)item.get("item"))), dmg, null).makeStack((int) Math.floor(amount)), this, forceCrafting);
	}

	@CCCommand(description = "Asks for all available ItemIdentifier inside the Logistics Network")
	@CCQueued
	public List<Pair<ItemIdentifier, Integer>> getAvailableItems() {
		Map<ItemIdentifier, Integer> items = SimpleServiceLocator.logisticsManager.getAvailableItems(getRouter().getIRoutersByCost());
		List<Pair<ItemIdentifier, Integer>> list = new LinkedList<>();
		for (Entry<ItemIdentifier, Integer> item : items.entrySet()) {
			int amount = item.getValue();
			list.add(new Pair<>(item.getKey(), amount));
		}
		return list;
	}

	@CCCommand(description = "Asks for all craftable ItemIdentifier inside the Logistics Network")
	@CCQueued
	public List<ItemIdentifier> getCraftableItems() {
		return SimpleServiceLocator.logisticsManager.getCraftableItems(getRouter().getIRoutersByCost());
	}

	@CCCommand(description = "Asks for the amount of an ItemIdentifier Id inside the Logistics Network")
	@CCQueued
	public int getItemAmount(Map<String, Object> item) throws Exception {
		Map<ItemIdentifier, Integer> items = SimpleServiceLocator.logisticsManager.getAvailableItems(getRouter().getIRoutersByCost());
		int dmg = 0;
		if(item.containsKey("damage")) {
			dmg = ((Double)item.get("damage")).intValue();
		}
		ItemIdentifier itemID = ItemIdentifier.get(Item.REGISTRY.getObject(new ResourceLocation((String)item.get("item"))), dmg, null);
		if (itemID == null) {
			throw new Exception("Invalid ItemIdentifierID");
		}
		if (items.containsKey(itemID)) {
			return items.get(itemID);
		}
		return 0;
	}

}
