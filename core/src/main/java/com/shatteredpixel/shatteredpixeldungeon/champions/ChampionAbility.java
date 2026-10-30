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

package com.shatteredpixel.shatteredpixeldungeon.champions;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist.Souls;

//one ability of a kit (Q/W/E/R)
//NO STATE in here. anything to remember goes in a buff on the hero, so it saves and both coop devices see the same
public abstract class ChampionAbility {

	public abstract String slot();

	public abstract int unlockLevel();

	public abstract int icon();

	public int color(){
		return 0xFFFFFF;
	}

	//null = no cooldown (charges etc)
	public abstract Class<? extends AbilityCooldown> cooldownClass();

	public abstract float cooldown( Hero hero );

	//asks for a cell first
	public boolean targeted(){
		return false;
	}

	public String prompt(){
		return Messages.get(this, "prompt");
	}

	//extra checks on top of level+cd (charges, ally alive, etc etc)
	public boolean ready( Hero hero ){
		return true;
	}

	public String notReadyText( Hero hero ){
		return Messages.get(ChampionAbility.class, "not_ready");
	}

	public boolean unlocked( Hero hero ){
		return hero.lvl >= unlockLevel();
	}

	public boolean onCooldown( Hero hero ){
		return cooldownClass() != null && hero.buff(cooldownClass()) != null;
	}

	public float cooldownLeft( Hero hero ){
		if (cooldownClass() == null) return 0;
		AbilityCooldown cd = hero.buff(cooldownClass());
		return cd == null ? 0 : cd.cooldown();
	}

	public boolean canUse( Hero hero ){
		return unlocked(hero) && !onCooldown(hero) && ready(hero);
	}

	//do the thing. MUST spend hero time (or busy() and finish later), and startCooldown() only when
	//it actually fires. forgot that once and you could spam R. was hilarious, had to fix it
	public abstract void activate( Hero hero, ChampionKit kit, Integer target );

	public void startCooldown( Hero hero ){
		//spiritist souls halve it
		startCooldown(hero, Souls.cooldown(hero, this, cooldown(hero)));
	}

	public void startCooldown( Hero hero, float turns ){
		if (cooldownClass() == null || turns <= 0) return;
		AbilityCooldown cd = Buff.affect(hero, cooldownClass(), turns);
		cd.max = turns;
	}

	public void resetCooldown( Hero hero ){
		if (cooldownClass() != null) Buff.detach(hero, cooldownClass());
	}

	public String name(){
		return Messages.get(this, "name");
	}

	//desc w/ this hero's numbers
	public String desc( Hero hero ){
		return Messages.get(this, "desc");
	}

	public String fullDesc( Hero hero ){
		String result = desc(hero);
		if (cooldownClass() != null){
			result += "\n\n" + Messages.get(ChampionAbility.class, "cooldown", Math.round(cooldown(hero)));
		}
		if (!unlocked(hero)){
			result += "\n\n" + Messages.get(ChampionAbility.class, "unlock", unlockLevel());
		}
		return result;
	}
}
