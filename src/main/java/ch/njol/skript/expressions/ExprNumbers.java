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
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Peter Güttinger
 */
@Name("Numbers")
@Description({"All numbers between two given numbers, useful for looping.",
		"Use 'numbers' if your start is not an integer and you want to keep the fractional part of the start number constant, or use 'integers' if you only want to loop integers.",
		"An integer loop from 1 to a number x can also be written as 'loop x times'."})
@Examples({"loop 5 times: # loops 1, 2, 3, 4, 5",
		"loop numbers from 2.5 to 5.5: # loops 2.5, 3.5, 4.5, 5.5",
		"loop integers from 2.9 to 5.1: # same as '3 to 5', i.e. loops 3, 4, 5"})
@Since("1.4.6")
public class ExprNumbers extends SimpleExpression<Number> {
	static {
		Skript.registerExpression(ExprNumbers.class, Number.class, ExpressionType.COMBINED,
				"[(all|the)] (numbers|1¦integers) (between|from) %number% (and|to) %number%",
				"%number% times");
	}
	
	@SuppressWarnings("null")
	private Expression<Number> start, end;
	boolean integer;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		start = matchedPattern == 0 ? (Expression<Number>) exprs[0] : new SimpleLiteral<>(1, false);
		end = (Expression<Number>) exprs[1 - matchedPattern];
		integer = parseResult.mark == 1 || matchedPattern == 1;
		return true;
	}
	
	@Override
	@Nullable
	protected Number[] get(final Event e) {
		final Number s = start.getSingle(e), f = end.getSingle(e);
		if (s == null || f == null || s.doubleValue() > f.doubleValue())
			return null;
		final Number[] array = integer ? new Long[(int) (Math.floor(f.doubleValue()) - Math.ceil(s.doubleValue()) + 1)] : new Double[(int) Math.floor(f.doubleValue() - s.doubleValue() + 1)];
		final double low = integer ? Math.ceil(s.doubleValue()) : s.doubleValue();
		for (int i = 0; i < array.length; i++) {
			if (integer)
				array[i] = Long.valueOf((long) low + i);
			else
				array[i] = Double.valueOf(low + i);
		}
		return array;
	}
	
	@Override
	@Nullable
	public Iterator<Number> iterator(final Event e) {
		final Number s = start.getSingle(e), f = end.getSingle(e);
		if (s == null || f == null || s.doubleValue() > f.doubleValue())
			return null;
		return new Iterator<Number>() {
			double i = integer ? Math.ceil(s.doubleValue()) : s.doubleValue(), max = integer ? Math.floor(f.doubleValue()) : f.doubleValue();
			
			@Override
			public boolean hasNext() {
				return i <= max;
			}
			
			@SuppressWarnings("null")
			@Override
			public Number next() {
				if (!hasNext())
					throw new NoSuchElementException();
				if (integer)
					return Long.valueOf((long) i++);
				else
					return Double.valueOf(i++);
			}
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return (integer ? "integers" : "numbers") + " from " + start.toString(e, debug) + " to " + end.toString(e, debug);
	}
	
	@Override
	public boolean isLoopOf(final String s) {
		return integer && (s.equalsIgnoreCase("integer") || s.equalsIgnoreCase("int"));
	}
	
	@Override
	public boolean isSingle() {
		return false;
	}
	
	@Override
	public Class<? extends Number> getReturnType() {
		return integer ? Long.class : Double.class;
	}
	
}
