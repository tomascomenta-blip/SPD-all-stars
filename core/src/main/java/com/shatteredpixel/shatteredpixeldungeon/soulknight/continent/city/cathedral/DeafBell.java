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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.CityCritter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//deaf bell. a bronze bell grown round a tongue of yog flesh, hanging in the tower and over the
//crossing. it can't move and doesn't bite; when it sees someone it swings and tolls, and the toll
//rings out from two cells away to six (marked): whoever it catches is dazed, hitting and dodging
//half as well, and the faithful stir. right under the bell is the only place it isn't heard
public class DeafBell extends CityCritter {

	{
		spriteClass = BellSprite.class;

		HP = HT = 140;
		defenseSkill = 0;
		EXP = 14;

		minDmg = 1; maxDmg = 2; acc = 0;
		armorMin = 10; armorMax = 16;

		properties.add(Property.INORGANIC);
		properties.add(Property.IMMOVABLE);

		state = WANDERING;
	}

	private static final int TOLL = 0;
	private static final int NEAR = 2, FAR = 6;

	@Override
	protected int eyes() {
		return 4;
	}

	@Override
	protected int specialCooldown() {
		return 5;
	}

	@Override
	protected boolean getCloser( int target ) {
		return false;
	}

	@Override
	protected boolean getFurther( int target ) {
		return false;
	}

	//no clapper on the outside: it doesn't fight what stands under it
	@Override
	protected boolean canAttack( Char enemy ) {
		return false;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (Dungeon.level.distance(pos, enemy.pos) > FAR) return false;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : SKCombat.openArea(pos, FAR)){
			if (Dungeon.level.distance(pos, c) >= NEAR && !Dungeon.level.pit[c]) cells.add(c);
		}
		if (cells.isEmpty()) return false;
		announce(TOLL, cells, enemy.pos);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (sprite instanceof BellSprite) ((BellSprite) sprite).toll();
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.4f);
			CellEmitter.center(pos).burst(Speck.factory(Speck.SCREAM), 3);
		}
		Senses.noise(pos, Senses.CRASH);
		boolean local = false;
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 18, 28);
			if (!ch.isAlive()) continue;
			Buff.prolong(ch, Daze.class, 6f);
			if (ch == Dungeon.local()) local = true;
		}
		if (local) GLog.w(Messages.get(this, "deaf"));
		if (Dungeon.level instanceof CathedralLevel) CathedralPlan.stir((CathedralLevel) Dungeon.level, 1);
	}

	//24x32, hung from above: 0-1 swaying, 2-5 swinging, 6 drawn back, 7 the toll, 8-10 falling apart
	public static class BellSprite extends ContinentMobSprite {

		public BellSprite(){
			super();
			setup(CathedralAssets.BELL, 24, 32);
			idle = loop(2, 0, 0, 1, 1);
			run = loop(6, 2, 3, 4, 5);
			attack = new Animation(10, false);
			attack.frames(frames, 6, 7, 7);
			zap = new Animation(4, false);
			zap.frames(frames, 2, 3, 6, 6);
			die = new Animation(6, false);
			die.frames(frames, 8, 9, 10);
			play(idle);
		}

		//with a callback: the plain attack anim would make it hit AGAIN when it ends
		void toll(){
			if (ch == null) return;
			attack(ch.pos, new Callback() {
				@Override
				public void call() {
					idle();
				}
			});
		}
	}
}
