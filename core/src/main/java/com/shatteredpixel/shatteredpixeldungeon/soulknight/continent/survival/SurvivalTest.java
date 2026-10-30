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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.DarkTaint;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.TaintedFood;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.BigTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//testing only, all of it behind -Dspd.test.surv=1 / survmeat=1 / tether=1.
//turn(): game side, at the start of the first living hero's turn: the bags, the colossus to carve
//and a line of numbers now and then. solo()/act()/answer(): the bot, render thread, one step after
//another like a player would: fire, cook, the recipes, the sacred torch, eat, poultice, bait, rest.
//with tether=1 the second hero of a co-op run walks off alone instead, waits for what comes, and goes back
public class SurvivalTest {

	private static final boolean SURV = DevFlags.survTest(), MEAT = DevFlags.survMeat(), TETHER = DevFlags.tetherTest();

	public static boolean on(){
		return SURV || MEAT || TETHER;
	}

	// *** game side ***

	private static int depth = -1, branch = -1, start, rounds;
	private static boolean ready = false, meatDone = false;

	//Actor.now() goes back on every save, this doesn't
	private static int clock(){
		return (int) (Statistics.duration + Actor.now());
	}
	private static Colossus carcass;

	public static void turn( Hero hero ){
		if (!on() || !Continent.in()) return;
		ArrayList<Hero> living = Party.living();
		if (living.isEmpty() || living.get(0) != hero) return;
		if (Dungeon.depth != depth || Dungeon.branch != branch){
			depth = Dungeon.depth;
			branch = Dungeon.branch;
			ready = false;
			meatDone = false;
			carried = 0;
			carcass = null;
			rounds = 0;
		}
		clearFoes();
		if (!ready){
			ready = true;
			start = clock();
			if (SURV){
				for (Hero h : heroes()) bag(h);
				Continent.essence = Math.max(Continent.essence, 3);
			}
			if (TETHER){
				//the one walking off needs to know where it can go
				for (int c = 0; c < Dungeon.level.length(); c++){
					if (!Dungeon.level.solid[c]) Dungeon.level.mapped[c] = true;
				}
				Continent.essence = Math.max(Continent.essence, 1);
			}
			CoopLog.line("surv test: ready on floor " + Continent.floor() + " layer " + Survival.layer() + " essence " + Continent.essence);
		}
		if (MEAT) meat(hero);
		if (TETHER && Party.active()) carry();
		else if (TETHER && (walkTarget == -1 || rounds % 10 == 0)) planWalk();
		//hurt, so the fire and the poultice have something to do (after the test levels are in)
		if (SURV && rounds == 4){
			for (Hero h : heroes()) h.HP = Math.max(1, h.HT / 3);
			CoopLog.line("surv test: heroes hurt to a third");
		}
		if (++rounds % 10 == 0) CoopLog.line("surv test: t=" + clock() + " " + state());
	}

	private static List<Hero> heroes(){
		if (Party.active()) return Party.heroes();
		return Collections.singletonList(Dungeon.hero);
	}

	private static void bag( Hero h ){
		Survival.give(h, new CampfireKit());
		Survival.give(h, new ColossusMeat.Raw().quantity(3));
		Survival.give(h, new TaintedFood.TaintedMeat().quantity(2));
		Survival.give(h, new TaintedFood.TaintedRation());
		Survival.give(h, new Torch().quantity(4));
		Survival.give(h, new ColossusBone().quantity(2));
		Continent.taint(h, 40);
		CoopLog.line("surv test: bag for hero " + Party.indexOf(h));
	}

