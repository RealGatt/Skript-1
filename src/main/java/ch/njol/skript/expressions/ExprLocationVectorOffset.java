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
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;


/**
 * @author bi0qaw
 */
@Name("Vectors - Location vector offset")
@Description("Offset a location by a vector")
@Examples({"set {_loc} to {_loc} ~ {_v}"})
@Since("2.2-dev28")
public class ExprLocationVectorOffset extends SimpleExpression<Location> {
	static {
		Skript.registerExpression(ExprLocationVectorOffset.class, Location.class, ExpressionType.SIMPLE, "%location%[ ]~[~][ ]%vectors%");
	}

	@SuppressWarnings("null")
	private Expression<Location> location;
	@SuppressWarnings("null")
	private Expression<Vector> vectors;

	@SuppressWarnings("null")
	@Override
	protected Location[] get(Event event) {
		Location l = location.getSingle(event);
		if (l == null) {
			return null;
		}
		Location clone = l.clone();
		for (Vector v : vectors.getArray(event)) {
			clone.add(v);
		}
		return new Location[] {clone};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Location> getReturnType() {
		return Location.class;
	}

	@Override
	public String toString(final @Nullable Event event, boolean b) {
		return location.toString() + " offset by vector " + vectors.toString();
	}

	@Override
	@SuppressWarnings({"unchecked", "null"})
	public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
		location = (Expression<Location>) expressions[0];
		vectors = (Expression<Vector>) expressions[1];
		return true;
	}
}
