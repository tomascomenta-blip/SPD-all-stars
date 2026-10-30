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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.watabou.utils.PathFinder;

//CoopAuto bots for the kraken ship. patch, steer, carry harpoons, hit the eye, else fight the tentacle
//dumb as rocks but good enough to test the fight
public class KrakenBot {

	public static boolean act( Hero me ){
		if (!(Dungeon.level instanceof KrakenLevel)) return false;
		KrakenLevel L = (KrakenLevel) Dungeon.level;

		if (L.stage == KrakenLevel.WON){
			for (Heap h : L.heaps.valueList()){
				if (h.pos != me.pos && L.distance(me.pos, h.pos) <= 8){
					CoopOrders.handleCell(h.pos);
					return true;
				}
			}
			CoopLog.line("bot kraken leaves");
			CoopOrders.handleCell(KrakenLevel.bowsprit());
			return true;
		}

		//low hp, chug
		if (me.HP < me.HT / 3){
			PotionOfHealing p = me.belongings.getItem(PotionOfHealing.class);
			if (p != null){
				CoopOrders.execute(p, "DRINK");
				return true;
			}
		}

		//get off the marked cell
		if (TargetedCell.cells.get(me.pos) != null && me.buff(KrakenHold.class) == null){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = me.pos + n;
				if (L.deck(c) && Actor.findChar(c) == null
						&& TargetedCell.cells.get(c) == null){
					CoopLog.line("bot kraken dodges");
					CoopOrders.handleCell(c);
					return true;
				}
			}
		}

		ShipHammer hammer = me.belongings.getItem(ShipHammer.class);
		boolean urgent = L.holes.size() >= 2 || L.shipHP < L.shipHT * 0.6f;
		if (hammer != null && L.holeNear(me.pos, 1) != -1 && (urgent || !L.holes.isEmpty())){
			CoopLog.line("bot kraken repairs, ship=" + L.shipHP + " holes=" + L.holes.size());
			CoopOrders.execute(hammer, ShipHammer.AC_REPAIR);
			return true;
		}

		Kraken k = Kraken.find();
		if (k != null && k.vulnerable()){
			CoopLog.line("bot kraken goes for the eye");
			CoopOrders.handleCell(k.pos);
			return true;
		}

		//hero 0 repairs, 1 steers, rest fight
		int idx = Party.active() ? Party.indexOf(me) : 0;
		int repairer = 0;
		if (hammer != null && urgent && (idx == repairer || L.holes.size() >= 4)){
			int hole = L.holeNear(me.pos, 30);
			if (hole != -1){
				CoopOrders.handleCell(hole);
				return true;
			}
		}

		if (L.stage == KrakenLevel.SAILING && !Kraken.helmManned() && (idx == 1 || Party.living().size() == 1)){
			if (me.pos != KrakenLevel.helmsmanCell()){
				CoopOrders.handleCell(KrakenLevel.helmsmanCell());
			} else {
				CoopOrders.named("rest");
			}
			return true;
		}
		if (me.pos == KrakenLevel.helmsmanCell() && L.stage == KrakenLevel.SAILING){
			CoopOrders.named("rest");
			return true;
		}

		//adjacent boarder first, then tentacle
		for (Mob m : L.mobs){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && m.invisible == 0 && !(m instanceof GiantTentacle.Segment)
					&& !(m instanceof GiantTentacle) && !(m instanceof Kraken) && L.adjacent(me.pos, m.pos)){
				CoopOrders.handleCell(m.pos);
				return true;
			}
		}
		GiantTentacle lying = k == null ? null : k.activeTentacle();
		if (lying != null){
			int target = lying.armoured() ? lying.segmentCell(lying.sucker) : -1;
			if (target == -1){
				for (int id : lying.segments){
					Actor a = Actor.findById(id);
					if (a instanceof Char && (target == -1 || L.distance(me.pos, ((Char) a).pos) < L.distance(me.pos, target))) target = ((Char) a).pos;
				}
			}
			if (target != -1 && !(me.belongings.getItem(KrakenHarpoon.class) != null && L.distance(me.pos, KrakenLevel.cannonCell()) <= 3)){
				CoopOrders.handleCell(target);
				return true;
			}
		}

		if (me.belongings.getItem(KrakenHarpoon.class) != null && (k == null || k.activeTentacle() != null || k.vulnerable() || Kraken.guardsAlive())){
			CoopOrders.handleCell(KrakenLevel.cannonCell());
			return true;
		}
		for (Heap h : L.heaps.valueList()){
			for (Item i : h.items){
				if (i instanceof KrakenHarpoon && L.distance(me.pos, h.pos) <= 5){
					CoopOrders.handleCell(h.pos);
					return true;
				}
			}
		}

		//armored one: hit the glowy sucker
		GiantTentacle gt = k == null ? null : k.activeTentacle();
		if (gt != null && gt.armoured()){
			int c = gt.segmentCell(gt.sucker);
			if (c != -1){
				CoopOrders.handleCell(c);
				return true;
			}
		}

		//lo más cercano on the deck
		Mob best = null;
		for (Mob m : L.mobs){
			if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || m instanceof Kraken || m.invisible > 0) continue;
			if (m instanceof GiantTentacle) continue;
			if (best == null || L.distance(me.pos, m.pos) < L.distance(me.pos, best.pos)) best = m;
		}
		if (best != null){
			CoopOrders.handleCell(best.pos);
			return true;
		}

		//idle: patch holes or hang near the bow
		if (hammer != null && !L.holes.isEmpty()){
			CoopOrders.handleCell(L.holeNear(me.pos, 30));
			return true;
		}
		if (L.stage == KrakenLevel.SAILING || L.stage == KrakenLevel.ARRIVED){
			int c = KrakenLevel.cell(5, 2);
			if (me.pos != c && L.distance(me.pos, c) > 2){
				CoopOrders.handleCell(c);
				return true;
			}
		}
		CoopOrders.named("rest");
		return true;
	}
}
