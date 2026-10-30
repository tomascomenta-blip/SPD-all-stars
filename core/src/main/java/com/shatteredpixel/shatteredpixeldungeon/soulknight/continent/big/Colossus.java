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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.Awakening;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.events.Migration;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Climbing;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.Remains;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//a monster that stands on a size x size patch of ground. pos is the middle of that patch (for an
//even size, the cell just down and right of the middle), and every cell of the patch counts as
//the colossus: Actor.findChar finds it there, paths go around it, swords hit it from any side.
//it walks where its whole body fits and shoves aside (and hurts) whatever small thing is in the way
public abstract class Colossus extends ContinentMob {

	public int size = 3;

	//the name card already came up on this screen (see ColossusPresence)
	public boolean announced = false;

	{
		//wandering picks spots where the whole body fits
		boolean wasWandering = state == WANDERING;
		WANDERING = new Wandering(){
			@Override
			protected int randomDestination() {
				return randomFit();
			}
		};
		if (wasWandering) state = WANDERING;
	}

	//every colossus on the current floor, for findChar
	private static final ArrayList<Colossus> placed = new ArrayList<>();

	@Override
	protected void onAdd() {
		super.onAdd();
		if (!placed.contains(this)) placed.add(this);
	}

	@Override
	protected void onRemove() {
		super.onRemove();
		placed.remove(this);
	}

	public static void clearAll(){
		placed.clear();
	}

	//the colossus covering this cell, if any
	public static Colossus at( int cell ){
		for (int i = 0; i < placed.size(); i++){
			Colossus c = placed.get(i);
			if (c.solid() ? c.covers(cell) : c.pos == cell) return c;
		}
		return null;
	}

	public static boolean any(){
		return !placed.isEmpty();
	}

	//room left free round a big one when they're scattered over a map: the bigger, the wider
	public static int spacing( int size ){
		return size >= 8 ? 16 : size >= 5 ? 9 : 5;
	}

	//another big one would be right next to it if it stood on this cell. wandering keeps clear of
	//them; the ones that go about together (a herd) don't count
	public boolean crowded( int cell ){
		for (int i = 0; i < placed.size(); i++){
			Colossus o = placed.get(i);
			if (o == this || !o.isAlive() || !o.solid() || together(o)) continue;
			int gap = Dungeon.level.distance(cell, o.pos) - half() - o.half();
			if (gap < spacing(Math.min(size, o.size)) / 2) return true;
		}
		return false;
	}

	//the same herd: one leads the other, or both follow the same one
	private boolean together( Colossus o ){
		return leader == o.id() || o.leader == id() || (leader != -1 && o.leader == leader);
	}

	// *** the patch ***

	public int half(){
		return size / 2;
	}

	public int left(){
		return pos % Dungeon.level.width() - half();
	}

	public int top(){
		return pos / Dungeon.level.width() - half();
	}

	//top left cell of the patch
	public int origin(){
		return top() * Dungeon.level.width() + left();
	}

	public boolean covers( int cell ){
		if (cell < 0 || pos < 0) return false;
		int w = Dungeon.level.width();
		int x = cell % w - left(), y = cell / w - top();
		return x >= 0 && y >= 0 && x < size && y < size;
	}

	public ArrayList<Integer> cells(){
		ArrayList<Integer> out = new ArrayList<>();
		int w = Dungeon.level.width(), o = origin();
		for (int y = 0; y < size; y++){
			for (int x = 0; x < size; x++){
				out.add(o + x + y * w);
			}
		}
		return out;
	}

	//chebyshev distance from a cell to the nearest cell of the patch (0 = on it, 1 = touching)
	public int reach( int cell ){
		int w = Dungeon.level.width();
		int x = cell % w, y = cell / w;
		int dx = Math.max(0, Math.max(left() - x, x - (left() + size - 1)));
		int dy = Math.max(0, Math.max(top() - y, y - (top() + size - 1)));
		return Math.max(dx, dy);
	}

	public boolean touches( int cell ){
		return reach(cell) == 1;
	}

