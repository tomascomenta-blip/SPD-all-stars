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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameSettings;

//anti-op: no single hit from the heroes' side takes more than N from a monster, bosses included
//(a +15 weapon still kills faster, it just can't one-shot a boss phase). picked before the run next to
//the difficulty, saved in the run, and in coop the guests get it with the host's save like the rest
public class AntiOP {

	public static final int MIN = 10, MAX = 500, STEP = 10, BIG_STEP = 50, NORMAL = 100;

	private static final String KEY_ON  = "sk_antiop";
	private static final String KEY_CAP = "sk_antiop_cap";
	private static final String BUNDLE  = "sk_antiop";

	//the run's cap, 0 = off
	private static int cap = 0;

	public static boolean on(){
		return cap > 0;
	}

	public static int cap(){
		return cap;
	}

	// *** next run, from the options window ***

	public static boolean chosenOn(){
		return GameSettings.getBoolean(KEY_ON, false);
	}

	public static void chosenOn( boolean value ){
		GameSettings.put(KEY_ON, value);
	}

	public static int chosenCap(){
		return clamp(GameSettings.getInt(KEY_CAP, NORMAL, MIN, MAX));
	}

	public static void chosenCap( int value ){
		GameSettings.put(KEY_CAP, clamp(value));
	}

	public static int clamp( int value ){
		value = Math.round(value / (float) STEP) * STEP;
		return Math.max(MIN, Math.min(MAX, value));
	}

	// *** the run ***

	//Dungeon.init (solo or the host)
	public static void init(){
		cap = chosenOn() ? chosenCap() : 0;
		if (DevFlags.antiOp() > 0) cap = DevFlags.antiOp();
	}

	public static void store( Bundle bundle ){
		bundle.put(BUNDLE, cap);
	}

	public static void restore( Bundle bundle ){
		cap = bundle.contains(BUNDLE) ? bundle.getInt(BUNDLE) : 0;
	}

	//Char.damage, after every other multiplier: what this hit really takes
	public static int cap( Char target, Object src, int dmg ){
		if (cap <= 0 || dmg <= cap || !applies(target, src)) return dmg;
		if (CoopLog.enabled()){
			CoopLog.line("antiop: " + target.getClass().getSimpleName() + " " + dmg + " -> " + cap
					+ " from " + src.getClass().getSimpleName());
		}
		return cap;
	}

	//what's left of the cap after 'dealt' (extra damage riding on the same hit, like grim)
	public static int room( Char target, Object src, int dealt, int extra ){
		if (cap <= 0 || !applies(target, src)) return extra;
		return Math.max(0, Math.min(extra, cap - dealt));
	}

	public static boolean applies( Char target, Object src ){
		return target instanceof Mob && target.alignment == Char.Alignment.ENEMY && fromParty(src);
	}

	//the heroes, their allies and whatever they throw, zap or set on fire. a monster's own hits don't
	//count, and neither does a boss hurting itself on purpose (they pass a class, e.g. Fire.class)
	public static boolean fromParty( Object src ){
		if (src instanceof Char) return ((Char) src).alignment == Char.Alignment.ALLY;
		if (src instanceof SKBulletField.Shot){
			Char owner = ((SKBulletField.Shot) src).owner;
			return owner != null && owner.alignment == Char.Alignment.ALLY;
		}
		return src != null && !(src instanceof Class);
	}
}
