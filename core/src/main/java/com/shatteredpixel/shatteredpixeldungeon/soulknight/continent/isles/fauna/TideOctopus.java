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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Yog;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//tide octopus. a tall wet mantle the colour of ink with plates of yog flesh that glow, eight arms
//spread over the ground. it lashes two arms down a marked strip, and when it's pressed it throws
//a cloud of ink over a wide patch: it stings and leaves you blind
public class TideOctopus extends WildColossus implements Yog {

	{
		size = 10;
		spriteClass = OctopusSprite.class;

		HP = HT = 3800;
		defenseSkill = 16;
		EXP = 180;
		baseSpeed = 0.8f;

		minDmg = 46; maxDmg = 62; acc = 68;
		armorMin = 10; armorMax = 22;
	}

	private static final int LASH = 0, INK = 1;

	@Override
	public int taintOnHit() {
		return 2;
	}

	@Override
	protected boolean wades() {
		return true;
	}

	//the eyes, under the mantle
	@Override
	public int[][] weakSpots() {
		return new int[][]{{6, 4}, {5, 4}};
	}

	@Override
	public int[][] climbable() {
		return new int[][]{{2, 3, 6}, {3, 3, 6}, {4, 3, 6}};
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d > 9) return false;
		if (d >= 3 && Random.Int(3) == 0){
			warn(INK, square(enemy.pos, 3), enemy.pos, 3f);
			return true;
		}
		warn(LASH, lane(enemy.pos, 9, 3), enemy.pos, 2f);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == LASH ? new int[]{105, 140} : new int[]{40, 60};
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
		if (kind == INK) Buff.prolong(ch, Blindness.class, 4f);
		else Buff.prolong(ch, Roots.class, 1f);
	}

	//0-1 idle, 2-5 crawl, 6 arms up, 7 the lash, 8 ink, 9 down, 10 dead
	public static class OctopusSprite extends WildSprite {

		public OctopusSprite(){
			super();
			beast(IslesAssets.OCTOPUS, 10, 272, 256, 56, 56);
			die = clip(5, 9, 10, 10);
		}

		@Override
		protected int ready( int kind ) {
			return kind == INK ? 8 : 6;
		}

		@Override
		protected int blow( int kind ) {
			return kind == INK ? 8 : 7;
		}

		@Override
		protected int[] downFrames() {
			return new int[]{9, 9, 9, 0};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(162, 130);
		}
	}
}
