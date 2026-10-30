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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//Cooperative runs only: every boss floor gets a second boss from the Summoner's Rift.
//  5 Goo + elemental drake   10 Tengu + Rift Herald   15 DM-300 + Atakhan
//  20 Dwarf King + Elder Dragon   25 Yog-Dzewa + Baron Nashor
//shows up when the fight starts (floor sealed), next to the normal boss.
//ALL of this is shared game logic, same randoms same order, or co-op explodes
public class RiftBosses {

	//the regular boss fight started on this level (Level.seal)
	public static void onSeal( Level level ){
		//Tengu's prison changes shape during the fight: there the Herald comes in the final arena
		if (level instanceof PrisonBossLevel) return;
		spawn(level, mainBoss(level));
	}

	public static RiftBoss create( int depth ){
		switch (depth){
			case 5:  {
				Drake d = new Drake();
				d.element = Random.Int(6);
				return d;
			}
			case 10: return new RiftHerald();
			case 15: return new Atakhan();
			case 20: return new ElderDragon();
			case 25: return new BaronNashor();
			default: return null;
		}
	}

	public static void spawn( Level level, Char mainBoss ){
		//HACK branch != 0 = the side floors of quests, no rift boss there pls
		if (!Party.active() || level.riftSpawned || Dungeon.branch != 0) return;
		RiftBoss boss = create(Dungeon.depth);
		if (boss == null) return;
		int from = mainBoss != null ? mainBoss.pos : Dungeon.hero.pos;
		int cell = findCell(level, from, boss);
		if (cell == -1) return;
		level.riftSpawned = true;
		boss.pos = cell;
		boss.state = boss.HUNTING;
		GameScene.add(boss);
		level.occupyCell(boss);
		boss.beckon(Dungeon.hero.pos);
		//a message for everyone, not about the hero who happened to start the fight
		final String text = Messages.get(RiftBosses.class, "arrives", boss.name());
		Party.withHero(Dungeon.local(), new Runnable() {
			@Override
			public void run() {
				GLog.w(text);
			}
		});
	}

	//free cell close to the regular boss (walking, not through walls) and as far as possible from the heroes
	static int findCell( Level level, int from, Mob boss ){
		PathFinder.buildDistanceMap(from, BArray.or(level.passable, level.avoid, null));
		int[] walk = PathFinder.distance.clone();
		for (int radius = 4; radius <= 10; radius += 3){
			int best = -1;
			int bestScore = -1;
			for (int c = 0; c < level.length(); c++){
				int d = walk[c];
				if (d == Integer.MAX_VALUE || d < 2 || d > radius) continue;
				if (level.solid[c] || !level.passable[c] || level.pit[c] || Actor.findChar(c) != null) continue;
				if (Char.hasProp(boss, Char.Property.LARGE) && !level.openSpace[c]) continue;
				if (level.getTransition(c) != null) continue;
				int score = Integer.MAX_VALUE;
				for (Hero h : Party.living()){
					score = Math.min(score, level.distance(h.pos, c));
				}
				if (score > bestScore){
					bestScore = score;
					best = c;
				}
			}
			if (best != -1) return best;
		}
		return -1;
	}

	//a boss of the Summoner's Rift still fights on this level
	public static boolean riftBossAlive( Level level ){
		for (Mob m : level.mobs){
			if (m instanceof RiftBoss && m.isAlive()) return true;
		}
		return false;
	}

	//Level.unseal: in cooperative runs the floor stays closed until both bosses are dead,
	//so nobody leaves the second boss (and its rewards) behind. Tengu's prison keeps its own script.
	public static boolean keepSealed( Level level ){
		return Party.active() && !(level instanceof PrisonBossLevel) && riftBossAlive(level);
	}

	public static Mob mainBoss( Level level ){
		for (Mob m : level.mobs){
			if (Char.hasProp(m, Char.Property.BOSS) && !(m instanceof RiftBoss)) return m;
		}
		return null;
	}

	// *** hooks called by the rest of the game ***

	//Char.damage: a boss hurt by another boss may turn on it
	public static void onDamage( Char victim, Object src ){
		if (Party.active()) BossRivalry.onDamage(victim, src);
	}

	//Mob.chooseEnemy: a boss angry at another boss fights it instead
	public static Char rival( Mob mob ){
		return Party.active() ? BossRivalry.rivalOf(mob) : null;
	}

	//Char.attack: multiplier for the damage of an attack
	public static float attackFactor( Char attacker ){
		return BaronEmpowered.damageFactor(attacker);
	}
}
