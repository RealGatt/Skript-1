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

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.HealthUtils;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

import javax.annotation.Nullable;

@Name("Final Damage")
@Description("How much damage is done in a damage event, considering all types of damage reduction. Can NOT be changed.")
@Examples({"send \"%final damage%\" to victim"})
@Since("2.2-dev19")
@Events("damage")
public class ExprFinalDamage extends SimpleExpression<Double> {
	static {
		Skript.registerExpression(ExprFinalDamage.class, Double.class, ExpressionType.SIMPLE, "[the] final damage");
	}
	
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		if (!ScriptLoader.isCurrentEvent(EntityDamageEvent.class)) {
			Skript.error("The expression 'final damage' can only be used in damage events", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		return true;
	}
	
	@Override
	@Nullable
	protected Double[] get(final Event e) {
		if (!(e instanceof EntityDamageEvent))
			return new Double[0];
		return new Double[] {HealthUtils.getDamage((EntityDamageEvent) e)};
	}
	
	@Override
	@Nullable
	public Class<?>[] acceptChange(final ChangeMode mode) {
		Skript.error("Final damage cannot be changed; try changing the 'damage'");
		return null;
	}
	
	@Override
	public boolean isSingle() {
		return true;
	}
	
	@Override
	public Class<? extends Double> getReturnType() {
		return Double.class;
	}
	
	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return "the final damage";
	}
	
}