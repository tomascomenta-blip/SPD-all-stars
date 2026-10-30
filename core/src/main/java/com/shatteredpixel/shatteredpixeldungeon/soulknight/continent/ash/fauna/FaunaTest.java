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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ColossusPreview;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.CityFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.FleshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinPriest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.Tharog;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

//testing only (-Dspd.test.fauna=... with colpreview): the beasts of the ash plains on the empty
//plain of the preview. "world" (or world1, world2) fills it the way a real floor of that layer is
//filled; a list (rex,pyre,bison...) puts those around the hero. there's black lava in pools for the
//fire ones. "flesh:" or "city:" in front (flesh:world, city:king,cathedral) is for those zones. -Dspd.test.faunabot=1 makes the hero dodge and hit back, =2 also go for them
public class FaunaTest {

	public static boolean on(){
		return DevFlags.fauna() != null && ColossusPreview.on();
	}

	public static void populate( Level level, int hero ){
		Level was = Dungeon.level;
		Dungeon.level = level;
		try {
			lava(level, hero);
			String what = DevFlags.fauna().trim().toLowerCase();
			String zone = zone();
			if (!zone.isEmpty()) what = what.substring(zone.length() + 1);
			if (what.startsWith("world")){
				int layer = what.length() > 5 ? Integer.parseInt(what.substring(5)) : OpenWorld.SURFACE;
				if (zone.equals("flesh")) FleshFauna.populate(level, layer);
				else if (zone.equals("city")) CityFauna.populate(level, layer);
				else AshFauna.populate(level, layer);
				//and what would come back later, to see it doesn't break
				for (int i = 0; i < 6; i++){
					Mob m = zone.equals("flesh") ? FleshFauna.respawn(level, layer)
							: zone.equals("city") ? CityFauna.respawn(level, layer) : AshFauna.respawn(level, layer);
					CoopLog.line("fauna respawn: " + (m == null ? "nothing" : m.getClass().getSimpleName() + " at " + m.pos));
				}
				//where everything went, to draw it outside the game
				for (Mob m : AshFauna.sorted(level.mobs)){
					CoopLog.line("fauna map: " + m.getClass().getSimpleName() + " " + m.pos + " " + (m instanceof Colossus ? ((Colossus) m).size : 1)
							+ " " + String.format("%.2f", AshFauna.danger(level, m.pos)));
				}
			} else if (!WildTest.world(level, what)){
				around(level, hero, what);
			}
			if (DevFlags.duel() > 0) duel(level, hero);
			roundTrip(level);
		} finally {
			Dungeon.level = was;
		}
	}

	//the goblin priest (or Tharog, -Dspd.test.duelboss=tharog) on the far side of the first colossus there (or a few steps off the hero if there's
	//none), awake from the start: the level is shut, the way it is when he's met in his clearing
	private static void duel( Level level, int hero ){
		Mob p = "tharog".equalsIgnoreCase(DevFlags.duelBoss()) ? new Tharog() : new GoblinPriest();
		int w = level.width();
		Colossus foe = null;
		for (Mob m : level.mobs) if (m instanceof Colossus){ foe = (Colossus) m; break; }
		int from = foe == null ? hero : foe.pos, gap = foe == null ? 5 : foe.half() + 3;
		int dx = Integer.signum(from % w - hero % w), dy = Integer.signum(from / w - hero / w);
		if (dx == 0 && dy == 0) dx = 1;
		for (int d = gap; d <= gap + 6; d++){
			for (int[] dir : new int[][]{{dx, dy}, {dx, 0}, {0, dy}, {dy, dx}, {-dy, -dx}}){
				int c = from + dir[0] * d + dir[1] * d * w;
				if (c < 0 || c >= level.length() || !level.passable[c] || level.avoid[c] || taken(level, c)) continue;
				p.pos = c;
				level.mobs.add(p);
				level.locked = true;
				CoopLog.line("fauna test: " + p.getClass().getSimpleName() + " at " + c + " by " + (foe == null ? "the hero" : foe.getClass().getSimpleName()));
				return;
			}
		}
		CoopLog.line("fauna test: no room for the goblin priest");
	}

