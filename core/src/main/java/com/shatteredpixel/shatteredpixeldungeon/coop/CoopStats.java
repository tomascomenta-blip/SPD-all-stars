/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * All-Stars Pixel Dungeon
 * Copyright (C) 2026 tomascomenta
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.coop;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

//what each hero did in a co-op run, for the scoreboard at the end (WndCoopScore). saved with the
//hero. damage counts for the hero who caused it, or for the hero whose turn it is (abilities, wands...);
//damage over time on the monsters' turns counts for nobody. the game never reads any of this
public class CoopStats implements Bundlable {

	public int dealt;       //damage done to enemies
	public int taken;       //damage taken
	public int kills;       //enemies finished off
	public int downs;       //times this hero fell
	public int gold;        //gold picked up
	public int friendDealt; //friendly fire: damage done to the other heroes
	public int friendDowns; //friendly fire: other heroes knocked down

	private static Hero heroOf( Object src ){
		if (!Party.active()) return null;
		if (src instanceof Hero && Party.indexOf((Hero) src) >= 0) return (Hero) src;
		Actor a = Actor.current();
		if (a instanceof Hero && Party.indexOf((Hero) a) >= 0) return (Hero) a;
		return null;
	}

	//Char.damage, once the hit went through (shields included)
	public static void damaged( Char ch, Object src, int amount ){
		if (amount <= 0 || !Party.active()) return;
		Hero by = heroOf(src);
		if (ch instanceof Hero && Party.indexOf((Hero) ch) >= 0){
			((Hero) ch).coopStats.taken += amount;
			if (by != null && by != ch) by.coopStats.friendDealt += amount;
		} else if (by != null && ch.alignment == Char.Alignment.ENEMY){
			by.coopStats.dealt += amount;
		}
	}

	//Char.damage, the hit was the last one
	public static void finished( Char ch, Object src ){
		if (!Party.active()) return;
		Hero by = heroOf(src);
		if (by == null || by == ch) return;
		if (ch instanceof Hero && Party.indexOf((Hero) ch) >= 0) by.coopStats.friendDowns++;
		else if (ch.alignment == Char.Alignment.ENEMY) by.coopStats.kills++;
	}

	@Override
	public void storeInBundle( Bundle b ){
		b.put("dealt", dealt);
		b.put("taken", taken);
		b.put("kills", kills);
		b.put("downs", downs);
		b.put("gold", gold);
		b.put("f_dealt", friendDealt);
		b.put("f_downs", friendDowns);
	}

	@Override
	public void restoreFromBundle( Bundle b ){
		dealt = b.getInt("dealt");
		taken = b.getInt("taken");
		kills = b.getInt("kills");
		downs = b.getInt("downs");
		gold = b.getInt("gold");
		friendDealt = b.getInt("f_dealt");
		friendDowns = b.getInt("f_downs");
	}
}
