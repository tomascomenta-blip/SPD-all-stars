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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//something enormous that nobody ever sees walks across the floor in a straight line. it's heard
//long before it gets there, every thud louder than the last. where its feet will come down is
//marked a few turns ahead, a 3x3 patch that kills whatever stays on it; it leaves its prints in
//the ground and the beasts run off its way. down in the tunnels it walks overhead: the roof
//comes down where it treads
public class GiantSteps extends WorldEvent {

	//sixteen headings, as thousandths of (cos, sin). no trigonometry in the turns: every device
	//puts the feet in the same cells
	private static final int[][] HEAD = {
			{1000, 0}, {924, 383}, {707, 707}, {383, 924}, {0, 1000}, {-383, 924}, {-707, 707}, {-924, 383},
			{-1000, 0}, {-924, -383}, {-707, -707}, {-383, -924}, {0, -1000}, {383, -924}, {707, -707}, {924, -383}
	};

	//cells along its way from one foot to the next, turns between them, turns of warning
	public static final int STRIDE = 6, PACE = 2, WARN = 3;
	//steps before it gets to the party and after, and the turn the first one lands
	private static final int BEFORE = 14, AFTER = 12, STEPS = BEFORE + AFTER + 1, LEAD = 4;
	//a foot is two cells off the middle of its way, to one side and the other
	private static final int SPREAD = 2;

	//a point its way goes through, and the heading
	int cx, cy, head;
	//down in the tunnels
	public boolean under;
	//the last step that came down and the last one marked
	public int landed = -1;
	int marked = -1;
	private boolean near = false;

	//what killed whoever stayed under it
	public static class Foot {
	}

	private static final Foot FOOT = new Foot();

	@Override
	public int kind() {
		return WorldEvents.STEPS;
	}

	@Override
	boolean begin( int layer ) {
		if (layer == OpenWorld.ABYSS) return false;
		int c = WorldEvents.partyCell();
		if (c == -1) return false;
		int w = Dungeon.level.width();
		head = Random.Int(HEAD.length);
		int off = Random.IntRange(-3, 3);
		cx = c % w - off * HEAD[head][1] / 1000;
		cy = c / w + off * HEAD[head][0] / 1000;
		under = layer != OpenWorld.SURFACE;
		GLog.w(Messages.get(this, under ? "far_under" : "far"));
		card++;
		CoopLog.line("events: giant heading " + head + " through " + cx + "," + cy + (under ? " overhead" : ""));
		return true;
	}

	// *** where the feet go ***

	public int footX( int k ){
		int along = (k - BEFORE) * STRIDE, side = k % 2 == 0 ? SPREAD : -SPREAD;
		return cx + along * HEAD[head][0] / 1000 - side * HEAD[head][1] / 1000;
	}

	public int footY( int k ){
		int along = (k - BEFORE) * STRIDE, side = k % 2 == 0 ? SPREAD : -SPREAD;
		return cy + along * HEAD[head][1] / 1000 + side * HEAD[head][0] / 1000;
	}

	private int landTurn( int k ){
		return LEAD + k * PACE;
	}

	private static boolean onMap( int x, int y ){
		Level l = Dungeon.level;
		return x >= 1 && y >= 1 && x < l.width() - 1 && y < l.height() - 1;
	}

