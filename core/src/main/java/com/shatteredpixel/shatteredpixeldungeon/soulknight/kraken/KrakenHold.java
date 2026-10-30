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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//hero out of action for a bit: overboard (climbs back up the stern ladder after a few turns) or
//grabbed by a hook tentacle (friends free you by hitting it). solo heroes just get held in place and
//can hit it themselves, otherwise solo = softlock
public class KrakenHold extends Buff {

	public static final int OVERBOARD = 0;
	public static final int GRABBED = 1;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public int kind = OVERBOARD;
	public int turns = 3;
	public int tentacle = -1;
	public int startHP = 0;
	public int needed = 0;
	public boolean helpless = true;

	public static void overboard( Hero h, int seaCell ){
		KrakenHold old = h.buff(KrakenHold.class);
		if (old != null) old.detach();
		KrakenHold k = new KrakenHold();
		k.kind = OVERBOARD;
		k.turns = 3;
		k.helpless = true;
		int from = h.pos;
		if (Actor.findChar(seaCell) == null){
			h.pos = seaCell;
			Actor.add(new Pushing(h, from, seaCell));
		}
		k.attachTo(h);
		KrakenVisuals.splash(seaCell);
		GLog.n(Party.active() ? Messages.get(KrakenHold.class, "overboard", Party.name(h)) : Messages.get(KrakenHold.class, "overboard_solo"));
		h.interrupt();
	}

	public static void grab( Hero h, GiantTentacle t ){
		KrakenHold k = new KrakenHold();
		k.kind = GRABBED;
		k.tentacle = t.id();
		k.startHP = t.HP;
		boolean solo = Party.living().size() <= 1;
		k.helpless = !solo;
		k.turns = solo ? 4 : 8;
		k.needed = Math.max(8, Math.round(t.HT * (solo ? 0.10f : 0.18f)));
		if (k.helpless){
			//dangling over the sea by the tentacle base
			int base = t.pos;
			int[] tries = {base - KrakenLevel.W, base + KrakenLevel.W, base - 2*KrakenLevel.W, base + 2*KrakenLevel.W};
			for (int c : tries){
				if (c >= 0 && c < Dungeon.level.length() && Actor.findChar(c) == null && !KrakenLevel.onShip(c)){
					int from = h.pos;
					h.pos = c;
					Actor.add(new Pushing(h, from, c));
					break;
				}
			}
		}
		k.attachTo(h);
		h.interrupt();
	}

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo(target)){
			if (helpless) target.paralysed++;
			else target.rooted = true;
			return true;
		}
		return false;
	}

	@Override
	public void detach() {
		if (helpless) target.paralysed = Math.max(0, target.paralysed - 1);
		else target.rooted = false;
		super.detach();
	}

	@Override
	public boolean act() {
		if (!target.isAlive()){
			detach();
			return true;
		}
		KrakenLevel L = Dungeon.level instanceof KrakenLevel ? (KrakenLevel) Dungeon.level : null;
		if (L == null){
			detach();
			return true;
		}
		if (kind == OVERBOARD){
			target.damage(Math.max(1, target.HT / 30), this);
			if (--turns <= 0){
				climbBack(L);
			}
		} else {
			Actor a = Actor.findById(tentacle);
			GiantTentacle t = a instanceof GiantTentacle && ((GiantTentacle) a).isAlive() ? (GiantTentacle) a : null;
			if (t == null){
				release(true);
				return true;
			}
			target.damage(Math.max(1, Random.NormalIntRange(3, 6) + L.assault), t);
			if (!target.isAlive()) return true;
			if (startHP - t.HP >= needed){
				GLog.p(Party.active() ? Messages.get(KrakenHold.class, "freed", Party.name((Hero) target)) : Messages.get(KrakenHold.class, "freed_solo"));
				release(true);
				return true;
			}
			if (--turns <= 0){
				//lets go: into the sea, or on deck if solo
				release(!helpless);
				return true;
			}
		}
		spend(TICK);
		return true;
	}

	public void release( boolean onDeck ){
		KrakenLevel L = Dungeon.level instanceof KrakenLevel ? (KrakenLevel) Dungeon.level : null;
		if (kind == OVERBOARD){
			if (L != null) climbBack(L);
			else detach();
			return;
		}
		detach();
		if (L == null || !target.isAlive()) return;
		if (onDeck){
			if (!L.deck(target.pos)){
				int c = L.freeDeckNear(target.pos);
				if (c != -1){
					int from = target.pos;
					target.pos = c;
					Actor.add(new Pushing(target, from, c));
					L.occupyCell(target);
				}
			}
		} else if (target instanceof Hero){
			overboard((Hero) target, target.pos);
		}
	}

	private void climbBack( KrakenLevel L ){
		detach();
		int c = L.freeDeckNear(KrakenLevel.ladderCell());
		if (c != -1){
			target.pos = c;
			if (target.sprite != null) target.sprite.place(c);
			L.occupyCell(target);
			KrakenVisuals.splash(c);
		}
		target.damage(Math.max(1, target.HT / 10), this);
		if (target instanceof Hero) GLog.w(Party.active() ? Messages.get(KrakenHold.class, "back", Party.name((Hero) target)) : Messages.get(KrakenHold.class, "back_solo"));
		Dungeon.observe();
	}

	@Override
	public int icon() {
		return kind == OVERBOARD ? BuffIndicator.WEAKNESS : BuffIndicator.ROOTS;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0.3f, 0.6f, 1f);
	}

	@Override
	public String name() {
		return Messages.get(this, kind == OVERBOARD ? "name_overboard" : "name_grabbed");
	}

	@Override
	public String desc() {
		return Messages.get(this, kind == OVERBOARD ? "desc_overboard" : "desc_grabbed", turns);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("kind", kind);
		bundle.put("turns", turns);
		bundle.put("tentacle", tentacle);
		bundle.put("start_hp", startHP);
		bundle.put("needed", needed);
		bundle.put("helpless", helpless);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		kind = bundle.getInt("kind");
		turns = bundle.getInt("turns");
		tentacle = bundle.getInt("tentacle");
		startHP = bundle.getInt("start_hp");
		needed = bundle.getInt("needed");
		helpless = bundle.getBoolean("helpless");
	}
}
