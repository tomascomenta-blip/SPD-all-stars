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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.echo;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Arrays;

//testing only, all of it behind -Dspd.test.echo=N (1-5) or =all.
//turn(): game side, brings the echoes out next to the party. act()/solo(): a dumb bot that fights them
public class EchoTest {

	private static int depth = -1;
	private static int next = 0;
	private static int wait = 0;
	private static boolean kitDone = false;

	//start of every hero turn. only the first living hero does anything, so it happens once a round
	//and the same way on every device
	public static void turn( Hero hero, String which ){
		ArrayList<Hero> living = Party.living();
		if (living.isEmpty() || living.get(0) != hero) return;
		boolean all = "all".equals(which);
		int only = 0;
		if (!all){
			try {
				only = Integer.parseInt(which.trim());
			} catch (NumberFormatException e){
				return;
			}
			if (only < 1 || only > 5) return;
		}
		if (Dungeon.depth != depth){
			depth = Dungeon.depth;
			next = all ? 1 : only;
			wait = 3;
		}
		//the floor's own monsters only get in the way of the measuring
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment != Char.Alignment.ENEMY || m instanceof EchoBoss || m instanceof QueenEcho.Whelp) continue;
			if (m.properties().contains(Char.Property.IMMOVABLE)) continue;
			m.EXP = 0;
			m.destroy();
			if (m.sprite != null) m.sprite.killAndErase();
		}
		Mob echo = null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof EchoBoss && m.isAlive()) echo = m;
		}
		if (echo != null){
			if (++rounds % 10 == 0){
				CoopLog.line("echo test: " + echo.getClass().getSimpleName() + " hp=" + echo.HP + "/" + echo.HT
						+ " t=" + (int) Actor.now());
			}
			return;
		}
		if (next <= 0) return;
		if (--wait > 0) return;
		if (!kitDone){
			kitDone = true;
			if (DevFlags.echoKit() > 0) giveKit(DevFlags.echoKit());
		}
		spawnNear(hero, next);
		next = all && next < 5 ? next + 1 : 0;
		wait = 4;
		rounds = 0;
	}

	private static int rounds = 0;

	private static void spawnNear( Hero hero, int world ){
		ArrayList<Integer> cells = EchoBoss.freeNear(hero.pos, 3, 4);
		int cell = -1;
		for (int c : cells){
			if (!Dungeon.level.heroFOV[c]) continue;
			boolean roomy = true;
			for (int n : PathFinder.NEIGHBOURS8){
				if (Dungeon.level.solid[c + n]) roomy = false;
			}
			if (roomy){
				cell = c;
				break;
			}
		}
		if (cell == -1 && !cells.isEmpty()) cell = cells.get(0);
		if (cell == -1) return;
		EchoBoss echo = EchoBoss.forWorld(world);
		echo.pos = cell;
		GameScene.add(echo);
		CoopLog.line("echo test: world " + world + " at " + cell + " hero at " + hero.pos);
	}

	//-Dspd.test.echokit=N: greatsword and plate armor +N for everyone, the gear of a party that gets that far
	private static void giveKit( int plus ){
		for (Hero h : Party.living()){
			MeleeWeapon w = new Greatsword();
			w.identify();
			w.upgrade(plus);
			w.cursed = false;
			h.belongings.weapon = w;
			Armor a = new PlateArmor();
			a.identify();
			a.upgrade(plus);
			a.cursed = false;
			h.belongings.armor = a;
			a.activate(h);
			h.STR = Math.max(h.STR, 19);
			h.updateHT(true);
			h.HP = h.HT;
			CoopLog.line("echo test kit: hero " + Party.indexOf(h) + " lvl=" + h.lvl + " HT=" + h.HT + " +" + plus);
		}
	}

	// *** the bot ***

	private static final boolean ON = DevFlags.echo() != null;
	private static float timer = 0, lastNow = -1;
	private static int stuck = 0, lastHP = -1;

	//solo runs: nobody else plays the hero
	public static void solo( Hero me ){
		if (!ON || me == null || !me.isAlive() || !me.ready) return;
		if (GameScene.topWindow() != null || GameScene.isSelectingCell()) return;
		timer -= Game.elapsed;
		if (timer > 0) return;
		timer = 0.1f;
		float now = Actor.now();
		if (now == lastNow) stuck++;
		else {
			stuck = 0;
			lastNow = now;
		}
		if (stuck >= 6 || !act(me)){
			stuck = 0;
			CoopOrders.named("rest");
		}
	}

	//closest unmarked cell up to 3 steps away (own little search, the bot runs off the game thread)
	private static int safeSpot( int from, int toward ){
		int len = Dungeon.level.length();
		int[] dist = new int[len];
		Arrays.fill(dist, -1);
		ArrayList<Integer> queue = new ArrayList<>();
		queue.add(from);
		dist[from] = 0;
		int best = -1;
		for (int i = 0; i < queue.size(); i++){
			int c = queue.get(i);
			if (best != -1 && dist[c] > dist[best]) break;
			if (c != from && TargetedCell.cells.get(c) == null){
				if (best == -1 || Dungeon.level.distance(c, toward) < Dungeon.level.distance(best, toward)) best = c;
				continue;
			}
			if (dist[c] >= 3) continue;
			for (int n : PathFinder.NEIGHBOURS8){
				int k = c + n;
				if (k < 0 || k >= len || dist[k] != -1 || !Dungeon.level.passable[k] || Actor.findChar(k) != null) continue;
				dist[k] = dist[c] + 1;
				queue.add(k);
			}
		}
		return best;
	}

	//off the marked cells, then at the echo (or its whelps). false = no echo around
	public static boolean act( Hero me ){
		if (!ON || me == null || !me.isAlive()) return false;
		Mob target = null;
		for (Mob m : Dungeon.level.mobs){
			if (!m.isAlive() || m.alignment != Char.Alignment.ENEMY) continue;
			if (!(m instanceof EchoBoss) && !(m instanceof QueenEcho.Whelp)) continue;
			if (target == null || Dungeon.level.distance(me.pos, m.pos) < Dungeon.level.distance(me.pos, target.pos)) target = m;
		}
		if (target == null) return false;
		if (me.HP != lastHP){
			lastHP = me.HP;
			CoopLog.line("echo bot hp=" + me.HP + "/" + me.HT + " t=" + (int) Actor.now()
					+ " target=" + target.getClass().getSimpleName() + " " + target.HP + "/" + target.HT);
		}
		if (me.HP < me.HT / 3){
			PotionOfHealing potion = me.belongings.getItem(PotionOfHealing.class);
			if (potion != null){
				CoopOrders.execute(potion, "DRINK");
				return true;
			}
		}
		//standing on a marked cell: walk to the closest free one (a 3x3 needs two steps)
		if (TargetedCell.cells.get(me.pos) != null){
			int best = safeSpot(me.pos, target.pos);
			if (best != -1){
				CoopOrders.handleCell(best);
				return true;
			}
		}
		//waiting out a dive or the way out of the portal
		if (target instanceof EchoBoss && (((EchoBoss) target).emerging()
				|| (target instanceof KrakenEcho && ((KrakenEcho) target).diving > 0))){
			CoopOrders.named("rest");
			return true;
		}
		CoopOrders.handleCell(target.pos);
		return true;
	}
}
