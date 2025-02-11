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
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Checker;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;

import javax.annotation.Nullable;

/**
 * @author Peter Güttinger
 */
@Name("Is Holding")
@Description("Checks whether a player is holdign a specific item. Cannot be used with endermen, use 'entity is [not] an enderman holding &lt;item type&gt;' instead.")
@Examples({"player is holding a stick",
		"victim isn't holding a sword of sharpness"})
@Since("1.0")
public class CondItemInHand extends Condition {
	
	static {
		if (Skript.isRunningMinecraft(1, 9)) {
			Skript.registerCondition(CondItemInHand.class,
					"[%livingentities%] ha(s|ve) %itemtypes% in [main] hand", "[%livingentities%] (is|are) holding %itemtypes% [in main hand]",
					"[%livingentities%] ha(s|ve) %itemtypes% in off[(-| )]hand", "[%livingentities%] (is|are) holding %itemtypes% in off[(-| )]hand",
					"[%livingentities%] (ha(s|ve) not|do[es]n't have) %itemtypes% in [main] hand", "[%livingentities%] (is not|isn't) holding %itemtypes% [in main hand]",
					"[%livingentities%] (ha(s|ve) not|do[es]n't have) %itemtypes% in off[(-| )]hand", "[%livingentities%] (is not|isn't) holding %itemtypes% in off[(-| )]hand");
		} else {
			Skript.registerCondition(CondItemInHand.class,
					"[%livingentities%] ha(s|ve) %itemtypes% in hand", "[%livingentities%] (is|are) holding %itemtypes% in hand",
					"[%livingentities%] (ha(s|ve) not|do[es]n't have) %itemtypes%", "[%livingentities%] (is not|isn't) holding %itemtypes%");
		}
	}
	
	@SuppressWarnings("null")
	private Expression<LivingEntity> entities;
	@SuppressWarnings("null")
	Expression<ItemType> types;
	boolean offTool;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] vars, final int matchedPattern, final Kleenean isDelayed, final ParseResult parser) {
		entities = (Expression<LivingEntity>) vars[0];
		types = (Expression<ItemType>) vars[1];
		if (Skript.isRunningMinecraft(1, 9)) {
			offTool = (matchedPattern == 2 || matchedPattern == 3 || matchedPattern == 6 || matchedPattern == 7);
			setNegated(matchedPattern >= 4);
		} else {
			offTool = false;
			setNegated(matchedPattern >= 2);
		}
		return true;
	}
	
	@Override
	public boolean check(final Event e) {
		return entities.check(e, new Checker<LivingEntity>() {
			@Override
			public boolean check(final LivingEntity en) {
				return types.check(e, new Checker<ItemType>() {
					@SuppressWarnings("deprecation")
					@Override
					public boolean check(final ItemType type) {
						if (Skript.isRunningMinecraft(1, 9))
							return (type.isOfType(en.getEquipment().getItemInHand()));
						else
							return type.isOfType(en.getEquipment().getItemInHand());
					}
				}, isNegated());
			}
		});
	}
	
	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return entities.toString(e, debug) + " " + (entities.isSingle() ? "is" : "are") + " holding " + types.toString(e, debug) + (offTool ? " in off-hand" : "");
	}
	
}
