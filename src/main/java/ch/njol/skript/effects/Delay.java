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
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.timings.SkriptTimings;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * @author Peter Güttinger
 */
@Name("Delay")
@Description("Delays the script's execution by a given timespan. Please note that delays are not persistent, e.g. trying to create a tempban script with <code>ban player → wait 7 days → unban player</code> will not work if you restart your server anytime within these 7 days. You also have to be careful even when using small delays!")
@Examples({"wait 2 minutes",
		"halt for 5 minecraft hours",
		"wait a tick"})
@Since("1.4")
public class Delay extends Effect {
	static {
		Skript.registerEffect(Delay.class, "(wait|halt) [for] %timespan%");
	}

	@SuppressWarnings("null")
	protected Expression<Timespan> duration;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		duration = (Expression<Timespan>) exprs[0];
		if (duration instanceof Literal) { // If we can, do sanity check for delays
			long millis = ((Literal<Timespan>) duration).getSingle().getMilliSeconds();
			if (millis < 50) {
				Skript.warning("Delays less than one tick are not possible, defaulting to one tick.");
			}
		}
		return true;
	}

	@Override
	@Nullable
	protected TriggerItem walk(final Event e) {
		debug(e, true);
		final long start = Skript.debug() ? System.nanoTime() : 0;
		final TriggerItem next = getNext();
		if (next != null) {
			delayed.add(e);
			final Timespan d = duration.getSingle(e);
			if (d == null)
				return null;
			Bukkit.getScheduler().scheduleSyncDelayedTask(Skript.getInstance(), new Runnable() {
				@Override
				public void run() {
					if (Skript.debug())
						Skript.info(getIndentation() + "... continuing after " + (System.nanoTime() - start) / 1000000000. + "s");
					
					Object timing = null;
					if (SkriptTimings.enabled()) { // getTrigger call is not free, do it only if we must
						Trigger trigger = getTrigger();
						if (trigger != null) {
							timing = SkriptTimings.start(trigger.getDebugLabel());
						}
					}
					
					TriggerItem.walk(next, e);
					
					SkriptTimings.stop(timing); // Stop timing if it was even started
				}
			}, d.getTicks_i() < 1 ? 1 : d.getTicks_i()); // Minimum delay is one tick, less than it is useless!
		}
		return null;
	}

	@SuppressWarnings("null")
	protected final static Set<Event> delayed = Collections.newSetFromMap(new WeakHashMap<Event, Boolean>());

	public final static boolean isDelayed(final Event e) {
		return delayed.contains(e);
	}

	public static void addDelayedEvent(Event event){
		delayed.add(event);
	}

	@Override
	protected void execute(final Event e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return "wait for " + duration.toString(e, debug) + (e == null ? "" : "...");
	}

}
