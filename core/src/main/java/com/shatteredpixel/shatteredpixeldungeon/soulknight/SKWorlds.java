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

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.Boar;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.ForestBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.ForestLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinElite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinGiant;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinGunner;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinShaman;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinSpearman;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.TrumpetFlower;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.StarshipBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.StarshipLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.StarshipMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaMobs;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//regions 1-4 can get swapped for one of the new worlds:
//1-5 forest, 6-10 knight kingdom, 11-15 starship, 16-20 undersea
//50% each, rolled from the seed at run start and saved. old saves keep the normal regions
//26 never changes. (21-25 used to be off limits too, now it can be the forgotten land, see FORGOTTEN)
public class SKWorlds {

	public static final int FOREST   = 1;
	public static final int KINGDOM  = 2;
	public static final int STARSHIP = 3;
	public static final int UNDERSEA = 4;
	public static final int FORGOTTEN = 5; //21-25 (+26)

	//which ones are actually done. index = region
	private static final boolean[] READY = new boolean[]{false, true, true, true, true, true};

	//seed only, never touch the game's Random here or every run after it shifts
	public static int roll( long seed ){
		return roll(seed, 50);
	}

	//chance 0-100 per region. 0 never, 100 always
	public static int roll( long seed, int chance ){
		java.util.Random r = new java.util.Random(seed ^ 0x536F756C4BL);
		int mask = 0;
		for (int region = FOREST; region <= FORGOTTEN; region++){
			if (r.nextInt(100) < chance) mask |= 1 << region;
		}
		return mask;
	}

	public static int region( int depth ){
		return (depth - 1) / 5 + 1;
	}

	public static boolean isSK( int depth ){
		if (Dungeon.branch != 0 || depth < 1 || depth > 25) return false;
		int region = region(depth);
		//-Dspd.test.skworld=1 forces all finished worlds, =all includes WIP ones, =0 none
		String force = DevFlags.skWorld();
		if (!READY[region] && !"all".equals(force)) return false;
		if (force != null) return !force.equals("0");
		return (Dungeon.skWorlds & (1 << region)) != 0;
	}

	//region of the run was one of the new worlds (ignores where you are now)
	public static boolean regionSK( int region ){
		if (region < FOREST || region > FORGOTTEN) return false;
		String force = DevFlags.skWorld();
		if (!READY[region] && !"all".equals(force)) return false;
		if (force != null) return !force.equals("0");
		return (Dungeon.skWorlds & (1 << region)) != 0;
	}

	public static boolean isSKLevel(){
		return isSK(Dungeon.depth);
	}

	public static Level newLevel( int depth ){
		if (!isSK(depth)) return null;
		boolean boss = depth % 5 == 0;
		switch (region(depth)){
			case FOREST:
				return boss ? new ForestBossLevel() : new ForestLevel();
			case KINGDOM:
				return boss ? new KingdomBossLevel()
						: new KingdomLevel();
			case STARSHIP:
				return boss ? new StarshipBossLevel()
						: new StarshipLevel();
			case UNDERSEA:
				return boss ? new UnderseaBossLevel()
						: new UnderseaLevel();
			case FORGOTTEN:
				return boss ? new ForgottenBossLevel()
						: new ForgottenLevel();
			default:
				return null;
		}
	}

	//amulet floor keeps the forgotten land look if region 5 was forgotten
	public static boolean forgottenAmulet( int depth ){
		if (depth != 26 || Dungeon.branch != 0) return false;
		String force = DevFlags.skWorld();
		if (!READY[FORGOTTEN] && !"all".equals(force)) return false;
		if (force != null) return !force.equals("0");
		return (Dungeon.skWorlds & (1 << FORGOTTEN)) != 0;
	}

	//null = rotación normal
	public static ArrayList<Class<? extends Mob>> mobRotation( int depth ){
		if (!isSK(depth)) return null;
		return rotation(depth);
	}

