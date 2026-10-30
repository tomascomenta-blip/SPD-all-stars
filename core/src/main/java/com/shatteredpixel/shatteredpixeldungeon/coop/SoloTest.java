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

package com.shatteredpixel.shatteredpixeldungeon.coop;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Gnoll;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.forger.LivingForge;
import com.shatteredpixel.shatteredpixeldungeon.champions.forger.MagmaPillar;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHero;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

//testing: -Dspd.test.cast=Q,W,E,R,F,A fires the kit one by one at some gnolls (F = forger's forge on
//himself, A = armor ability). -Dspd.test.mobs=N gnolls, default 4. the gnolls did nothing wrong
public class SoloTest {

	private static final String SCRIPT = System.getProperty("spd.test.cast");
	private static float clock = 0;
	private static int step = -1;
	private static boolean spawned = false;
	private static float wait = 0;

	public static void tick( Hero hero ){
		if (SCRIPT == null || hero == null || !hero.ready) return;
		clock += Game.elapsed;
		if (clock < 2.5f) return;
		if (!spawned){
			spawned = true;
			spawn(hero, Integer.getInteger("spd.test.mobs", 4));
			clock = 0;
			return;
		}
		wait += Game.elapsed;
		if (GameScene.topWindow() != null){
			//windows opened by the script stay for a while, then close
			if (wait > 4f){
				GameScene.topWindow().hide();
				wait = 0;
			}
			return;
		}
		//the cell selection of the last action is answered first
		if (GameScene.isSelectingCell()){
			Char target = nearestEnemy(hero);
			CoopLog.line("solo test: aiming at " + (target == null ? "nothing" : "" + target.pos));
			GameScene.handleCell(target == null ? hero.pos + 1 : target.pos);
			wait = 0;
			return;
		}
		if (wait < 3f) return;
		wait = 0;
		String[] parts = SCRIPT.split(",");
		step++;
		if (step >= parts.length){
			if (step == parts.length){
				report(hero, "end");
				System.out.println("SOLOTEST done");
			}
			return;
		}
		String a = parts[step].trim();
		report(hero, "before " + a);
		CoopLog.line("solo test: step " + step + " " + a);
		ChampionKit kit = hero.belongings.getItem(ChampionKit.class);
		switch (a){
			case "Q": case "W": case "E": case "R":
				if (kit == null) return;
				int i = "QWER".indexOf(a);
				kit.cast(hero, kit.abilities()[i]);
				break;
			case "H":
				WndHero.lastIdx = 1;
				GameScene.show(new WndHero());
				break;
			case "T":
				hero.rest(false);
				break;
			case "F":
				LivingForge.forge(hero, hero, true);
				break;
			case "A":
				armor(hero);
				break;
		}
	}

	private static void armor( Hero hero ){
		if (hero.armorAbility == null){
			hero.armorAbility = hero.heroClass.armorAbilities()[0];
			Talent.initArmorTalents(hero);
			if (System.getProperty("spd.coop.alltalents") != null){
				for (Talent t : hero.armorAbility.talents()){
					java.util.LinkedHashMap<Talent, Integer> tier = hero.talents.get(3);
					if (tier.containsKey(t)) tier.put(t, t.maxPoints());
				}
			}
		}
		ClassArmor ca = null;
		for (Item it : hero.belongings){
			if (it instanceof ClassArmor) ca = (ClassArmor) it;
		}
		if (ca == null){
			ca = ClassArmor.upgrade(hero, hero.belongings.armor);
			ca.identify();
			hero.belongings.armor = null;
			ca.collect();
			ca.doEquip(hero);
		}
		ca.charge = 100;
		ca.execute(hero, "ABILITY");
	}

	private static void report( Hero hero, String label ){
		StringBuilder sb = new StringBuilder("SOLOTEST " + label + ": hero pos=" + hero.pos + " hp=" + hero.HP + "/" + hero.HT
				+ " shield=" + hero.shielding() + " buffs=");
		for (Buff b : hero.buffs()) sb.append(b.getClass().getSimpleName()).append(' ');
		for (Char ch : Actor.chars()){
			if (ch.alignment != Char.Alignment.ENEMY) continue;
			sb.append(" | ").append(ch.getClass().getSimpleName()).append(" pos=").append(ch.pos).append(" hp=").append(ch.HP).append("/").append(ch.HT).append(" ");
			for (Buff b : ch.buffs()) sb.append(b.getClass().getSimpleName()).append(' ');
		}
		int pillars = 0;
		for (int i = 0; i < Dungeon.level.length(); i++) if (MagmaPillar.at(i)) pillars++;
		sb.append(" | pillars=").append(pillars);
		System.out.println(sb);
	}

	private static Char nearestEnemy( Hero hero ){
		Char best = null;
		for (Char ch : Actor.chars()){
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && Dungeon.level.heroFOV[ch.pos]){
				if (best == null || Dungeon.level.distance(hero.pos, ch.pos) < Dungeon.level.distance(hero.pos, best.pos)){
					best = ch;
				}
			}
		}
		return best;
	}

	private static void spawn( Hero hero, int n ){
		int placed = 0;
		PathFinder.buildDistanceMap(hero.pos, Dungeon.level.passable);
		for (int d = Integer.getInteger("spd.test.mobdist", 3); d <= 12 && placed < n; d++){
			for (int i = 0; i < Dungeon.level.length() && placed < n; i++){
				if (PathFinder.distance[i] != d) continue;
				if (!Dungeon.level.passable[i] || Dungeon.level.solid[i] || Actor.findChar(i) != null) continue;
				Mob m = new Gnoll();
				m.pos = i;
				m.state = m.HUNTING;
				GameScene.add(m);
				placed++;
			}
		}
		CoopLog.line("solo test: " + placed + " gnolls");
	}
}
