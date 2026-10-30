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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshCritter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.CritterSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//marrow worm: a worm of red marrow rings with a round mouth full of teeth. they live in the old bones
//three or four together, half out of a cracked one while nothing passes, and the bite bleeds
public class MarrowWorm extends AshCritter {

	{
		spriteClass = MarrowSprite.class;

		HP = HT = 60;
		defenseSkill = 22;
		EXP = 5;

		minDmg = 14; maxDmg = 22; acc = 58;
		armorMin = 2; armorMax = 6;
	}

	//the first one of a nest brings the rest on its first turn
	public int nest = 0;

	@Override
	protected int ears() {
		return 4;
	}

	@Override
	protected boolean act() {
		if (nest > 0){
			int n = nest;
			nest = 0;
			ArrayList<Integer> cells = AshFauna.openAround(pos, 1);
			for (int i = 0; i < n && !cells.isEmpty(); i++){
				MarrowWorm m = new MarrowWorm();
				m.pos = cells.remove(Random.Int(cells.size()));
				m.state = m.WANDERING;
				if (EXP == 0) m.summoned();
				GameScene.add(m);
			}
		}
		if (sprite instanceof MarrowSprite) ((MarrowSprite) sprite).peek(state != HUNTING);
		return super.act();
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.isAlive() && Random.Int(2) == 0) Buff.affect(enemy, Bleeding.class).set(5);
		return damage;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("nest", nest);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		nest = bundle.getInt("nest");
	}

	//0-1 idle, 2-5 crawling, 6-7 the bite, 8 half out of a bone
	public static class MarrowSprite extends CritterSprite {

		private Animation out, peeking;
		private boolean hiding = false;

		public MarrowSprite(){
			super();
			critter(BonesAssets.MARROW);
			out = idle;
			peeking = loop(1, 8);
		}

		public void peek( boolean on ){
			if (on == hiding) return;
			hiding = on;
			look(on ? peeking : out);
		}
	}
}
