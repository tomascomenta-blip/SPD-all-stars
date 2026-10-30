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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.watabou.utils.PointF;

import java.util.ArrayList;

//frost raptor. a big biped of frosted grey down, a crest of ice shards and a sickle claw. they hunt
//in packs of two or three: when one sees you they all come, and each one leaps onto a marked patch
//where you stand and tears at whatever is still there
public class FrostRaptor extends WildColossus {

	{
		size = 5;
		spriteClass = RaptorSprite.class;

		HP = HT = 700;
		defenseSkill = 26;
		EXP = 50;
		baseSpeed = 1.3f;

		minDmg = 38; maxDmg = 50; acc = 66;
		armorMin = 6; armorMax = 14;
	}

	private static final int LEAP = 0;

	//the base of the crest
	@Override
	public int[][] weakSpots() {
		return new int[][]{{3, 0}, {4, 0}};
	}

	@Override
	protected int eyes() {
		return 3;
	}

	@Override
	protected int ears() {
		return 3;
	}

	//when the pack was last called: one call is enough for all of them (calling a raptor makes it call)
	private float called = -100;

	@Override
	public void notice() {
		super.notice();
		if (enemy == null || Actor.now() - called < 5) return;
		called = Actor.now();
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof FrostRaptor && m != this && m.state != m.HUNTING && Dungeon.level.distance(m.pos, pos) <= 16){
				((FrostRaptor) m).called = called;
				m.beckon(enemy.pos);
			}
		}
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d < 2 || d > 6) return false;
		warn(LEAP, square(enemy.pos, 1), enemy.pos, 2f);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return new int[]{85, 115};
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		Buff.affect(ch, Bleeding.class).set(12);
	}

	//it lands by the patch
	@Override
	protected void afterBlast( int kind, ArrayList<Integer> cells, int aim ) {
		boolean[] f = fits();
		int spot = nearestFit(f, aim);
		if (spot == -1 || spot == pos || Dungeon.level.distance(spot, pos) > 7) return;
		int old = pos;
		walkTo(spot);
		if (sprite != null) sprite.move(old, pos);
	}

	//0-1 idle, 2-5 run, 6 crouches, 7 the claw, 8 down, 9 dead, 10 leaping
	public static class RaptorSprite extends WildSprite {

		public RaptorSprite(){
			super();
			beast(FrostAssets.RAPTOR, 5, 112, 112, 16, 32);
		}

		@Override
		protected int blow( int kind ) {
			return 10;
		}

		@Override
		public void strike( int kind ) {
			once(clip(10, 10, 10, 7, 0));
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(62, 58);
		}
	}
}
