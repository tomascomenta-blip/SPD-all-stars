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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//rib digger: a huge blind mole that lives inside the skeletons and wears an old colossus's ribcage
//for a shell. it rakes with spade claws of bone (marked, in front) and digs: old bones fly out and
//come down on a few marked patches round whoever it heard
public class RibDigger extends WildColossus {

	{
		size = 5;
		spriteClass = DiggerSprite.class;

		HP = HT = 940;
		defenseSkill = 10;
		EXP = 58;

		minDmg = 40; maxDmg = 54; acc = 60;
		armorMin = 16; armorMax = 28;
	}

	private static final int CLAWS = 0, BONES = 1;

	//the gap under the ribcage
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 4}, {3, 4}};
	}

	//blind, it digs by ear
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
		if (d <= 2){
			face(enemy.pos);
			ArrayList<Integer> f = front(2, 1);
			if (f.contains(enemy.pos)){
				warn(CLAWS, f, enemy.pos, 2f);
				return true;
			}
		}
		if (d > 9) return false;
		ArrayList<Integer> cells = square(enemy.pos, 1);
		int w = Dungeon.level.width();
		for (int i = 0; i < 2; i++){
			int c = enemy.pos + Random.IntRange(-3, 3) + Random.IntRange(-3, 3) * w;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			for (int k : square(c, 1)) if (!cells.contains(k)) cells.add(k);
		}
		warn(BONES, cells, enemy.pos, 2f);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == CLAWS ? new int[]{90, 120} : new int[]{50, 70};
	}

	@Override
	protected int dust( int kind ) {
		return kind == BONES ? Speck.BONE : Speck.DUST;
	}

	@Override
	protected String blowSound( int kind ) {
		return kind == BONES ? Assets.Sounds.BONES : Assets.Sounds.HIT_STRONG;
	}

	//0-1 idle, 2-5 walk, 6 claws up, 7 the rake, 8 down, 9 dead, 10 digging and throwing bones
	public static class DiggerSprite extends WildSprite {

		public DiggerSprite(){
			super();
			beast(BonesAssets.DIGGER, 5, 112, 112, 16, 32);
		}

		@Override
		protected int ready( int kind ) {
			return kind == BONES ? 10 : 6;
		}

		@Override
		protected int blow( int kind ) {
			return kind == BONES ? 10 : 7;
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(54, 60);
		}
	}
}
