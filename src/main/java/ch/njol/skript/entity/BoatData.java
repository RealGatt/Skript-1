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
package ch.njol.skript.entity;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.Boat;

import javax.annotation.Nullable;
import java.util.Random;

public class BoatData extends EntityData<Boat> {
	static {
		// It will only register for 1.10+,
		// See SimpleEntityData if 1.9 or lower.
		if (Skript.methodExists(Boat.class, "getWoodType")) { //The 'boat' is the same of 'oak boat', 'any boat' works as supertype and it can spawn random boat.
		}
	}
	
	public BoatData(){
		this(0);
	}
	
	public BoatData(@Nullable TreeSpecies type){
		this(type != null ? type.ordinal() + 2 : 1);
	}
	
	private BoatData(int type){
		matchedPattern = type;
	}
	
	@Override
	protected boolean init(Literal<?>[] exprs, int matchedPattern, ParseResult parseResult) {
		
		return true;
	}

	@Override
	protected boolean init(@Nullable Class<? extends Boat> c, @Nullable Boat e) {
		return true;
	}

	@Override
	public void set(Boat entity) {

	}

	@Override
	protected boolean match(Boat entity) {
		return true;
	}

	@Override
	public Class<? extends Boat> getType() {
		return Boat.class;
	}

	@Override
	public EntityData getSuperType() {
		return new BoatData(matchedPattern);
	}

	@Override
	protected int hashCode_i() {
		return matchedPattern <= 1 ? 0 : matchedPattern;
	}

	@Override
	protected boolean equals_i(EntityData<?> obj) {
		if (obj instanceof BoatData)
			return matchedPattern == ((BoatData)obj).matchedPattern;
		return false;
	}

	@Override
	public boolean isSupertypeOf(EntityData<?> e) {
		if (e instanceof BoatData)
			return matchedPattern <= 1 || matchedPattern == ((BoatData)e).matchedPattern;
		return false;
	}
	
	@SuppressWarnings("null")
	public boolean isOfItemType(ItemType i){
		if (i.getRandom() == null)
			return false;
		int ordinal = -1;
		switch (i.getRandom().getType()){
			case BOAT: ordinal = 0 ; break; //It is to make 'boat' and 'any boat' works as supertype when comparing.
				//$CASES-OMITTED$
			default: return false;
		}
		return hashCode_i() == ordinal + 2 || (matchedPattern + ordinal == 2) || ordinal == 0;
		
	}
}
