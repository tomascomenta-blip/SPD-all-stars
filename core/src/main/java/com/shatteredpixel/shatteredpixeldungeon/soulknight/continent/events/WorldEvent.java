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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.events;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

//one thing happening to a floor of the continent. it lives in the turns (act, once a turn right
//before the heroes) and keeps what the screens need to draw it: how strong it is, where it comes
//from, and a count that goes up each time there's something new to show on the card
public abstract class WorldEvent implements Bundlable {

	public int depth, branch;
	public int age = 0;
	boolean calming = false;
	//goes up each time the screens should bring up the card again
	public int card = 0;

	public abstract int kind();

	//sets it up on the floor the party is on, false if there's no room for it here
	abstract boolean begin( int layer );

	//one turn of it, false once it's over
	abstract boolean act();

	//a guardian has started fighting: it winds down
	void calm(){
		calming = true;
	}

	//the party left the floor in the middle of it
	void leave(){
	}

	public boolean here(){
		return depth == Dungeon.depth && branch == Dungeon.branch;
	}

	// *** for the screens ***

	//0 to 1, how much of it is on the floor right now
	public float power(){
		return 0;
	}

	//the cell it comes from while it's still coming (dust on the horizon that way), -1 for none
	public int origin(){
		return -1;
	}

	public abstract String title();

	public abstract String line();

	@Override
	public void storeInBundle( Bundle bundle ) {
		bundle.put("depth", depth);
		bundle.put("branch", branch);
		bundle.put("age", age);
		bundle.put("calming", calming);
		bundle.put("card", card);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		depth = bundle.getInt("depth");
		branch = bundle.getInt("branch");
		age = bundle.getInt("age");
		calming = bundle.getBoolean("calming");
		card = bundle.getInt("card");
	}
}