	//the table itself, no checks. the rift's portals pull monsters from here too
	public static ArrayList<Class<? extends Mob>> rotation( int depth ){
		ArrayList<Class<? extends Mob>> mobs;
		switch (depth){
			case 1: default:
				mobs = new ArrayList<>(Arrays.asList(
						Boar.class, Boar.class, Boar.class,
						GoblinSpearman.class));
				break;
			case 2:
				mobs = new ArrayList<>(Arrays.asList(
						Boar.class, Boar.class,
						GoblinSpearman.class,
						GoblinGunner.class, GoblinGunner.class));
				break;
			case 3:
				mobs = new ArrayList<>(Arrays.asList(
						Boar.class,
						GoblinSpearman.class, GoblinSpearman.class,
						GoblinGunner.class, GoblinGunner.class,
						TrumpetFlower.class,
						GoblinElite.class));
				break;
			case 4: case 5:
				mobs = new ArrayList<>(Arrays.asList(
						GoblinSpearman.class,
						GoblinGunner.class,
						TrumpetFlower.class,
						GoblinElite.class, GoblinElite.class,
						GoblinShaman.class, GoblinShaman.class));
				break;

			//kingdom
			case 6:
				mobs = new ArrayList<>(Arrays.asList(
						KingdomMobs.Swordsman.class, KingdomMobs.Swordsman.class,
						KingdomMobs.Soldier.class, KingdomMobs.Soldier.class,
						KingdomMobs.RoyalSlime.class));
				break;
			case 7:
				mobs = new ArrayList<>(Arrays.asList(
						KingdomMobs.Swordsman.class, KingdomMobs.Swordsman.class,
						KingdomMobs.Soldier.class, KingdomMobs.Soldier.class,
						KingdomMobs.RoyalSlime.class, KingdomMobs.Gunner.class));
				break;
			case 8:
				mobs = new ArrayList<>(Arrays.asList(
						KingdomMobs.Swordsman.class,
						KingdomMobs.Soldier.class, KingdomMobs.Soldier.class,
						KingdomMobs.Gunner.class, KingdomMobs.Rifleman.class,
						KingdomMobs.RoyalSlime.class, KingdomMobs.Wizard.class));
				break;
			case 9: case 10:
				mobs = new ArrayList<>(Arrays.asList(
						KingdomMobs.Swordsman.class, KingdomMobs.Soldier.class,
						KingdomMobs.Gunner.class,
						KingdomMobs.Rifleman.class, KingdomMobs.Rifleman.class,
						KingdomMobs.Wizard.class, KingdomMobs.Wizard.class,
						KingdomMobs.RoyalSlime.class));
				break;

			//starship
			case 11:
				mobs = new ArrayList<>(Arrays.asList(
						StarshipMobs.Varkolyn.class, StarshipMobs.Varkolyn.class, StarshipMobs.Varkolyn.class,
						StarshipMobs.AlienLaser.class, StarshipMobs.AlienSword.class));
				break;
			case 12:
				mobs = new ArrayList<>(Arrays.asList(
						StarshipMobs.Varkolyn.class, StarshipMobs.Varkolyn.class,
						StarshipMobs.AlienLaser.class, StarshipMobs.AlienSword.class,
						StarshipMobs.Ufo.class, StarshipMobs.Tentacle.class));
				break;
			case 13:
				mobs = new ArrayList<>(Arrays.asList(
						StarshipMobs.Varkolyn.class,
						StarshipMobs.AlienLaser.class, StarshipMobs.AlienLaser.class,
						StarshipMobs.AlienSword.class,
						StarshipMobs.Ufo.class, StarshipMobs.Ufo.class,
						StarshipMobs.Tentacle.class, StarshipMobs.Battery.class));
				break;
			case 14: case 15:
				mobs = new ArrayList<>(Arrays.asList(
						StarshipMobs.Varkolyn.class, StarshipMobs.AlienLaser.class,
						StarshipMobs.AlienSword.class, StarshipMobs.AlienSword.class,
						StarshipMobs.Ufo.class, StarshipMobs.Tentacle.class,
						StarshipMobs.Battery.class, StarshipMobs.Battery.class));
				break;

			//undersea
			case 16:
				mobs = new ArrayList<>(Arrays.asList(
						UnderseaMobs.InfectedCrew.class, UnderseaMobs.InfectedCrew.class,
						UnderseaMobs.KrakenMinion.class, UnderseaMobs.KrakenMinion.class,
						UnderseaMobs.Subject001.class));
				break;
			case 17:
				mobs = new ArrayList<>(Arrays.asList(
						UnderseaMobs.InfectedCrew.class, UnderseaMobs.InfectedCrew.class,
						UnderseaMobs.KrakenMinion.class,
						UnderseaMobs.Subject001.class, UnderseaMobs.Subject002.class));
				break;
			case 18:
				mobs = new ArrayList<>(Arrays.asList(
						UnderseaMobs.InfectedCrew.class,
						UnderseaMobs.KrakenMinion.class, UnderseaMobs.KrakenMinion.class,
						UnderseaMobs.Subject001.class,
						UnderseaMobs.Subject002.class, UnderseaMobs.Subject002.class));
				break;
			case 19: case 20:
				mobs = new ArrayList<>(Arrays.asList(
						UnderseaMobs.InfectedCrew.class, UnderseaMobs.InfectedCrew.class,
						UnderseaMobs.KrakenMinion.class,
						UnderseaMobs.Subject001.class, UnderseaMobs.Subject001.class,
						UnderseaMobs.Subject002.class));
				break;

			//forgotten: ice age up top, dinos below
			case 21:
			case 22:
			case 23:
			case 24:
			case 25:
				mobs = ForgottenMobs.rotation(depth);
				break;
		}
		//rare variants, same idea as vanilla's rare mobs
		if (region(depth) == FOREST && depth >= 3 && Random.Float() < 0.04f){
			mobs.set(0, GoblinGiant.class);
		} else if (region(depth) == KINGDOM && depth >= 8 && Random.Float() < 0.04f){
			mobs.set(0, KingdomMobs.DarkKnight.class);
		} else if (region(depth) == UNDERSEA && depth >= 18 && Random.Float() < 0.04f){
			mobs.set(0, UnderseaMobs.Subject004.class);
		}
		return mobs;
	}

