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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.AshMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogEye;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;

import java.util.ArrayList;
import java.util.Arrays;

//who lives where on the continent
public class ContinentMobs {

	public static ArrayList<Class<? extends Mob>> rotation( int depth, int branch ){
		switch (branch){
			case Continent.ASH: default:
				return AshMobs.rotation(depth);
		}
	}

	//what a shrine pulls in while it charges
	public static ArrayList<Class<? extends Mob>> siege(){
		return list(YogEye.class, YogLarva.class, YogLarva.class);
	}

	@SafeVarargs
	private static ArrayList<Class<? extends Mob>> list( Class<? extends Mob>... c ){
		return new ArrayList<>(Arrays.asList(c));
	}
}