	//the floor's own monsters stay out of it
	private static void clearFoes(){
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment != Char.Alignment.ENEMY || m instanceof Stalker || m == carcass) continue;
			m.EXP = 0;
			m.destroy();
			if (m.sprite != null) m.sprite.killAndErase();
		}
	}

	//a colossus by the party, and three turns later it falls: Colossus.die -> Remains
	private static void meat( Hero hero ){
		if (meatDone) return;
		int t = clock() - start;
		if (carcass == null && t >= 3){
			//a big one if there's room for it, a small one if not
			int size = 5, best = spot(5, hero.pos);
			if (best == -1){
				size = 3;
				best = spot(3, hero.pos);
			}
			if (best == -1){
				CoopLog.line("surv test: no room for a colossus");
				meatDone = true;
				return;
			}
			carcass = new BigTest.Obsidian();
			carcass.size = size;
			carcass.pos = best;
			carcass.state = carcass.PASSIVE;
			GameScene.add(carcass);
			CoopLog.line("surv test: colossus at " + best);
		} else if (carcass != null && t >= 6){
			int at = carcass.pos;
			carcass.die(hero);
			StringBuilder s = new StringBuilder("surv test: the colossus fell, left:");
			for (Heap h : Dungeon.level.heaps.valueList()){
				if (Dungeon.level.distance(h.pos, at) > 3) continue;
				for (Item i : h.items) s.append(' ').append(i.getClass().getSimpleName()).append('x').append(i.quantity());
			}
			CoopLog.line(s.toString());
			carcass = null;
			meatDone = true;
		}
	}

	//co-op: the second hero is carried off alone and brought back later, all on the turn thread.
	//the old floors are cut up by lava, walking off a screen away isn't something a bot does reliably
	private static int carried = 0, awayAt;

	private static void carry(){
		Hero w = Party.get(1), home = Party.get(0);
		if (w == null || home == null || !w.isAlive() || carried == 2) return;
		int t = clock() - start;
		if (carried == 0 && t >= 8){
			int cell = -1, bestScore = Integer.MAX_VALUE;
			for (int c = 0; c < Dungeon.level.length(); c++){
				if (!Dungeon.level.passable[c] || Dungeon.level.avoid[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
				int score = Math.abs(Dungeon.level.distance(c, home.pos) - 22);
				if (score < bestScore){
					cell = c;
					bestScore = score;
				}
			}
			if (cell == -1){
				CoopLog.line("surv test: nowhere to carry the second hero");
				carried = 2;
				return;
			}
			move(w, cell);
			carried = 1;
			awayAt = t;
			CoopLog.line("surv test: hero 1 carried off to " + cell + ", " + Dungeon.level.distance(cell, home.pos) + " from the others");
		} else if (carried == 1 && t >= awayAt + 30){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = home.pos + n;
				if (Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null){
					move(w, c);
					CoopLog.line("surv test: hero 1 brought back to " + c);
					break;
				}
			}
			carried = 2;
		}
	}

	private static void move( Hero h, int cell ){
		ScrollOfTeleportation.appear(h, cell);
		Dungeon.level.occupyCell(h);
		Dungeon.observe();
	}

	//where the one walking off should go: the closest cell it can really reach that is well away
	//from the others. worked out here, on the turn thread, where the path finder may be used
	private static int walkTarget = -1;

	private static void planWalk(){
		Hero w = Party.active() ? Party.get(1) : Dungeon.hero;
		if (w == null || !w.isAlive()) return;
		int home = Party.active() ? Party.get(0).pos : Dungeon.level.entrance();
		int len = Dungeon.level.length();
		//the ground the hero's own path would take (known cells, and whatever the hero keeps off)
		boolean[] known = new boolean[len];
		for (int c = 0; c < len; c++){
			known[c] = Dungeon.level.passable[c] && (Dungeon.level.visited[c] || Dungeon.level.mapped[c]);
		}
		boolean[] ok = Dungeon.findPassable(w, known, Dungeon.level.heroFOV, false).clone();
		PathFinder.buildDistanceMap(w.pos, ok);
		//the closest walk that leaves the others behind; if there's none, as far as it gets
		int best = -1, bestD = Integer.MAX_VALUE, far = -1, farD = -1;
		for (int c = 0; c < len; c++){
			int d = PathFinder.distance[c];
			if (d == Integer.MAX_VALUE) continue;
			int fromHome = Dungeon.level.distance(c, home);
			if (d < bestD && fromHome >= Tether.LEASH + 5){
				best = c;
				bestD = d;
			}
			if (fromHome > farD){
				far = c;
				farD = fromHome;
			}
		}
		if (best == -1) best = far;
		if (best != walkTarget) CoopLog.line("surv test: the walker heads for " + best + " (" + (best == -1 ? "-" : PathFinder.distance[best] + " steps") + ")");
		walkTarget = best;
	}

	private static int spot( int size, int near ){
		boolean[] fits = Colossus.fits(size, null);
		int best = -1, bestD = 99;
		for (int c = 0; c < fits.length; c++){
			int d = Dungeon.level.distance(c, near);
			if (fits[c] && !Dungeon.level.water[c] && !Dungeon.level.avoid[c] && d >= size / 2 + 2 && d < bestD){
				best = c;
				bestD = d;
			}
		}
		return best;
	}

	private static String state(){
		StringBuilder s = new StringBuilder();
		for (Hero h : heroes()){
			Tether t = h.buff(Tether.class);
			SacredLight l = h.buff(SacredLight.class);
			s.append("| hero ").append(Party.indexOf(h)).append(" @").append(h.pos).append(' ').append(h.HP).append('/').append(h.HT)
					.append(" taint ").append(DarkTaint.of(h)).append(" sight ").append(h.viewDistance);
			if (t != null) s.append(t.lost ? " ALONE " + t.away : " near " + Tether.nearestFriend(h));
			if (Stalker.of(h) != null) s.append(" hunted@").append(Stalker.of(h).pos);
			if (l != null) s.append(" sacred ").append(l.left);
			s.append(' ');
		}
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Campfire) s.append("| fire @").append(m.pos).append(" fuel ").append(((Campfire) m).fuel).append(' ');
		}
		s.append("| essence ").append(Continent.essence);
		return s.toString();
	}

	// *** the bot ***

	private static final String[] PLAN = {"meat", "place", "look", "bait", "poultice", "kit", "torches", "sacred",
			"cook", "light", "eat", "apply", "throw", "rest", "bag", "done"};
	private static final String[] WALK = {"walkoff", "alone", "return", "back", "done"};

	private static int step = 0, tries = 0, waited = 0, mark = 0;
	private static float timer = 1f, lastClock = -1;
	private static String shotName = null;
	private static float shotTimer = 0;
	private static boolean bagOpen = false;

	private static boolean walker( Hero me ){
		return TETHER && (!Party.active() || Party.indexOf(me) == 1);
	}

	//where the others are: the leader in co-op, the way in when testing alone
	private static int home(){
		return Party.active() ? Party.get(0).pos : Dungeon.level.entrance();
	}

	private static String current( Hero me ){
		String[] plan = walker(me) ? WALK : PLAN;
		return step < plan.length ? plan[step] : "done";
	}

	private static void next( String why ){
		CoopLog.line("surv bot: " + why);
		step++;
		tries = 0;
		waited = 0;
	}

	//screenshots a little later, once the frame has been drawn
	private static void shot( String name, float delay ){
		shotName = name;
		shotTimer = delay;
	}

	private static void frame(){
		if (shotName == null) return;
		if ((shotTimer -= Game.elapsed) > 0) return;
		String dir = System.getProperty("spd.coop.shots");
		if (dir != null){
			int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
			Pixmap p = Pixmap.createFromFrameBuffer(0, 0, w, h);
			Pixmap flipped = new Pixmap(w, h, p.getFormat());
			for (int y = 0; y < h; y++) flipped.drawPixmap(p, 0, y, w, 1, 0, h - 1 - y, w, 1);
			String who = Party.active() ? "_" + Party.localIndex() : "";
			PixmapIO.writePNG(Gdx.files.absolute(dir + "/surv_" + shotName + who + ".png"), flipped);
			p.dispose();
			flipped.dispose();
			CoopLog.line("surv bot: shot " + shotName);
		}
		shotName = null;
	}

	//solo runs: nobody else drives the hero
	public static void solo( Hero me ){
		if (!on() || me == null || !me.isAlive()) return;
		frame();
		if (!me.ready || !Continent.in()) return;
		if ((timer -= Game.elapsed) > 0) return;
		timer = 0.15f;
		Window w = GameScene.topWindow();
		if (w != null){
			answer(w);
			return;
		}
		if (GameScene.isSelectingCell()){
			GameScene.handleCell(aim(me));
			return;
		}
		if (!act(me)) CoopOrders.named("rest");
	}

	//co-op: CoopAuto asks first, false = let the usual bot play
	public static boolean act( Hero me ){
		if (!on() || me == null || !me.isAlive() || !Continent.in()) return false;
		frame();
		//orders that don't spend any time: stuck on this step. the game clock is pulled back on
		//every save (Actor.fixTime), what it took away goes to Statistics.duration
		float clock = Statistics.duration + Actor.now();
		if (clock == lastClock){
			if (++tries > 60){
				if (walker(me) && current(me).equals("walkoff")){
					//no way there from here: wait for another plan
					tries = 0;
					walkTarget = -1;
					CoopLog.line("surv bot: can't get there, waiting for another way");
				} else {
					next("gave up on " + current(me));
				}
				return order("rest");
			}
		} else {
			lastClock = clock;
			tries = 0;
		}
		return walker(me) ? walk(me) : plan(me);
	}

	private static boolean order( String named ){
		CoopOrders.named(named);
		return true;
	}

	private static Campfire fire( Hero me ){
		Campfire best = null;
		for (Mob m : Dungeon.level.mobs){
			if (!(m instanceof Campfire) || !((Campfire) m).lit()) continue;
			if (best == null || Dungeon.level.distance(me.pos, m.pos) < Dungeon.level.distance(me.pos, best.pos)) best = (Campfire) m;
		}
		return best;
	}

	private static boolean plan( Hero me ){
		String s = current(me);
		Campfire fire = fire(me);
		switch (s){
			case "meat": {
				if (!MEAT){
					next("no colossus to carve");
					return plan(me);
				}
				if (!meatDone || carcass != null) return order("rest");
				Heap best = null;
				for (Heap h : Dungeon.level.heaps.valueList()){
					boolean ours = false;
					for (Item i : h.items) ours |= i instanceof ColossusMeat.Raw || i instanceof ColossusBone;
					if (ours && (best == null || Dungeon.level.distance(me.pos, h.pos) < Dungeon.level.distance(me.pos, best.pos))) best = h;
				}
				//one item per pick-up, and the heap may hold more than the colossus left
				if (best == null || ++waited > 240){
					next("picked up the colossus: raw " + Survival.count(me, ColossusMeat.Raw.class) + " bones " + Survival.count(me, ColossusBone.class));
					return order("rest");
				}
				CoopOrders.handleCell(best.pos);
				return true;
			}
			case "place": {
				if (fire != null && Dungeon.level.distance(me.pos, fire.pos) <= 8){
					next("there's a fire at " + fire.pos);
					return order("rest");
				}
				CampfireKit kit = me.belongings.getItem(CampfireKit.class);
				if (kit == null){
					next("no kit to place");
					return order("rest");
				}
				if (Campfire.spotNear(me.pos) == -1){
					if (++waited > 12){
						next("no room for the fire anywhere");
						return order("rest");
					}
					int open = openGround(me);
					if (open == -1) return order("rest");
					CoopOrders.handleCell(open);
					return true;
				}
				CoopOrders.execute(kit, CampfireKit.AC_PLACE);
				return true;
			}
			case "look":
				if (fire == null){
					next("no fire to look at");
					return order("rest");
				}
				if (waited++ == 1) shot("fogata", 0.5f);
				if (waited >= 4) next("had a look at the fire, hp " + me.HP + "/" + me.HT + " taint " + DarkTaint.of(me));
				return order("rest");
			case "cook": case "kit": case "torches": case "bait": case "poultice": case "sacred":
				if (fire == null){
					next("no fire for " + s);
					return order("rest");
				}
				//tapping it walks there and sits down: the window opens
				CoopOrders.handleCell(fire.pos);
				return true;
			case "light": {
				SacredTorch t = me.belongings.getItem(SacredTorch.class);
				if (me.buff(SacredLight.class) != null){
					next("sacred light burning, essence " + Continent.essence + ", sight " + me.viewDistance);
					shot("luz_sagrada", 1.0f);
					return order("rest");
				}
				if (t == null){
					next("no sacred torch");
					return order("rest");
				}
				CoopOrders.execute(t, SacredTorch.AC_LIGHT);
				return true;
			}
			case "eat": {
				ColossusMeat.Roast r = me.belongings.getItem(ColossusMeat.Roast.class);
				next(r == null ? "nothing roasted to eat" : "eats the roast, taint " + DarkTaint.of(me));
				if (r == null) return order("rest");
				CoopOrders.execute(r, Food.AC_EAT);
				return true;
			}
			case "apply": {
				Poultice p = me.belongings.getItem(Poultice.class);
				next(p == null ? "no poultice" : "poultice on, hp " + me.HP + "/" + me.HT + " taint " + DarkTaint.of(me));
				if (p == null) return order("rest");
				CoopOrders.execute(p, Poultice.AC_APPLY);
				return true;
			}
			case "throw": {
				Bait b = me.belongings.getItem(Bait.class);
				next(b == null ? "no bait" : "throws the bait");
				if (b == null) return order("rest");
				CoopOrders.execute(b, Item.AC_THROW);
				return true;
			}
			case "rest":
				if (fire != null && Dungeon.level.distance(me.pos, fire.pos) > 1 && waited == 0){
					CoopOrders.handleCell(beside(fire.pos));
					return true;
				}
				if (++waited % 5 == 0) CoopLog.line("surv bot: resting by the fire hp " + me.HP + "/" + me.HT + " taint " + DarkTaint.of(me));
				if (waited == 12) shot("descanso", 0.3f);
				if (waited >= 25) next("rested");
				return order("rest");
			case "bag":
				if (Party.active()){
					next("bag only in solo");
					return order("rest");
				}
				if (!bagOpen){
					bagOpen = true;
					GameScene.show(new WndBag(me.belongings.backpack));
					shot("inventario", 0.8f);
					return true;
				}
				return true;
			default:
				if (mark++ == 0) CoopLog.line("surv bot: done " + state());
				return order("rest");
		}
	}

	//a free cell next to that one (the fire, the leader)
	private static int beside( int cell ){
		int w = Dungeon.level.width();
		for (int c : new int[]{cell - 1, cell + 1, cell - w, cell + w}){
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null) return c;
		}
		return cell;
	}

	private static boolean walk( Hero me ){
		Tether t = me.buff(Tether.class);
		Stalker st = Stalker.of(me);
		switch (current(me)){
			case "walkoff":
				if (t != null && t.lost){
					mark = clock();
					next("alone at " + me.pos + ", " + Tether.nearestFriend(me) + " from the others");
					shot("solo", 0.8f);
					return order("rest");
				}
				//co-op: carried off by the game side. alone: there already, or nowhere to go yet
				if (Party.active() || walkTarget == -1 || walkTarget == me.pos) return order("rest");
				CoopOrders.handleCell(walkTarget);
				return true;
			case "alone": {
				int t0 = clock() - mark;
				if (st != null && waited == 0){
					waited = 1;
					CoopLog.line("surv bot: something hunts me from " + st.pos + " after " + t0 + " turns");
				}
				if (st != null && Dungeon.level.distance(st.pos, me.pos) <= 2 && waited == 1){
					waited = 2;
					shot("acechador", 0.3f);
				}
				if (waited >= 2 && t0 >= 18 || t0 >= 60){
					next("going back after " + t0 + " turns alone, taint " + DarkTaint.of(me));
					return order("rest");
				}
				return order("rest");
			}
			case "return":
				if (t == null || !t.lost){
					next("back with the group at " + me.pos + ", taint " + DarkTaint.of(me));
					return order("rest");
				}
				if (Party.active()) return order("rest");
				CoopOrders.handleCell(beside(home()));
				return true;
			case "back":
				if (st == null){
					next("the stalker is gone");
					shot("vuelta", 0.5f);
				}
				return order("rest");
			default:
				if (mark != -1){
					mark = -1;
					CoopLog.line("surv bot: walk done " + state());
				}
				return order("rest");
		}
	}

	//a known cell with floor all around it, near enough, for the fire
	private static int openGround( Hero me ){
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int c = 0; c < Dungeon.level.length(); c++){
			if (!Dungeon.level.visited[c] && !Dungeon.level.mapped[c]) continue;
			int d = Dungeon.level.distance(c, me.pos);
			if (d < 2 || d >= bestD || d > 12) continue;
			if (Campfire.spotNear(c) != -1 && Dungeon.level.passable[c] && !Dungeon.level.water[c]){
				best = c;
				bestD = d;
			}
		}
		return best;
	}

	//the bait (or anything else aimed): a free cell a few steps off
	private static int aim( Hero me ){
		int best = me.pos;
		for (int c = 0; c < Dungeon.level.length(); c++){
			int d = Dungeon.level.distance(c, me.pos);
			if (d >= 3 && d <= 4 && Dungeon.level.heroFOV[c] && Dungeon.level.passable[c] && Actor.findChar(c) == null) return c;
		}
		return best;
	}

	//the windows of the plan, pressed like a player would
	public static boolean answer( Window w ){
		if (!on()) return false;
		frame();
		Hero me = Dungeon.local();
		if (me == null) return false;
		String s = current(me);
		if (w instanceof WndCampfire){
			int index = -1;
			switch (s){
				case "cook":     index = WndCampfire.COOK; break;
				case "kit":      index = WndCampfire.RECIPES + FireRecipes.Recipe.KIT.ordinal(); break;
				case "torches":  index = WndCampfire.RECIPES + FireRecipes.Recipe.TORCHES.ordinal(); break;
				case "bait":     index = WndCampfire.RECIPES + FireRecipes.Recipe.BAIT.ordinal(); break;
				case "poultice": index = WndCampfire.RECIPES + FireRecipes.Recipe.POULTICE.ordinal(); break;
				case "sacred":   index = WndCampfire.RECIPES + FireRecipes.Recipe.SACRED_TORCH.ordinal(); break;
			}
			RedButton b = ((WndCampfire) w).button(index);
			if (b == null || !b.active){
				next("can't " + s + " at the fire");
				click(w, ((WndCampfire) w).button(WndCampfire.RECIPES + FireRecipes.Recipe.values().length));
				return true;
			}
			if (s.equals("cook") && shotName == null && waited++ == 0){
				shot("ventana", 0.3f);
				return true;
			}
			if (shotName != null) return true;
			next("presses " + b.text());
			click(w, b);
			return true;
		}
		if (w instanceof WndOptions && s.equals("light")){
			ArrayList<Button> buttons = CoopSession.buttonsOfWindow(w);
			if (shotName == null && waited++ == 0){
				shot("sacrificio", 0.3f);
				return true;
			}
			if (shotName != null || buttons.isEmpty()) return true;
			CoopLog.line("surv bot: sacrifices a drop of essence (" + Continent.essence + " left)");
			click(w, buttons.get(0));
			return true;
		}
		if (w instanceof WndBag && s.equals("bag")){
			if (shotName != null) return true;
			w.hide();
			next("looked at the bag");
			return true;
		}
		return false;
	}

	private static void click( Window w, Button b ){
		if (b == null){
			if (!CoopSession.windowBack(w)) w.onBackPressed();
			return;
		}
		if (!CoopSession.buttonClicked(b)) b.coopClick();
	}
}
