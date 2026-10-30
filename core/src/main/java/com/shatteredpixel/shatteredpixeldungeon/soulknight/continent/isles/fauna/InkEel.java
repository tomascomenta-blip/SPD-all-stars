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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//ink eel. a huge black eel that crawls along the shore. it rears up and throws itself down a short
//marked strip, or spits a ball of ink that bursts over a marked patch: it burns and blinds
public class InkEel extends WildColossus {

	{
		size = 5;
		spriteClass = EelSprite.class;

		HP = HT = 780;
		defenseSkill = 22;
		EXP = 55;
		baseSpeed = 1.2f;

		minDmg = 38; maxDmg = 52; acc = 64;
		armorMin = 6; armorMax = 16;
	}

	private static final int LUNGE = 0, INK = 1;

	@Override
	protected boolean wades() {
		return true;
	}

	//the gills, low at the front
	@Override
	public int[][] weakSpots() {
		return new int[][]{{4, 3}, {4, 4}};
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d >= 3 && d <= 8 && Random.Int(2) == 0){
			warn(INK, square(enemy.pos, 1), enemy.pos, 2f);
			return true;
		}
		if (d <= 5){
			warn(LUNGE, lane(enemy.pos, 5, 3), enemy.pos, 2f);
			return true;
		}
		return false;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == LUNGE ? new int[]{75, 100} : new int[]{35, 50};
	}

	@Override
	protected int dust( int kind ) {
		return kind == INK ? Speck.SMOKE : Speck.DUST;
	}

	@Override
	protected String blowSound( int kind ) {
		return kind == INK ? Assets.Sounds.GAS : Assets.Sounds.HIT_STRONG;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		if (kind == INK){
			Buff.prolong(ch, Blindness.class, 3f);
			Buff.affect(ch, Ooze.class).set(Ooze.DURATION);
		}
	}

	//0-1 idle, 2-5 crawl, 6 rears up, 7 lunges, 8 down, 9 dead, 10 spits ink
	public static class EelSprite extends WildSprite {

		public EelSprite(){
			super();
			beast(IslesAssets.EEL, 5, 112, 112, 16, 32);
		}

		@Override
		protected int ready( int kind ) {
			return 6;
		}

		@Override
		protected int blow( int kind ) {
			return kind == INK ? 10 : 7;
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(81, 90);
		}
	}
}
