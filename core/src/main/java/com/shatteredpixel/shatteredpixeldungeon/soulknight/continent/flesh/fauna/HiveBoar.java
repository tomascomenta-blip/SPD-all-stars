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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//hive boar. a boar with bone tusks and a hive of flesh grown on its back, full of fleshy wasps. it
//charges down a marked lane, and when it shakes itself the swarm comes out and falls on everyone
//around (each spot marked first). the hive is what hurts it most
public class HiveBoar extends FleshColossus {

	{
		size = 5;
		spriteClass = BoarSprite.class;

		HP = HT = 800;
		defenseSkill = 12;
		EXP = 55;

		minDmg = 40; maxDmg = 54; acc = 60;
		armorMin = 10; armorMax = 22;
	}

	private static final int CHARGE = 0, SWARM = 1;
	private static final int CHARGE_RANGE = 8, SWARM_RANGE = 8;

	//the hive
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 0}, {2, 1}};
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d >= 3 && d <= CHARGE_RANGE && Random.Int(3) > 0){
			int[] dir = toward(enemy.pos);
			ArrayList<Integer> p = path(dir[0], dir[1], CHARGE_RANGE);
			LinkedHashSet<Integer> lane = lane(p);
			if (p.size() >= 2 && lane.contains(enemy.pos)){
				warn(CHARGE, lane, enemy.pos, 2f);
				return true;
			}
		}
		if (d > SWARM_RANGE) return false;
		LinkedHashSet<Integer> cells = new LinkedHashSet<>();
		for (Hero h : Party.living()){
			if (reach(h.pos) <= SWARM_RANGE) cells.addAll(square(h.pos, 1));
		}
		if (cells.isEmpty()) return false;
		warn(SWARM, cells, enemy.pos, 2f);
		return true;
	}

	private boolean charging = false;

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind == SWARM){
			strikeAnim(SWARM);
			Senses.noise(pos, Senses.FIGHT);
			if (seen()) Sample.INSTANCE.play(Assets.Sounds.BEE, 1f, 0.8f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(PoisonParticle.SPLASH, 3);
			for (Char ch : SKCombat.foesIn(this, cells)){
				blast(ch, 30, 45);
				if (ch.isAlive()) Buff.affect(ch, Poison.class).set(5);
			}
			return;
		}
		int[] dir = toward(aim);
		charging = true;
		int n = run(path(dir[0], dir[1], CHARGE_RANGE));
		charging = false;
		if (!isAlive()) return;
		Senses.noise(pos, Senses.CRASH);
		if (n > 0) strikeAnim(CHARGE);
	}

	@Override
	protected void onTrample( Char ch ) {
		if (!charging || !SKCombat.foes(this, ch)){
			super.onTrample(ch);
			return;
		}
		if (!firstRunOver(ch)) return;
		blast(ch, 85, 115);
		if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 2f);
	}

	//6 head down, 7 the tusks, 8 down, 9 dead, 10 the swarm out of the hive
	public static class BoarSprite extends Sheet {

		public BoarSprite(){
			super();
			layout(FleshAssets.BOAR, 5, 112, 112, 16, 32, 8, 9);
		}

		@Override
		public void windup( int kind ) {
			play(loop(1, kind == SWARM ? 10 : 6));
		}

		@Override
		public void strike( int kind ) {
			once(kind == SWARM ? clip(5, 10, 10, 0) : clip(10, 7, 7, 0));
		}
	}
}