	private static boolean taken( Level level, int cell ){
		for (Mob m : level.mobs){
			if (m instanceof Colossus ? ((Colossus) m).covers(cell) : m.pos == cell) return true;
		}
		return false;
	}

	//a few pools of black lava, away from the hero
	private static void lava( Level level, int hero ){
		int w = level.width(), h = level.height();
		int pools = Math.max(2, level.length() / 1500);
		for (int i = 0; i < pools; i++){
			int cx = 6 + Random.Int(w - 12), cy = 6 + Random.Int(h - 12);
			if (level.distance(cx + cy * w, hero) < 10) continue;
			int rx = 2 + Random.Int(4), ry = 2 + Random.Int(3);
			for (int y = cy - ry; y <= cy + ry; y++){
				for (int x = cx - rx; x <= cx + rx; x++){
					float dx = (x - cx) / (float) rx, dy = (y - cy) / (float) ry;
					if (dx * dx + dy * dy > 1f || x < 3 || y < 3 || x >= w - 3 || y >= h - 3) continue;
					if (level.map[x + y * w] == Terrain.EMPTY) level.map[x + y * w] = Terrain.WATER;
				}
			}
		}
		level.buildFlagMaps();
	}

	//every beast through a save and back, the way a floor is stored
	private static void roundTrip( Level level ){
		for (Mob m : AshFauna.sorted(level.mobs)){
			if (!(m instanceof Fauna)) continue;
			Bundle b = new Bundle();
			b.put("m", m);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Bundle.write(b, out);
			Mob copy = null;
			try {
				copy = (Mob) Bundle.read(new ByteArrayInputStream(out.toByteArray())).get("m");
			} catch (IOException e){
				CoopLog.line("fauna bundle: " + e);
			}
			boolean same = copy != null && copy.getClass() == m.getClass() && copy.pos == m.pos && copy.HP == m.HP
					&& (!(m instanceof AshColossus) || ((AshColossus) copy).essence == ((AshColossus) m).essence
						&& ((AshColossus) copy).fleas == ((AshColossus) m).fleas);
			CoopLog.line("fauna bundle: " + m.getClass().getSimpleName() + (same ? " ok" : " DIFFERS"));
		}
	}

	//"flesh:..." and "city:..." are the beasts of those zones, anything else the ash
	public static String zone(){
		String what = DevFlags.fauna() == null ? "" : DevFlags.fauna().trim().toLowerCase();
		if (what.startsWith("flesh:")) return "flesh";
		if (what.startsWith("city:")) return "city";
		return "";
	}

	private static Mob make( String kind ){
		switch (zone()){
			case "flesh": return FleshFauna.make(kind);
			case "city":  return CityFauna.make(kind);
			default:      return AshFauna.make(kind);
		}
	}

	//those kinds, one after the other on a ring round the hero
	private static void around( Level level, int hero, String what ){
		boolean[] taken = new boolean[level.length()];
		int w = level.width();
		float angle = 0;
		for (String k : what.split(",")){
			k = k.trim();
			int n = 1;
			if (k.contains("*")){
				n = Integer.parseInt(k.substring(k.indexOf('*') + 1));
				k = k.substring(0, k.indexOf('*'));
			}
			for (int i = 0; i < n; i++){
				Mob m = make(k);
				if (m == null) m = WildTest.make(k);
				if (m == null){
					CoopLog.line("fauna test: no such beast " + k);
					break;
				}
				int size = m instanceof Colossus ? ((Colossus) m).size : 1;
				int cell = -1;
				for (int t = 0; t < 60 && cell == -1; t++, angle += 0.45f){
					int dist = 6 + size / 2 + (size > 1 ? 2 : 0) + t / 4;
					int x = hero % w + Math.round((float) Math.cos(angle) * dist);
					int y = hero / w + Math.round((float) Math.sin(angle) * dist);
					if (x < 2 || y < 2 || x >= w - 2 || y >= level.height() - 2) continue;
					int c = x + y * w;
					if (fits(level, m, c, size, taken)) cell = c;
				}
				angle += 1.1f;
				if (cell == -1){
					CoopLog.line("fauna test: no room for " + k);
					continue;
				}
				if (m instanceof LavaSalamander) pool(level, cell, 2);
				m.pos = cell;
				if (m instanceof AshColossus) ((AshColossus) m).home = cell;
				if (m instanceof WalkingPyre){
					int[] essence = AshFauna.essenceFor(level, OpenWorld.SURFACE);
					if (essence.length > 0) ((WalkingPyre) m).essence = essence[0];
				}
				if (DevFlags.faunaWeak()) m.HP = Math.max(1, m.HT / 60);
				if (DevFlags.faunaDown() && m instanceof AshColossus && size >= 5) ((AshColossus) m).down = 200;
				if (m instanceof ScoriaJackal) ((ScoriaJackal) m).pack = 3;
				if (m instanceof BasaltBison && k.equals("bison") && i == 0 && n == 1) herd(level, (BasaltBison) m, taken);
				mark(level, cell, size, taken);
				level.mobs.add(m);
				CoopLog.line("fauna test: " + k + " at " + cell + " hero at " + hero + " hp=" + m.HT);
			}
		}
	}

