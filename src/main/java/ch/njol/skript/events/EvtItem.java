/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Copyright 2011-2017 Peter Güttinger and contributors
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.effects.EffSpawn;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Checker;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

/**
 * @author Peter Güttinger
 */
public class EvtItem extends SkriptEvent {
	private final static boolean hasConsumeEvent = Skript.classExists("org.bukkit.event.player.PlayerItemConsumeEvent");
	private final static boolean hasPrepareCraftEvent = Skript.classExists("org.bukkit.event.inventory.PrepareItemCraftEvent");
	
	static {
		Skript.registerEvent("Dispense", EvtItem.class, BlockDispenseEvent.class, "dispens(e|ing) [[of] %itemtypes%]")
				.description("Called when a dispenser dispenses an item.")
				.examples("")
				.since("<i>unknown</i> (before 2.1)");
		Skript.registerEvent("Item Spawn", EvtItem.class, ItemSpawnEvent.class, "item spawn[ing] [[of] %itemtypes%]")
				.description("Called whenever an item stack is spawned in a world, e.g. as drop of a block or mob, a player throwing items out of his inventory, or a dispenser dispensing an item (not shooting it).")
				.examples("")
				.since("<i>unknown</i> (before 2.1)");
		Skript.registerEvent("Drop", EvtItem.class, PlayerDropItemEvent.class, "[player] drop[ing] [[of] %itemtypes%]")
				.description("Called when a player drops an item from his inventory.")
				.examples("")
				.since("<i>unknown</i> (before 2.1)");
		// TODO limit to InventoryAction.PICKUP_* and similar (e.g. COLLECT_TO_CURSOR)
		Skript.registerEvent("Craft", EvtItem.class, CraftItemEvent.class, "[player] craft[ing] [[of] %itemtypes%]")
				.description("Called when a player crafts an item.")
				.examples("")
				.since("<i>unknown</i> (before 2.1)");
		if (hasPrepareCraftEvent) {
			Skript.registerEvent("Prepare Craft", EvtItem.class, PrepareItemCraftEvent.class, "[player] (preparing|beginning) craft[ing] [[of] %itemtypes%]")
					.description("Called just before displaying crafting result to player. Note that setting the result item might or might not work due to Bukkit bugs.")
					.examples("")
					.since("2.2-Fixes-V10");
		}
		Skript.registerEvent("Pick Up", EvtItem.class, PlayerPickupItemEvent.class, "[player] (pick[ ]up|picking up) [[of] %itemtypes%]")
				.description("Called when a player picks up an item. Please note that the item is still on the ground when this event is called.")
				.examples("")
				.since("<i>unknown</i> (before 2.1)");
		// TODO brew event
//		Skript.registerEvent("Brew", EvtItem.class, BrewEvent.class, "brew[ing] [[of] %itemtypes%]")
//				.description("Called when a potion finished brewing.")
//				.examples("")
//				.since("2.0");
		if (hasConsumeEvent) {
			Skript.registerEvent("Consume", EvtItem.class, PlayerItemConsumeEvent.class, "[player] ((eat|drink)[ing]|consum(e|ing)) [[of] %itemtypes%]")
					.description("Called when a player is done eating/drinking something, e.g. an apple, bread, meat, milk or a potion.")
					.examples("")
					.since("2.0");
		}
		
		Skript.registerEvent("Inventory Click", EvtItem.class, InventoryClickEvent.class, "[player] inventory(-| )click[ing] [[at] %itemtypes%]")
				.description("Called when clicking on inventory slot.")
				.examples("")
				.since("2.2-Fixes-V10");
	}
	
	@Nullable
	private Literal<ItemType> types;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean init(final Literal<?>[] args, final int matchedPattern, final ParseResult parser) {
		types = (Literal<ItemType>) args[0];
		return true;
	}
	
	@SuppressWarnings("null")
	@Override
	public boolean check(final Event e) {
		if (e instanceof ItemSpawnEvent) // To make 'last dropped item' possible.
			EffSpawn.lastSpawned = ((ItemSpawnEvent) e).getEntity();
		if (types == null)
			return true;
		final ItemStack is;
		if (e instanceof BlockDispenseEvent) {
			is = ((BlockDispenseEvent) e).getItem();
		} else if (e instanceof ItemSpawnEvent) {
			is = ((ItemSpawnEvent) e).getEntity().getItemStack();
		} else if (e instanceof PlayerDropItemEvent) {
			is = ((PlayerDropItemEvent) e).getItemDrop().getItemStack();
		} else if (e instanceof CraftItemEvent) {
			is = ((CraftItemEvent) e).getRecipe().getResult();
		} else if (hasPrepareCraftEvent && e instanceof PrepareItemCraftEvent) {
			is = ((PrepareItemCraftEvent) e).getRecipe().getResult();
		} else if (e instanceof PlayerPickupItemEvent) {
			is = ((PlayerPickupItemEvent) e).getItem().getItemStack();
		} else if (hasConsumeEvent && e instanceof PlayerItemConsumeEvent) {
			is = ((PlayerItemConsumeEvent) e).getItem();
//		} else if (e instanceof BrewEvent)
//			is = ((BrewEvent) e).getContents().getContents()
		} else if (e instanceof InventoryClickEvent) {
			is = ((InventoryClickEvent) e).getCurrentItem();
		} else {
			assert false;
			return false;
		}
		return types.check(e, new Checker<ItemType>() {
			@Override
			public boolean check(final ItemType t) {
				return t.isOfType(is);
			}
		});
	}
	
	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return "dispense/spawn/drop/craft/pickup/consume/break" + (types == null ? "" : " of " + types);
	}
	
}
