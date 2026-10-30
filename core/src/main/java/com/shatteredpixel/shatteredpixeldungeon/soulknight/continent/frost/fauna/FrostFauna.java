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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the beasts of the frozen moor, by layer (OpenWorld.SURFACE, DEPTHS, ABYSS), like AshFauna does for the ash.
//the shepherd walks the surface with a few elk or mammoths of his own; elk, raptors and wolves go
//in groups; seal-wolves only start in a hole of black water
public class FrostFauna {

	public static final WildFauna.Biome BIOME = new WildFauna.Biome() {

		@Override
		public int zone() {
			return Continent.Z_FROST;
		}

		@Override
		public ArrayList<Class<? extends AshColossus>> bigs( int sizeClass ) {
			switch (sizeClass){
				case WildFauna.S3:  return WildFauna.kinds(BlackIceMantis.class, SealWolf.class);
				case WildFauna.S5:  default: return WildFauna.kinds(IceElk.class, FlayedMammoth.class, FrostRaptor.class);
				case WildFauna.S10: return WildFauna.kinds(CorruptWyvern.class);
				case WildFauna.S15: return WildFauna.kinds(WalkingMountain.class);
			}
		}

		@Override
		public Class<? extends AshColossus> guardian() {
			return FrostShepherd.class;
		}

		@Override
		public Mob small( int layer ){
			float r = Random.Float();
			switch (layer){
				case OpenWorld.SURFACE: default:
					if (r < 0.25f) return new FrostHare();
					if (r < 0.45f) return new IceCrow();
					if (r < 0.75f) return new BlindWolf();
					return new IceLarva();
				case OpenWorld.DEPTHS:
					if (r < 0.50f) return new BlindWolf();
					return new IceLarva();
				case OpenWorld.ABYSS:
					if (r < 0.50f) return new IceCrow();
					return new BlindWolf();
			}
		}

		@Override
		public void herd( Level level, AshColossus c, boolean[] taken ){
			if (c instanceof FrostShepherd){
				WildFauna.herdOf(level, c, Random.Int(2) == 0 ? IceElk.class : FlayedMammoth.class, 2 + Random.Int(2), taken);
			} else if (c instanceof IceElk && c.leader == -1){
				WildFauna.herdOf(level, c, IceElk.class, 1 + Random.Int(2), taken);
			} else if (c instanceof FrostRaptor && c.leader == -1){
				WildFauna.herdOf(level, c, FrostRaptor.class, 1 + Random.Int(2), taken);
			}
		}

		@Override
		public void spawned( Mob m ){
			if (m instanceof BlindWolf) ((BlindWolf) m).pack = 3 + Random.Int(3);
		}

		@Override
		public int weight( Mob m ){
			if (m instanceof BlindWolf) return 5;
			return m instanceof YogLarva ? YogLarva.BROOD : 1;
		}

		@Override
		public boolean home( Level level, AshColossus c, int cell ){
			return !(c instanceof SealWolf) || level.water[cell];
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