	//the free cell next to the patch that's closest to someone who wants to hit it
	public int approachCell( int from ){
		int best = -1, bestD = Integer.MAX_VALUE;
		Level l = Dungeon.level;
		int w = l.width();
		for (int y = top() - 1; y <= top() + size; y++){
			for (int x = left() - 1; x <= left() + size; x++){
				if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
				int c = x + y * w;
				if (covers(c) || !l.passable[c]) continue;
				int d = l.distance(from, c);
				if (d < bestD){
					bestD = d;
					best = c;
				}
			}
		}
		return best == -1 ? pos : best;
	}

	//is any part of it in sight of the local hero?
	public boolean seen(){
		boolean[] fov = Dungeon.level.heroFOV;
		for (int c : cells()){
			if (c >= 0 && c < fov.length && fov[c]) return true;
		}
		return false;
	}

	public void block( boolean[] passable ){
		if (!solid()){
			if (pos >= 0 && pos < passable.length) passable[pos] = false;
			return;
		}
		for (int c : cells()){
			if (c >= 0 && c < passable.length) passable[c] = false;
		}
	}

	//the line under its name when it's first seen
	public String epithet(){
		String s = Messages.get(this, "epithet");
		return s.equals(Messages.NO_TEXT_FOUND) ? "" : s;
	}

	//false while it's out of reach under the ground (the ash whale): then it only takes its middle
	//cell, like any small thing, and walks over nobody
	public boolean solid(){
		return true;
	}

	// *** walking ***

	//where this colossus can stand, by its middle cell. its own kind of ground can be widened
	//with walkable() (lava for the fiery ones, deep ash for the whale...)
	public boolean[] fits(){
		return fits(size, this);
	}

