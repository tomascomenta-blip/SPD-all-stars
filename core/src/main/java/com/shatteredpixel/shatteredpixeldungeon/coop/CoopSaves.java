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
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//the co-op runs saved on this device. slots 90-97 are the runs it hosted (90 was the only one
//before 962), 98 is where a guest keeps its copy of the host's run (never listed)
public class CoopSaves {

	public static final int FIRST_SLOT = 90;
	public static final int SLOTS = 8;
	public static final int GUEST_SLOT = FIRST_SLOT + SLOTS;

	public static class Info {
		public int slot;
		public String[] names = new String[0];
		public HeroClass[] classes = new HeroClass[0];
		public int[] levels = new int[0];
		public int depth;
		public long lastPlayed;
		public int difficulty;
		public int challenges;
		public String customSeed;
		public boolean friendlyFire;

		public String host(){
			return names.length > 0 ? names[0] : "?";
		}

		//saved by someone else as host (a guest copy from before 962, or a renamed player)
		public boolean hostedBy( String name ){
			return name != null && name.equals(host());
		}
	}

	public static boolean exists( int slot ){
		return GamesInProgress.gameExists(slot);
	}

	public static Info get( int slot ){
		if (!exists(slot)) return null;
		try {
			Bundle b = FileUtils.bundleFromFile(GamesInProgress.gameFile(slot));
			Info info = new Info();
			info.slot = slot;
			info.depth = b.getInt("depth");
			info.lastPlayed = b.getLong("last_played");
			info.difficulty = b.contains("sk_difficulty") ? b.getInt("sk_difficulty") : 1;
			info.challenges = b.getInt("challenges");
			info.customSeed = b.getString("custom_seed");
			info.friendlyFire = b.getBoolean("friendly_fire");

			ArrayList<Bundle> heroes = new ArrayList<>();
			heroes.add(b.getBundle("hero"));
			if (b.contains("coop_heroes")){
				for (Bundle h : b.getBundleArray("coop_heroes")) heroes.add(h);
			}
			String[] names = b.contains("coop_names") ? b.getStringArray("coop_names") : new String[]{"?"};
			int n = Math.min(heroes.size(), names.length);
			info.names = new String[n];
			info.classes = new HeroClass[n];
			info.levels = new int[n];
			for (int i = 0; i < n; i++){
				info.names[i] = names[i];
				info.classes[i] = heroes.get(i).getEnum("class", HeroClass.class);
				info.levels[i] = heroes.get(i).getInt("lvl");
			}
			return info;
		} catch (Exception e){
			return null;
		}
	}

	//most recent first
	public static ArrayList<Info> all(){
		ArrayList<Info> list = new ArrayList<>();
		for (int s = FIRST_SLOT; s < FIRST_SLOT + SLOTS; s++){
			Info i = get(s);
			if (i != null) list.add(i);
		}
		Collections.sort(list, new Comparator<Info>() {
			@Override
			public int compare(Info a, Info b) {
				return Long.compare(b.lastPlayed, a.lastPlayed);
			}
		});
		return list;
	}

	//-1 when all 8 are taken
	public static int firstEmpty(){
		for (int s = FIRST_SLOT; s < FIRST_SLOT + SLOTS; s++){
			if (!exists(s)) return s;
		}
		return -1;
	}

	public static void delete( int slot ){
		Dungeon.deleteGame(slot, true);
	}
}
