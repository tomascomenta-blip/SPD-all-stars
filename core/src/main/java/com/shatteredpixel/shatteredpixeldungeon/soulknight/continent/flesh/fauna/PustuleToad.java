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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//pustule toad. a squat bile-green toad covered in yellow pustules, with a sac under its throat. it
//swells up and spits a gob of spores where you stand (marked), and if you get in close it bursts
//its pustules all around itself
public class PustuleToad extends FleshColossus {

	{
		size = 3;
		spriteClass = ToadSprite.class;

		HP = HT = 400;
		defenseSkill = 12;
		EXP = 28;
		baseSpeed = 0.8f;

		minDmg = 32; maxDmg = 44; acc = 56;
		armorMin = 6; armorMax = 16;
	}

	private static final int SPIT = 0, BURST = 1;
	private static final int RANGE = 7;

	//the throat sac
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 2}};
	}

	@Override
	protected int ears() {
		return 2;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d <= 1){
			warn(BURST, ring(2), enemy.pos, 1.5f);
			return true;
		}
		if (d > RANGE) return false;
		int from = edgeToward(enemy.pos);
		if (new Ballistica(from, enemy.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos != enemy.pos) return false;
		warn(SPIT, square(enemy.pos, 1), enemy.pos, 2f);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim, Senses.FIGHT);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 1.2f);
		for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.STENCH), 2);
		for (Char ch : SKCombat.foesIn(this, cells)){
			if (kind == SPIT){
				blast(ch, 50, 70);
				if (ch.isAlive()) Buff.affect(ch, Poison.class).set(6);
			} else {
				blast(ch, 40, 55);
				if (ch.isAlive()) Buff.prolong(ch, Vertigo.class, 3f);
			}
		}
	}

	//2-5 hopping, 6 swollen, 7 the spit, 8 the pustules bursting
	public static class ToadSprite extends Sheet {

		public ToadSprite(){
			super();
			layout(FleshAssets.TOAD, 3, 64, 64, 8, 16, -1, -1);
		}

		@Override
		public void windup( int kind ) {
			play(loop(1, 6));
		}

		@Override
		public void strike( int kind ) {
			once(kind == BURST ? clip(6, 8, 8, 0) : clip(10, 7, 7, 0));
		}
	}
}
