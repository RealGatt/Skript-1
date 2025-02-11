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
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

import javax.annotation.Nullable;
import java.util.Arrays;

@Name("Sorted List")
@Description("Sorts given list in natural order. All objects in list must be comparable; usually if you think you can compare it, it can be compared.")
@Examples({"set {_list::*} to  sorted {_list::*"})
@Since("2.2-dev19")
public class ExprSortedList extends SimpleExpression<Object> {
	
	static{
		Skript.registerExpression(ExprSortedList.class, Object.class, ExpressionType.COMBINED, "sorted %objects%");
	}
	
	@SuppressWarnings("null")
	private Expression<Object> list;
	
	@SuppressWarnings({"null", "unchecked"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		Class<? extends Object> type = exprs[0].getReturnType();
		if (Comparable.class.isAssignableFrom(type)) {
			Skript.error("List of type " + Classes.toString(type) + " does not support sorting.");
			return false;
		}
		list = (Expression<Object>) exprs[0];
		return true;
	}
	
	@Override
	@Nullable
	protected Object[] get(Event e) {
		Object[] unsorted = list.getAll(e);
		Object[] sorted = new Object[unsorted.length]; // Not yet sorted...
		
		for (int i = 0; i < sorted.length; i++) {
			Object value = unsorted[i];
			if (value instanceof Long) {
				// Hope it fits to the double...
				sorted[i] = new Double(((Long) value).longValue());
			} else {
				// No conversion needed
				sorted[i] = value;
			}
		}
		
		try {
			Arrays.sort(sorted); // Now sorted
		} catch (IllegalArgumentException ex) { // In case elements are not comparable
			Skript.error("Tried to sort a list, but some objects are not comparable!");
		}
		return sorted;
	}
	
	@Override
	public Class<? extends Object> getReturnType() {
		return Object.class;
	}
	
	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "sorted list";
	}
	
}