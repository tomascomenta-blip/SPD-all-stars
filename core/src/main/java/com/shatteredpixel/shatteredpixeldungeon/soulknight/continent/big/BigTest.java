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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//testing only (-Dspd.test.bigtest=1 with colpreview): two plain colossi come for the hero, who
//hits back whatever touches it. checks the footprint, the walking, the trampling and the hits
public class BigTest {

	public static boolean on(){
		return DevFlags.bigTest();
	}

	public static void populate( Level level, int hero ){
		ArrayList<Integer> spots = Colossus.spots(level, 15, 1, hero, 22);
		if (!spots.isEmpty()){
			Pyre p = new Pyre();
			p.pos = spots.get(0);
			level.mobs.add(p);
		}
		spots = Colossus.spots(level, 5, 2, hero, 10);
		for (int c : spots){
			Obsidian o = new Obsidian();
			o.pos = c;
			level.mobs.add(o);
		}
	}

	private static float timer = 3f;

	public static void tick( Hero h ){
		if (!on() || h == null || !h.isAlive() || !h.ready || GameScene.topWindow() != null) return;
		if ((timer -= Game.elapsed) > 0) return;
		timer = 0.15f;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Colossus && ((Colossus) m).touches(h.pos)){
				//a cell of its body next to the hero, the way a player would tap it
				for (int n : PathFinder.NEIGHBOURS8){
					if (((Colossus) m).covers(h.pos + n)){
						CoopLog.line("bigtest: hero hits " + m.getClass().getSimpleName() + " hp=" + m.HP + " me=" + h.HP);
						CoopOrders.handleCell(h.pos + n);
						return;
					}
				}
			}
		}
		CoopOrders.named("rest");
	}

	public static class Obsidian extends Colossus {
		{
			size = 5;
			HP = HT = 300;
			defenseSkill = 10;
			minDmg = 10; maxDmg = 16;
			spriteClass = ObsidianSprite.class;
			state = HUNTING;
		}

		@Override
		public void move( int step, boolean travelling ) {
			super.move(step, travelling);
			CoopLog.line("bigtest: obsidian at " + pos);
		}
	}

	public static class ObsidianSprite extends ColossusSprite {
		public ObsidianSprite(){
			super();
			setup("sprites/colosos/coloso.png", 5, 112, 112, 16, 32);
		}
	}

	public static class Pyre extends Colossus {
		{
			size = 15;
			HP = HT = 2000;
			defenseSkill = 5;
			minDmg = 20; maxDmg = 30;
			baseSpeed = 0.5f;
			spriteClass = PyreSprite.class;
			state = HUNTING;
		}
	}

	public static class PyreSprite extends ColossusSprite {
		public PyreSprite(){
			super();
			setup("sprites/colosos/pira.png", 15, 288, 312, 24, 64);
		}
	}
}
