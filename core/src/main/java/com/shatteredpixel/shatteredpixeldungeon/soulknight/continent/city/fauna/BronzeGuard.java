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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//bronze guard. a giant empty suit of armour still keeping a gate nobody uses. it sweeps the
//halberd in front of it, and now and then lifts the visor: inside there's a single yog eye whose
//gaze burns a straight line (both marked). broken enough, it kneels to whoever puts the collar on
public class BronzeGuard extends CityColossus {

	{
		size = 5;
		spriteClass = GuardSprite.class;

		HP = HT = 900;
		defenseSkill = 10;
		EXP = 55;
		baseSpeed = 0.7f;

		minDmg = 40; maxDmg = 55; acc = 62;
		armorMin = 20; armorMax = 36;

		properties.add(Property.INORGANIC);
	}

	private static final int HALBERD = 0, GAZE = 1;
	private static final int GAZE_RANGE = 10;

	//the slit of the visor
	@Override
	public int[][] weakSpots() {
		return new int[][]{{3, 0}};
	}

	@Override
	public boolean tameable() {
		return true;
	}

	//the armour has to be cracked open first (a third of it or less left)
	@Override
	public String tameBlock( Hero h ) {
		return HP * 3 <= HT ? null : Messages.get(this, "whole");
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d <= 3){
			ArrayList<Integer> sweep = front(enemy.pos, 3, 1);
			if (caught(sweep, enemy)){
				warn(HALBERD, sweep, enemy.pos, 2f);
				return true;
			}
		}
		if (d >= 2 && d <= GAZE_RANGE && Random.Int(2) == 0){
			ArrayList<Integer> line = gaze(enemy.pos);
			if (caught(line, enemy)){
				warn(GAZE, line, enemy.pos, 2f);
				return true;
			}
		}
		return false;
	}

	private ArrayList<Integer> gaze( int aim ){
		ArrayList<Integer> out = new ArrayList<>();
		int from = edgeToward(aim);
		Ballistica b = new Ballistica(from, aim, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		for (int c : b.path){
			if (Dungeon.level.distance(c, from) > GAZE_RANGE || Dungeon.level.solid[c]) break;
			if (!covers(c)) out.add(c);
		}
		return out;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim, Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(kind == GAZE ? Assets.Sounds.RAY : Assets.Sounds.HIT_STRONG, 1f, 0.7f);
			if (kind == GAZE) for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.RED_LIGHT), 1);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			if (kind == HALBERD){
				blast(ch, 90, 120);
			} else {
				blast(ch, 70, 100);
				if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 3f);
			}
		}
	}

	//6 halberd raised, 7 the sweep, 8 down, 9 dead, 10 visor open
	public static class GuardSprite extends Sheet {

		public GuardSprite(){
			super();
			layout(CityAssets.GUARD, 5, 112, 112, 16, 32, 8, 9);
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(60, 26);
		}

		@Override
		protected String tamedSheet() {
			return CityAssets.GUARD_TAMED;
		}

		@Override
		public void windup( int kind ) {
			play(loop(1, kind == GAZE ? 10 : 6));
		}

		@Override
		public void strike( int kind ) {
			once(kind == GAZE ? clip(6, 10, 10, 10, 0) : clip(10, 7, 7, 7, 0));
		}
	}
}
