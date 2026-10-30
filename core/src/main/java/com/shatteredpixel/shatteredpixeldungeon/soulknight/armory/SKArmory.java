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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.armory;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.exit.ExitRoom;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//gun tiers, random gun for a depth, and the gun merchant on the 2nd floor of each new world
public class SKArmory {

	@SuppressWarnings("unchecked")
	private static final Class<? extends ArmoryGun>[][] TIERS = new Class[][]{
			{ArmoryGuns.OldPistol.class, ArmoryGuns.Smg.class, ArmoryGuns.Bow.class, ArmoryGuns.BubbleGun.class, ArmoryGuns.StunPistol.class},
			{ArmoryGuns.DesertEagle.class, ArmoryGuns.Shotgun.class, ArmoryGuns.Crossbow.class, ArmoryGuns.ToxicBlaster.class, ArmoryGuns.FrostGun.class},
			{ArmoryGuns.AssaultRifle.class, ArmoryGuns.Flamethrower.class, ArmoryGuns.ThunderStaff.class, ArmoryGuns.GrenadeLauncher.class, ArmoryGuns.FireworkLauncher.class},
			{ArmoryGuns.SniperRifle.class, ArmoryGuns.Gatling.class, ArmoryGuns.Bazooka.class, ArmoryGuns.LaserRifle.class, ArmoryGuns.PlasmaCannon.class}
	};

	public static final int SHOP_GUNS = 3;

	//for tests
	public static ArrayList<ArmoryGun> all(){
		ArrayList<ArmoryGun> list = new ArrayList<>();
		for (Class<? extends ArmoryGun>[] tier : TIERS){
			for (Class<? extends ArmoryGun> c : tier) list.add(make(c, 0));
		}
		return list;
	}

	public static int tierForDepth( int depth ){
		return Math.max(1, Math.min(4, 1 + (depth-1)/5));
	}

	public static ArmoryGun random( int tier ){
		tier = Math.max(1, Math.min(4, tier));
		ArmoryGun g = Reflection.newInstance(Random.element(TIERS[tier-1]));
		g.random();
		return g;
	}

	//full charges. the wand constructor runs before the gun sets its numbers so we redo the charges here. ugh, java
	public static ArmoryGun make( Class<? extends ArmoryGun> cls, int level ){
		ArmoryGun g = Reflection.newInstance(cls);
		g.level(level);
		g.curCharges = g.maxCharges;
		return g;
	}

	//merchant: quiet room on the 2nd floor, 3 guns on the ground
	public static void placeShop( Level level, ArrayList<Room> rooms ){
		if (Dungeon.depth % 5 != 2) return;
		ArrayList<Room> spots = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && !(r instanceof EntranceRoom) && !(r instanceof ExitRoom)
					&& r.width() >= 5 && r.height() >= 5){
				spots.add(r);
			}
		}
		Random.shuffle(spots);
		int tier = tierForDepth(Dungeon.depth);
		for (Room r : spots){
			int center = level.pointToCell(r.center());
			if (!usable(level, center)) continue;
			ArrayList<Integer> stalls = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8){
				if (stalls.size() >= SHOP_GUNS) break;
				int c = center + n;
				if (usable(level, c)) stalls.add(c);
			}
			if (stalls.size() < SHOP_GUNS) continue;
			SKMerchant m = new SKMerchant();
			m.pos = center;
			level.mobs.add(m);
			for (int i = 0; i < SHOP_GUNS; i++){
				//last slot is sometimes next tier
				int t = (i == SHOP_GUNS-1 && Random.Int(3) == 0) ? tier+1 : tier;
				level.drop(random(t), stalls.get(i)).type = Heap.Type.FOR_SALE;
			}
			return;
		}
	}

	private static boolean usable( Level level, int c ){
		if (c < 0 || c >= level.length()) return false;
		if (!level.passable[c] || level.pit[c] || level.heaps.get(c) != null) return false;
		if (level.map[c] == Terrain.ENTRANCE || level.map[c] == Terrain.EXIT) return false;
		for (Mob m : level.mobs){
			if (m.pos == c) return false;
		}
		return Actor.findChar(c) == null;
	}
}
