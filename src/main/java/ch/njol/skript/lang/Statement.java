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
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.function.EffFunctionCall;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * Supertype of conditions and effects
 * 
 * @author Peter Güttinger
 * @see Condition
 * @see Effect
 */
public abstract class Statement extends TriggerItem implements SyntaxElement {
	
	@SuppressWarnings({"rawtypes", "unchecked", "null"})
	@Nullable
	public static Statement parse(final String s, final String defaultError) {
		final ParseLogHandler log = SkriptLogger.startParseLogHandler();
		try {
			final EffFunctionCall f = EffFunctionCall.parse(s);
			if (f != null) {
				log.printLog();
				return f;
			} else if (log.hasError()) {
				log.printError();
				return null;
			} else {
				log.printError();
			}
		} finally {
			log.stop();
		}
		return (Statement) SkriptParser.parse(s, (Iterator) Skript.getStatements().iterator(), defaultError);
	}

	/**
	 * Parser instance which is being used or was used to parse this element.
	 * Note that this variable is naturally not used for static methods.
	 */
	@SuppressWarnings("null")
	protected ParserInstance pi;
	
}
