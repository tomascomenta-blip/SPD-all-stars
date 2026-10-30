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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;

import java.util.ArrayList;

//the beasts of the isles, by layer (OpenWorld.SURFACE, DEPTHS, ABYSS), like AshFauna does for the ash.
//yog in its true form is the guardian, down in the abyss, as far from the road as it fits. no essence out here
public class IslesFauna {

	public static final WildFauna.Biome BIOME = new WildFauna.Biome() {

		@Override
		public int zone() {
			return Continent.Z_ISLES;
		}

		@Override
		public ArrayList<Class<? extends AshColossus>> bigs( int sizeClass ) {
			switch (sizeClass){
				case WildFauna.S3:  return WildFauna.kinds(YogJellyfish.class);
				case WildFauna.S5:  default: return WildFauna.kinds(LighthouseCrab.class, InkEel.class);
				case WildFauna.S10: return WildFauna.kinds(TideOctopus.class);
				case WildFauna.S15: return WildFauna.kinds(IslandLizard.class);
			}
		}

		@Override
		public Class<? extends AshColossus> guardian() {
			return YogTrueForm.class;
		}

		@Override
		public int guardianLayer() {
			return OpenWorld.ABYSS;
		}

		@Override
		public Mob small( int layer ) {
			return new Castaway();
		}

		@Override
		public boolean essence() {
			return false;
		}
	};

	//fills a new floor, called at the end of its createMobs
	public static void populate( Level level, int layer ){
		WildFauna.populate(BIOME, level, layer);
	}

	//now and then, what died around here comes back far from the heroes (straight onto the floor)
	public static void respawn( Level level, int layer ){
		WildFauna.respawn(BIOME, level, layer);
	}

	//the same without adding it, for createMob: placed (pos set) or null
	public static Mob next( Level level, int layer ){
		return WildFauna.next(BIOME, level, layer);
	}
}
