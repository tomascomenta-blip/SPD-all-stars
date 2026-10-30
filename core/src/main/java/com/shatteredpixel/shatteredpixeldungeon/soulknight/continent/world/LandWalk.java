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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.watabou.noosa.Game;

//testing only: -Dspd.test.landwalk=W,N,E... the hero walks out of each surface through the pass
//on that edge, one leg after the other, as a player tapping the way. solo it's the hero, in co-op
//every bot does it (the party crosses together, so the legs stay the same on every device)
public class LandWalk {

	private static int lastZone = -1;
	private static int leg = -1;
	private static boolean done = false;
	private static float timer = 0;
	private static float since = 0;

	private static String[] route(){
		String r = DevFlags.landWalk();
		return r == null ? null : r.split(",");
	}

	private static int side( String s ){
		switch (s.trim().toUpperCase()){
			case "N": return Biome.N;
			case "E": return Biome.E;
			case "S": return Biome.S;
			default:  return Biome.W;
		}
	}

	//as if they had a map of it, the hero only walks where it knows. on arrival, a game action
	//(the same on every device, and saved with the level the guests load)
	public static void onArrive( Level level ){
		if (route() == null) return;
		for (int i = 0; i < level.length(); i++){
			if (!level.solid[i] || level.getTransition(i) != null) level.mapped[i] = true;
		}
	}

	//solo: called every frame
	public static void tick( Hero h ){
		if (route() == null || h == null || !h.isAlive() || !h.ready) return;
		if (GameScene.topWindow() != null || GameScene.isSelectingCell()) return;
		if ((timer -= Game.elapsed) > 0) return;
		timer = 0.3f;
		act(h);
	}

	//co-op: the bot asks before doing its own thing
	public static boolean act( Hero me ){
		String[] route = route();
		if (route == null || done || me == null || !me.isAlive()) return false;
		Level level = Dungeon.level;
		//a new leg each time the party is in another biome (a reload of the same one isn't)
		int zone = Continent.zone();
		if (zone != lastZone){
			lastZone = zone;
			leg++;
			since = Actor.now();
			String where = Continent.place();
			CoopLog.line("land walk: leg " + leg + "/" + route.length + " in " + (where != null ? where : "?")
					+ " at " + (me.pos % level.width()) + "," + (me.pos / level.width()));
		}
		if (leg >= route.length){
			done = true;
			CoopLog.line("land walk: route done");
			return false;
		}
		int side = side(route[leg]);
		EdgeTransition pass = null;
		for (LevelTransition t : level.transitions){
			if (t instanceof EdgeTransition && ((EdgeTransition) t).side == side) pass = (EdgeTransition) t;
		}
		if (pass == null){
			CoopLog.line("land walk: no pass on side " + route[leg] + " here, stopping");
			done = true;
			return false;
		}
		if (Actor.now() - since > 3000){
			CoopLog.line("land walk: stuck on leg " + leg + " at " + me.pos + ", stopping");
			done = true;
			return false;
		}
		int target = pass.cell();
		//a cell of the strip that can be stood on
		for (int i = 0; i < level.length(); i++){
			if (pass.inside(level.cellToPoint(i)) && level.passable[i]){
				target = i;
				if (Math.abs(i - pass.cell()) <= 2) break;
			}
		}
		CoopOrders.handleCell(target);
		return true;
	}
}
