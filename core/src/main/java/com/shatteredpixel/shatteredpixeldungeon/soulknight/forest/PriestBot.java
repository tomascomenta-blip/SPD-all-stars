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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forest;

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

//test bots (CoopAuto) vs the high priest: get off marked cells, break the ritual totems (spirit first),
//and when the giant star comes hit him if close enough or run to a sacred stone. otherwise just fight
//him like any mob. -Dspd.test.ritualbot=1 also makes them break the ritual totems they see on floors 1-4
public class PriestBot {

	//solo test (-Dspd.test.priestbot=1): walk to the clearing, fight, grab the loot
	private static boolean mapped = false;
	private static int lastHP = -1;

	public static void solo( Hero me ){
		if (me == null || !me.isAlive() || !me.ready) return;
		if (!(Dungeon.level instanceof ForestBossLevel)){
			if (!act(me)) CoopOrders.named("rest");
			return;
		}
		ForestBossLevel L = (ForestBossLevel) Dungeon.level;
		if (!mapped){
			mapped = true;
			for (int i = 0; i < L.length(); i++) L.mapped[i] = true;
		}
		GoblinPriest p = ForestBossLevel.priest(L);
		if (p != null && me.HP != lastHP){
			CoopLog.line("bot priest hero_hp " + lastHP + " -> " + me.HP + " t=" + (int) Actor.now()
					+ " priest_hp=" + p.HP + "/" + p.HT + " act=" + p.act + " star=" + p.starTimer);
			lastHP = me.HP;
		}
		if (act(me)) return;
		if (p != null){
			CoopOrders.handleCell(p.intro == 0 ? p.pos + L.width() * 2 : p.pos);
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
		if ("1".equals(System.getProperty("spd.test.ritualbot")) && Dungeon.level instanceof ForestLevel
				&& !(Dungeon.level instanceof ForestBossLevel)){
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof RitualTotems.FloorTotem && m.isAlive()){
					for (int i = 0; i < Dungeon.level.length(); i++) Dungeon.level.mapped[i] = true;
					if (Dungeon.level.adjacent(me.pos, m.pos)){
						CoopOrders.handleCell(m.pos);
						return true;
					}
					//closest free cell next to it
					int best = -1;
					for (int n : PathFinder.NEIGHBOURS8){
						int c = m.pos + n;
						if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
						if (best == -1 || Dungeon.level.distance(me.pos, c) < Dungeon.level.distance(me.pos, best)) best = c;
					}
					if (best == -1) return false;
					CoopOrders.handleCell(best);
					return true;
				}
			}
		}
		if (!(Dungeon.level instanceof ForestBossLevel)) return false;
		ForestBossLevel L = (ForestBossLevel) Dungeon.level;
		GoblinPriest p = ForestBossLevel.priest(L);
		if (p == null || !p.fighting()) return false;

		if (me.HP < me.HT / 3){
			PotionOfHealing potion = me.belongings.getItem(PotionOfHealing.class);
			if (potion != null){
				CoopOrders.execute(potion, "DRINK");
				return true;
			}
		}

		//giant star: hit him if close, else run to a stone
		if (p.starTimer > 0){
			int d = L.distance(me.pos, p.pos);
			boolean canReach = d <= 1 || (d <= 2 && p.starTimer >= 3);
			if (!L.safeFromStar(me.pos) && (!canReach || p.starTimer <= 1)){
				int best = -1;
				for (int i = 0; i < L.length(); i++){
					if (!L.safeFromStar(i) || !L.passable[i] || Actor.findChar(i) != null) continue;
					if (best == -1 || L.distance(me.pos, i) < L.distance(me.pos, best)) best = i;
				}
				if (best != -1){
					CoopLog.line("bot priest runs to a stone, star in " + p.starTimer);
					CoopOrders.handleCell(best);
					return true;
				}
			}
			if (L.safeFromStar(me.pos) && !canReach){
				CoopOrders.named("rest");
				return true;
			}
			CoopOrders.handleCell(p.pos);
			return true;
		}

		//off marked cells first, siempre
		if (TargetedCell.cells.get(me.pos) != null){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = me.pos + n;
				if (L.passable[c] && Actor.findChar(c) == null && TargetedCell.cells.get(c) == null){
					CoopLog.line("bot priest dodges");
					CoopOrders.handleCell(c);
					return true;
				}
			}
		}

		//ritual totems, spirit first
		if (p.ritualUp()){
			GoblinPriest.RitualTotem[] t = p.ritualTotems();
			GoblinPriest.RitualTotem target = t[GoblinPriest.RitualTotem.SPIRIT];
			if (target == null){
				for (GoblinPriest.RitualTotem r : t){
					if (r == null) continue;
					if (target == null || L.distance(me.pos, r.pos) < L.distance(me.pos, target.pos)) target = r;
				}
			}
			if (target != null){
				CoopOrders.handleCell(target.pos);
				return true;
			}
		}
		return false;
	}
}