	private static void herd( Level level, BasaltBison lead, boolean[] taken ){
		lead.infest(5);
		for (int i = 0; i < 2; i++){
			BasaltBison b = new BasaltBison();
			int w = level.width();
			for (int t = 0; t < 30; t++){
				int c = lead.pos + Random.IntRange(-9, 9) + Random.IntRange(-9, 9) * w;
				if (c < 0 || c >= level.length() || !fits(level, b, c, 5, taken)) continue;
				b.pos = c;
				b.home = c;
				b.leader = lead.id();
				mark(level, c, 5, taken);
				level.mobs.add(b);
				break;
			}
		}
	}

	private static void pool( Level level, int c, int r ){
		int w = level.width();
		for (int y = -r; y <= r; y++){
			for (int x = -r; x <= r; x++){
				int k = c + x + y * w;
				if (k > 0 && k < level.length() && level.map[k] == Terrain.EMPTY) level.map[k] = Terrain.WATER;
			}
		}
		level.buildFlagMaps();
	}

	private static boolean fits( Level level, Mob m, int c, int size, boolean[] taken ){
		int w = level.width(), h = size / 2;
		for (int y = -h - 1; y <= size - h; y++){
			for (int x = -h - 1; x <= size - h; x++){
				int k = c + x + y * w;
				if (k < 0 || k >= level.length() || taken[k]) return false;
			}
		}
		if (m instanceof AshColossus){
			boolean[] f = Colossus.fits(size, (AshColossus) m);
			return f[c];
		}
		return level.passable[c] && !level.water[c];
	}

	private static void mark( Level level, int c, int size, boolean[] taken ){
		int w = level.width(), h = size / 2;
		for (int y = -h; y < size - h; y++){
			for (int x = -h; x < size - h; x++){
				int k = c + x + y * w;
				if (k >= 0 && k < level.length()) taken[k] = true;
			}
		}
	}

	//the nearest spot to a hero where a big one fits without touching them (cmob test), or -1
	public static int nearSpot( Colossus c, int hero ){
		boolean[] f = Colossus.fits(c.size, c);
		Level l = Dungeon.level;
		int best = -1, bestD = Integer.MAX_VALUE, w = l.width();
		for (int i = 0; i < f.length; i++){
			if (!f[i]) continue;
			int d = l.distance(i, hero);
			if (d < c.half() + 3 || d > c.half() + 14 || d >= bestD) continue;
			int x = i % w, y = i / w;
			//its body has to stay clear of everyone
			boolean clear = true;
			for (Char ch : Actor.chars()){
				if (Math.abs(ch.pos % w - x) <= c.half() + 1 && Math.abs(ch.pos / w - y) <= c.half() + 1) clear = false;
			}
			if (clear){
				best = i;
				bestD = d;
			}
		}
		return best;
	}

	// *** the bot ***

	private static float timer = 3f, logTimer = 0;

