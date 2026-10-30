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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.quests;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.watabou.utils.Bundle;

//what every side quest remembers. the whole party shares one quest but each hero picks their own
//reward, so 'claimed' keeps track of who already did
public class QuestState {

	public final String key;

	public int depth = -1;          //quest floor, -1 = not placed yet
	public boolean given = false;   //someone talked to the giver
	public boolean slain = false;   //quest mob dead
	public boolean done = false;    //finished, rewards can be claimed
	public boolean failed = false;  //too late, the region boss gets the punishment
	public boolean bonus = false;   //done the hard way -> better rewards
	public int[] claimed = new int[0];

	public QuestState( String key ){
		this.key = key;
	}

	public void reset(){
		depth = -1;
		given = slain = done = failed = bonus = false;
		claimed = new int[0];
	}

	public boolean claimedBy( Hero h ){
		int idx = ChampionHooks.indexOf(h);
		for (int c : claimed) if (c == idx) return true;
		return false;
	}

	public void claim( Hero h ){
		if (claimedBy(h)) return;
		int[] n = new int[claimed.length + 1];
		System.arraycopy(claimed, 0, n, 0, claimed.length);
		n[claimed.length] = ChampionHooks.indexOf(h);
		claimed = n;
	}

	//asked by the region boss: anything to pay for? (spoiler: usually yes)
	public boolean punish(){
		return given && !slain;
	}

	public void store( Bundle bundle ){
		Bundle b = new Bundle();
		b.put("depth", depth);
		b.put("given", given);
		b.put("slain", slain);
		b.put("done", done);
		b.put("failed", failed);
		b.put("bonus", bonus);
		b.put("claimed", claimed);
		storeMore(b);
		bundle.put(key, b);
	}

	public void restore( Bundle bundle ){
		reset();
		if (!bundle.contains(key)) return;
		Bundle b = bundle.getBundle(key);
		depth = b.getInt("depth");
		given = b.getBoolean("given");
		slain = b.getBoolean("slain");
		done = b.getBoolean("done");
		failed = b.getBoolean("failed");
		bonus = b.getBoolean("bonus");
		claimed = b.getIntArray("claimed");
		if (claimed == null) claimed = new int[0];
		restoreMore(b);
	}

	protected void storeMore( Bundle b ){
	}

	protected void restoreMore( Bundle b ){
	}
}
