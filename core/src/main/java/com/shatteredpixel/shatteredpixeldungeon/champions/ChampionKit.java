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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

//the champion's thingy. opens the ability window. sits in the backpack, not equipped
public abstract class ChampionKit extends Item {

	public static final String AC_ABILITIES = "ABILITIES";

	{
		defaultAction = AC_ABILITIES;
		unique = true;
		bones = false;
	}

	public abstract ChampionAbility[] abilities();

	//extra button in the window (only the forger uses it, for the forge). null = no button
	public String extraLabel( Hero hero ){
		return null;
	}

	public String extraInfo( Hero hero ){
		return "";
	}

	public int extraIcon(){
		return HeroIcon.NONE;
	}

	public boolean extraEnabled( Hero hero ){
		return false;
	}

	public void extraAction( Hero hero ){
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_ABILITIES);
		return actions;
	}

	@Override
	public String actionName( String action, Hero hero ) {
		if (action.equals(AC_ABILITIES)){
			return Messages.get(ChampionKit.class, "ac_abilities");
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (action.equals(AC_ABILITIES)){
			GameScene.show(new WndChampionKit(this, hero));
		}
	}

	public boolean canCast( Hero hero, ChampionAbility ability, boolean warn ){
		if (!hero.belongings.contains(this)){
			return false;
		}
		if (!ability.unlocked(hero)){
			if (warn) GLog.w(Messages.get(ChampionKit.class, "locked", ability.name(), ability.unlockLevel()));
			return false;
		}
		if (ability.onCooldown(hero)){
			if (warn) GLog.w(Messages.get(ChampionKit.class, "cooldown", ability.name(), Math.round(Math.max(1, ability.cooldownLeft(hero)))));
			return false;
		}
		if (!ability.ready(hero)){
			if (warn) GLog.w(ability.notReadyText(hero));
			return false;
		}
		if (hero.buff(MagicImmune.class) != null){
			if (warn) GLog.w(Messages.get(ChampionKit.class, "magic_immune"));
			return false;
		}
		return true;
	}

	//from the window, w/ that window's hero
	public void cast( final Hero hero, final ChampionAbility ability ){
		if (!canCast(hero, ability, true)) return;
		if (ability.targeted()){
			GameScene.selectCell(new CellSelector.Listener() {
				@Override
				public void onSelect( Integer cell ) {
					if (cell == null) return;
					if (!canCast(hero, ability, true)) return;
					ability.activate(hero, ChampionKit.this, cell);
				}

				@Override
				public String prompt() {
					return ability.prompt();
				}
			});
		} else {
			ability.activate(hero, this, null);
		}
	}

	public int readyCount( Hero hero ){
		int count = 0;
		for (ChampionAbility a : abilities()){
			if (a.canUse(hero)) count++;
		}
		return count;
	}

	@Override
	public String status() {
		//UI side, game thread might be touching the backpack. try/catch y a rezar
		try {
			Hero owner = ChampionHooks.ownerOf(this);
			if (owner == null) return null;
			return Integer.toString(readyCount(owner));
		} catch (Exception e){
			return null;
		}
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		Hero owner;
		try {
			owner = ChampionHooks.ownerOf(this);
		} catch (Exception e){
			owner = null;
		}
		for (ChampionAbility a : abilities()){
			desc += "\n\n_" + a.slot() + " - " + Messages.titleCase(a.name()) + ":_ ";
			if (owner != null){
				desc += a.desc(owner);
			} else {
				desc += Messages.get(a, "desc_short");
			}
		}
		return desc;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public boolean keptThroughLostInventory() {
		return true;
	}

	@Override
	public int value() {
		return 0;
	}
}
