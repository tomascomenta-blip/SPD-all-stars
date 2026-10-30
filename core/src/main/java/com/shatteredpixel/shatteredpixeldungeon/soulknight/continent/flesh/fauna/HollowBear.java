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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//hollow bear. a grey bear with empty sockets and nothing inside: its chest is a black hole crossed
//by ribs, with eyes looking out of the dark. it swipes at what's in front (marked), and when it
//opens up, pale hands push out of the hole and what lives in there crawls out
public class HollowBear extends FleshColossus {

	{
		size = 5;
		spriteClass = BearSprite.class;

		HP = HT = 950;
		defenseSkill = 6;
		EXP = 55;
		baseSpeed = 0.8f;

		minDmg = 44; maxDmg = 58; acc = 60;
		armorMin = 12; armorMax = 26;
	}

	private static final int SWIPE = 0, OPEN = 1;

	//what's still inside
	private int inside = 6;

	//the hole in its chest
	@Override
	public int[][] weakSpots() {
		return new int[][]{{4, 2}, {4, 3}};
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
		if (inside > 0 && d <= 6 && Random.Int(3) == 0){
			open();
			return true;
		}
		if (d > 2) return false;
		ArrayList<Integer> f = front(enemy.pos, 2, 1);
		if (!f.contains(enemy.pos)) return false;
		warn(SWIPE, f, enemy.pos, 1.5f);
		return true;
	}

	//no mark: what comes out is small, it has to get to you first
	private void open(){
		if (sprite instanceof BearSprite) ((BearSprite) sprite).open();
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.BONES, 1f, 0.6f);
			GLog.w(Messages.get(this, "open"));
		}
		int n = Math.min(inside, 2);
		inside -= n;
		int at = besideToward(enemy != null ? enemy.pos : pos);
		for (int i = 0; i < n && at != -1; i++){
			Mob m = Random.Int(3) == 0 ? new FlayedFox() : new BarkLouse();
			letOut(m, at);
		}
		spend(TICK);
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim, Senses.CRASH);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.6f);
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 90, 120);
			knock(ch, 2);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("inside", inside);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		inside = bundle.contains("inside") ? bundle.getInt("inside") : 6;
	}

	//6 rearing, 7 the swipe, 8 down, 9 dead, 10 the chest opening, hands out, 11-12 the hands on their way
	//out (and back in), so they never pop up all at once
	public static class BearSprite extends Sheet {

		public BearSprite(){
			super();
			layout(FleshAssets.BEAR, 5, 112, 112, 16, 32, 8, 9);
		}

		public void open(){
			once(clip(6, 11, 12, 10, 10, 10, 10, 12, 11, 0));
		}

		@Override
		public void strike( int kind ) {
			once(clip(10, 7, 7, 7, 12, 11, 0));
		}
	}
}
