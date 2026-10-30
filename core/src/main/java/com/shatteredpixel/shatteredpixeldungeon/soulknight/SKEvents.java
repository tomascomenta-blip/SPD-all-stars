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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKArmory;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKMerchant;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKStatue;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashSet;

//rooms that DO something. one per floor, saved w/ the floor, acts every turn like a blob
//(blob = same on every device, that's the whole reason)
//ambush: doors slam shut, 2-3 waves (marked a turn early), chest when the last one dies
//trap chest: open it and the walls start shooting at you for a bit. surprise
public class SKEvents extends Blob {

	{
		actPriority = BUFF_PRIO;
	}

	public ArrayList<Ambush> ambushes = new ArrayList<>();
	public ArrayList<TrapChest> trapChests = new ArrayList<>();

	public static SKEvents get( Level level ){
		return (SKEvents) level.blobs.get(SKEvents.class);
	}

	private static SKEvents getOrMake( Level level ){
		SKEvents ev = get(level);
		if (ev == null){
			ev = new SKEvents();
			ev.seed(level, 0, 0);
			level.blobs.put(SKEvents.class, ev);
		}
		return ev;
	}

	//levelgen. only the host builds floors
	//extra stuff for normal floors (not mobs/items)
	public static void decorate( Level level, ArrayList<Room> rooms ){
		HashSet<Room> busy = new HashSet<>();
		for (Room r : rooms){
			if (hasShop(level, r)) busy.add(r);
		}
		SKEvents ev = getOrMake(level);

		//ambush wants big rooms, trap chest is fine in small ones
		ArrayList<Room> open = new ArrayList<>();
		ArrayList<Room> small = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && !r.isEntrance() && !r.isExit() && !busy.contains(r)
					&& r.width() >= 6 && r.height() >= 6){
				int free = freeCells(level, r).size();
				if (r.width() >= 7 && r.height() >= 7 && free >= 16) open.add(r);
				else if (free >= 9) small.add(r);
			}
		}
		Random.shuffle(open);
		Random.shuffle(small);

		//-Dspd.test.skevents=1 -> ambush + trap chest on every floor
		boolean force = DevFlags.skEvents();
		//70%
		if (!open.isEmpty() && (Random.Int(10) < 7 || force)){
			Room r = open.remove(0);
			ev.ambushes.add(new Ambush(level, r));
			busy.add(r);
		}
		//trap chest ~half the time
		small.addAll(open);
		if (Random.Int(2) == 0 || force){
			for (Room r : small){
				TrapChest t = TrapChest.make(level, r);
				if (t != null){
					ev.trapChests.add(t);
					busy.add(r);
					break;
				}
			}
		}
		SKProps.place(level, rooms, busy);
		SKStatue.place(level, rooms);
		int props = 0, statues = 0;
		for (Mob m : level.mobs){
			if (m instanceof SKProps.SKProp) props++;
			if (m instanceof SKStatue) statues++;
		}
		CoopLog.line("sk decorate depth=" + Dungeon.depth + " ambush=" + ev.ambushes.size() + " trapchest="
				+ ev.trapChests.size() + " props=" + props + " statue=" + statues);
	}

	private static boolean hasShop( Level level, Room r ){
		for (Mob m : level.mobs){
			if ((m instanceof SKMerchant || m instanceof SKProps.SKProp) && r.inside(level.cellToPoint(m.pos))) return true;
		}
		for (Heap h : level.heaps.valueList()){
			if (h.type == Heap.Type.FOR_SALE && r.inside(level.cellToPoint(h.pos))) return true;
		}
		return false;
	}

	static ArrayList<Integer> freeCells( Level level, Room r ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int y = r.top + 1; y < r.bottom; y++){
			for (int x = r.left + 1; x < r.right; x++){
				int c = x + y*level.width();
				if (level.passable[c] && !level.pit[c] && !level.solid[c]) cells.add(c);
			}
		}
		return cells;
	}

	@Override
	public boolean act() {
		spend(TICK);
		if (Dungeon.level == null || Dungeon.level.blobs.get(SKEvents.class) != this) return true;
		for (Ambush a : ambushes) a.tick();
		for (TrapChest t : trapChests) t.tick();
		return true;
	}

	//was it a trap? (it was a trap)
	public static void onChestOpened( Heap heap ){
		SKEvents ev = Dungeon.level == null ? null : get(Dungeon.level);
		if (ev == null) return;
		for (TrapChest t : ev.trapChests){
			if (t.cell == heap.pos && t.timer == -1){
				t.spring();
			}
		}
	}

	/* ambush */

	public static class Ambush implements Bundlable {

		static final int WAITING = 0, FIGHTING = 1, DONE = 2;

		int left, top, right, bottom; //walls included
		int[] doors = new int[0];
		int state = WAITING;
		int wave, waves;
		int[] alive = new int[0];   //current wave's mob ids
		int[] marked = new int[0];

		public Ambush(){
		}

		Ambush( Level level, Room r ){
			left = r.left; top = r.top; right = r.right; bottom = r.bottom;
			ArrayList<Integer> ds = new ArrayList<>();
			for (Room.Door d : r.connected.values()){
				if (d != null) ds.add(level.pointToCell(d));
			}
			doors = toArray(ds);
		}

		boolean inside( int cell ){
			int w = Dungeon.level.width();
			int x = cell % w, y = cell / w;
			return x > left && x < right && y > top && y < bottom;
		}

		void tick(){
			switch (state){
				case WAITING:  waitForHero(); break;
				case FIGHTING: fight();       break;
			}
		}

		private void waitForHero(){
			Hero trigger = null;
			for (Hero h : Party.living()){
				if (inside(h.pos) && !nextToDoor(h.pos)){
					trigger = h;
					break;
				}
			}
			if (trigger == null) return;
			//don't close a door on someone standing in it. obviously. (it happened)
			for (int d : doors){
				if (Actor.findChar(d) != null) return;
			}
			Level level = Dungeon.level;
			for (int d : doors){
				Level.set(d, Terrain.LOCKED_DOOR);
				GameScene.updateMap(d);
			}
			pullParty(trigger);
			SKCombat.focusLocal();
			state = FIGHTING;
			waves = Dungeon.depth >= 11 ? 3 : 2;
			wave = 0;
			CoopLog.line("ambush starts depth=" + Dungeon.depth + " room=" + left + "," + top);
			GLog.w(Messages.get(SKEvents.class, "ambush"));
			Sample.INSTANCE.play(Assets.Sounds.ALERT);
			markWave();
		}

		private boolean nextToDoor( int cell ){
			for (int d : doors){
				if (Dungeon.level.adjacent(cell, d)) return true;
			}
			return false;
		}

		//pull nearby heroes in, like boss rooms
		private void pullParty( Hero trigger ){
			Level level = Dungeon.level;
			ArrayList<Integer> taken = new ArrayList<>();
			for (Hero h : Party.living()){
				if (inside(h.pos) || level.distance(h.pos, trigger.pos) > 6) continue;
				int best = -1;
				for (int c : roomCells()){
					if (taken.contains(c) || Actor.findChar(c) != null || !level.passable[c] || level.pit[c]) continue;
					if (best == -1 || level.distance(c, trigger.pos) < level.distance(best, trigger.pos)) best = c;
				}
				if (best == -1) break;
				taken.add(best);
				ScrollOfTeleportation.appear(h, best);
			}
		}

		private ArrayList<Integer> roomCells(){
			ArrayList<Integer> cells = new ArrayList<>();
			int w = Dungeon.level.width();
			for (int y = top + 1; y < bottom; y++){
				for (int x = left + 1; x < right; x++){
					cells.add(x + y*w);
				}
			}
			return cells;
		}

		//pick + mark spawn cells for the next wave
		private void markWave(){
			Level level = Dungeon.level;
			int count = Math.min(6, 2 + Dungeon.depth/6 + Math.max(0, Party.living().size() - 1));
			ArrayList<Integer> cells = new ArrayList<>();
			for (int c : roomCells()){
				if (!level.passable[c] || level.pit[c] || Actor.findChar(c) != null) continue;
				boolean nearHero = false;
				for (Hero h : Party.living()){
					if (level.distance(c, h.pos) < 2) nearHero = true;
				}
				if (!nearHero) cells.add(c);
			}
			Random.shuffle(cells);
			while (cells.size() > count) cells.remove(cells.size() - 1);
			marked = toArray(cells);
			wave++;
			//GLog.i("ambush wave " + wave + "/" + waves + " cells " + cells.size());
			SKCombat.mark(cells, 1f);
			for (int c : cells){
				if (level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.SMOKE), 3);
			}
		}

		private void fight(){
			if (marked.length > 0){
				spawnWave();
				return;
			}
			for (int id : alive){
				Actor a = Actor.findById(id);
				if (a instanceof Char && ((Char) a).isAlive()) return;
			}
			if (Party.living().isEmpty()) return;
			if (wave < waves){
				markWave();
			} else {
				finish();
			}
		}

		private void spawnWave(){
			Level level = Dungeon.level;
			ArrayList<Class<? extends Mob>> rotation = SKWorlds.mobRotation(Dungeon.depth);
			ArrayList<Integer> ids = new ArrayList<>();
			for (int c : marked){
				if (Actor.findChar(c) != null || !level.passable[c]) continue;
				Mob m = Reflection.newInstance(rotation.get(Random.Int(rotation.size())));
				SKChampion.roll(m);
				m.pos = c;
				m.state = m.HUNTING;
				GameScene.add(m);
				ScrollOfTeleportation.appearVFX(m);
				ids.add(m.id());
			}
			marked = new int[0];
			int[] a = new int[ids.size()];
			for (int i = 0; i < a.length; i++) a[i] = ids.get(i);
			alive = a;
			CoopLog.line("ambush wave " + wave + "/" + waves + " monsters=" + a.length);
		}

		private void finish(){
			state = DONE;
			Level level = Dungeon.level;
			for (int d : doors){
				if (level.map[d] == Terrain.LOCKED_DOOR){
					Level.set(d, Terrain.DOOR);
					GameScene.updateMap(d);
				}
			}
			int w = level.width();
			int center = (left + right)/2 + ((top + bottom)/2)*w;
			int cell = freeNear(center);
			Item prize = Random.Int(5) < 2
					? SKArmory.random(SKArmory.tierForDepth(Dungeon.depth))
					: Generator.random();
			Heap heap = level.drop(prize, cell);
			heap.drop(new Gold().random());
			heap.type = Heap.Type.CHEST;
			if (heap.sprite != null){
				heap.sprite.link();
				heap.sprite.drop();
			}
			if (level.heroFOV[cell]) CellEmitter.center(cell).burst(Speck.factory(Speck.STAR), 8);
			Sample.INSTANCE.play(Assets.Sounds.MASTERY, 0.6f, 1.2f);
			GLog.p(Messages.get(SKEvents.class, "ambush_done"));
			CoopLog.line("ambush done depth=" + Dungeon.depth);
		}

		private int freeNear( int cell ){
			Level level = Dungeon.level;
			if (level.passable[cell] && level.heaps.get(cell) == null && !level.pit[cell]) return cell;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = cell + n;
				if (level.passable[c] && level.heaps.get(c) == null && !level.pit[c]) return c;
			}
			for (int c : roomCells()){
				if (level.passable[c] && level.heaps.get(c) == null && !level.pit[c]) return c;
			}
			return cell;
		}

		@Override
		public void storeInBundle( Bundle b ) {
			b.put("rect", new int[]{left, top, right, bottom});
			b.put("doors", doors);
			b.put("state", state);
			b.put("wave", wave);
			b.put("waves", waves);
			b.put("alive", alive);
			b.put("marked", marked);
		}

		@Override
		public void restoreFromBundle( Bundle b ) {
			int[] r = b.getIntArray("rect");
			left = r[0]; top = r[1]; right = r[2]; bottom = r[3];
			doors = b.getIntArray("doors");
			state = b.getInt("state");
			wave = b.getInt("wave");
			waves = b.getInt("waves");
			alive = b.getIntArray("alive");
			marked = b.getIntArray("marked");
		}
	}

	// *** trap chests ***

	public static class TrapChest implements Bundlable {

		static final int SHOTS = 5;

		int cell;           //the chest
		int[] guns;         //shooter cells, in front of the walls
		int timer = -1;     //-1 = closed, then turns left

		public TrapChest(){
		}

		static TrapChest make( Level level, Room r ){
			int w = level.width();
			int center = level.pointToCell(r.center());
			if (!level.passable[center] || level.pit[center] || level.heaps.get(center) != null) return null;
			for (Mob m : level.mobs){
				if (m.pos == center) return null;
			}
			//4 guns, una en medio de cada pared
			ArrayList<Integer> guns = new ArrayList<>();
			int cx = (r.left + r.right)/2, cy = (r.top + r.bottom)/2;
			int[][] spots = {{cx, r.top + 1}, {cx, r.bottom - 1}, {r.left + 1, cy}, {r.right - 1, cy}};
			for (int[] s : spots){
				int c = s[0] + s[1]*w;
				if (level.passable[c] && !level.pit[c]) guns.add(c);
			}
			if (guns.size() < 2) return null;

			TrapChest t = new TrapChest();
			t.cell = center;
			t.guns = toArray(guns);
			Item prize = Random.Int(4) == 0
					? SKArmory.random(SKArmory.tierForDepth(Dungeon.depth) + 1)
					: Generator.random();
			Heap heap = level.drop(prize, center);
			heap.drop(new Gold().random());
			heap.type = Heap.Type.CHEST;
			return t;
		}

		void spring(){
			timer = SHOTS;
			GLog.n(Messages.get(SKEvents.class, "trap_chest"));
			Sample.INSTANCE.play(Assets.Sounds.TRAP);
			CoopLog.line("trap chest sprung depth=" + Dungeon.depth + " cell=" + cell);
		}

		void tick(){
			if (timer <= 0) return;
			timer--;
			Level level = Dungeon.level;
			int depth = Dungeon.scalingDepth();
			for (int g : guns){
				Hero target = null;
				for (Hero h : Party.living()){
					if (target == null || level.distance(g, h.pos) < level.distance(g, target.pos)) target = h;
				}
				int aim = target != null && level.distance(g, target.pos) <= 10 ? target.pos : cell;
				SKBulletField.get().fire(null, g, SKBulletField.angleTo(g, aim), 1.5f,
						ItemSpriteSheet.SHOT_BULLET,
						2 + depth/2, 5 + depth);
				if (level.heroFOV[g]) CellEmitter.center(g).burst(SparkParticle.FACTORY, 4);
			}
		}

		@Override
		public void storeInBundle( Bundle b ) {
			b.put("cell", cell);
			b.put("guns", guns);
			b.put("timer", timer);
		}

		@Override
		public void restoreFromBundle( Bundle b ) {
			cell = b.getInt("cell");
			guns = b.getIntArray("guns");
			timer = b.getInt("timer");
		}
	}

	private static int[] toArray( ArrayList<Integer> list ){
		int[] a = new int[list.size()];
		for (int i = 0; i < a.length; i++) a[i] = list.get(i);
		return a;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("ambushes", ambushes);
		bundle.put("trap_chests", trapChests);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ambushes.clear();
		trapChests.clear();
		for (Bundlable b : bundle.getCollection("ambushes")){
			if (b instanceof Ambush) ambushes.add((Ambush) b);
		}
		for (Bundlable b : bundle.getCollection("trap_chests")){
			if (b instanceof TrapChest) trapChests.add((TrapChest) b);
		}
	}

	@Override
	public String tileDesc() {
		return null;
	}
}
