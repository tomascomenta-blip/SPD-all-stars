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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//soot rats. never one: a black tide of them pours out of the chimneys and the sewers. each is
//nothing, but with three or more on someone every bite opens a wound, and they smell it
public class SootRats extends CityCritter {

	{
		spriteClass = RatSprite.class;

		HP = HT = 32;
		defenseSkill = 36;
		EXP = 3;

		minDmg = 10; maxDmg = 15; acc = 54;
		armorMin = 0; armorMax = 2;
	}

	//the first one of a swarm brings the rest on its first turn
	public int swarm = 0;

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected boolean act() {
		if (swarm > 0) spill();
		if (state != HUNTING){
			Hero prey = Senses.scent(this, 12);
			if (prey != null){
				aggro(prey);
				target = prey.pos;
			}
		}
		return super.act();
	}

	private void spill(){
		int n = swarm;
		swarm = 0;
		ArrayList<Integer> cells = AshFauna.openAround(pos, 2);
		for (int i = 0; i < n && !cells.isEmpty(); i++){
			SootRats r = new SootRats();
			r.pos = cells.remove(Random.Int(cells.size()));
			r.leader = id();
			r.state = state == HUNTING ? r.HUNTING : r.WANDERING;
			if (EXP == 0) r.summoned();
			GameScene.add(r);
		}
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		int mates = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			if (Actor.findChar(enemy.pos + n) instanceof SootRats) mates++;
		}
		if (mates >= 3 && enemy.isAlive() && Random.Int(3) == 0) Buff.affect(enemy, Bleeding.class).set(4);
		return damage;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("swarm", swarm);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		swarm = bundle.getInt("swarm");
	}

	//8 is three of them in a heap, for idling
	public static class RatSprite extends Sheet {

		public RatSprite(){
			super();
			layout(CityAssets.RATS);
			idle = loop(3, 0, 1, 0, 8, 8, 1);
			play(idle);
		}
	}
}
