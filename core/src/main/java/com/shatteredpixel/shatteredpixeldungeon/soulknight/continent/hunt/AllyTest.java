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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.CityFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.FleshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.Survival;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;

//testing only (-Dspd.test.ally=turtle|rex|bison|elk...): on every floor the party gets to, a beast of the dark
//continent comes along already tamed, at the leader's side, wherever there's room for its patch. to see how
//it fights things it never meets out there (the bosses of the dungeon, the clock in the sky...)
public class AllyTest {

	//Dungeon.switchLevel, once the level and its actors are in
	public static void arrive( Level level ){
		String kind = DevFlags.ally();
		if (kind == null || Hunt.tamedHere() != null) return;
		Hero leader = Party.active() ? Party.heroes().get(0) : Dungeon.hero;
		if (leader == null) return;
		Mob m = make(kind.trim().toLowerCase());
		if (!(m instanceof AshColossus)){
			CoopLog.line("ally test: no such beast " + kind);
			return;
		}
		AshColossus c = (AshColossus) m;
		int cell = spot(level, c, leader.pos);
		if (cell == -1){
			CoopLog.line("ally test: no room for a " + kind + " here");
			return;
		}
		c.pos = cell;
		c.home = cell;
		level.mobs.add(c);
		Actor.add(c);
		c.tame(leader);
		if (leader.belongings.getItem(BoneGrapple.class) == null) Survival.give(leader, new BoneGrapple());
		CoopLog.line("ally test: a tamed " + c.getClass().getSimpleName() + " at " + cell + " on depth " + Dungeon.depth);
	}

	private static Mob make( String kind ){
		Mob m = AshFauna.make(kind);
		if (m == null) m = FleshFauna.make(kind);
		if (m == null) m = CityFauna.make(kind);
		if (m == null) m = WildTest.make(kind);
		return m;
	}

	//the closest cell to the leader where its whole patch fits without standing on anyone
	private static int spot( Level level, Colossus c, int near ){
		boolean[] f = Colossus.fits(c.size, c);
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int i = 0; i < f.length; i++){
			if (!f[i]) continue;
			int d = level.distance(i, near);
			if (d <= c.half() || d >= bestD) continue;
			boolean free = true;
			for (Mob o : level.mobs){
				if (level.distance(i, o.pos) <= c.half() + (o instanceof Colossus ? ((Colossus) o).half() : 0)) free = false;
			}
			if (free){
				best = i;
				bestD = d;
			}
		}
		return best;
	}
}
