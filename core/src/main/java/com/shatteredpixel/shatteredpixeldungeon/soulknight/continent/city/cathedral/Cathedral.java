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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.Awakening;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.FurnaceCathedral;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Climbing;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;

//going into the furnace cathedral. while it lies knocked down its doors stay open, and whoever
//stands on the doorstep can walk in with the party, like down any stairs. inside is a floor of its
//own (CathedralLevel). the floor outside stands still meanwhile, so what happens out there is
//settled here: how long it had left on the ground, when it gets up, whether its heart was broken.
//all of it is game state, saved with the continent and the same on every device
public class Cathedral {

	//the floor inside, in the branch of the city
	public static final int DEPTH = 24;

	//the furnace burns hotter every this many turns inside, up to MAX_HEAT
	public static final int HEAT_EVERY = 30, MAX_HEAT = 5;
	//turns inside before it gets up out there: a base, and more the longer it was going to lie
	private static final int RISE = 70, RISE_PER = 6;
	//once it's up, the inside shakes and things fall every this many turns
	public static final int DEBRIS_EVERY = 4;
	//the furnace lights the inside a little
	public static final int GLOW_SIGHT = 5;

	//how they come out
	public static final int NONE = 0, DOOR = 1, BROKEN = 2;

	//the cathedral the party went into and which of its falls let them in
	private static int who = -1, fall = -1;
	//the floor out there and the doorstep on it
	private static int fromDepth = 0, fromBranch = 0, door = -1;
	//turns spent inside while it was down this time, and when it gets up
	private static int clock = 0, riseAt = 0;
	private static int outcome = NONE;
	//the heart of the one they're in is broken: whatever way they come out, it's dead
	private static boolean broken = false;
	//which cathedral the floor inside was made for
	private static int madeFor = -1;
	//testing (CathedralTest): the party walks in as soon as it arrives
	static boolean testEnter = false;

	public static void reset(){
		who = -1;
		fall = -1;
		fromDepth = fromBranch = 0;
		door = -1;
		clock = riseAt = 0;
		outcome = NONE;
		broken = false;
		madeFor = -1;
		testEnter = false;
	}

	//with the rest of the continent (Continent.store)
	public static void store( Bundle b ){
		Bundle c = new Bundle();
		c.put("who", who);
		c.put("fall", fall);
		c.put("from_depth", fromDepth);
		c.put("from_branch", fromBranch);
		c.put("door", door);
		c.put("clock", clock);
		c.put("rise_at", riseAt);
		c.put("outcome", outcome);
		c.put("broken", broken);
		c.put("made_for", madeFor);
		c.put("test_enter", testEnter);
		b.put("cathedral", c);
	}

	public static void restore( Bundle b ){
		reset();
		if (!b.contains("cathedral")) return;
		Bundle c = b.getBundle("cathedral");
		who = c.getInt("who");
		fall = c.getInt("fall");
		fromDepth = c.getInt("from_depth");
		fromBranch = c.getInt("from_branch");
		door = c.getInt("door");
		clock = c.getInt("clock");
		riseAt = c.getInt("rise_at");
		outcome = c.getInt("outcome");
		broken = c.getBoolean("broken");
		madeFor = c.getInt("made_for");
		testEnter = c.getBoolean("test_enter");
	}

	// *** where they are ***

	public static boolean here(){
		return Dungeon.level instanceof CathedralLevel;
	}

	public static boolean depth( int depth, int branch ){
		return depth == DEPTH && branch == Continent.CITY;
	}

	//the place, for the log and the menu (Continent.place)
	public static String place(){
		return Messages.get(Cathedral.class, "place");
	}

	public static String arrival(){
		return Messages.get(Cathedral.class, "arrive");
	}

	//under the depth icon: the layer it lies on out there
	public static String depthLabel(){
		return fromDepth <= 21 ? "0" : "-" + (fromDepth - 21);
	}

	public static int heat(){
		return Math.min(MAX_HEAT, clock / HEAT_EVERY);
	}

	public static int clock(){
		return clock;
	}

	public static boolean risen(){
		return clock >= riseAt;
	}

	//turns left before it gets up, 0 once it has
	public static int toRise(){
		return Math.max(0, riseAt - clock);
	}

	//fire in there hurts more the hotter it burns
	public static int fire( int dmg ){
		return Math.round(dmg * (1f + 0.15f * heat()));
	}

	private static ArrayList<Hero> party(){
		ArrayList<Hero> list = new ArrayList<>();
		if (Party.active()) list.addAll(Party.heroes());
		else if (Dungeon.hero != null) list.addAll(Collections.singletonList(Dungeon.hero));
		return list;
	}

	// *** the doors ***

