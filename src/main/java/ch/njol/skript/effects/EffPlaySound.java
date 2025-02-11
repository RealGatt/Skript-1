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
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import javax.annotation.Nullable;

@Name("Play Sound")
@Description("Plays a sound at given location for everyone or just for given players. Playing sounds from resource packs is supported.")
@Examples("")
@Since("2.2-dev28")
public class EffPlaySound extends Effect {
	
	static {
		Skript.registerEffect(EffPlaySound.class, "play sound %string% [with volume %number%] [(and|with) pitch %number%] at %location% [for %players%]");
	}
	
	@SuppressWarnings("null")
	private Expression<String> sound;
	@Nullable
	private Expression<Number> volume;
	@Nullable
	private Expression<Number> pitch;
	
	@SuppressWarnings("null")
	private Expression<Location> location;
	@Nullable
	private Expression<Player> players;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		sound = (Expression<String>) exprs[0];
		volume = (Expression<Number>) exprs[1];
		pitch = (Expression<Number>) exprs[2];
		location = (Expression<Location>) exprs[3];
		players = (Expression<Player>) exprs[4];
		
		return true;
	}

	@SuppressWarnings("null")
	@Override
	protected void execute(Event e) {
		Location l = location.getSingle(e);
		
		String s = sound.getSingle(e);
		float vol = volume != null ? volume.getSingle(e).floatValue() : 0;
		float pi = pitch != null ? pitch.getSingle(e).floatValue() : 0;
		
		if (players != null) {
			for (Player p : players.getAll(e)) {
				p.playSound(l, s, vol, pi);
			}
		} else {
			l.getWorld().playSound(l, Sound.valueOf(s), vol, pi);
		}
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		if (e != null)
			return "play sound " + sound.getSingle(e);
		return "play sound";
	}
	
}
