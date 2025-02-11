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
@Name("Vectors - Create from location")
@Description("Creates a vector from a location")
@Examples({"set {_v} to vector of {_loc}"})
@Since("2.2-dev28")
public class ExprVectorOfLocation extends SimpleExpression<Vector> {
	static {
		Skript.registerExpression(ExprVectorOfLocation.class, Vector.class, ExpressionType.SIMPLE, "vector (of|from|to) %location%", "%location%['s] vector");
	}

	@SuppressWarnings("null")
	private Expression<Location> location;

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Vector> getReturnType() {
		return Vector.class;
	}

	@Override
	@SuppressWarnings({"unchecked", "null"})
	public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
		location = (Expression<Location>)expressions[0];
		return true;
	}

	@Override
	@SuppressWarnings("null")
	protected Vector[] get(Event event) {
		Location l = location.getSingle(event);
		if (l == null){
			return null;
		}
		return new Vector[] { l.toVector() };
	}

	@Override
	public String toString(final @Nullable Event event, boolean b) {
		return "vector of location " + location.toString();
	}
}
