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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.DarkTaint;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Yog;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.Fauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.events.WorldEvents;

import java.util.ArrayList;

//what gives a hero away over there: light, noise, blood and corruption. the monsters read it,
//anything that makes a racket writes to it
public class Senses {

	//how loud things are, in cells
	public static final int STEP = 3, FIGHT = 7, CRASH = 10, ROAR = 13, STAMPEDE = 16;

	//how far a monster makes out a hero in the dark, and carrying a light (from the edge of its body)
	public static final int DARK_SIGHT = 4, LIT_SIGHT = 12;

	//corruption from which the brood comes looking (0-100)
	public static final int CALLING = 40;

	public static class Noise {
		public int cell, radius;
		public float time;
		Level where;
	}

	private static final ArrayList<Noise> recent = new ArrayList<>();

	//something loud happened here, heard up to radius cells away
	public static void noise( int cell, int radius ){
		if (cell < 0 || radius <= 0) return;
		radius = WorldEvents.muffle(radius);
		float now = Actor.now();
		Level here = Dungeon.level;
		//the same racket twice in a turn is still one noise
		for (int i = recent.size() - 1; i >= 0 && recent.get(i).time >= now; i--){
			Noise o = recent.get(i);
			if (o.cell == cell && o.where == here){
				o.radius = Math.max(o.radius, radius);
				return;
			}
		}
		Noise n = new Noise();
		n.cell = cell;
		n.radius = radius;
		n.time = now;
		n.where = here;
		recent.add(n);
		if (recent.size() > 32) recent.remove(0);
	}

	//noises from the last few turns, oldest first
	public static ArrayList<Noise> heardSince( float time ){
		ArrayList<Noise> out = new ArrayList<>();
		for (Noise n : recent) if (n.time >= time && n.where == Dungeon.level) out.add(n);
		return out;
	}

	public static void clear(){
		recent.clear();
	}

	public static boolean lit( Char ch ){
		return ch.buff(Light.class) != null;
	}

	public static boolean bleeding( Char ch ){
		return ch.HP < ch.HT / 2;
	}

	public static int taint( Char ch ){
		return DarkTaint.of(ch);
	}

	public static boolean inOpenWorld(){
		return Dungeon.level instanceof OpenWorld;
	}

	// *** reading them ***

	//cells from a monster to a cell: from the edge of the body for the big ones
	public static int dist( Char m, int cell ){
		if (m instanceof Colossus) return ((Colossus) m).reach(cell);
		return Dungeon.level.distance(m.pos, cell);
	}

	//the dark hides what isn't close, unless it carries a light (or stands right by someone who does).
	//eyes: how much further than usual this one makes things out
	public static void dim( Mob m, boolean[] fov, int eyes ){
		if (fov == null) return;
		Level l = Dungeon.level;
		int w = l.width(), h = l.height();
		//the weather hides them too
		int cut = WorldEvents.hideCut();
		int dark = Math.max(1, DARK_SIGHT + eyes - cut), far = Math.max(2, LIT_SIGHT + eyes - cut * 2);
		ArrayList<Integer> torches = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch != m && ch.alignment != m.alignment && lit(ch)) torches.add(ch.pos);
		}
		int half = m instanceof Colossus ? ((Colossus) m).half() : 0;
		int r = far + half + 1;
		int cx = m.pos % w, cy = m.pos / w;
		for (int y = Math.max(0, cy - r); y <= Math.min(h - 1, cy + r); y++){
			for (int x = Math.max(0, cx - r); x <= Math.min(w - 1, cx + r); x++){
				int c = x + y * w;
				if (!fov[c]) continue;
				int d = dist(m, c);
				if (d <= dark) continue;
				boolean shown = false;
				if (d <= far){
					for (int t : torches){
						if (l.distance(t, c) <= 1){
							shown = true;
							break;
						}
					}
				}
				if (!shown) fov[c] = false;
			}
		}
	}

	//the loudest noise since 'since' that reaches this one. ears: how much better than usual it hears
	public static Noise heard( Mob m, float since, int ears ){
		Noise best = null;
		int bestMargin = -1;
		for (Noise n : heardSince(since)){
			int margin = n.radius + ears - dist(m, n.cell);
			if (margin >= 0 && margin > bestMargin){
				best = n;
				bestMargin = margin;
			}
		}
		return best;
	}

	//the worst hurt hero a nose can follow from here, or null
	public static Hero scent( Char m, int range ){
		if (WorldEvents.scentless()) return null;
		Hero best = null;
		for (Hero h : Party.living()){
			if (!bleeding(h) || h.invisible > 0 || dist(m, h.pos) > range) continue;
			if (best == null || h.HP * best.HT < best.HP * h.HT) best = h;
		}
		return best;
	}

	//the most corrupted hero calling the brood within range, or null
	public static Hero calling( Char m, int range ){
		Hero best = null;
		for (Hero h : Party.living()){
			int t = taint(h);
			if (t < CALLING || dist(m, h.pos) > range + (t - CALLING) / 4) continue;
			if (best == null || t > taint(best)) best = h;
		}
		return best;
	}

	//every turn, for the monsters over there that don't read the senses themselves (yogs, walkers...):
	//noise draws whatever isn't after someone yet, and corruption draws the brood. the cell it should
	//head to (beckon), or -1. target = where it's going now
	public static int stir( Mob m, int target ){
		if (!inOpenWorld() || m instanceof Fauna || m.state != m.WANDERING && m.state != m.SLEEPING) return -1;
		if (m instanceof Yog){
			Hero h = calling(m, 14);
			if (h != null){
				if (m.state == m.SLEEPING || target == -1 || Dungeon.level.distance(target, h.pos) > 2) return h.pos;
				return -1;
			}
		}
		Noise n = heard(m, Actor.now() - 1f, 0);
		if (n != null && (m.state == m.SLEEPING || target != n.cell)) return n.cell;
		return -1;
	}
}