	//FurnaceCathedral.knockDown: it fell on its side and the portal hangs open
	public static void openDoors( Level l, FurnaceCathedral c ){
		if (l == null || !c.isAlive()) return;
		if (Awakening.on()){
			//awake, the furnace lets nobody in (and there's no time for it)
			CoopLog.line("cathedral: down, but the continent is awake: the doors stay shut");
			if (c.seen()) GLog.w(Messages.get(Cathedral.class, "shut_awake"));
			return;
		}
		int cell = threshold(l, c);
		if (cell == -1) return;
		dropDoor(l, c.id());
		ArrayList<LevelTransition> list = new ArrayList<>(l.transitions);
		list.add(new CathedralDoor(l, cell, c.id()));
		l.transitions = list;
		CoopLog.line("cathedral: doors open at " + cell + " for " + c.down + " turns t=" + (int) Actor.now());
		if (c.seen()) GLog.p(Messages.get(Cathedral.class, "open"));
	}

	//it got up or died: the doors are gone
	public static void shutDoors( FurnaceCathedral c ){
		Level l = Dungeon.level;
		if (l == null || doorOf(l, c.id()) == null) return;
		dropDoor(l, c.id());
		CoopLog.line("cathedral: doors shut t=" + (int) Actor.now());
	}

	//the local hero just stepped onto the open doorstep: how to go in (only on this screen)
	private static int hinted = -1;

	public static void hint( FurnaceCathedral c ){
		Hero me = Dungeon.local();
		CathedralDoor d = c.down > 0 && Dungeon.level != null ? doorOf(Dungeon.level, c.id()) : null;
		if (d == null || me == null || !d.inside(me.pos)){
			if (me != null && hinted == c.id() && (d == null || !d.inside(me.pos))) hinted = -1;
			return;
		}
		if (hinted == c.id()) return;
		hinted = c.id();
		GLog.i(Messages.get(Cathedral.class, "threshold"));
	}

	public static CathedralDoor doorOf( Level l, int id ){
		for (LevelTransition t : l.transitions){
			if (t instanceof CathedralDoor && ((CathedralDoor) t).cathedral == id) return (CathedralDoor) t;
		}
		return null;
	}

	//the doorstep, or the cells beside it if a wall is right in front of the portal
	private static int threshold( Level l, FurnaceCathedral c ){
		int step = c.doorstep();
		int w = l.width();
		int[] tries = {step, step - 1, step + 1};
		for (int t : tries){
			if (t < w || t >= l.length() - w || t / w != step / w) continue;
			if (l.passable[t] && !l.pit[t] && !c.covers(t)) return t;
		}
		return -1;
	}

	// *** going in ***

	//a hero on the doorstep walks in (AshLand.activateTransition). false: it stays shut
	public static boolean enter( Hero hero, CathedralDoor d ){
		FurnaceCathedral c = d.cathedral();
		if (c == null || c.down <= 0 || Awakening.on()){
			if (hero == Dungeon.local()) GLog.w(Messages.get(Cathedral.class, Awakening.on() ? "shut_awake" : "shut"));
			return false;
		}
		//the same fall as last time: the clock goes on from where they left it
		if (who != c.id() || fall != c.falls){
			clock = 0;
			broken = false;
			riseAt = RISE + RISE_PER * Math.min(10, c.down);
		}
		who = c.id();
		fall = c.falls;
		fromDepth = Dungeon.depth;
		fromBranch = Dungeon.branch;
		door = d.cell();
		outcome = NONE;
		//a different cathedral has a different inside
		if (madeFor != c.id()){
			Dungeon.generatedLevels.remove((Integer) (DEPTH + 1000 * Continent.CITY));
			madeFor = c.id();
		}
		for (Hero h : party()){
			Climbing b = h.buff(Climbing.class);
			if (b != null) b.detach();
		}
		CoopLog.line("cathedral: hero " + Party.indexOf(hero) + " walks in, " + c.down + " turns left down, rises in "
				+ (riseAt - clock) + " t=" + (int) Actor.now());
		Sample.INSTANCE.play(Assets.Sounds.OPEN, 1f, 0.6f);
		return true;
	}

	//the id the floor inside was made with (it mixes it into the seed)
	static int madeFor(){
		return madeFor;
	}

	// *** coming out ***

	//through the portal, or thrown out when the heart breaks: back by the doorstep
	public static void leave( int how ){
		if (broken) how = BROKEN;
		outcome = how;
		CoopLog.line("cathedral: the party comes out (" + (how == BROKEN ? "heart broken" : "doors")
				+ ") after " + clock + " turns, heat " + heat() + (risen() ? ", it was up" : ""));
		Level.beforeTransition();
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = fromDepth;
		InterlevelScene.returnBranch = fromBranch;
		InterlevelScene.returnPos = door;
		Game.switchScene(InterlevelScene.class);
	}

