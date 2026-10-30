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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.BurningFist;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogEye;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;

import java.util.ArrayList;
import java.util.Arrays;

//who lives on the ash plains, floors 21-24 of the branch. eyes everywhere, the elites further in.
//no tongues in here: those go in the corruption puddles of the level (YogTongue)
public class AshMobs {

	public static ArrayList<Class<? extends Mob>> rotation( int depth ){
		switch (depth){
			case 21: default:
				return new ArrayList<>(Arrays.asList(
						YogEye.class, YogEye.class, AshWalker.class, AshWalker.class,
						SmokeVulture.class, YogLarva.class));
			case 22:
				return new ArrayList<>(Arrays.asList(
						YogEye.class, YogEye.class, AshWalker.class, AshWalker.class,
						Salamander.class, Salamander.class, SmokeVulture.class, YogLarva.class));
			case 23:
				return new ArrayList<>(Arrays.asList(
						YogEye.class, YogEye.class, AshWalker.class, Salamander.class,
						SmokeVulture.class, SmokeVulture.class, YogLarva.class, BurningFist.class));
			case 24: case 25:
				return new ArrayList<>(Arrays.asList(
						YogEye.class, YogEye.class, AshWalker.class, Salamander.class,
						SmokeVulture.class, YogLarva.class, BurningFist.class, ObsidianColossus.class));
		}
	}
}
