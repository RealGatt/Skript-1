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
import ch.njol.util.VectorMath;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;

/**
 * @author bi0qaw
 */
@Name("Vectors - Cylindrical shape")
@Description("Forms a 'cylindrical shaped' vector using yaw to manipulate the current point")
@Examples({"loop 360 times:",
		"	set {_v} to cylindrical vector radius 1, yaw loop-value, height 2",
		"set {_v} to cylindrical vector radius 1, yaw 90, height 2"})
@Since("2.2-dev28")
public class ExprVectorCylindrical extends SimpleExpression<Vector> {
	static {
		Skript.registerExpression(ExprVectorCylindrical.class, Vector.class, ExpressionType.SIMPLE, "[new] cylindrical vector [(from|with)] [radius] %number%, [yaw] %number%(,| and) [height] %number%");
	}

	@SuppressWarnings("null")
	private Expression<Number> radius, yaw, height;

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public String toString(final @Nullable Event event, boolean b) {
		return "cylindrical vector with radius " + radius.toString() + ", yaw " + yaw.toString() + " and height " + height.toString();
	}

	@Override
	public Class<? extends Vector> getReturnType() {
		return Vector.class;
	}

	@Override
	@SuppressWarnings({"unchecked", "null"})
	public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
		radius = (Expression<Number>) expressions[0];
		yaw = (Expression<Number>) expressions[1];
		height = (Expression<Number>) expressions[2];
		return true;
	}

	@Override
	@SuppressWarnings("null")
	protected Vector[] get(Event event) {
		Number r = radius.getSingle(event);
		Number y = yaw.getSingle(event);
		Number h = height.getSingle(event);
		if (r == null || y == null || h == null) {
			return null;
		}
		return new Vector[]{ VectorMath.fromCylindricalCoordinates(r.doubleValue(),VectorMath.fromSkriptYaw(y.floatValue()), h.doubleValue()) };
	}

}
