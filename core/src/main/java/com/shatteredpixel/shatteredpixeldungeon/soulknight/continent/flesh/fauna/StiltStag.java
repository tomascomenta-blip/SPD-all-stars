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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;

//stilt stag. a starved stag on legs like stilts, a bare skull with bone antlers. it only moves when
//no one is looking: while a hero has it in sight it stands mid-step and turns its face to them. look
//away (or let the dark take it) and it's somewhere else, closer. its kick comes without warning, but
//it never finishes anyone off
public class StiltStag extends FleshColossus {

	{
		size = 3;
		spriteClass = StagSprite.class;

		HP = HT = 380;
		defenseSkill = 22;
		EXP = 30;
		baseSpeed = 2f;

		minDmg = 40; maxDmg = 56; acc = 70;
		armorMin = 6; armorMax = 14;
	}

	//the skull
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 0}};
	}

	@Override
	protected int eyes() {
		return 4;
	}

	//any hero has a cell of its body in sight
	private Hero watcher(){
		for (Hero h : Party.living()){
			if (h.fieldOfView == null) continue;
			for (int c : cells()){
				if (c >= 0 && c < h.fieldOfView.length && h.fieldOfView[c]) return h;
			}
		}
		return null;
	}

	private boolean frozen = false;

	@Override
	protected boolean act() {
		Hero h = tamed ? null : watcher();
		if (h != null && paralysed <= 0){
			if (!frozen){
				frozen = true;
				face(h.pos);
				if (sprite != null) sprite.turnTo(pos, h.pos);
				if (sprite instanceof StagSprite) ((StagSprite) sprite).freeze(true);
			}
			//it still keeps track of who's there
			if (state != HUNTING){
				aggro(h);
				target = h.pos;
			}
			spend(TICK);
			return true;
		}
		if (frozen){
			frozen = false;
			if (sprite instanceof StagSprite) ((StagSprite) sprite).freeze(false);
		}
		return super.act();
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy instanceof Hero && damage >= enemy.HP) damage = Math.max(0, enemy.HP - 1);
		return damage;
	}

	//8 frozen mid-step, face turned to you
	public static class StagSprite extends Sheet {

		public StagSprite(){
			super();
			layout(FleshAssets.STAG, 3, 64, 128, 8, 80, -1, -1);
			run = loop(12, 2, 3, 4, 5);
		}

		public void freeze( boolean on ){
			look(on ? loop(1, 8) : loop(2, 0, 0, 1, 1));
		}
	}
}
