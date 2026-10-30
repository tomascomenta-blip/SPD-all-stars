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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//brass raptor. a running thing of brass plates and yog flesh, in packs of two or three: when one
//finds someone the pack comes, and they leap on the prey from a few cells off (the landing marked)
public class BrassRaptor extends CityColossus {

	{
		size = 3;
		spriteClass = RaptorSprite.class;

		HP = HT = 360;
		defenseSkill = 22;
		EXP = 26;
		baseSpeed = 1.25f;

		minDmg = 32; maxDmg = 44; acc = 62;
		armorMin = 8; armorMax = 18;
	}

	private static final int LEAP_MIN = 3, LEAP_MAX = 6;

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected int roam() {
		return 24;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	@Override
	public void notice() {
		super.notice();
		if (enemy == null) return;
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof BrassRaptor && m != this && m.state != m.HUNTING && Dungeon.level.distance(m.pos, pos) <= 16){
				m.beckon(enemy.pos);
			}
		}
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d < LEAP_MIN || d > LEAP_MAX) return false;
		warn(0, square(enemy.pos, 1), enemy.pos, 2f);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		//lands as close to the marked spot as it fits
		int spot = nearestFit(fits(), aim);
		if (spot != -1 && spot != pos && Dungeon.level.distance(spot, aim) <= 2){
			int from = pos;
			walkTo(spot);
			if (sprite != null) sprite.move(from, spot);
		}
		strikeAnim(kind);
		Senses.noise(aim, Senses.FIGHT);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.8f);
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 70, 95);
		}
	}

	//8 in the air
	public static class RaptorSprite extends Sheet {

		public RaptorSprite(){
			super();
			layout(CityAssets.RAPTOR, 3, 64, 64, 8, 16, -1, -1);
		}

		@Override
		public void windup( int kind ) {
			play(loop(1, 6));
		}

		@Override
		public void strike( int kind ) {
			once(clip(8, 8, 8, 7, 0));
		}
	}
}
