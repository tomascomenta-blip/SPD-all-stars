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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;

import java.util.ArrayList;

//one of three pieces out of the clockmaker's chest. shared like the subclass window, so the list
//is built from the hero who opened it
public class WndChooseGear extends Window {

	private static final int WIDTH = 130;
	private static final float GAP = 2;

	public WndChooseGear( final ClockmakerChest chest, final Hero hero ){
		super();

		IconTitle titlebar = new IconTitle();
		titlebar.icon(new ItemSprite(chest.image(), null));
		titlebar.label(chest.name());
		titlebar.setRect(0, 0, WIDTH, 0);
		add(titlebar);

		RenderedTextBlock message = PixelScene.renderTextBlock(6);
		message.text(Messages.get(this, "message", ClockmakerChest.MIN_LEVEL), WIDTH);
		message.setPos(titlebar.left(), titlebar.bottom() + GAP);
		add(message);

		float pos = message.bottom() + 3 * GAP;

		ArrayList<Item> options = ClockmakerChest.options(hero);
		for (int i = 0; i < options.size(); i++){
			final int index = i;
			final Item item = options.get(i);
			RedButton btn = new RedButton(Messages.titleCase(item.name()) + "\n" + summary(item), 6){
				@Override
				protected void onClick() {
					GameScene.show(new WndOptions(new ItemSprite(item),
							Messages.titleCase(item.name()),
							Messages.get(WndChooseGear.class, "are_you_sure"),
							Messages.get(WndChooseGear.class, "yes"),
							Messages.get(WndChooseGear.class, "no")){
						@Override
						protected void onSelect( int choice ) {
							hide();
							//isOpen, not parent: the copy kept for another player's hero has no parent
							if (choice == 0 && WndChooseGear.this.isOpen()){
								WndChooseGear.this.hide();
								chest.choose(index, hero);
							}
						}
					});
				}
			};
			btn.icon(new ItemSprite(item));
			btn.leftJustify = true;
			btn.multiline = true;
			btn.setSize(WIDTH - 20, btn.reqHeight() + 2);
			btn.setRect(0, pos, WIDTH - 20, Math.max(20, btn.reqHeight() + 2));
			add(btn);

			IconButton info = new IconButton(Icons.get(Icons.INFO)){
				@Override
				protected void onClick() {
					GameScene.show(new WndInfoItem(item));
				}
			};
			info.setRect(WIDTH - 20, btn.top() + (btn.height() - 20) / 2, 20, 20);
			add(info);

			pos = btn.bottom() + GAP;
		}

		RedButton cancel = new RedButton(Messages.get(this, "cancel")){
			@Override
			protected void onClick() {
				hide();
			}
		};
		cancel.setRect(0, pos, WIDTH, 18);
		add(cancel);

		resize(WIDTH, (int) cancel.bottom());
	}

	//"+5, 11-70 damage" or "+5, 5-42 armor"
	private static String summary( Item item ){
		if (item instanceof MeleeWeapon){
			MeleeWeapon w = (MeleeWeapon) item;
			return Messages.get(WndChooseGear.class, "weapon_line", w.level(), w.min(), w.max(), w.STRReq());
		} else if (item instanceof Armor){
			Armor a = (Armor) item;
			return Messages.get(WndChooseGear.class, "armor_line", a.level(), a.DRMin(), a.DRMax(), a.STRReq());
		}
		return "";
	}
}
