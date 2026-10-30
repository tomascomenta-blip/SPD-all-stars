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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.ArmoryGun;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKArmory;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.Boar;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinElite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinGiant;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinGunner;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinPriest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinShaman;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinSpearman;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.TrumpetFlower;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.Tharog;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.Tribe;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.GoblinPistol;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.GoblinShotgun;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.MeteorStaff;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.SKGun;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.DarkGrandKnight;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.StarshipMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.VarkolynLeader;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.AbyssalSubmariner;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaMobs;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//test stuff, solo runs w/ -Dspd.test.start=CLASS
//-Dspd.test.skguns=1 -> every gun in the backpack
//-Dspd.test.skshoot=1 -> + spawns some forest mobs and shoots them
//-Dspd.test.skzoo=forest|kingdom|starship|undersea -> one calm mob of each kind around you (bubble ones
//twice, w/ and w/o bubble). for checking sprites. the zoo!
public class SKTest {

	private static boolean prepared = false;
	private static float clock = 0;
	private static int shot = 0;

	public static void tick( Hero hero ){
		if (SKScript.active()){
			SKScript.tick(hero);
			return;
		}
		String zoo = System.getProperty("spd.test.skzoo");
		if (zoo != null && hero != null){
			zoo(hero, zoo);
			return;
		}
		String gunsProp = System.getProperty("spd.test.skguns");
		boolean guns = "1".equals(gunsProp) || "late".equals(gunsProp);
		boolean shoot = "1".equals(System.getProperty("spd.test.skshoot"));
		if (!(guns || shoot) || hero == null) return;

		if (!prepared){
			prepared = true;
			//"late" = only the late guns, they don't all fit in the backpack
			boolean late = "late".equals(System.getProperty("spd.test.skguns"));
			Item[] all = {new GoblinPistol(), new GoblinShotgun(), new MeteorStaff()};
			if (!late) for (Item i : all) i.collect();
			java.util.ArrayList<ArmoryGun> armory = SKArmory.all();
			for (int k = late ? 12 : 0; k < armory.size(); k++) armory.get(k).identify().collect();
			CoopLog.line("sk test: guns in the backpack");
			return;
		}
		if (!shoot || !hero.ready || GameScene.topWindow() != null) return;

		clock += Game.elapsed;
		if (clock < 3f) return;
		clock = 0;

		if (GameScene.isSelectingCell()){
			Char t = nearest(hero);
			GameScene.handleCell(t == null ? hero.pos + 1 : t.pos);
			return;
		}

		if (nearest(hero) == null){
			spawnAround(hero);
			return;
		}

		ArrayList<SKGun> owned = new ArrayList<>(hero.belongings.getAllItems(SKGun.class));
		if (owned.isEmpty()) return;
		SKGun gun = owned.get(shot++ % owned.size());
		CoopLog.line("sk test: firing " + gun.name() + " charges=" + gun.curCharges);
		gun.execute(hero, Wand.AC_ZAP);
	}

	private static boolean zooDone = false;

