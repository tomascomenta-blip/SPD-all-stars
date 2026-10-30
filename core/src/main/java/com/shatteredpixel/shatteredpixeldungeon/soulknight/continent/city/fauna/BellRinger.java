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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//bell ringer. a hunched thing whose head is a bronze bell. as soon as it sees someone it tolls, and
//every big thing of the city that hears it comes; it keeps tolling while the fight goes on. right
//next to it the toll is deafening (marked)
public class BellRinger extends CityColossus {

	{
		size = 3;
		spriteClass = RingerSprite.class;

		HP = HT = 380;
		defenseSkill = 12;
		EXP = 26;
		baseSpeed = 0.8f;

		minDmg = 30; maxDmg = 40; acc = 54;
		armorMin = 10; armorMax = 20;

		properties.add(Property.INORGANIC);
	}

	private static final int TOLL = 0;
	private static final int EVERY = 15, CALLS = 30;

	private float tolled = -99;

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	public void notice() {
		super.notice();
		if (enemy != null) toll(enemy.pos);
	}

	@Override
	protected boolean act() {
		if (state == HUNTING && enemy != null && enemySeen && Actor.now() - tolled >= EVERY) toll(enemy.pos);
		return super.act();
	}

	//the bell: loud enough for the whole quarter, and the big ones within reach come to it
	private void toll( int at ){
		if (Actor.now() - tolled < 3) return;
		tolled = Actor.now();
		Senses.noise(pos, Senses.STAMPEDE);
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m == this || !(m instanceof AshColossus) || m.alignment != alignment || ((AshColossus) m).tamed) continue;
			if (m.state != m.HUNTING && Dungeon.level.distance(m.pos, pos) <= CALLS) m.beckon(at);
		}
		if (sprite instanceof RingerSprite) ((RingerSprite) sprite).toll();
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.6f);
			GLog.w(Messages.get(this, "tolls"));
		}
	}

	@Override
	protected boolean special( Char enemy ) {
		if (SKCombat.foesIn(this, ring(1)).isEmpty()) return false;
		warn(TOLL, ring(1), enemy.pos, 2f);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (sprite instanceof RingerSprite) ((RingerSprite) sprite).toll();
		Senses.noise(pos, Senses.ROAR);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.ALERT, 1f, 0.5f);
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 40, 60);
			if (ch.isAlive()) Buff.prolong(ch, Vertigo.class, 3f);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("tolled", tolled);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		tolled = bundle.contains("tolled") ? bundle.getFloat("tolled") : -99;
	}

	//8-9 tolling
	public static class RingerSprite extends Sheet {

		public RingerSprite(){
			super();
			layout(CityAssets.RINGER, 3, 64, 64, 8, 16, -1, -1);
		}

		public void toll(){
			if (curAnim == idle || curAnim == run) once(clip(8, 8, 9, 8, 9, 0));
		}
	}
}
