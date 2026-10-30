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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.watabou.utils.PathFinder;

//test bots (CoopAuto) in the throne hall: get off marked cells, take the banner down, cut a chandelier
//rope when the knight's under it, otherwise fight him like any other mob
public class KnightBot {

	//solo test (-Dspd.test.knightbot=1): walk to the hall, fight, pick up the loot
	private static boolean mapped = false;
	private static int lastHP = -1;

	public static void solo( Hero me ){
		if (me == null || !me.isAlive() || !me.ready) return;
		if (!(Dungeon.level instanceof KingdomBossLevel)){
			CoopOrders.named("rest");
			return;
		}
		KingdomBossLevel L = (KingdomBossLevel) Dungeon.level;
		if (!mapped){
			mapped = true;
			for (int i = 0; i < L.length(); i++) L.mapped[i] = true;
		}
		DarkGrandKnight k = KingdomBossLevel.knight(L);
		if (k != null && me.HP != lastHP){
			CoopLog.line("bot knight hero_hp " + lastHP + " -> " + me.HP + " t=" + (int) Actor.now()
					+ " knight_hp=" + k.HP + " act=" + k.act + " adjacent=" + L.adjacent(me.pos, k.pos));
			lastHP = me.HP;
		}
		if (act(me)) return;
		if (k != null){
			CoopOrders.handleCell(k.pos);
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

	public static boolean act( Hero me ){
		if (!(Dungeon.level instanceof KingdomBossLevel)) return false;
		KingdomBossLevel L = (KingdomBossLevel) Dungeon.level;
		DarkGrandKnight k = KingdomBossLevel.knight(L);
		if (k == null || !k.fighting()) return false;

		if (me.HP < me.HT / 3){
			PotionOfHealing p = me.belongings.getItem(PotionOfHealing.class);
			if (p != null){
				CoopOrders.execute(p, "DRINK");
				return true;
			}
		}

		//step out of the marked stuff
		if (TargetedCell.cells.get(me.pos) != null){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = me.pos + n;
				if (L.passable[c] && Actor.findChar(c) == null && TargetedCell.cells.get(c) == null){
					CoopLog.line("bot knight dodges");
					CoopOrders.handleCell(c);
					return true;
				}
			}
		}

		//chandelier, when he's under it. the fun part
		for (Mob m : L.mobs){
			if (!(m instanceof ThroneProps.RopeCleat) || ((ThroneProps.RopeCleat) m).cut) continue;
			int i = ((ThroneProps.RopeCleat) m).chandelier;
			if (i < 0 || i >= L.chandeliers.length) continue;
			if (L.distance(k.pos, L.chandeliers[i]) <= 1 && L.distance(me.pos, m.pos) <= 4){
				CoopLog.line("bot knight cuts the rope");
				CoopOrders.handleCell(m.pos);
				return true;
			}
		}

		//banner first. with more heroes only every other one goes for it
		ThroneProps.Banner b = k.banner();
		int idx = Party.active() ? Party.indexOf(me) : 0;
		if (b != null && (idx % 2 == 0 || Party.living().size() == 1)){
			CoopOrders.handleCell(b.pos);
			return true;
		}
		return false;
	}
}
