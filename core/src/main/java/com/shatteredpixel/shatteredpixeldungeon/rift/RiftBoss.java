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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

//rift boss base: joins the normal boss in co-op. real boss flags (no cheesing w/ cc) and one
//reward per player on death (riftLoot)
public abstract class RiftBoss extends Mob {

	{
		properties.add(Property.BOSS);
		immunities.add(Paralysis.class);
		immunities.add(Amok.class);
		immunities.add(Charm.class);
		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
		immunities.add(Terror.class);
		immunities.add(Dread.class);
		immunities.add(Frost.class);
		state = HUNTING;
	}

	//the reward for one player, null if this boss gives none as an item
	protected Item riftLoot(){
		return null;
	}

	//rewards that are not items (e.g. a buff for every hero), called once when it dies
	protected void rewardParty(){
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
		//one reward for the whole party, whatever its size: the players decide who keeps it
		Item loot = riftLoot();
		if (loot != null){
			Heap h = Dungeon.level.drop(loot, pos);
			if (h.sprite != null) h.sprite.drop(pos);
		}
		rewardParty();
		//the regular boss of the floor already fell: the floor can open now
		if (Dungeon.level.locked && RiftBosses.mainBoss(Dungeon.level) == null){
			Dungeon.level.unseal();
		}
	}

}
