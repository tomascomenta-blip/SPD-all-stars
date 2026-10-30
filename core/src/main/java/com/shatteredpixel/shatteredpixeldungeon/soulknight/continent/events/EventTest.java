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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.events;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.DarkTaint;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

//-Dspd.test.event=steps|tide|storm|migration (several with commas, one after the other): the event
//starts a few turns after the run lands on the continent; any other word (natural) leaves them to
//their own clock. alone, the hero waits it out and steps off whatever gets marked (or walks the
//biomes with -Dspd.test.landwalk); in a cooperative run the bots play as usual
public class EventTest {

	private static float timer = 2f, logTimer = 0;
	private static boolean revealed = false;
	private static WorldEvent saved = null;

	//from the render thread, every frame: it only gives orders, like a player
	public static void tick( Hero me ){
		if (CoopSession.active() || me == null || !me.isAlive() || !me.ready) return;
		if (GameScene.topWindow() != null || GameScene.isSelectingCell()) return;
		if ((timer -= Game.elapsed) > 0) return;
		timer = 0.12f;
		if (!revealed) reveal(me);
		roundTrip();
		if ((logTimer -= 0.12f) <= 0){
			logTimer = 3f;
			CoopLog.line("event bot: t=" + (int) Actor.now() + " hero " + me.HP + "/" + me.HT + " @" + me.pos
					+ " sight " + me.viewDistance + " taint " + DarkTaint.of(me) + " | " + state());
		}
		if (DevFlags.landWalk() != null && !me.rooted) return;
		if (TargetedCell.cells.get(me.pos) != null && !me.rooted){
			int safe = safeSpot(me.pos);
			if (safe != -1){
				CoopOrders.handleCell(safe);
				return;
			}
		}
		CoopOrders.named("rest");
	}

	//halfway through each event, what would go in the save is written out, read back and compared
	private static void roundTrip(){
		WorldEvent e = WorldEvents.active();
		if (e == null || e == saved || e.age < 15) return;
		saved = e;
		try {
			Bundle b = new Bundle();
			WorldEvents.store(b);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Bundle.write(b, out);
			Bundle back = Bundle.read(new ByteArrayInputStream(out.toByteArray())).getBundle("world_events");
			WorldEvent copy = (WorldEvent) back.get("active");
			Bundle x = new Bundle(), y = new Bundle();
			e.storeInBundle(x);
			copy.storeInBundle(y);
			CoopLog.line("event bot: save round trip of " + copy.getClass().getSimpleName() + " "
					+ (x.toString().equals(y.toString()) ? "ok" : "DIFFERS " + x + " / " + y));
		} catch (Exception ex){
			CoopLog.line("event bot: save round trip FAILED " + ex);
		}
	}

	//for the screenshots: the ground around the hero as if already walked, so what's left on it shows
	private static void reveal( Hero me ){
		revealed = true;
		Level l = Dungeon.level;
		for (int i = 0; i < l.length(); i++){
			if (l.distance(i, me.pos) <= 24) l.visited[i] = true;
		}
		GameScene.updateFog(me.pos, 25);
	}

	private static String state(){
		WorldEvent e = WorldEvents.active();
		if (e == null) return "no event";
		StringBuilder s = new StringBuilder(WorldEvents.NAMES[e.kind()]).append(" age ").append(e.age)
				.append(" power ").append(Math.round(e.power() * 100));
		if (e instanceof GiantSteps) s.append(" landed ").append(((GiantSteps) e).landed);
		if (e instanceof YogTide) s.append(" reach ").append(((YogTide) e).reach);
		if (e instanceof Migration){
			int n = 0;
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.buff(Migration.Migrant.class) != null) n++;
			}
			s.append(" herd ").append(n);
		}
		return s.toString();
	}

	//closest unmarked free cell, up to 4 steps away (and never the way out)
	private static int safeSpot( int from ){
		Level l = Dungeon.level;
		int[] dist = new int[l.length()];
		Arrays.fill(dist, -1);
		ArrayList<Integer> queue = new ArrayList<>();
		queue.add(from);
		dist[from] = 0;
		for (int i = 0; i < queue.size(); i++){
			int c = queue.get(i);
			if (c != from && TargetedCell.cells.get(c) == null) return c;
			if (dist[c] >= 4) continue;
			for (int n : PathFinder.NEIGHBOURS8){
				int k = c + n;
				if (k < 0 || k >= l.length() || dist[k] != -1 || !l.passable[k] || l.avoid[k] || Actor.findChar(k) != null
						|| l.getTransition(k) != null) continue;
				dist[k] = dist[c] + 1;
				queue.add(k);
			}
		}
		return -1;
	}
}
