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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//two bosses, same team... until one of them breathes fire on the other. then it's personal for a few turns
public class BossRivalry extends FlavourBuff {

	{
		type = buffType.NEUTRAL;
	}

	public static final float DURATION = 5f;
	//after a rivalry ends, they don't start another one at once
	public static final float CALM = 15f;
	public static final float CHANCE = 0.5f;

	public int rival = -1;

	//damage from one character to another: a boss hurt by another boss may turn on it
	public static void onDamage( Char victim, Object src ){
		Char attacker = null;
		if (src instanceof Char) attacker = (Char) src;
		else if (src instanceof Source) attacker = ((Source) src).rivalrySource();
		if (attacker == null || attacker == victim || !victim.isAlive() || !attacker.isAlive()) return;
		if (!Char.hasProp(victim, Char.Property.BOSS) || !Char.hasProp(attacker, Char.Property.BOSS)) return;
		if (victim.alignment != attacker.alignment) return;
		if (victim.buff(BossRivalry.class) != null || victim.buff(Calm.class) != null) return;
		if (Random.Float() >= CHANCE) return;
		BossRivalry r = Buff.affect(victim, BossRivalry.class, DURATION);
		r.rival = attacker.id();
		Buff.affect(victim, Calm.class, DURATION + CALM);
		if (victim.sprite != null) victim.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(BossRivalry.class, "angry"));
	}

	//the boss a mob is fighting instead of the heroes, null if none
	public static Char rivalOf( Char ch ){
		BossRivalry r = ch.buff(BossRivalry.class);
		if (r == null) return null;
		Actor a = Actor.findById(r.rival);
		if (a instanceof Char && ((Char) a).isAlive()) return (Char) a;
		r.detach();
		return null;
	}

	//abilities that are not characters themselves can still say whose they are
	public interface Source {
		Char rivalrySource();
	}

	public static class Calm extends FlavourBuff {
	}

	private static final String RIVAL = "rival";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(RIVAL, rival);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		rival = bundle.getInt(RIVAL);
	}
}
