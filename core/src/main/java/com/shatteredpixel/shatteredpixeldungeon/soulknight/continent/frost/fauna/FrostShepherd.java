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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the shepherd of the frost, guardian of the moor. a tall giant of blue grey skin under a hooded hide
//cloak, with a hooked staff of bone and his flock frozen in blocks of ice at his feet. the beasts
//of the moor that walk with him come when he calls. he plants the staff and a fan of ice spikes
//comes out of the ground (marked), or raises it and brings the blizzard: patches of it fall on
//every hero he sees and around them, all marked
public class FrostShepherd extends WildColossus {

	{
		size = 15;
		spriteClass = ShepherdSprite.class;

		HP = HT = 9000;
		defenseSkill = 10;
		EXP = 500;
		baseSpeed = 0.7f;

		minDmg = 55; maxDmg = 75; acc = 74;
		armorMin = 18; armorMax = 34;

		territory = 18;

		properties.add(Property.MINIBOSS);
	}

	private static final int SPIKES = 0, BLIZZARD = 1;

	private boolean called = false;

	//the hand on the staff (front edge) and the head under the hood (climb him)
	@Override
	public int[][] weakSpots() {
		return new int[][]{{14, 10}, {14, 11}, {7, 2}};
	}

	//the cloak on his back
	@Override
	public int[][] climbable() {
		return new int[][]{{4, 4, 10}, {5, 4, 10}, {6, 4, 10}, {7, 4, 10}};
	}

	@Override
	protected int eyes() {
		return 6;
	}

	@Override
	protected int ears() {
		return 4;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	//the first time he sees someone, the flock that walks with him comes
	@Override
	public void notice() {
		super.notice();
		if (called || enemy == null) return;
		called = true;
		Senses.noise(pos, Senses.ROAR);
		int n = 0;
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof SKMob && ((SKMob) m).leader == id() && m.isAlive() && m.alignment == alignment){
				m.beckon(enemy.pos);
				n++;
			}
		}
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.6f);
			GLog.w(Messages.get(this, n > 0 ? "calls" : "sees"));
		}
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d > 14) return false;
		if (d >= 4 && Random.Int(3) == 0){
			warn(BLIZZARD, blizzard(), enemy.pos, 3f);
			return true;
		}
		if (d <= 9){
			ArrayList<Integer> c = cone(enemy.pos, 9);
			if (c.contains(enemy.pos)){
				warn(SPIKES, c, enemy.pos, 3f);
				return true;
			}
		}
		warn(BLIZZARD, blizzard(), enemy.pos, 3f);
		return true;
	}

	//a 3x3 patch on every hero in sight, and a few more about
	private ArrayList<Integer> blizzard(){
		ArrayList<Integer> out = new ArrayList<>();
		Level l = Dungeon.level;
		int w = l.width();
		for (Hero h : Party.living()){
			if (reach(h.pos) > 16) continue;
			for (int c : square(h.pos, 1)) if (!out.contains(c)) out.add(c);
		}
		for (int i = 0; i < 6; i++){
			int x = pos % w + Random.IntRange(-12, 12), y = pos / w + Random.IntRange(-12, 12);
			if (x < 1 || y < 1 || x >= w - 1 || y >= l.height() - 1) continue;
			for (int c : square(x + y * w, 1)) if (!out.contains(c)) out.add(c);
		}
		return out;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == SPIKES ? new int[]{125, 165} : new int[]{65, 90};
	}

	@Override
	protected int dust( int kind ) {
		return kind == BLIZZARD ? Speck.BLIZZARD : Speck.ROCK;
	}

	@Override
	protected String blowSound( int kind ) {
		return kind == BLIZZARD ? Assets.Sounds.GAS : Assets.Sounds.SHATTER;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		Buff.prolong(ch, Chill.class, kind == BLIZZARD ? 6f : 3f);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("called", called);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		called = bundle.getBoolean("called");
	}

	//0-1 idle, 2-5 walk, 6 staff up, 7 spikes, 8 blizzard, 9 on his knees, 10 frozen dead
	public static class ShepherdSprite extends WildSprite {

		public ShepherdSprite(){
			super();
			beast(FrostAssets.SHEPHERD, 15, 304, 400, 32, 112);
			die = clip(4, 9, 10, 10);
		}

		@Override
		protected int ready( int kind ) {
			return 6;
		}

		@Override
		protected int blow( int kind ) {
			return kind == BLIZZARD ? 8 : 7;
		}

		@Override
		protected int[] downFrames() {
			return new int[]{9, 9, 9, 0};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(248, 276);
		}
	}
}