	//solo: every few frames, from the render thread (it only gives orders, like a player)
	public static void tick( Hero h ){
		if (on() && DevFlags.faunaShow()) show();
		//-Dspd.test.duel=2: the hero only watches, a turn at a time
		if (on() && DevFlags.duel() == 2 && h != null && h.isAlive() && h.ready
				&& GameScene.topWindow() == null && !GameScene.isSelectingCell()){
			if ((timer -= Game.elapsed) > 0) return;
			timer = 0.2f;
			CoopOrders.named("rest");
			return;
		}
		if (!on() || DevFlags.faunaBot() <= 0 || h == null || !h.isAlive() || !h.ready) return;
		if (GameScene.topWindow() != null || GameScene.isSelectingCell()) return;
		if ((timer -= Game.elapsed) > 0) return;
		timer = 0.12f;
		if (!act(h)) CoopOrders.named("rest");
	}

	//pictures only: every beast drawn wherever it is, for the screenshots
	private static void show(){
		if (Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.sprite != null && m.isAlive()) m.sprite.visible = true;
		}
	}

	public static boolean act( Hero me ){
		if (!on() || DevFlags.faunaBot() <= 0 || me == null || !me.isAlive()) return false;
		if ((logTimer -= 0.12f) <= 0){
			logTimer = 3f;
			CoopLog.line("fauna bot: hero " + me.HP + "/" + me.HT + " @" + me.pos + " t=" + (int) Actor.now() + " | " + who(me));
		}
		if (DevFlags.faunaLit() && me.buff(Light.class) == null){
			Torch t = me.belongings.getItem(Torch.class);
			if (t != null){
				CoopOrders.execute(t, Torch.AC_LIGHT);
				return true;
			}
		}
		if (me.HP < me.HT / 3){
			PotionOfHealing p = me.belongings.getItem(PotionOfHealing.class);
			if (p != null){
				CoopOrders.execute(p, "DRINK");
				return true;
			}
		}
		if (TargetedCell.cells.get(me.pos) != null && !me.rooted){
			int safe = safeSpot(me.pos);
			if (safe != -1){
				CoopOrders.handleCell(safe);
				return true;
			}
		}
		//hit whatever of ours is touching
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!(m instanceof Fauna) || !m.isAlive() || m.invisible > 0) continue;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = me.pos + n;
				if (c < 0 || c >= Dungeon.level.length()) continue;
				if (m instanceof Colossus ? ((Colossus) m).covers(c) : m.pos == c){
					CoopOrders.handleCell(c);
					return true;
				}
			}
		}
		if (DevFlags.faunaBot() >= 2){
			Mob best = null;
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (!(m instanceof Fauna) || !m.isAlive() || m.invisible > 0) continue;
				if (best == null || Dungeon.level.distance(me.pos, m.pos) < Dungeon.level.distance(me.pos, best.pos)) best = m;
			}
			if (best != null){
				int to = best instanceof Colossus ? ((Colossus) best).approachCell(me.pos) : best.pos;
				if (TargetedCell.cells.get(to) == null){
					CoopOrders.handleCell(to);
					return true;
				}
			}
		}
		return false;
	}

	//closest unmarked free cell, up to 4 steps away
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
				if (k < 0 || k >= l.length() || dist[k] != -1 || !l.passable[k] || l.water[k] || Actor.findChar(k) != null) continue;
				dist[k] = dist[c] + 1;
				queue.add(k);
			}
		}
		return -1;
	}

	private static String who( Hero me ){
		StringBuilder s = new StringBuilder();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!(m instanceof Fauna) || !m.isAlive()) continue;
			int d = m instanceof Colossus ? ((Colossus) m).reach(me.pos) : Dungeon.level.distance(m.pos, me.pos);
			if (d > 25) continue;
			s.append(m.getClass().getSimpleName()).append(' ').append(m.HP).append('/').append(m.HT)
					.append(" d=").append(d)
					.append(m.state == m.HUNTING ? " H" : m.state == m.WANDERING ? " W" : m.state == m.SLEEPING ? " S" : " -")
					.append(m.invisible > 0 ? " hidden" : "").append("; ");
		}
		return s.toString();
	}
}
