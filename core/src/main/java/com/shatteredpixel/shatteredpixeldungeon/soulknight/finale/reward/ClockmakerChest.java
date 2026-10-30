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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//a little chest with a clock face for a lock. inside, three pieces forged from the colossus;
//you get to keep one. the piece takes the upgrades of what you're already using in that slot
//(at least +3), so trading up to tier 6 never feels like a step back
public class ClockmakerChest extends Item {

	public static final String AC_OPEN = "OPEN";

	public static final int MIN_LEVEL = 3;

	{
		stackable = false;
		image = ItemSpriteSheet.CLOCKMAKER_CHEST;
		defaultAction = AC_OPEN;
		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_OPEN);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ) {
		super.execute(hero, action);

		if (action.equals(AC_OPEN)){
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndChooseGear(ClockmakerChest.this, hero));
				}
			});
		}
	}

	//the three choices, already at the level they'd have for this hero
	public static ArrayList<Item> options( Hero hero ){
		ArrayList<Item> list = new ArrayList<>();
		list.add(make(0, hero));
		list.add(make(1, hero));
		list.add(make(2, hero));
		return list;
	}

	public static Item make( int index, Hero hero ){
		Item item;
		int lvl;
		switch (index){
			case 0: default:
				item = new MinuteHand();
				lvl = weaponLevel(hero);
				break;
			case 1:
				item = new Pendulum();
				lvl = weaponLevel(hero);
				break;
			case 2:
				item = new GearPlate();
				lvl = armorLevel(hero);
				break;
		}
		lvl = Math.max(MIN_LEVEL, lvl);
		item.upgrade(lvl);
		item.cursed = false;
		item.identify();
		return item;
	}

	private static int weaponLevel( Hero hero ){
		int lvl = 0;
		if (hero.belongings.weapon instanceof MeleeWeapon){
			lvl = Math.max(lvl, hero.belongings.weapon.trueLevel());
		}
		if (hero.belongings.secondWep instanceof MeleeWeapon){
			lvl = Math.max(lvl, hero.belongings.secondWep.trueLevel());
		}
		return lvl;
	}

	private static int armorLevel( Hero hero ){
		Armor a = hero.belongings.armor;
		return a == null ? 0 : a.trueLevel();
	}

	public void choose( int index, Hero hero ){
		if (!hero.belongings.backpack.contains(this)) return;
		detach(hero.belongings.backpack);

		Item item = make(index, hero);
		if (!item.collect(hero.belongings.backpack)){
			Dungeon.level.drop(item, hero.pos).sprite.drop();
		}
		CoopLog.line("thal gear " + item.getClass().getSimpleName() + " +" + item.level() + " for " + hero.heroClass);

		hero.spend(Actor.TICK);
		hero.busy();
		if (hero.sprite != null){
			hero.sprite.operate(hero.pos);
		} else {
			hero.next();
		}
		Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
		GLog.p(Messages.get(this, "got", item.name()));
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}
}
