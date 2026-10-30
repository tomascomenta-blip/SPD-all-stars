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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//island lizard. asleep it's an island: a mound of yog earth with twisted trees, rocks and a ring of
//foam, only the tip of a tail and a shut eyelid at the shore give it away. step on it and it wakes,
//shakes everyone off its back (that hurts, but doesn't kill) and the eyes on its back open
public class IslandLizard extends WildColossus {

	{
		size = 15;
		spriteClass = LizardSprite.class;

		HP = HT = 6500;
		defenseSkill = 8;
		EXP = 320;
		baseSpeed = 0.7f;

		minDmg = 50; maxDmg = 68; acc = 66;
		armorMin = 14; armorMax = 30;

		properties.add(Property.MINIBOSS);
	}

	private static final int BITE = 0, THRASH = 1;

	public boolean asleep = true;

	@Override
	public boolean solid() {
		return !asleep;
	}

	@Override
	public boolean peaceful() {
		return asleep;
	}

	@Override
	protected boolean wades() {
		return true;
	}

	//the head, at the front edge
	@Override
	public int[][] weakSpots() {
		return new int[][]{{14, 6}, {14, 7}};
	}

	//the island on its back
	@Override
	public int[][] climbable() {
		return new int[][]{{5, 3, 12}, {6, 2, 13}, {7, 2, 13}, {8, 2, 13}, {9, 3, 12}};
	}

	@Override
	protected int ears() {
		return asleep ? 0 : 4;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean act() {
		if (asleep){
			for (Hero h : Party.living()){
				if (covers(h.pos)){
					wake(h);
					return true;
				}
			}
			spend(TICK);
			return true;
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (asleep && isAlive() && src instanceof Char) wake((Char) src);
		super.damage(dmg, src);
	}

	private void wake( Char who ){
		asleep = false;
		CoopLog.line("fauna: IslandLizard wakes t=" + (int) Actor.now());
		Senses.noise(pos, Senses.STAMPEDE);
		if (sprite instanceof LizardSprite) ((LizardSprite) sprite).wake();
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
			GLog.w(Messages.get(this, "wakes"));
		}
		//whoever was on it gets thrown off
		trample(pos);
		if (who != null && who.isAlive()){
			aggro(who);
			target = who.pos;
		}
		state = HUNTING;
		spend(TICK);
	}

	@Override
	protected void onTrample( Char ch ) {
		ambush(ch, 30, 50);
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d <= 2 && Random.Int(3) == 0){
			warn(THRASH, ring(2), enemy.pos, 3f);
			return true;
		}
		if (d <= 5){
			face(enemy.pos);
			ArrayList<Integer> f = front(4, 1);
			if (f.contains(enemy.pos)){
				warn(BITE, f, enemy.pos, 3f);
				return true;
			}
		}
		return false;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == BITE ? new int[]{130, 175} : new int[]{80, 110};
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		if (kind == THRASH) knock(ch, 3);
	}

	@Override
	protected void afterBlast( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind == THRASH && seen()){
			for (int c : cells) if (Dungeon.level.heroFOV[c] && Random.Int(3) == 0) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("asleep", asleep);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		asleep = !bundle.contains("asleep") || bundle.getBoolean("asleep");
	}

	//0-1 asleep (an island), 2 waking, 3-4 idle, 5-8 walk, 9 ready, 10 bite, 11 the back wakes,
	//12 down, 13 dead
	public static class LizardSprite extends WildSprite {

		private Animation awake;

		public LizardSprite(){
			super();
			beast(IslesAssets.LIZARD, 15, 320, 320, 40, 56);
			awake = loop(2, 3, 3, 4, 4);
			idle = loop(1, 0, 1);
			run = loop(6, 5, 6, 7, 8);
			attack = clip(10, 9, 10, 10);
			zap = attack.clone();
			die = clip(4, 12, 13, 13);
			play(idle);
		}

		public void wake(){
			idle = awake;
			once(clip(4, 2, 2, 11, 11, 3));
		}

		@Override
		protected int ready( int kind ) {
			return kind == BITE ? 9 : 11;
		}

		@Override
		protected int blow( int kind ) {
			return kind == BITE ? 10 : 2;
		}

		@Override
		public void strike( int kind ) {
			once(clip(10, blow(kind), blow(kind), blow(kind), 3));
		}

		@Override
		protected int[] downFrames() {
			return new int[]{12, 12, 12, 3};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(272, 170);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof IslandLizard && !((IslandLizard) ch).asleep){
				idle = awake;
				idle();
			}
		}
	}
}
