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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WebParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;

import java.util.ArrayList;

//ash tarantula. black chitin, hairs tipped with embers and an egg sac glowing under it. it lies in
//wait in the low ground and throws sheets of hardened ash over whoever comes close (marked first),
//then bites what it caught. it can be tamed by paying it sacred essence
public class AshTarantula extends AshColossus {

	{
		size = 8;
		spriteClass = TarantulaSprite.class;

		HP = HT = 2600;
		defenseSkill = 14;
		EXP = 120;
		baseSpeed = 0.8f;

		minDmg = 45; maxDmg = 60; acc = 70;
		armorMin = 14; armorMax = 28;

		territory = 14;
	}

	private static final int WEB = 0, POUNCE = 1;

	//the egg sac under the abdomen
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 5}};
	}

	@Override
	public int[][] climbable() {
		return new int[][]{{1, 1, 3}, {2, 1, 3}, {3, 1, 3}};
	}

	@Override
	public boolean tameable() {
		return true;
	}

	@Override
	public String tameBlock( Hero h ) {
		return Continent.essence >= 1 ? null : Messages.get(this, "price");
	}

	@Override
	public void tame( Hero h ) {
		Continent.essence--;
		super.tame(h);
	}

	@Override
	protected int ears() {
		return 4;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d > 6) return false;
		if (d <= 2){
			warn(POUNCE, ring(2), enemy.pos, 2f);
		} else {
			warn(WEB, square(enemy.pos, 2), enemy.pos, 2f);
		}
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim, Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(kind == WEB ? Assets.Sounds.MISS : Assets.Sounds.HIT_STRONG);
			if (kind == WEB) for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(WebParticle.FACTORY, 3);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			if (kind == WEB){
				blast(ch, 20, 35);
				if (ch.isAlive()) Buff.prolong(ch, Roots.class, 4f);
			} else {
				blast(ch, 110, 150);
			}
		}
	}

	//0-1 idle, 2-5 walk, 6 rears up, 7 bite, 8 web, 9 down, 10 dead; then the extra sheet:
	//11 weak spot, 12-13 down, 14-15 climbed, 16-17 tamed
	public static class TarantulaSprite extends AshColossusSprite {

		public TarantulaSprite(){
			super();
			setup(FaunaAssets.TARANTULA, 8, 208, 192, 40, 48);
			idle = loop(2, 0, 1);
			run = loop(6, 2, 3, 4, 5);
			attack = clip(10, 6, 7, 7);
			die = clip(6, 9, 10);
			play(idle);
		}

		@Override
		public void windup( int kind ) {
			play(loop(1, kind == WEB ? 8 : 6));
		}

		@Override
		public void strike( int kind ) {
			once(kind == WEB ? clip(8, 8, 8, 0) : clip(10, 7, 7, 0));
		}

		@Override
		protected int[] downFrames() {
			return new int[]{12, 12, 13, 1};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(72, 124);
		}

		@Override
		protected int[] tamedIdle() {
			return new int[]{16, 17};
		}
	}
}
