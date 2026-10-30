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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.DarkTaint;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//corrupt food boiled until the dark comes out of it. what's left is a warm paste: on a wound it
//closes it over a few turns, and pulls a little of the corruption out
public class Poultice extends Item {

	public static final String AC_APPLY = "APPLY";

	public static final float HEAL = 0.3f;
	public static final int CLEANSE = 6;

	{
		image = ItemSpriteSheet.POULTICE;
		stackable = true;
		defaultAction = AC_APPLY;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_APPLY);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_APPLY)) return;

		detach(hero.belongings.backpack);
		Buff.affect(hero, Healing.class).setHeal(Math.round(hero.HT * HEAL), 0.25f, 0);
		DarkTaint t = hero.buff(DarkTaint.class);
		if (t != null) t.cleanse(CLEANSE);
		CoopLog.line("survival: hero " + Party.indexOf(hero) + " used a poultice");
		GLog.p(Messages.get(this, "applied"));
		Sample.INSTANCE.play(Assets.Sounds.DRINK, 0.7f, 0.8f);
		hero.spend(1f);
		hero.busy();
		hero.sprite.operate(hero.pos);
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
	public int value() {
		return 15 * quantity;
	}
}
