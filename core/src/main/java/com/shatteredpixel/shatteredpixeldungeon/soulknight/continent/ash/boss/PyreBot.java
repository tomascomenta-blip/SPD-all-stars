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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.boss;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward.ThalTest;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

//test bots on the ash plain: off the marks and the lava, knock over the brazier the pyre is going for,
//in the last phase half of them keep the braziers down, the rest hit the pyre (walkers first if they're on us).
//solo with -Dspd.test.pyrebot=1, the coop bots call act() like for the other bosses
public class PyreBot {

	private static float timer = 0;
	private static float lastNow = -1;
	private static int stuck = 0;
	private static int lastHP = -1;
	private static boolean mapped = false;
	private static float waited = 0;

	public static void tick( Hero me ){
		if (!"1".equals(System.getProperty("spd.test.pyrebot"))) return;
		if (me == null || !me.isAlive()) return;
		timer -= Game.elapsed;
		if (timer > 0) return;
		timer = 0.1f;
		waited += 0.1f;
		Window w = GameScene.topWindow();
		if (w != null){
			//the walkthrough of Thal-Vekhorr's loot answers its own windows
			if (!ThalTest.enabled() || waited > 45f) w.hide();
			return;
		}
		if (GameScene.isSelectingCell() || !me.ready) return;
		if (!(Dungeon.level instanceof AshBossLevel)){
			CoopOrders.named("rest");
			return;
		}
		float now = Actor.now();
		if (now == lastNow) stuck++; else { stuck = 0; lastNow = now; }
		if (stuck >= 6){
			stuck = 0;
			CoopLog.line("bot pyre stuck at " + now + ", rests");
			CoopOrders.named("rest");
			return;
		}
		AshBossLevel L = (AshBossLevel) Dungeon.level;
		if (!mapped){
			mapped = true;
			for (int i = 0; i < L.length(); i++) L.mapped[i] = true;
		}
		AshPyre p = AshBossLevel.pyre(L);
		if (p != null && me.HP != lastHP){
			CoopLog.line("bot pyre hero_hp " + lastHP + " -> " + me.HP + " t=" + (int) now + " pyre=" + p.HP + "/" + p.HT
					+ " phase=" + p.phase + " mode=" + p.mode + " heat=" + p.heat);
			lastHP = me.HP;
		}
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(me.pos + n);
			if (ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY && !(ch instanceof AshPyre)){
				CoopOrders.handleCell(ch.pos);
				return;
			}
		}
		if (p != null && p.intro == 0 && ThalTest.enabled() && waited < 45f){
			CoopOrders.named("rest");
			return;
		}
		if (act(me)) return;
		//after the fight: pick everything up, then wait by the way out
		for (Heap h : L.heaps.valueList()){
			if (h.pos != me.pos){
				CoopOrders.handleCell(h.pos);
				return;
			}
		}
		CoopOrders.named("rest");
	}

	private static boolean marked( int cell ){
		return TargetedCell.cells.get(cell) != null;
	}

	public static boolean act( Hero me ){
		if (!(Dungeon.level instanceof AshBossLevel)) return false;
		AshBossLevel L = (AshBossLevel) Dungeon.level;
		AshPyre p = AshBossLevel.pyre(L);
		if (p == null) return false;
		if (p.intro == 0){
			//the co-op bots use Thal-Vekhorr's loot before going in (-Dspd.test.thalrewards=1)
			if (ThalTest.coopBot(me)) return true;
			CoopOrders.handleCell(p.pos);
			return true;
		}
		if (!p.fighting()){
			CoopOrders.named("rest");
			return true;
		}

		if (me.HP < me.HT / 3){
			PotionOfHealing pot = me.belongings.getItem(PotionOfHealing.class);
			if (pot != null){
				CoopOrders.execute(pot, "DRINK");
				return true;
			}
		}

		//out of the lava and off the marks
		if (L.lava(me.pos) || marked(me.pos)){
			int best = -1, bestScore = Integer.MIN_VALUE;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = me.pos + n;
				if (!L.passable[c] || L.lava(c) || Actor.findChar(c) != null) continue;
				int score = marked(c) ? -10 : 0;
				score -= L.distance(c, p.pos) <= 1 ? 2 : 0;
				if (score > bestScore){
					bestScore = score;
					best = c;
				}
			}
			if (best != -1){
				CoopLog.line("bot pyre dodges");
				CoopOrders.handleCell(best);
				return true;
			}
		}

		int idx = Party.active() ? Party.indexOf(me) : 0;
		int alive = Party.living().size();

		//the brazier it's walking to: everyone if it's drinking, otherwise every other hero
		PyreBrazier goal = null;
		if (p.mode == AshPyre.CHANNEL) goal = p.fuel();
		else if (p.mode == AshPyre.GUTTER && p.fuel() != null && (idx % 2 == 0 || alive <= 2
				|| L.distance(me.pos, p.fuel().pos) <= L.distance(p.pos, p.fuel().pos))) goal = p.fuel();
		else if (p.phase >= 4 && p.mode == AshPyre.FIGHT && (idx % 2 == 1 || alive == 1)) goal = nearestLit(L, me);
		//its fire is almost out: one of us knocks over the brazier closest to it before it gets there
		else if (p.phase < 4 && p.mode == AshPyre.FIGHT && p.heat <= 20 && (idx % 2 == 1 || alive == 1)) goal = nearestLit(L, p.pos);
		//dark: everyone on it
		if (p.mode == AshPyre.DARK) goal = null;
		if (goal != null && goal.lit){
			go(L, me, goal.pos);
			return true;
		}

		//walkers chewing on us first
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(me.pos + n);
			if (ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY && !(ch instanceof AshPyre)){
				CoopOrders.handleCell(ch.pos);
				return true;
			}
		}
		go(L, me, p.pos);
		return true;
	}

	//walk/attack toward a cell; if the lava cut us off, wade through the shortest way.
	//own search: PathFinder belongs to the game thread and this runs on the render thread
	private static void go( AshBossLevel L, Hero me, int target ){
		if (L.adjacent(me.pos, target) || reach(L, target, false)[me.pos] < Integer.MAX_VALUE){
			CoopOrders.handleCell(target);
			return;
		}
		int[] dist = reach(L, target, true);
		int best = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = me.pos + n;
			if (c < 0 || c >= L.length() || Actor.findChar(c) != null) continue;
			if (!L.passable[c] && !L.lava(c)) continue;
			if (dist[c] < dist[me.pos] && (best == -1 || dist[c] < dist[best])) best = c;
		}
		if (best != -1){
			CoopLog.line("bot pyre wades in");
			CoopOrders.handleCell(best);
		} else {
			CoopOrders.handleCell(target);
		}
	}

	private static int[] reach( AshBossLevel L, int from, boolean lava ){
		int len = L.length(), w = L.width();
		int[] dist = new int[len];
		java.util.Arrays.fill(dist, Integer.MAX_VALUE);
		int[] queue = new int[len];
		int head = 0, tail = 0;
		dist[from] = 0;
		queue[tail++] = from;
		while (head < tail){
			int c = queue[head++];
			for (int n : PathFinder.NEIGHBOURS8){
				int k = c + n;
				if (k < 0 || k >= len || Math.abs(k % w - c % w) > 1 || dist[k] != Integer.MAX_VALUE) continue;
				if (!L.passable[k] && !(lava && L.lava(k))) continue;
				dist[k] = dist[c] + 1;
				queue[tail++] = k;
			}
		}
		return dist;
	}

	private static PyreBrazier nearestLit( AshBossLevel L, Hero me ){
		return nearestLit(L, me.pos);
	}

	private static PyreBrazier nearestLit( AshBossLevel L, int from ){
		PyreBrazier best = null;
		for (PyreBrazier b : L.braziers()){
			if (!b.lit) continue;
			if (best == null || L.distance(from, b.pos) < L.distance(from, best.pos)) best = b;
		}
		return best;
	}
}
