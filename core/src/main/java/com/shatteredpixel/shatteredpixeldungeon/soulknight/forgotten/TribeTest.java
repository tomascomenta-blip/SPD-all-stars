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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

//-Dspd.test.tribe=1 (solo): 21 -> go talk to the shaman, take the quest. 24 -> bring a lit flame to the altar
//=2: takes the flame on 21 and walks it down to the altar, fighting what gets in the way (use god=1)
//=3: just measures the way of the flame on 21-24 (TorchRoute) and takes the stairs at once,
//=4 the same on the floors as they were before (no braziers along the way)
public class TribeTest {

	private static boolean talked = false, delivered = false;
	private static float timer = 0;

	//per floor, for the walk
	private static int floorDepth = -1, floorStart = 0, lowest = 100, outs = 0;
	//the clock gets moved back on every new floor, so the last turn seen here
	private static float lastNow = 0;
	private static boolean wasLit = true, done = false;
	//the flame on paper, carried from floor to floor (TorchRoute.measure), with the new and the old rules
	private static int paperNew = 100, paperOld = 100;

	public static void tick( Hero hero ){
		int mode = DevFlags.tribe();
		if (!(Dungeon.level instanceof ForgottenLevel) || hero == null) return;
		if (Dungeon.depth != floorDepth) arrived(hero);
		if (mode == 2) watch(hero);
		Window w = GameScene.topWindow();
		if (w != null){
			//the walking bot doesn't read (the reward of the altar included)
			if (mode >= 2) w.hide();
			return;
		}
		timer -= Game.elapsed;
		if (timer > 0 || !hero.ready) return;
		timer = mode == 1 ? 1f : 0.1f;
		if (mode >= 3){
			stairs(hero);
			return;
		}
		if (mode == 2){
			walk(hero);
			return;
		}
		old(hero);
	}

	private static void arrived( Hero hero ){
		if (floorDepth != -1 && DevFlags.tribe() == 2){
			CoopLog.line("tribe walk: floor " + floorDepth + " done, turns=" + (int) (lastNow - floorStart)
					+ " lowest=" + lowest + " outs=" + outs);
		}
		floorDepth = Dungeon.depth;
		floorStart = (int) Actor.now();
		lowest = 100;
		outs = 0;
		if (Dungeon.depth < 21 || Dungeon.depth > 24) return;
		if (Dungeon.depth == 21) paperNew = paperOld = 100;
		ForgottenLevel L = (ForgottenLevel) Dungeon.level;
		int[] m = TorchRoute.measure(L, paperNew, false);
		int[] o = TorchRoute.measure(L, paperOld, true);
		if (m == null || o == null){
			CoopLog.line("tribe route: floor " + Dungeon.depth + " no way through!");
			return;
		}
		CoopLog.line("tribe route: floor " + Dungeon.depth + " seed=" + Dungeon.seed + " steps=" + m[0] + " cold=" + m[1]
				+ " braziers=" + m[2] + " longest=" + m[3] + " | new: in=" + paperNew + " end=" + m[4] + " lowest=" + m[5] + " outs=" + m[6]
				+ " | old: in=" + paperOld + " end=" + o[4] + " lowest=" + o[5] + " outs=" + o[6]);
		paperNew = m[4];
		paperOld = o[4];
	}

	//every frame, so no turn of the flame gets missed
	private static void watch( Hero hero ){
		lastNow = Actor.now();
		if (Tribe.Quest.completed && !done){
			done = true;
			CoopLog.line("tribe walk: delivered at t=" + (int) Actor.now() + " lowest on 24=" + lowest);
			System.out.println("TRIBE delivered");
			quit();
			return;
		}
		Tribe.SacredFlame f = hero.belongings.getItem(Tribe.SacredFlame.class);
		if (f == null) return;
		if (f.lit) lowest = Math.min(lowest, f.flame);
		if (wasLit && !f.lit){
			outs++;
			CoopLog.line("tribe walk: the flame went out on floor " + Dungeon.depth + " t=" + (int) Actor.now());
		} else if (!wasLit && f.lit){
			CoopLog.line("tribe walk: lit again on floor " + Dungeon.depth + " t=" + (int) Actor.now());
		}
		wasLit = f.lit;
	}

	private static void stairs( Hero hero ){
		if (Dungeon.depth >= 24){
			if (!done){
				done = true;
				System.out.println("TRIBE measured");
				quit();
			}
			return;
		}
		int exit = Dungeon.level.exit();
		if (hero.pos != exit){
			ScrollOfTeleportation.appear(hero, exit);
			Dungeon.observe();
			return;
		}
		CoopOrders.handleCell(exit);
	}

	//nothing else to see, next seed
	private static void quit(){
		Game.instance.finish();
		new Thread(){
			@Override
			public void run() {
				try { Thread.sleep(1500); } catch (InterruptedException e) {}
				System.exit(0);
			}
		}.start();
	}