	//the ground under a foot
	public static ArrayList<Integer> patch( int x, int y ){
		ArrayList<Integer> out = new ArrayList<>();
		Level l = Dungeon.level;
		if (!onMap(x, y)) return out;
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				int c = x + dx + (y + dy) * l.width();
				if (!l.solid[c]) out.add(c);
			}
		}
		return out;
	}

	// *** the turns ***

	@Override
	boolean act() {
		age++;
		Level l = Dungeon.level;
		int party = WorldEvents.partyCell();
		//the feet that come down within the warning get marked now
		if (!calming){
			int lead = Math.max(WARN, (int) Math.ceil(SKCombat.announceDelay(WARN)));
			while (marked + 1 < STEPS && landTurn(marked + 1) - age <= lead){
				marked++;
				ArrayList<Integer> cells = patch(footX(marked), footY(marked));
				if (cells.isEmpty()) continue;
				int foot = footX(marked) + footY(marked) * l.width();
				mark(cells, foot, landTurn(marked) - age);
				if (!near && party != -1 && l.distance(foot, party) <= 24){
					near = true;
					GLog.n(Messages.get(this, under ? "near_under" : "near"));
					card++;
				}
			}
		}
		while (landed + 1 < STEPS && landTurn(landed + 1) <= age){
			if (calming && landed + 1 > marked) break;
			landed++;
			land(landed);
		}
		if (landed >= STEPS - 1 || calming && landed >= marked){
			GLog.i(Messages.get(this, calming && landed < STEPS - 1 ? "turns" : "gone"));
			return false;
		}
		return true;
	}

	//like SKCombat.mark, but only who is close enough to care stops what they're doing
	private static void mark( ArrayList<Integer> cells, int foot, float delay ){
		for (int c : cells) GameScene.targetedCell(c, delay);
		for (Hero h : Party.living()){
			if (Dungeon.level.distance(h.pos, foot) <= 12) h.interrupt();
		}
	}

	private void land( int k ){
		int x = footX(k), y = footY(k);
		ArrayList<Integer> cells = patch(x, y);
		if (cells.isEmpty()) return;
		crush(cells);
		if (!under) addPrint(x, y, k % 2);
		scatter(k);
	}

	//whatever is under the foot: the small ones are done for, the big ones only take a beating
	private void crush( ArrayList<Integer> cells ){
		ArrayList<Char> under = new ArrayList<>();
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null && ch.isAlive() && !under.contains(ch)) under.add(ch);
		}
		Collections.sort(under, new Comparator<Char>() {
			@Override
			public int compare( Char a, Char b ) {
				return a.id() - b.id();
			}
		});
		for (Char ch : under){
			int dmg = ch instanceof Colossus && ((Colossus) ch).size >= 5
					? Math.max(1, ch.HT / (((Colossus) ch).size >= 10 ? 10 : 4))
					: Math.max(ch.HT, ch.HP) * 2;
			//what the giant crushes is no kill of the party's: no experience for it
			if (ch instanceof Mob && dmg >= ch.HP) ((Mob) ch).EXP = 0;
			ch.damage(dmg, FOOT);
			CoopLog.line("events: the giant treads on " + ch.getClass().getSimpleName() + " (" + ch.HP + " left)");
		}
	}

	//everything around where the next feet will land runs off to one side of its way
	private void scatter( int k ){
		Level l = Dungeon.level;
		for (Mob m : AshFauna.sorted(l.mobs)){
			if (!m.isAlive() || m.alignment == Char.Alignment.ALLY || WorldEvents.guardian(m)
					|| Char.hasProp(m, Char.Property.IMMOVABLE)) continue;
			if (m instanceof Colossus && ((Colossus) m).size >= 15) continue;
			boolean close = false;
			for (int j = k + 1; j <= k + 3 && j < STEPS && !close; j++){
				int x = Math.max(0, Math.min(l.width() - 1, footX(j))), y = Math.max(0, Math.min(l.height() - 1, footY(j)));
				int c = x + y * l.width();
				close = (m instanceof Colossus ? ((Colossus) m).reach(c) : l.distance(m.pos, c)) <= 10;
			}
			if (close) flee(m);
		}
	}

	private void flee( Mob m ){
		Level l = Dungeon.level;
		int w = l.width(), px = m.pos % w, py = m.pos / w;
		//which side of its way it's on, and further that way
		int nx = -HEAD[head][1], ny = HEAD[head][0];
		long dot = (long) (px - cx) * nx + (long) (py - cy) * ny;
		int s = dot >= 0 ? 1 : -1;
		int tx = Math.max(2, Math.min(w - 3, px + s * nx * 14 / 1000));
		int ty = Math.max(2, Math.min(l.height() - 3, py + s * ny * 14 / 1000));
		int to = WorldEvents.freeNear(tx + ty * w, 4);
		if (to == -1) return;
		if (m.state != m.FLEEING) m.clearEnemy();
		m.followTo(to);
	}

	// *** the prints it leaves ***

	//how long a print stays fresh, and how long until it's gone (turns on the continent)
	public static final int FRESH = 200, LIFE = 900, MAX_PRINTS = 60;

	//{depth, branch, x, y, foot, clock}. copied on every change, so the screens can read it any time
	private static volatile ArrayList<int[]> prints = new ArrayList<>();
	public static volatile int printsVersion = 0;

	public static ArrayList<int[]> prints(){
		return prints;
	}

	private void addPrint( int x, int y, int foot ){
		ArrayList<int[]> next = new ArrayList<>(prints);
		next.add(new int[]{depth, branch, x, y, foot, WorldEvents.clock()});
		while (next.size() > MAX_PRINTS) next.remove(0);
		prints = next;
		printsVersion++;
	}

	static void clearPrints(){
		prints = new ArrayList<>();
		printsVersion++;
	}

	static void storePrints( Bundle b ){
		ArrayList<int[]> list = prints;
		int[] flat = new int[list.size() * 6];
		for (int i = 0; i < list.size(); i++) System.arraycopy(list.get(i), 0, flat, i * 6, 6);
		b.put("prints", flat);
	}

	static void restorePrints( Bundle b ){
		ArrayList<int[]> list = new ArrayList<>();
		if (b.contains("prints")){
			int[] flat = b.getIntArray("prints");
			for (int i = 0; i + 5 < flat.length; i += 6){
				int[] p = new int[6];
				System.arraycopy(flat, i, p, 0, 6);
				list.add(p);
			}
		}
		prints = list;
		printsVersion++;
	}

	//each biome has its ground pressed in its own way
	public static String printSheet( Biome b ){
		return "environment/surface/events/prints_" + b.key + ".png";
	}

	// *** for the screens ***

	@Override
	public float power() {
		return landed < 0 ? 0 : Math.max(0, 1f - Math.abs(landed - BEFORE) / (float) BEFORE);
	}

	//the next foot while it's still a way off
	@Override
	public int origin() {
		if (landed >= BEFORE - 3) return -1;
		Level l = Dungeon.level;
		int k = landed + 1;
		int x = Math.max(0, Math.min(l.width() - 1, footX(k))), y = Math.max(0, Math.min(l.height() - 1, footY(k)));
		return x + y * l.width();
	}

	@Override
	public String title() {
		return Messages.get(this, "name");
	}

	@Override
	public String line() {
		return Messages.get(this, near ? (under ? "card_near_under" : "card_near") : "card_far");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("cx", cx);
		bundle.put("cy", cy);
		bundle.put("head", head);
		bundle.put("under", under);
		bundle.put("landed", landed);
		bundle.put("marked", marked);
		bundle.put("near", near);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		cx = bundle.getInt("cx");
		cy = bundle.getInt("cy");
		head = bundle.getInt("head");
		under = bundle.getBoolean("under");
		landed = bundle.getInt("landed");
		marked = bundle.getInt("marked");
		near = bundle.getBoolean("near");
	}
}
