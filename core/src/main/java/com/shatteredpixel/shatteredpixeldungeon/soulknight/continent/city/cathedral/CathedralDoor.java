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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.FurnaceCathedral;
import com.watabou.utils.Bundle;

//the open portal of a fallen furnace cathedral: its doorstep, as a way down into it. it's there
//only while it lies on the ground (see Cathedral.openDoors)
public class CathedralDoor extends LevelTransition {

	public int cathedral = -1;

	public CathedralDoor(){
		super();
	}

	public CathedralDoor( Level level, int cell, int cathedral ){
		super(level, cell, Type.BRANCH_EXIT, Cathedral.DEPTH, Continent.CITY, Type.REGULAR_ENTRANCE);
		this.cathedral = cathedral;
	}

	public FurnaceCathedral cathedral(){
		Actor a = Actor.findById(cathedral);
		return a instanceof FurnaceCathedral && ((FurnaceCathedral) a).isAlive() ? (FurnaceCathedral) a : null;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("cathedral", cathedral);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		cathedral = bundle.getInt("cathedral");
	}
}