	private static void walk( Hero hero ){
		Mob goal = null;
		for (Mob m : Dungeon.level.mobs){
			//first the shaman, on foot, then the stairs down to the altar
			if (Dungeon.depth == 21 && !Tribe.Quest.given && m instanceof Tribe.Shaman) goal = m;
			if (Dungeon.depth == 24 && m instanceof Tribe.Altar) goal = m;
		}
		int to = goal != null ? goal.pos : Dungeon.level.exit();
		//a key lying around opens the locked door that may be on the way
		for (Heap h : Dungeon.level.heaps.valueList()){
			for (Item i : h.items){
				if (i instanceof IronKey) to = h.pos;
			}
		}
		//whatever is chewing on us first
		for (int n : PathFinder.NEIGHBOURS8){
			Char c = Actor.findChar(hero.pos + n);
			if (c instanceof Mob && c.alignment == Char.Alignment.ENEMY && c.isAlive()
					&& !(c instanceof SKProps.SKProp) && !c.isInvulnerable(hero.getClass())){
				CoopOrders.handleCell(c.pos);
				return;
			}
		}
		//one step at a time along the way the flame would take (TorchRoute), so the hero's own
		//pathing (which only walks on what it has seen) doesn't get in the way of the test
		if (hero.pos == to){
			//on the stairs: down
			CoopOrders.handleCell(to);
			return;
		}
		int[] p = TorchRoute.way(Dungeon.level, hero.pos, to);
		if (p == null || p.length == 0){
			if (++stuck % 40 == 1) CoopLog.line("tribe walk: no way from " + hero.pos + " to " + to);
			CoopOrders.named("rest");
			return;
		}
		int next = p[0];
		Char c = Actor.findChar(next);
		if (hero.pos == lastPos){
			//stuck in tar or held by a plant: a step only fails, waiting lets it wear off
			if (still > 0 && still % 10 == 0){
				still++;
				CoopOrders.named("rest");
				return;
			}
			if (++still % 60 == 0){
				Heap h = Dungeon.level.heaps.get(next);
				CoopLog.line("tribe walk: stuck at " + hero.pos + " next " + next + " terrain " + Dungeon.level.map[next]
						+ " char " + (c == null ? "-" : c.getClass().getSimpleName()) + " heap " + (h == null ? "-" : h.type));
			}
		} else {
			still = 0;
			lastPos = hero.pos;
		}
		if (next != to && Dungeon.level.secret[next]){
			//a hidden door: look for it
			CoopOrders.named("search");
			return;
		}
		if (c != null && next != to && c.alignment == Char.Alignment.ALLY){
			//a friend in the way, give them a moment
			CoopOrders.named("rest");
			return;
		}
		//a chest on the way: walk over it, tapping it would try to open it
		Heap h = Dungeon.level.heaps.get(next);
		if (h != null && h.type != Heap.Type.HEAP && next != to && p.length > 1){
			CoopOrders.handleCell(p[1]);
			return;
		}
		//a crate or an ice wall gets hit, a monster too (and a mimic, once it's found out)
		CoopOrders.handleCell(next);
	}

	private static int stuck = 0, still = 0, lastPos = -1;

	private static void old( Hero hero ){
		Class<?> want = Dungeon.depth == 21 ? Tribe.Shaman.class : Dungeon.depth == 24 ? Tribe.Altar.class : null;
		if (want == null) return;
		//trust the game's flag, an enemy showing up can interrupt the talk and then it just retries
		talked = Tribe.Quest.given;
		delivered = Tribe.Quest.completed;
		if ((want == Tribe.Shaman.class && talked) || (want == Tribe.Altar.class && delivered)) return;
		if (want == Tribe.Altar.class && hero.belongings.getItem(Tribe.SacredFlame.class) == null){
			new Tribe.SacredFlame().collect(hero.belongings.backpack);
		}
		Mob target = null;
		for (Mob m : Dungeon.level.mobs){
			if (want.isInstance(m)) target = m;
		}
		if (target == null){
			CoopLog.line("tribe test: no " + want.getSimpleName() + " on floor " + Dungeon.depth);
			return;
		}
		if (!Dungeon.level.adjacent(hero.pos, target.pos)){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = target.pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
					ScrollOfTeleportation.appear(hero, c);
					Dungeon.observe();
					CoopLog.line("tribe test: next to the " + want.getSimpleName());
					return;
				}
			}
			return;
		}
		CoopLog.line("tribe test: interacts with the " + want.getSimpleName() + " alive=" + target.isAlive()
				+ " can=" + target.canInteract(hero) + " should=" + target.heroShouldInteract() + " at=" + target.pos + " hero=" + hero.pos
				+ " found=" + Actor.findChar(target.pos));
		CoopOrders.handleCell(target.pos);
		CoopLog.line("tribe test: action " + hero.curAction + " ready=" + hero.ready + " quest=" + Tribe.Quest.completed);
	}
}
