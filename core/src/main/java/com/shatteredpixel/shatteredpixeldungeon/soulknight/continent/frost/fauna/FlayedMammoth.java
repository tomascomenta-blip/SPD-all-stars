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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//flayed mammoth. no hide left: frozen muscle, tusks of bone, a skinned trunk, a blind eye. it sweeps
//the tusks across a marked band in front and throws whoever it caught, or lifts the trunk and
//trumpets: the cold air hits everything around it (marked) and leaves the head spinning
public class FlayedMammoth extends WildColossus {

	{
		size = 5;
		spriteClass = MammothSprite.class;

		HP = HT = 1050;
		defenseSkill = 8;
		EXP = 65;
		baseSpeed = 0.8f;

		minDmg = 46; maxDmg = 60; acc = 60;
		armorMin = 12; armorMax = 24;
	}

	private static final int TUSKS = 0, TRUMPET = 1;

	//the back
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 0}, {3, 0}};
	}

	//blind: it goes by ear
	@Override
	protected int eyes() {
		return -2;
	}

	@Override
	protected int ears() {
		return 6;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d <= 2 && Random.Int(3) == 0){
			warn(TRUMPET, ring(3), enemy.pos, 3f);
			return true;
		}
		if (d <= 3){
			face(enemy.pos);
			ArrayList<Integer> f = front(3, 1);
			if (f.contains(enemy.pos)){
				warn(TUSKS, f, enemy.pos, 2f);
				return true;
			}
		}
		return false;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == TUSKS ? new int[]{110, 145} : new int[]{50, 75};
	}

	@Override
	protected int dust( int kind ) {
		return kind == TRUMPET ? Speck.BLIZZARD : Speck.DUST;
	}

	@Override
	protected String blowSound( int kind ) {
		return kind == TRUMPET ? Assets.Sounds.CHALLENGE : Assets.Sounds.HIT_STRONG;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		if (kind == TUSKS) knock(ch, 2);
		else Buff.prolong(ch, Vertigo.class, 3f);
	}

	//0-1 idle, 2-5 walk, 6 tusks back, 7 the sweep, 8 down, 9 dead, 10 trumpets
	public static class MammothSprite extends WildSprite {

		public MammothSprite(){
			super();
			beast(FrostAssets.MAMMOTH, 5, 112, 112, 16, 32);
		}

		@Override
		protected int ready( int kind ) {
			return kind == TRUMPET ? 10 : 6;
		}

		@Override
		protected int blow( int kind ) {
			return kind == TRUMPET ? 10 : 7;
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(62, 56);
		}
	}
}
