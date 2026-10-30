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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//bone chimera: skeletons of different beasts bound with dry tendon, a horned skull with one light in
//the socket and a smaller one on a neck of vertebrae, each leg from another animal. the big skull
//bites down a marked band in front; when both roar, the air around it shakes (marked) and the head
//spins
public class BoneChimera extends WildColossus {

	{
		size = 5;
		spriteClass = ChimeraSprite.class;

		HP = HT = 900;
		defenseSkill = 16;
		EXP = 60;
		baseSpeed = 1.1f;

		minDmg = 42; maxDmg = 56; acc = 64;
		armorMin = 10; armorMax = 20;

		properties.add(Property.UNDEAD);
	}

	private static final int BITE = 0, ROAR = 1;

	//the gap in the ribs, from below
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 4}, {3, 4}};
	}

	@Override
	protected int eyes() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d <= 2 && Random.Int(3) == 0){
			warn(ROAR, ring(3), enemy.pos, 3f);
			return true;
		}
		if (d <= 3){
			face(enemy.pos);
			ArrayList<Integer> f = front(3, 1);
			if (f.contains(enemy.pos)){
				warn(BITE, f, enemy.pos, 2f);
				return true;
			}
		}
		return false;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == BITE ? new int[]{100, 130} : new int[]{55, 75};
	}

	@Override
	protected int dust( int kind ) {
		return kind == ROAR ? Speck.SCREAM : Speck.BONE;
	}

	@Override
	protected String blowSound( int kind ) {
		return kind == ROAR ? Assets.Sounds.CHALLENGE : Assets.Sounds.HIT_CRUSH;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		if (kind == ROAR) Buff.prolong(ch, Vertigo.class, 3f);
	}

	//0-1 idle, 2-5 walk, 6 ready, 7 the bite, 8 down, 9 dead, 10 both skulls roaring
	public static class ChimeraSprite extends WildSprite {

		public ChimeraSprite(){
			super();
			beast(BonesAssets.CHIMERA, 5, 112, 112, 16, 32);
		}

		@Override
		protected int ready( int kind ) {
			return kind == ROAR ? 10 : 6;
		}

		@Override
		protected int blow( int kind ) {
			return kind == ROAR ? 10 : 7;
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(56, 66);
		}
	}
}