	//self may be null (a colossus that isn't there yet): then plain floor is the only ground
	public static boolean[] fits( int size, Colossus self ){
		Level l = Dungeon.level;
		int w = l.width(), h = l.height(), n = l.length();
		int[] sum = new int[(w + 1) * (h + 1)];
		for (int y = 0; y < h; y++){
			int row = 0;
			for (int x = 0; x < w; x++){
				int c = x + y * w;
				boolean bad = self != null ? !self.walkable(c) : !l.passable[c] || l.pit[c];
				if (!bad){
					Colossus other = at(c);
					bad = other != null && other != self;
				}
				row += bad ? 1 : 0;
				sum[(x + 1) + (y + 1) * (w + 1)] = sum[(x + 1) + y * (w + 1)] + row;
			}
		}
		boolean[] out = new boolean[n];
		int hf = size / 2;
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				int x0 = x - hf, y0 = y - hf, x1 = x0 + size, y1 = y0 + size;
				if (x0 < 0 || y0 < 0 || x1 > w || y1 > h) continue;
				int bad = sum[x1 + y1 * (w + 1)] - sum[x0 + y1 * (w + 1)] - sum[x1 + y0 * (w + 1)] + sum[x0 + y0 * (w + 1)];
				out[x + y * w] = bad == 0;
			}
		}
		return out;
	}

	protected boolean walkable( int cell ){
		Level l = Dungeon.level;
		return l.passable[cell] && !l.pit[cell];
	}

	public boolean fitsAt( int cell ){
		boolean[] f = fits();
		return cell >= 0 && cell < f.length && f[cell];
	}

	@Override
	protected boolean getCloser( int target ) {
		if (rooted || target == pos) return false;
		boolean[] f = fits();
		int step = PathFinder.getStep(pos, target, f);
		if (step == -1 || !f[step]) return false;
		walkTo(step);
		return true;
	}

	@Override
	protected boolean getFurther( int target ) {
		if (rooted || target == pos) return false;
		boolean[] f = fits();
		int best = -1, bestD = Dungeon.level.distance(pos, target);
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (c < 0 || c >= f.length || !f[c]) continue;
			int d = Dungeon.level.distance(c, target);
			if (d > bestD){
				bestD = d;
				best = c;
			}
		}
		if (best == -1) return false;
		walkTo(best);
		return true;
	}

	@Override
	public void move( int step, boolean travelling ) {
		super.move(step, travelling);
		ColossusPresence.step(this);
	}

	protected void walkTo( int step ){
		int from = pos;
		move(step);
		Climbing.carry(this);
		if (solid()) trample(from);
	}

	@Override
	protected boolean moveSprite( int from, int to ) {
		if (sprite.parent != null && seen()){
			sprite.move(from, to);
		} else {
			sprite.turnTo(from, to);
			sprite.place(to);
		}
		return true;
	}

	//whatever small thing is under the new patch gets shoved out of the way, and hurt
	protected void trample( int from ){
		ArrayList<Char> under = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch != this && !(ch instanceof Colossus) && covers(ch.pos) && !Climbing.on(ch, this)) under.add(ch);
		}
		Collections.sort(under, new Comparator<Char>() {
			@Override
			public int compare( Char a, Char b ) {
				return a.id() - b.id();
			}
		});
		for (Char ch : under){
			int to = shoveCell(ch.pos, from);
			if (to != -1){
				ch.pos = to;
				if (ch.sprite != null) ch.sprite.place(to);
				Dungeon.level.occupyCell(ch);
			}
			onTrample(ch);
			if (ch instanceof Hero) Dungeon.observe();
		}
	}

	//nearest free cell off the patch, preferring the side the colossus is walking away from
	private int shoveCell( int cell, int from ){
		Level l = Dungeon.level;
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int r = 1; r <= size + 1 && best == -1; r++){
			for (int c : ringCells(cell, r)){
				if (!l.passable[c] || l.pit[c] || covers(c) || Actor.findChar(c) != null) continue;
				int d = l.distance(c, from);
				if (d < bestD){
					bestD = d;
					best = c;
				}
			}
		}
		return best;
	}

	private static ArrayList<Integer> ringCells( int cell, int r ){
		ArrayList<Integer> out = new ArrayList<>();
		Level l = Dungeon.level;
		int w = l.width(), x0 = cell % w, y0 = cell / w;
		for (int y = y0 - r; y <= y0 + r; y++){
			for (int x = x0 - r; x <= x0 + r; x++){
				if (Math.max(Math.abs(x - x0), Math.abs(y - y0)) != r) continue;
				if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
				out.add(x + y * w);
			}
		}
		return out;
	}

	//being stepped on. by default a fraction of its normal hit, more the bigger it is. a herd on
	//the move kills what's in its way, but that's no kill of the party's: no experience for it
	protected void onTrample( Char ch ){
		int dmg = Math.max(1, damageRoll() * Math.min(size, 10) / 20);
		if (ch instanceof Mob && dmg >= ch.HP && buff(Migration.Migrant.class) != null) ((Mob) ch).EXP = 0;
		ch.damage(dmg, this);
	}

	// *** fighting ***

	//meat for the fire, and a bone from the big ones
	@Override
	public void die( Object cause ) {
		super.die(cause);
		Remains.drop(this);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return enemy != null && reach(enemy.pos) <= 1;
	}

	@Override
	protected boolean act() {
		//far from everyone it doesn't think at all, it just waits to be found
		if (farFromParty()){
			spend(TICK * 3);
			return true;
		}
		return super.act();
	}

	@Override
	protected boolean farFromParty(){
		//awake, the big ones come from wherever they are
		if (Awakening.calls(this)) return false;
		for (Hero h : Party.active() ? Party.living() : Collections.singletonList(Dungeon.hero)){
			if (h != null && h.isAlive() && reach(h.pos) <= AWAKE_RANGE) return false;
		}
		return true;
	}

	//somewhere it fits, for wandering
	public int randomFit(){
		boolean[] f = fits();
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < f.length; i++){
			if (f[i] && Dungeon.level.distance(i, pos) <= 20) cells.add(i);
		}
		return cells.isEmpty() ? pos : Random.element(cells);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("big_size", size);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (bundle.contains("big_size")) size = bundle.getInt("big_size");
	}

	//a few spots where something this big fits, spread out, for placing colossi on a new floor
	public static ArrayList<Integer> spots( Level level, int size, int count, int awayFrom, int minDist ){
		Level was = Dungeon.level;
		Dungeon.level = level;
		try {
			boolean[] f = fits(size, null);
			ArrayList<Integer> all = new ArrayList<>();
			for (int i = 0; i < f.length; i++){
				if (f[i] && (awayFrom < 0 || level.distance(i, awayFrom) >= minDist)) all.add(i);
			}
			Random.shuffle(all);
			ArrayList<Integer> out = new ArrayList<>();
			for (int c : all){
				boolean clear = true;
				for (int o : out){
					if (level.distance(o, c) < size + 2){
						clear = false;
						break;
					}
				}
				if (clear) out.add(c);
				if (out.size() >= count) break;
			}
			return out;
		} finally {
			Dungeon.level = was;
		}
	}
}
