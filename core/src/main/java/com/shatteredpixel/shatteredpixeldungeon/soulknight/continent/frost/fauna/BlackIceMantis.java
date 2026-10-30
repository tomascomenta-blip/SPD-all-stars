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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;

import java.util.ArrayList;

//black ice mantis. facets of black ice with white edges, four glass legs and two toothed scythes.
//it doesn't wander: it waits still with the scythes up, and whoever comes close gets them brought
//down across a marked band in front of it
public class BlackIceMantis extends WildColossus {

	{
		size = 3;
		spriteClass = MantisSprite.class;

		HP = HT = 380;
		defenseSkill = 24;
		EXP = 32;

		minDmg = 34; maxDmg = 46; acc = 66;
		armorMin = 8; armorMax = 16;
	}

	private static final int SCYTHES = 0;

	//the head
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 0}};
	}

	@Override
	protected int roam() {
		return 0;
	}

	@Override
	protected int eyes() {
		return 2;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (reach(enemy.pos) > 2) return false;
		face(enemy.pos);
		ArrayList<Integer> f = front(2, 1);
		if (!f.contains(enemy.pos)) return false;
		warn(SCYTHES, f, enemy.pos, 2f);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return new int[]{80, 110};
	}

	@Override
	protected String blowSound( int kind ) {
		return Assets.Sounds.HIT_SLASH;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		Buff.affect(ch, Bleeding.class).set(10);
	}

	//0-1 idle, 2-5 walk, 6 scythes up, 7 the cut, 8 lying in wait
	public static class MantisSprite extends WildSprite {

		public MantisSprite(){
			super();
			beast(FrostAssets.MANTIS, 3, 64, 64, 8, 16);
			idle = loop(1, 8, 8, 8, 0);
			play(idle);
		}
	}
}