	//loading screen text, null = vanilla one
	public static String intro( String splash ){
		if (SKAssets.SPLASH_FOREST.equals(splash)) return Messages.get(SKWorlds.class, "intro_forest");
		if (SKAssets.SPLASH_KINGDOM.equals(splash)) return Messages.get(SKWorlds.class, "intro_kingdom");
		if (SKAssets.SPLASH_STARSHIP.equals(splash)) return Messages.get(SKWorlds.class, "intro_starship");
		if (SKAssets.SPLASH_UNDERSEA.equals(splash)) return Messages.get(SKWorlds.class, "intro_undersea");
		if (SKAssets.SPLASH_FORGOTTEN.equals(splash)) return Messages.get(SKWorlds.class, "intro_forgotten");
		return null;
	}

	//first floor of a new run, nothing's initialized yet so we go off the seed
	public static String splashForNewRun( long seed ){
		if (!READY[FOREST]) return null;
		String force = DevFlags.skWorld();
		boolean forest = force != null ? !force.equals("0") : (roll(seed) & (1 << FOREST)) != 0;
		return forest ? SKAssets.SPLASH_FOREST : null;
	}

	public static String splash( int depth ){
		if (!isSK(depth)) return null;
		switch (region(depth)){
			case FOREST:
				return SKAssets.SPLASH_FOREST;
			case KINGDOM:
				return SKAssets.SPLASH_KINGDOM;
			case STARSHIP:
				return SKAssets.SPLASH_STARSHIP;
			case UNDERSEA:
				return SKAssets.SPLASH_UNDERSEA;
			case FORGOTTEN:
				return SKAssets.SPLASH_FORGOTTEN;
			default:
				return null;
		}
	}
}
