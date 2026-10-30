/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Game;

public enum HeroSubClass {

	NONE(HeroIcon.NONE),

	BERSERKER(HeroIcon.BERSERKER),
	GLADIATOR(HeroIcon.GLADIATOR),

	BATTLEMAGE(HeroIcon.BATTLEMAGE),
	WARLOCK(HeroIcon.WARLOCK),
	
	ASSASSIN(HeroIcon.ASSASSIN),
	FREERUNNER(HeroIcon.FREERUNNER),
	
	SNIPER(HeroIcon.SNIPER),
	WARDEN(HeroIcon.WARDEN),

	CHAMPION(HeroIcon.CHAMPION),
	MONK(HeroIcon.MONK),

	PRIEST(HeroIcon.PRIEST),
	PALADIN(HeroIcon.PALADIN),

	//the champion classes (ours)
	CONQUEROR(HeroIcon.CONQUEROR),
	HEADSMAN(HeroIcon.HEADSMAN),

	TRAPPER(HeroIcon.TRAPPER),
	SKIRMISHER(HeroIcon.SKIRMISHER),

	TAMER(HeroIcon.TAMER),
	INFERNALIST(HeroIcon.INFERNALIST),

	SOULREAPER(HeroIcon.SOULREAPER),
	LANTERNKEEPER(HeroIcon.LANTERNKEEPER),

	PITBOSS(HeroIcon.PITBOSS),
	SHOWMAN(HeroIcon.SHOWMAN),

	MISTLORD(HeroIcon.MISTLORD),
	EXHUMER(HeroIcon.EXHUMER),

	CURATOR(HeroIcon.CURATOR),
	COLOSSUS(HeroIcon.COLOSSUS),

	TROUBADOUR(HeroIcon.TROUBADOUR),
	MINSTREL(HeroIcon.MINSTREL),

	//botanico
	WARGARDENER(HeroIcon.WARGARDENER),
	DRUID(HeroIcon.DRUID),
	//ornn
	MASTERSMITH(HeroIcon.MASTERSMITH),
	LIVINGMOUNTAIN(HeroIcon.LIVINGMOUNTAIN),
	//mordekaiser
	TYRANT(HeroIcon.TYRANT),
	REVENANT(HeroIcon.REVENANT),
	//alt worlds: Automaton and Beastmaster
	SWARM_ENGINEER(HeroIcon.SWARM_ENGINEER),
	TESLA_COIL(HeroIcon.TESLA_COIL),
	PACK_ALPHA(HeroIcon.PACK_ALPHA),
	FOREST_WARDEN(HeroIcon.FOREST_WARDEN),
	//alt worlds: Spiritist and Gunslinger
	SPECTRE_LORD(HeroIcon.SPECTRE_LORD),
	PLAGUE_HERALD(HeroIcon.PLAGUE_HERALD),
	QUICKDRAW(HeroIcon.QUICKDRAW),
	BOUNTY_HUNTER(HeroIcon.BOUNTY_HUNTER);

	int icon;

	HeroSubClass(int icon){
		this.icon = icon;
	}
	
	public String title() {
		return Messages.get(this, name());
	}

	public String shortDesc() {
		return Messages.get(this, name()+"_short_desc");
	}

	public String desc() {
		//Include the staff effect description in the battlemage's desc if possible
		if (this == BATTLEMAGE){
			String desc = Messages.get(this, name() + "_desc");
			if (Game.scene() instanceof GameScene){
				MagesStaff staff = Dungeon.hero.belongings.getItem(MagesStaff.class);
				if (staff != null && staff.wandClass() != null){
					desc += "\n\n" + Messages.get(staff.wandClass(), "bmage_desc");
					desc = desc.replaceAll("_", "");
				}
			}
			return desc;
		} else {
			return Messages.get(this, name() + "_desc");
		}
	}

	public int icon(){
		return icon;
	}

}
