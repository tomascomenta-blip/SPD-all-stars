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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//living carriage. a coach of flesh with the horses grown into the shafts. it rolls up and down the
//streets and, when it sees someone, lines up and runs them over down a marked lane
public class LivingCarriage extends CityColossus {

	{
		size = 5;
		spriteClass = CarriageSprite.class;

		HP = HT = 760;
		defenseSkill = 12;
		EXP = 50;

		minDmg = 38; maxDmg = 50; acc = 58;
		armorMin = 12; armorMax = 26;
	}

	private static final int RANGE = 11;

	private boolean charging = false;

	//where the necks of the horses go into the coach
	@Override
	public int[][] weakSpots() {
		return new int[][]{{4, 2}};
	}

	@Override
	protected int roam() {
		return 26;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d < 2 || d > RANGE) return false;
		int[] dir = toward(enemy.pos);
		ArrayList<Integer> p = path(dir[0], dir[1], RANGE);
		LinkedHashSet<Integer> lane = lane(p);
		if (p.size() < 3 || !lane.contains(enemy.pos)) return false;
		warn(0, lane, enemy.pos, 3f);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		int[] dir = toward(aim);
		charging = true;
		run(path(dir[0], dir[1], RANGE));
		charging = false;
		if (!isAlive()) return;
		Senses.noise(pos, Senses.CRASH);
		strikeAnim(kind);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1f, 0.6f);
	}

	@Override
	protected void onTrample( Char ch ) {
		if (!charging || !SKCombat.foes(this, ch)){
			super.onTrample(ch);
			return;
		}
		if (!firstRunOver(ch)) return;
		blast(ch, 90, 120);
		if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
	}

	//6 the horses rear, 7 lunging, 8 overturned, 9 dead, 10 at full gallop
	public static class CarriageSprite extends Sheet {

		public CarriageSprite(){
			super();
			layout(CityAssets.CARRIAGE, 5, 112, 112, 16, 32, 8, 9);
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(90, 64);
		}

		@Override
		public void windup( int kind ) {
			play(loop(3, 6, 10));
		}

		@Override
		public void strike( int kind ) {
			once(clip(8, 10, 10, 7, 0));
		}
	}
}