	//Continent.onArrive, every floor and every load: has to be safe to run twice
	public static void onArrive(){
		Level l = Dungeon.level;
		if (l instanceof CathedralLevel){
			CathedralTest.inside(l);
			return;
		}
		CathedralTest.outside(l);
		if (outcome == NONE) return;
		int how = outcome;
		outcome = NONE;
		if (Dungeon.depth != fromDepth || Dungeon.branch != fromBranch) return;
		FurnaceCathedral c = null;
		for (Mob m : l.mobs){
			if (m instanceof FurnaceCathedral && m.id() == who) c = (FurnaceCathedral) m;
		}
		if (c == null || !c.isAlive()) return;
		if (how == BROKEN){
			c.down = Math.max(c.down, 20);
			Buff.affect(c, Collapse.class);
			dropDoor(l, c.id());
			CoopLog.line("cathedral: out by its corpse, it falls apart next turn");
		} else if (risen()){
			c.down = 0;
			dropDoor(l, c.id());
			CoopLog.line("cathedral: out by the doors, it's standing again");
		} else {
			CoopLog.line("cathedral: out by the doors, still down for " + c.down);
		}
	}

	private static void dropDoor( Level l, int id ){
		CathedralDoor d = doorOf(l, id);
		if (d == null) return;
		ArrayList<LevelTransition> list = new ArrayList<>(l.transitions);
		list.remove(d);
		l.transitions = list;
	}

	// *** the clock inside ***

	//CathedralLevel.Kiln, once a turn while the party is inside
	static void tick( CathedralLevel l ){
		if (Party.living().isEmpty()) return;
		//the heart is broken but whoever was to lead them out fell: someone else does
		if (broken && outcome == NONE){
			boolean going = false;
			for (Hero h : Party.living()) going |= h.buff(Expel.class) != null;
			if (!going) Buff.affect(Party.living().get(0), Expel.class);
		}
		int was = heat();
		boolean wasUp = risen();
		clock++;
		if (heat() > was){
			CoopLog.line("cathedral: heat " + heat() + " t=" + (int) Actor.now());
			l.hotter(heat());
		}
		if (risen() && !wasUp){
			CoopLog.line("cathedral: it gets up outside t=" + (int) Actor.now());
			l.risen();
		}
		if (risen() && (clock - riseAt) % DEBRIS_EVERY == 0){
			l.debris();
		}
	}

	//the heart is broken: a moment for the walls to come down, then out
	static void heartBroken(){
		broken = true;
		CoopLog.line("cathedral: the heart breaks after " + clock + " turns, heat " + heat());
		Hero leader = null;
		for (Hero h : party()){
			if (leader == null && h.isAlive() && !h.coopDown) leader = h;
		}
		if (leader != null) Buff.affect(leader, Expel.class);
	}

	//on the leader: two turns of the cathedral coming apart, then everyone is out
	public static class Expel extends Buff {

		{
			type = buffType.NEUTRAL;
			revivePersists = true;
		}

		private int left = 3;

		@Override
		public boolean act() {
			if (--left > 0){
				spend(TICK);
				return true;
			}
			detach();
			leave(BROKEN);
			return false;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("left", left);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt("left");
		}
	}

	//on the cathedral out there, once the party is back by it: it dies on its first turn, with
	//everything it kept inside spilling round its doors
	public static class Collapse extends Buff {

		{
			type = buffType.NEUTRAL;
		}

		@Override
		public boolean act() {
			detach();
			if (!(target instanceof FurnaceCathedral) || !target.isAlive()) return true;
			FurnaceCathedral c = (FurnaceCathedral) target;
			int at = c.pos;
			GLog.p(Messages.get(Cathedral.class, "dies"));
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
			c.die(this);
			spoils(at);
			return true;
		}
	}

	//what the furnace kept: gold, a good find, healing and a scroll
	private static void spoils( int at ){
		Level l = Dungeon.level;
		ArrayList<Item> loot = new ArrayList<>();
		loot.add(new Gold(500 + Random.Int(301)));
		Generator.Category[] good = {Generator.Category.RING, Generator.Category.WAND, Generator.Category.ARTIFACT};
		loot.add(Generator.random(good[Random.Int(good.length)]));
		loot.add(new PotionOfHealing().quantity(2));
		loot.add(new ScrollOfUpgrade());
		loot.add(new Torch().quantity(2));
		ArrayList<Integer> cells = new ArrayList<>();
		for (int r = 0; r <= 3 && cells.size() < loot.size(); r++){
			for (int n : PathFinder.NEIGHBOURS9){
				int c = at + n * Math.max(1, r);
				if (c >= 0 && c < l.length() && l.passable[c] && !l.pit[c] && !cells.contains(c)) cells.add(c);
			}
		}
		if (cells.isEmpty()) cells.add(at);
		for (int i = 0; i < loot.size(); i++){
			Item it = loot.get(i);
			if (it == null) continue;
			Heap h = l.drop(it, cells.get(i % cells.size()));
			if (h.sprite != null) h.sprite.drop();
		}
		CoopLog.line("cathedral: spoils " + loot.size() + " things round " + at);
	}
}
