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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;

import java.util.ArrayList;

//marrow centipede: eleven rings of red marrow, each with its own plate of bone, a bone head with
//pincers. it rears up (marked) and strikes down a narrow strip, and the pincers open wounds
public class MarrowCentipede extends WildColossus {

	{
		size = 3;
		spriteClass = CentipedeSprite.class;

		HP = HT = 340;
		defenseSkill = 26;
		EXP = 30;
		baseSpeed = 1.2f;

		minDmg = 30; maxDmg = 42; acc = 64;
		armorMin = 8; armorMax = 16;
	}

	private static final int STRIKE = 0;

	//the head
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 2}};
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d < 1 || d > 5) return false;
		ArrayList<Integer> lane = lane(enemy.pos, 5, 1);
		if (!lane.contains(enemy.pos)) return false;
		warn(STRIKE, lane, enemy.pos, 2f);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return new int[]{70, 95};
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		Buff.affect(ch, Bleeding.class).set(10);
	}

	//0-1 idle, 2-5 crawl, 6 ready, 7 the strike, 8 reared up
	public static class CentipedeSprite extends WildSprite {

		public CentipedeSprite(){
			super();
			beast(BonesAssets.CENTIPEDE, 3, 64, 64, 8, 16);
		}

		@Override
		protected int ready( int kind ) {
			return 8;
		}
	}
}
