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

package com.shatteredpixel.shatteredpixeldungeon.champions.scout;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

//E is a passive. every hit poisons. yes an E that's passive, deal with it
public class ToxicShot extends ChampionAbility {

	public static final ToxicShot INSTANCE = new ToxicShot();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.LOL_TOXIC_SHOT; }
	@Override public int color() { return 0x55BB33; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return null; }

	@Override
	public float cooldown( Hero hero ) {
		return 0;
	}

	@Override
	public boolean ready( Hero hero ) {
		return false;
	}

	@Override
	public String notReadyText( Hero hero ) {
		return Messages.get(ChampionAbility.class, "passive");
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		//nothing
	}

	public static float poisonTurns( Hero hero ){
		return 2 + hero.lvl/4f + hero.pointsInTalent(Talent.SCO_POTENT_VENOM);
	}

	public static void proc( Hero hero, Char enemy ){
		if (enemy.isAlive() && CoopRules.hits(hero, enemy)){
			Buff.affect(enemy, Poison.class).set(poisonTurns(hero));
		}
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", Math.round(poisonTurns(hero)));
	}
}
