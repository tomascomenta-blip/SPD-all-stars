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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the walking mountain: a glacier on legs, three snowed peaks and a frozen village on a ledge of its
//back. it pays no mind to anyone until it's hurt. then it stamps down a marked band in front of it,
//or shakes itself and an avalanche comes down its flanks all around (marked too). the village can
//be climbed; up there its blows can't reach you, but it shakes
public class WalkingMountain extends WildColossus {

	{
		size = 15;
		spriteClass = MountainSprite.class;

		HP = HT = 7500;
		defenseSkill = 0;
		EXP = 340;
		baseSpeed = 0.5f;

		minDmg = 50; maxDmg = 70; acc = 62;
		armorMin = 24; armorMax = 40;

		properties.add(Property.MINIBOSS);
	}

	private static final int STAMP = 0, AVALANCHE = 1;

	private boolean angry = false;

	@Override
	public boolean peaceful() {
		return !angry;
	}

	@Override
	protected int roam() {
		return 30;
	}

	//the head, low at the front
	@Override
	public int[][] weakSpots() {
		return new int[][]{{14, 9}, {14, 10}};
	}

	//the ledge with the village
	@Override
	public int[][] climbable() {
		return new int[][]{{6, 5, 11}, {7, 5, 11}, {8, 5, 11}};
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!angry && dmg > 0 && isAlive() && src instanceof Char){
			angry = true;
			aggro((Char) src);
			if (seen()) GLog.w(Messages.get(this, "angry"));
		}
		super.damage(dmg, src);
	}

	@Override
	protected Char chooseEnemy() {
		return angry ? super.chooseEnemy() : null;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d <= 3 && Random.Int(2) == 0){
			warn(AVALANCHE, ring(3), enemy.pos, 3f);
			return true;
		}
		if (d <= 5){
			face(enemy.pos);
			ArrayList<Integer> f = front(5, 0);
			if (f.contains(enemy.pos)){
				warn(STAMP, f, enemy.pos, 3f);
				return true;
			}
			warn(AVALANCHE, ring(3), enemy.pos, 3f);
			return true;
		}
		return false;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == STAMP ? new int[]{130, 170} : new int[]{80, 110};
	}

	@Override
	protected int dust( int kind ) {
		return kind == AVALANCHE ? Speck.BLIZZARD : Speck.ROCK;
	}

	@Override
	protected String blowSound( int kind ) {
		return Assets.Sounds.ROCKS;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		if (kind == AVALANCHE){
			knock(ch, 2);
			Buff.prolong(ch, Chill.class, 4f);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("angry", angry);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		angry = bundle.getBoolean("angry");
	}

	//0-1 idle, 2-5 walk, 6 a leg up, 7 the stamp, 8 shaking off an avalanche, 9 down, 10 dead
	public static class MountainSprite extends WildSprite {

		public MountainSprite(){
			super();
			beast(FrostAssets.MOUNTAIN, 15, 344, 368, 40, 96);
			run = loop(4, 2, 3, 4, 5);
			die = clip(4, 9, 10, 10);
		}

		@Override
		protected int ready( int kind ) {
			return kind == AVALANCHE ? 8 : 6;
		}

		@Override
		protected int blow( int kind ) {
			return kind == AVALANCHE ? 8 : 7;
		}

		@Override
		protected int[] downFrames() {
			return new int[]{9, 9, 9, 0};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(286, 240);
		}
	}
}
