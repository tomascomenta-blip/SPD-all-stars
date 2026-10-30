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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.watabou.utils.PathFinder;

//test bots (CoopAuto) vs the varkolyn queen: off marked cells, hunt her nests while she hides, first hero
//works the lair airlock and everyone grabs a handhold while it pulls. otherwise fight her like any mob
public class VarkolynBot {

	//solo test (-Dspd.test.varkbot=1): walk to the bridge, fight, pick up the loot
	private static boolean mapped = false;
	private static int lastHP = -1;

	public static void solo( Hero me ){
		if (me == null || !me.isAlive() || !me.ready) return;
		if (!(Dungeon.level instanceof StarshipBossLevel)){
			CoopOrders.named("rest");
			return;
		}
		StarshipBossLevel L = (StarshipBossLevel) Dungeon.level;
		if (!mapped){
			mapped = true;
			for (int i = 0; i < L.length(); i++) L.mapped[i] = true;
		}
		VarkolynLeader q = StarshipBossLevel.queen(L);
		if (q != null && me.HP != lastHP){
			CoopLog.line("bot queen hero_hp " + lastHP + " -> " + me.HP + " t=" + (int) Actor.now()
					+ " queen_hp=" + q.HP + "/" + q.HT + " flee=" + q.fleeing() + " lair=" + q.inLair());
			lastHP = me.HP;
		}
		if (act(me)) return;
		if (q != null){
			if (q.intro == 0) goNear(me, q.pos + L.width() * 3);
			else goNear(me, q.pos);
			return;
		}
		for (Heap h : L.heaps.valueList()){
			if (h.pos != me.pos && L.distance(me.pos, h.pos) <= 10){
				CoopOrders.handleCell(h.pos);
				return;
			}
		}
		CoopOrders.named("rest");
	}

	//go next to something (hit it if already there)
	private static void goNear( Hero me, int cell ){
		if (Dungeon.level.adjacent(me.pos, cell) || Actor.findChar(cell) == null){
			CoopOrders.handleCell(cell);
			return;
		}
		int best = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			if (best == -1 || Dungeon.level.distance(me.pos, c) < Dungeon.level.distance(me.pos, best)) best = c;
		}
		CoopOrders.handleCell(best != -1 ? best : cell);
	}

	public static boolean act( Hero me ){
		if (!(Dungeon.level instanceof StarshipBossLevel)) return false;
		StarshipBossLevel L = (StarshipBossLevel) Dungeon.level;
		VarkolynLeader q = StarshipBossLevel.queen(L);
		if (q == null || !q.fighting()) return false;

		if (me.HP < me.HT / 3){
			PotionOfHealing p = me.belongings.getItem(PotionOfHealing.class);
			if (p != null){
				CoopOrders.execute(p, "DRINK");
				return true;
			}
		}

		//airlock's pulling: grab a handhold if one's close, else hit her while she clings
		VarkolynLair lair = VarkolynLair.find();
		if (q.inLair() && lair != null && (L.airlock == StarshipBossLevel.AIRLOCK_ARMED || L.airlock == StarshipBossLevel.AIRLOCK_OPEN)
				&& L.distance(me.pos, lair.airlockFaceCenter(L)) <= 3){
			if (lair.isHandhold(L, me.pos)){
				CoopOrders.named("rest");
				return true;
			}
			int best = -1;
			for (int h : lair.handholds){
				if (Actor.findChar(h) != null) continue;
				if (best == -1 || L.distance(me.pos, h) < L.distance(me.pos, best)) best = h;
			}
			if (best != -1){
				CoopLog.line("bot queen goes to a handhold");
				CoopOrders.handleCell(best);
				return true;
			}
		}

		//out of marked cells
		if (TargetedCell.cells.get(me.pos) != null){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = me.pos + n;
				if (L.passable[c] && Actor.findChar(c) == null && TargetedCell.cells.get(c) == null){
					CoopLog.line("bot queen dodges");
					CoopOrders.handleCell(c);
					return true;
				}
			}
		}

		//she's hiding -> go for the nests
		if (q.fleeing()){
			int best = -1;
			for (int c : q.nestCells()){
				if (best == -1 || L.distance(me.pos, c) < L.distance(me.pos, best)) best = c;
			}
			if (best != -1){
				goNear(me, best);
				return true;
			}
			return false;
		}

		//in the lair the first hero works the airlock when it's ready
		int idx = Party.active() ? Party.indexOf(me) : 0;
		if (q.inLair() && lair != null && lair.console != -1 && idx == 0 && L.airlock == StarshipBossLevel.AIRLOCK_READY){
			CoopLog.line("bot queen goes to the console");
			goNear(me, lair.console);
			return true;
		}
		//lair: queen before tentacles and brood
		if (q.inLair() && !q.isInvulnerable(Hero.class)){
			goNear(me, q.pos);
			return true;
		}
		return false;
	}
}
