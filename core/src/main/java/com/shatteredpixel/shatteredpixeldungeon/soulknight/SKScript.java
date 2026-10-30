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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster.Taming;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.Boar;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.ForestFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.SKGun;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKStatue;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//test only: -Dspd.test.skscript=STATUE,TRAP,BARREL,CRATE walks the hero to each thing one by one
//(a step every few secs) and logs what happened. for screenshots + making sure nothing crashes
public class SKScript {

	private static final String SCRIPT = System.getProperty("spd.test.skscript");
	private static float clock = 0;
	private static int step = -1;

	public static boolean active(){
		return SCRIPT != null;
	}

	public static void tick( Hero hero ){
		if (SCRIPT == null || hero == null || !hero.ready) return;
		clock += Game.elapsed;
		Window w = GameScene.topWindow();
		if (w instanceof WndSKOptions) return; //leave it open, screenshot
		if (w != null){
			//just take the first blessing, da igual cuál
			if (clock > 1.5f){
				ArrayList<Button> b = CoopSession.buttonsOfWindow(w);
				CoopLog.line("sk script: window " + w.getClass().getSimpleName() + " buttons=" + b.size());
				if (!b.isEmpty()) b.get(0).coopClick(); else w.hide();
				clock = 0;
			}
			return;
		}
		if (clock < 4f) return;
		clock = 0;
		String[] parts = SCRIPT.split(",");
		step++;
		if (step >= parts.length){
			if (step == parts.length) CoopLog.line("sk script: done hp=" + hero.HP + "/" + hero.HT + " gold=" + Dungeon.gold);
			return;
		}
		String a = parts[step].trim();
		CoopLog.line("sk script: step " + a + " hp=" + hero.HP + "/" + hero.HT + " gold=" + Dungeon.gold);
		//LOOK:SimpleClassName walks next to the first one
		if (a.startsWith("LOOK:")){
			String name = a.substring(5);
			Mob found = null;
			for (Mob m : Dungeon.level.mobs){
				if (m.getClass().getSimpleName().equals(name)){ found = m; break; }
			}
			lookAt(hero, found);
			return;
		}
		//USE:SimpleClassName same + interact/attack
		if (a.startsWith("USE:")){
			String name = a.substring(4);
			for (Mob m : Dungeon.level.mobs){
				if (m.getClass().getSimpleName().equals(name)){
					moveNextTo(hero, m.pos);
					if (hero.handle(m.pos)) hero.next();
					CoopLog.line("sk script: use " + name + " at " + m.pos);
					return;
				}
			}
			CoopLog.line("sk script: no " + name);
			return;
		}
		switch (a){
			case "STATUE": {
				SKStatue s = find(SKStatue.class);
				if (s == null){ CoopLog.line("sk script: no statue"); return; }
				Dungeon.gold += 500;
				moveNextTo(hero, s.pos);
				s.interact(hero);
				break;
			}
			case "TRAP": {
				SKEvents ev = SKEvents.get(Dungeon.level);
				if (ev == null || ev.trapChests.isEmpty()){ CoopLog.line("sk script: no trap chest"); return; }
				int cell = ev.trapChests.get(0).cell;
				Heap h = Dungeon.level.heaps.get(cell);
				CoopLog.line("sk script: trap chest at " + cell + " heap=" + (h == null ? "none" : h.type + " " + h.items.size()));
				moveNextTo(hero, cell);
				if (hero.handle(cell)) hero.next();
				break;
			}
			case "BARREL": {
				SKProps.Barrel b = find(SKProps.Barrel.class);
				if (b == null){ CoopLog.line("sk script: no barrel"); return; }
				CoopLog.line("sk script: breaking " + b.getClass().getSimpleName() + " at " + b.pos);
				moveNextTo(hero, b.pos);
				if (hero.handle(b.pos)) hero.next();
				break;
			}
			case "CRATE": {
				SKProps.Crate c = find(SKProps.Crate.class);
				if (c == null){ CoopLog.line("sk script: no crate"); return; }
				CoopLog.line("sk script: hitting crate at " + c.pos + " hp=" + c.HP + "/" + c.HT);
				moveNextTo(hero, c.pos);
				if (hero.handle(c.pos)) hero.next();
				break;
			}
			case "WAIT":
				hero.rest(false);
				break;
			case "HT": {
				StringBuilder sb = new StringBuilder("sk script: difficulty=" + Dungeon.difficulty + " worlds=" + Dungeon.skWorlds
						+ " level=" + Dungeon.level.getClass().getSimpleName() + " mobs:");
				int n = 0;
				for (Mob m : Dungeon.level.mobs){
					if (m.alignment != Char.Alignment.ENEMY || n++ >= 4) continue;
					sb.append(' ').append(m.getClass().getSimpleName()).append('=').append(m.HP).append('/').append(m.HT);
				}
				CoopLog.line(sb.toString());
				break;
			}
			case "OPTIONS":
				GameScene.show(new WndSKOptions());
				break;
			case "TAME": {
				Mob animal = null;
				for (Mob m : Dungeon.level.mobs){
					if (Taming.animal(m) && m.alignment == Char.Alignment.ENEMY){ animal = m; break; }
				}
				if (animal == null){
					animal = new Boar();
					for (int n : PathFinder.NEIGHBOURS8){
						int c = hero.pos + 2*n;
						if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null){ animal.pos = c; break; }
					}
					GameScene.add(animal);
				}
				animal.HP = Math.max(1, animal.HT/5);
				moveNextTo(hero, animal.pos);
				String p = Taming.problem(hero, animal);
				CoopLog.line("sk script: tame " + animal.getClass().getSimpleName() + " problem=" + p);
				if (p == null) Taming.tame(hero, animal);
				break;
			}
			case "PET": {
				for (Mob m : Dungeon.level.mobs){
					if (m.alignment == Char.Alignment.ALLY || m instanceof Boar)
						CoopLog.line("sk script: mob " + m.getClass().getSimpleName() + " at " + m.pos + " align=" + m.alignment + " buffs=" + m.buffs());
				}
				for (Mob m : Dungeon.level.mobs){
					if (m.buff(Taming.Tamed.class) != null){
						CoopLog.line("sk script: pet " + m.getClass().getSimpleName() + " at " + m.pos + " hero at " + hero.pos + " ally=" + (m.alignment == Char.Alignment.ALLY) + " hp=" + m.HP + "/" + m.HT);
					}
				}
				break;
			}
			case "RELOAD": {
				SKGun g = hero.belongings.getItem(SKGun.class);
				if (g == null){ CoopLog.line("sk script: no gun"); return; }
				g.curCharges = 0;
				boolean can = g.canReload(hero);
				g.execute(hero, SKGun.AC_RELOAD);
				CoopLog.line("sk script: reload " + g.getClass().getSimpleName() + " could=" + can + " charges=" + g.curCharges + "/" + g.maxCharges + " again=" + g.canReload(hero));
				break;
			}
			case "HUNGER": {
				Hunger h = hero.buff(Hunger.class);
				CoopLog.line("sk script: hunger " + (h == null ? "none" : h.hunger()) + " class=" + hero.heroClass);
				break;
			}
			case "STARVE": {
				//fast forward, keep hunger honest
				Hunger h = hero.buff(Hunger.class);
				if (h != null) for (int i = 0; i < 400; i++) h.act();
				CoopLog.line("sk script: after 400 hunger turns " + (h == null ? "none" : h.hunger()));
				break;
			}
			case "CAMP": lookAt(hero, find(ForestFeatures.Campfire.class)); break;
			case "TOTEM": lookAt(hero, find(ForestFeatures.GoblinTotem.class)); break;
			case "BEARER": lookAt(hero, find(KingdomFeatures.BannerBearer.class)); break;
			case "RACK": {
				KingdomFeatures.WeaponRack r =
						find(KingdomFeatures.WeaponRack.class);
				if (r == null){ CoopLog.line("sk script: no rack"); return; }
				moveNextTo(hero, r.pos);
				if (hero.handle(r.pos)) hero.next();
				break;
			}
			case "FLOOR": case "SPIKES": case "SLIT": {
				SKFloor f = SKFloor.get(Dungeon.level);
				int cell = -1;
				if (f != null){
					if (a.equals("SLIT")){
						if (!f.slits.isEmpty()) cell = f.slits.get(0).wall + 2*Dungeon.level.width();
					} else {
						for (int c : f.cells()){
							if (a.equals("FLOOR") || f.kindAt(c) == SKFloor.SPIKES){ cell = c; break; }
						}
					}
				}
				CoopLog.line("sk script: " + a + " at " + cell + " floor cells=" + (f == null ? 0 : f.cells().size()));
				if (cell != -1) moveNextTo(hero, cell);
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Mob> T find( Class<T> cls ){
		for (Mob m : Dungeon.level.mobs){
			if (cls.isInstance(m) && m.isAlive()) return (T) m;
		}
		return null;
	}

	private static void lookAt( Hero hero, Mob m ){
		CoopLog.line("sk script: look at " + (m == null ? "nothing" : m.getClass().getSimpleName() + " " + m.pos));
		if (m != null) moveNextTo(hero, m.pos);
	}

	private static void moveNextTo( Hero hero, int cell ){
		if (!Dungeon.level.adjacent(hero.pos, cell)){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = cell + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
					ScrollOfTeleportation.appear(hero, c);
					break;
				}
			}
		}
		if (hero.sprite != null) Camera.main.snapTo(hero.sprite.center());
	}
}
