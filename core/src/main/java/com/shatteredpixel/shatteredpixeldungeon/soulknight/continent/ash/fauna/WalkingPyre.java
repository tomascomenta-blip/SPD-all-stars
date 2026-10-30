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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.utils.PointF;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the walking pyre. the guardian of the ash no longer waits at the end of the plains: it roams the
//whole surface on its knuckles, a hill of burning logs and slag plates with a heart of sacred essence
//showing through a gap in its chest. it sees a torch from very far. its fist comes down on a 5x5
//patch (three turns of warning, and it kills whoever stays); whoever hugs its body gets stamped on
public class WalkingPyre extends AshColossus {

	{
		size = 15;
		spriteClass = PyreSprite.class;

		HP = HT = 7000;
		defenseSkill = 0;
		EXP = 320;
		baseSpeed = 0.5f;

		minDmg = 60; maxDmg = 80; acc = 66;
		armorMin = 24; armorMax = 44;

		properties.add(Property.FIERY);
		properties.add(Property.INORGANIC);
		properties.add(Property.MINIBOSS);
	}

	private static final int FIST = 0, STAMP = 1;
	private static final int FIST_REACH = 5;

	//the heart (in the gap of the chest), the near knee and both elbows
	@Override
	public int[][] weakSpots() {
		return new int[][]{{7, 9}, {2, 11}, {12, 11}, {13, 3}};
	}

	//the slag plates of the back and shoulders
	@Override
	public int[][] climbable() {
		return new int[][]{{1, 10, 11}, {2, 9, 12}, {3, 7, 13}, {4, 7, 12}, {5, 2, 12}, {6, 1, 13}, {7, 1, 12},
				{8, 1, 5}, {8, 8, 13}, {9, 1, 1}, {9, 5, 5}, {9, 9, 13}, {10, 8, 13}, {11, 9, 12}};
	}

	@Override
	protected int eyes() {
		return 4;
	}

	@Override
	protected int ears() {
		return 4;
	}

	@Override
	protected int roam() {
		return 30;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		//people right against its body: now and then it stamps all around instead
		int touching = SKCombat.foesIn(this, ring(1)).size();
		if (touching >= 2 || touching == 1 && d <= 1 && Random.Int(2) == 0){
			warn(STAMP, ring(2), enemy.pos, 2f);
			return true;
		}
		if (d > FIST_REACH) return false;
		warn(FIST, square(enemy.pos, 2), enemy.pos, 3f);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim, Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.5f);
			for (int c : cells){
				if (!Dungeon.level.heroFOV[c]) continue;
				CellEmitter.get(c).burst(kind == FIST ? FlameParticle.FACTORY : Speck.factory(Speck.DUST), 3);
			}
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			if (kind == FIST){
				blast(ch, 170, 230);
				if (ch.isAlive() && !Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 5f);
			} else {
				blast(ch, 90, 130);
				knock(ch, 3);
				if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 3f);
			}
		}
	}

	//6 the fist up, 7 smashing down, 8 back
	public static class PyreSprite extends AshColossusSprite {

		@Override
		protected int[] downFrames() {
			return new int[]{13, 13, 14, 13};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(141, 211);
		}

		public PyreSprite(){
			super();
			setup(FaunaAssets.PYRE, 15, 288, 312, 24, 64);
			run = loop(5, 2, 3, 4, 5);
		}
	}
}