	private static void zoo( Hero hero, String world ){
		if (zooDone || !hero.ready) return;
		zooDone = true;
		Class<?>[] kinds;
		switch (world){
			case "kingdom":
				kinds = new Class<?>[]{KingdomMobs.Soldier.class,
						KingdomMobs.Swordsman.class,
						KingdomMobs.Gunner.class,
						KingdomMobs.Rifleman.class,
						KingdomMobs.RoyalSlime.class,
						KingdomMobs.Wizard.class,
						KingdomMobs.DarkKnight.class,
						DarkGrandKnight.class};
				break;
			case "starship":
				kinds = new Class<?>[]{StarshipMobs.Varkolyn.class,
						StarshipMobs.AlienLaser.class,
						StarshipMobs.AlienSword.class,
						StarshipMobs.Ufo.class,
						StarshipMobs.Tentacle.class,
						StarshipMobs.Battery.class,
						VarkolynLeader.class};
				break;
			case "undersea":
				kinds = new Class<?>[]{UnderseaMobs.InfectedCrew.class,
						UnderseaMobs.KrakenMinion.class,
						UnderseaMobs.Subject001.class,
						UnderseaMobs.Subject001.class,
						UnderseaMobs.Subject002.class,
						UnderseaMobs.Subject004.class,
						AbyssalSubmariner.class};
				break;
			case "forgotten": {
				String F = "com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.";
				kinds = new Class<?>[]{
						ForgottenMobs.Mammoth.class,
						ForgottenMobs.Sabertooth.class,
						ForgottenMobs.FrostWolf.class,
						ForgottenMobs.Yeti.class,
						ForgottenMobs.Sloth.class,
						ForgottenMobs.WoollyRhino.class,
						ForgottenMobs.Raptor.class,
						ForgottenMobs.Triceratops.class,
						ForgottenMobs.Pterodactyl.class,
						ForgottenMobs.Dilophosaurus.class,
						ForgottenMobs.Ankylosaurus.class,
						ForgottenMobs.Compy.class,
						ForgottenMobs.Spinosaurus.class,
						ForgottenMobs.CarnivorePlant.class,
						Tribe.Shaman.class,
						Tribe.Warrior.class,
						ForgottenFeatures.Brazier.class,
						ForgottenFeatures.FrozenBlock.class,
						ForgottenFeatures.Nest.class,
						Tharog.class};
				break;
			}
			default:
				kinds = new Class<?>[]{Boar.class, GoblinSpearman.class,
						GoblinGunner.class, GoblinElite.class, GoblinShaman.class, TrumpetFlower.class,
						GoblinGiant.class,
						GoblinPriest.class};
		}
		int placed = 0;
		boolean popped = false;
		for (int r = 2; r <= 6 && placed < kinds.length; r++){
			for (int i = 0; i < Dungeon.level.length() && placed < kinds.length; i++){
				if (Dungeon.level.distance(hero.pos, i) != r || !Dungeon.level.passable[i] || Actor.findChar(i) != null) continue;
				boolean crowded = false;
				for (int n : PathFinder.NEIGHBOURS8){
					if (Actor.findChar(i + n) != null) crowded = true;
				}
				if (crowded) continue;
				try {
					Mob m = (Mob) kinds[placed].newInstance();
					m.pos = i;
					m.state = m.PASSIVE;
					GameScene.add(m);
					//2nd one = popped bubble look
					if (m instanceof UnderseaMobs.Subject001){
						if (popped) m.damage(1, hero);
						popped = true;
					}
				} catch (Exception e){
					CoopLog.line("sk zoo: " + e);
				}
				placed++;
			}
		}
		CoopLog.line("sk zoo: " + world + " " + placed + " of " + kinds.length);
	}

	private static Char nearest( Hero hero ){
		Char best = null;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[m.pos]
					&& (best == null || Dungeon.level.distance(hero.pos, m.pos) < Dungeon.level.distance(hero.pos, best.pos))){
				best = m;
			}
		}
		return best;
	}

	private static void spawnAround( Hero hero ){
		Class<?>[] kinds = {GoblinGunner.class, Boar.class, GoblinElite.class, TrumpetFlower.class, GoblinShaman.class};
		int placed = 0;
		for (int i = 0; i < Dungeon.level.length() && placed < 3; i++){
			int d = Dungeon.level.distance(hero.pos, i);
			if (d < 3 || d > 4 || !Dungeon.level.passable[i] || Actor.findChar(i) != null || !Dungeon.level.heroFOV[i]) continue;
			boolean crowded = false;
			for (int n : PathFinder.NEIGHBOURS8){
				if (Actor.findChar(i + n) != null) crowded = true;
			}
			if (crowded) continue;
			try {
				Mob m = (Mob) kinds[(placed + shot) % kinds.length].newInstance();
				m.pos = i;
				m.state = m.HUNTING;
				GameScene.add(m);
				placed++;
			} catch (Exception e){
				CoopLog.line("sk test: " + e);
			}
		}
		CoopLog.line("sk test: spawned " + placed);
	}
}
