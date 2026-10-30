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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//bone raptor. raw red muscle under plates of bone, ribs in the open air, a bone skull full of teeth
//and a sickle claw. they hunt in threes: the one that spots you calls the others, and from a few
//cells off they leap onto you (where they'll land is marked)
public class BoneRaptor extends FleshColossus {

	{
		size = 3;
		spriteClass = RaptorSprite.class;

		HP = HT = 300;
		defenseSkill = 26;
		EXP = 26;
		baseSpeed = 1.5f;

		minDmg = 30; maxDmg = 42; acc = 64;
		armorMin = 4; armorMax = 12;
	}

	private static final int LEAP = 0;

	//the bare ribs
	@Override
	public int[][] weakSpots() {
		return new int[][]{{1, 1}};
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	//when the pack was last called: one call is enough for all of them (calling a raptor makes it call)
	private float called = -100;

	@Override
	public void notice() {
		super.notice();
		if (enemy == null || Actor.now() - called < 5) return;
		called = Actor.now();
		Senses.noise(pos, Senses.FIGHT);
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof BoneRaptor && m != this && m.state != m.HUNTING && Dungeon.level.distance(m.pos, pos) <= 16){
				((BoneRaptor) m).called = called;
				m.beckon(enemy.pos);
			}
		}
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d < 2 || d > 6) return false;
		warn(LEAP, square(enemy.pos, 1), enemy.pos, 1.5f);
		return true;
	}

	private boolean leaping = false;

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		ArrayList<Char> hit = SKCombat.foesIn(this, cells);
		int from = pos;
		int to = nearestFit(fits(), aim);
		if (to != -1 && to != pos){
			leaping = true;
			face(aim);
			walkTo(to);
			leaping = false;
			if (sprite != null) sprite.move(from, pos);
		}
		strikeAnim(LEAP);
		Senses.noise(pos, Senses.CRASH);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 0.8f);
		for (Char ch : hit){
			if (ch.isAlive()) blast(ch, 60, 80);
		}
	}

	@Override
	protected void onTrample( Char ch ) {
		if (!leaping) super.onTrample(ch);
	}

	//6 crouched, 7 the claw, 8 in the air
	public static class RaptorSprite extends Sheet {

		public RaptorSprite(){
			super();
			layout(FleshAssets.RAPTOR, 3, 64, 64, 8, 16, -1, -1);
			run = loop(12, 2, 3, 4, 5);
		}

		@Override
		public void windup( int kind ) {
			play(loop(1, 6));
		}

		@Override
		public void strike( int kind ) {
			once(clip(8, 8, 7, 7, 0));
		}
	}
}
