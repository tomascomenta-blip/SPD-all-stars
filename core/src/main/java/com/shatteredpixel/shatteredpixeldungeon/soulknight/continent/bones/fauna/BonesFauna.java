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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the beasts of the graveyard of colossi, by layer (OpenWorld.SURFACE, DEPTHS, ABYSS), like AshFauna does
//for the ash. the last one standing lies on the surface as a heap of bones; hyenas go in packs and
//marrow worms in nests
public class BonesFauna {

	public static final WildFauna.Biome BIOME = new WildFauna.Biome() {

		@Override
		public int zone() {
			return Continent.Z_BONES;
		}

		@Override
		public ArrayList<Class<? extends AshColossus>> bigs( int sizeClass ) {
			switch (sizeClass){
				case WildFauna.S3:  return WildFauna.kinds(MarrowCentipede.class, OssuaryVulture.class);
				case WildFauna.S5:  default: return WildFauna.kinds(RibDigger.class, SkinlessColossus.class, BoneChimera.class);
				case WildFauna.S10: return WildFauna.kinds(Mourner.class);
				case WildFauna.S15: return WildFauna.kinds();
			}
		}

		@Override
		public Class<? extends AshColossus> guardian() {
			return LastStanding.class;
		}

		@Override
		public Mob small( int layer ){
			float r = Random.Float();
			switch (layer){
				case OpenWorld.SURFACE: default:
					if (r < 0.20f) return new CharredPhoenix();
					if (r < 0.45f) return new MarrowWorm();
					if (r < 0.75f) return new OssuaryHyena();
					return new BoneBeetle();
				case OpenWorld.DEPTHS:
					if (r < 0.45f) return new MarrowWorm();
					if (r < 0.70f) return new BoneBeetle();
					return new OssuaryHyena();
				case OpenWorld.ABYSS:
					if (r < 0.45f) return new CharredPhoenix();
					return new OssuaryHyena();
			}
		}

		@Override
		public void spawned( Mob m ){
			if (m instanceof OssuaryHyena) ((OssuaryHyena) m).pack = 3 + Random.Int(3);
			if (m instanceof MarrowWorm) ((MarrowWorm) m).nest = 2 + Random.Int(2);
		}

		@Override
		public int weight( Mob m ){
			if (m instanceof OssuaryHyena) return 5;
			if (m instanceof MarrowWorm) return 3;
			return m instanceof YogLarva ? YogLarva.BROOD : 1;
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
