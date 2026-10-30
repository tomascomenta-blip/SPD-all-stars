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

package com.shatteredpixel.shatteredpixeldungeon.champions.ironlord;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.executioner.Hemorrhage;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHero;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

//test only, never runs in a normal game. -Dspd.test.script=wnd|armor|ironlord + -Dspd.test.start=CLASS
//screenshots + poking abilities in solo. why is it in the ironlord folder? because that's where i was. FIXME move it
public class TestScript {

	private static float t = 0;
	private static int step = 0;
	private static Mob[] rats = new Mob[3];

	private static void log( String s ){
		System.out.println("TEST " + s);
	}

	private static int hp = 40;

	private static void spawnRats( Hero hero, int n ){
		int count = 0;
		for (int dist = 2; dist <= 3 && count < n; dist++){
			for (int d : PathFinder.NEIGHBOURS8){
				int c = hero.pos + d * dist;
				if (c < 0 || c >= Dungeon.level.length()) continue;
				if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
				Rat r = new Rat();
				r.pos = c;
				r.HP = r.HT = hp;
				GameScene.add(r);
				rats[count++] = r;
				if (count >= n) break;
			}
		}
		log("spawned " + count + " rats");
	}

	private static float lastLog = 0;
	private static boolean log2( Hero hero ){
		if (t - lastLog > 2){
			lastLog = t;
			RealmOfDeath.Duel d = hero.buff(RealmOfDeath.Duel.class);
			log("state ready=" + hero.ready + " now=" + Actor.now() + " duelLeft=" + (d == null ? -1 : d.left) + " rat=" + (rats[0] == null ? "-" : rats[0].HP + "@" + rats[0].pos) + " hero=" + hero.pos + " act=" + hero.curAction);
		}
		return true;
	}

	public static void tick(){
		String script = System.getProperty("spd.test.script");
		if (script == null) return;
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.ready) return;
		t += Game.elapsed;

		if (script.equals("wnd") && step == 0 && t > 3){
			step = 1;
			WndHero w = new WndHero();
			w.select(1);
			GameScene.show(w);
		}

		if (script.equals("armor")){
			if (step == 0 && t > 2){
				step = 1;
				hero.armorAbility = hero.heroClass.armorAbilities()[0];
				Talent.initArmorTalents(hero);
				ClassArmor ca = ClassArmor.upgrade(hero, (Armor) hero.belongings.armor);
				ca.charge = 100;
				hero.belongings.armor = ca;
				ca.activate(hero);
				spawnRats(hero, 3);
				for (Mob m : rats){
					if (m != null) Hemorrhage.apply(hero, m, 3);
				}
				log("armor ability " + hero.armorAbility.getClass().getSimpleName());
			} else if (step == 1 && t > 5){
				step = 2;
				for (Mob m : rats) if (m != null) log("before HP=" + m.HP + " pos=" + m.pos);
				((ClassArmor) hero.belongings.armor).execute(hero, "ABILITY");
			} else if (step == 2 && t > 9){
				step = 3;
				for (Mob m : rats) if (m != null) log("after HP=" + m.HP + " alive=" + m.isAlive());
				log("hero HP=" + hero.HP + "/" + hero.HT + " shield=" + hero.shielding());
			}
		}

		if (script.equals("ironlord")){
			ChampionKit kit = hero.belongings.getItem(ChampionKit.class);
			if (step == 0 && t > 2){
				step = 1;
				spawnRats(hero, 3);
			} else if (step == 1 && t > 4){
				step = 2;
				for (Mob m : rats) if (m != null) log("before HP=" + m.HP + " pos=" + m.pos);
				Obliterate.INSTANCE.activate(hero, kit, rats[0].pos);
			} else if (step == 2 && t > 7){
				step = 3;
				for (Mob m : rats) if (m != null) log("after Q HP=" + m.HP + " pos=" + m.pos);
				Indestructible.INSTANCE.activate(hero, kit, null);
				log("shield=" + hero.shielding());
			} else if (step == 3 && t > 10){
				step = 4;
				Indestructible.INSTANCE.activate(hero, kit, null);
				log("after W2 HP=" + hero.HP + "/" + hero.HT + " shield=" + hero.shielding());
			} else if (step == 4 && t > 12){
				step = 5;
				Mob far = null;
				for (Mob m : rats) if (m != null && m.isAlive()) { far = m; break; }
				if (far != null) DeathsGrasp.INSTANCE.activate(hero, kit, far.pos);
				for (Mob m : rats) if (m != null) log("after E HP=" + m.HP + " pos=" + m.pos + " hero=" + hero.pos);
			} else if (step == 5 && t > 14){
				step = 55;
				hp = Integer.getInteger("spd.test.rathp", 300);
				rats = new Mob[3];
				spawnRats(hero, 1);
			} else if (step == 55 && t > 16){
				step = 6;
				Mob far = null;
				for (Mob m : rats) if (m != null && m.isAlive()) { far = m; break; }
				if (far != null){
					log("R on rat at " + far.pos + " hero=" + hero.pos + " HT=" + hero.HT);
					RealmOfDeath.INSTANCE.activate(hero, kit, far.pos);
					log("after R hero=" + hero.pos + " rat=" + far.pos + " HT=" + hero.HT + " inRealm=" + Dungeon.level.inRealm(hero.pos));
				}
			} else if (step == 6 && t > 17 && (log2(hero) || true) && hero.ready && hero.buff(RealmOfDeath.Duel.class) != null && rats[0] != null && rats[0].isAlive()){
				GameScene.handleCell(rats[0].pos);
			} else if (step == 6 && t > 19){
				step = 7;
				log("duel running=" + (hero.buff(RealmOfDeath.Duel.class) != null) + " hero=" + hero.pos + " HT=" + hero.HT);
				if (System.getProperty("spd.test.cancel") != null && hero.buff(RealmOfDeath.Duel.class) != null){
					RealmOfDeath.INSTANCE.activate(hero, kit, null);
				}
			} else if (step == 7 && t > 30){
				step = 8;
				log("end hero=" + hero.pos + " HT=" + hero.HT + " inRealm=" + Dungeon.level.inRealm(hero.pos)
						+ " duel=" + (hero.buff(RealmOfDeath.Duel.class) != null)
						+ " spoils=" + (hero.buff(RealmOfDeath.Spoils.class) != null));
				for (Mob m : rats) if (m != null) log("rat alive=" + m.isAlive() + " pos=" + m.pos + " HT=" + m.HT);
			}
		}
	}
}
