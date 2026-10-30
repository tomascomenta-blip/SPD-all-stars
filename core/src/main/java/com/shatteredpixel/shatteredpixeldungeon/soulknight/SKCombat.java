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

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.Random;

import java.util.ArrayList;

//shared rules for telegraphed + area attacks
public class SKCombat {

	//never less than what the slowest hero needs to react. slowed heroes got deleted otherwise
	public static float announceDelay( float base ){
		float d = base;
		for (Hero h : Party.living()){
			d = Math.max(d, (float)Math.ceil(h.cooldown()));
		}
		return GameMath.gate(base, d, base + 2f);
	}

	public static void mark( Iterable<Integer> cells, float delay ){
		for (int c : cells){
			GameScene.targetedCell(c, delay);
		}
		for (Hero h : Party.living()){
			h.interrupt();
		}
	}

	//a mob's special hit (slam, burst, laser): amulet curse boost like vanilla magic attacks, half armor
	public static int specialDamage( Char attacker, Char target, int min, int max ){
		int dmg = Math.round(Random.NormalIntRange(min, max) * AscensionChallenge.statModifier(attacker));
		return Math.max(1, dmg - target.drRoll()/2);
	}

	//after teleporting heroes around (lair, ambush): snap the camera back to the local hero
	public static void focusLocal(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Hero me = Dungeon.local();
				if (me == null || me.sprite == null) return;
				me.sprite.visible = true;
				Camera.main.snapTo(me.sprite.center());
				Camera.main.panFollow(me.sprite, 5f);
			}
		});
	}

	//square, in bounds, sin paredes
	public static ArrayList<Integer> area( int center, int radius ){
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width();
		int cx = center % w, cy = center / w;
		for (int y = cy - radius; y <= cy + radius; y++){
			for (int x = cx - radius; x <= cx + radius; x++){
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
				int c = x + y*w;
				if (!Dungeon.level.solid[c]) cells.add(c);
			}
		}
		return cells;
	}

	//same but line of sight from center, so no hitting through walls
	public static ArrayList<Integer> openArea( int center, int radius ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : area(center, radius)){
			if (c == center || new Ballistica(center, c, Ballistica.PROJECTILE).collisionPos == c){
				cells.add(c);
			}
		}
		return cells;
	}

	//heroes+allies vs enemies
	public static boolean foes( Char attacker, Char ch ){
		if (ch == null || attacker == null || ch == attacker || !ch.isAlive()) return false;
		if (attacker.alignment == Char.Alignment.ENEMY) return ch.alignment == Char.Alignment.ALLY;
		//friendly fire: what a hero does also hurts the other heroes
		if (attacker.alignment == Char.Alignment.ALLY) return ch.alignment == Char.Alignment.ENEMY || CoopRules.friendlyTarget(attacker, ch);
		return false;
	}

	//fixed order, coop!!
	public static ArrayList<Char> foesIn( Char attacker, Iterable<Integer> cells ){
		ArrayList<Char> found = new ArrayList<>();
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (foes(attacker, ch) && !found.contains(ch)) found.add(ch);
		}
		return found;
	}
}
