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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;

//ossuary vulture: charred down, wings of bone feathers, a bare neck of tendon and a hooked beak of
//bone. it flies over everything and smells blood from very far: whoever is badly hurt has it
//overhead soon. it drops on a marked patch and tears
public class OssuaryVulture extends WildColossus {

	{
		size = 3;
		spriteClass = VultureSprite.class;

		HP = HT = 320;
		defenseSkill = 28;
		EXP = 30;
		baseSpeed = 1.6f;
		flying = true;

		minDmg = 28; maxDmg = 40; acc = 62;
		armorMin = 2; armorMax = 10;
	}

	private static final int DROP = 0;

	//over water and bones alike, never over rock or a chasm
	@Override
	protected boolean walkable( int cell ) {
		return !Dungeon.level.solid[cell] && !Dungeon.level.pit[cell];
	}

	//the head
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 0}};
	}

	@Override
	protected int eyes() {
		return 4;
	}

	@Override
	protected boolean act() {
		if (state != HUNTING && !tamed){
			Hero prey = Senses.scent(this, 24);
			if (prey != null){
				aggro(prey);
				target = prey.pos;
			}
		}
		return super.act();
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d < 2 || d > 6) return false;
		warn(DROP, square(enemy.pos, 1), enemy.pos, 2f);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return new int[]{60, 85};
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		Buff.affect(ch, Bleeding.class).set(8);
	}

	//0-1 idle, 2-5 hopping, 6 ready, 7 the peck, 8-9 flying
	public static class VultureSprite extends WildSprite {

		public VultureSprite(){
			super();
			beast(BonesAssets.VULTURE, 3, 64, 64, 8, 16);
			run = loop(8, 8, 9);
		}
	}
}
