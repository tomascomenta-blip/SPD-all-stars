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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.noosa.Image;

import java.util.ArrayList;

//sitting by a fire: cook, feed it, make things. built for the hero who sat down (in co-op the
//others keep a detached copy and replay the clicks), so the buttons are always the same ones in
//the same order, whoever looks and whatever's in the bag
public class WndCampfire extends Window {

	private static final int WIDTH_P = 130, WIDTH_L = 160;
	private static final int BTN_H = 18;
	private static final float GAP = 2;

	public static final int COOK = 0, FEED = 1, RECIPES = 2;

	private final ArrayList<RedButton> buttons = new ArrayList<>();

	public WndCampfire( final Campfire fire, final Hero hero ){
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		Image icon = new Image(Survival.CAMPFIRE);
		icon.frame(0, 4, 16, 20);
		IconTitle title = new IconTitle(icon, Messages.titleCase(fire.name()));
		title.setRect(0, 0, width, 0);
		add(title);

		RenderedTextBlock text = PixelScene.renderTextBlock(6);
		text.text(fire.lit() ? Messages.get(this, "burning", fire.fuel) : Messages.get(this, "embers"), width);
		text.setPos(0, title.bottom() + GAP * 2);
		add(text);

		float pos = text.bottom() + GAP * 3;

		int cookable = FireRecipes.cookable(hero);
		RedButton cook = new RedButton(Messages.get(this, "cook", cookable), 7){
			@Override
			protected void onClick() {
				hide();
				FireRecipes.cook(fire, hero);
			}
		};
		cook.enable(fire.lit() && cookable > 0);
		pos = place(cook, pos, width);

		RedButton feed = new RedButton(Messages.get(this, "feed", Survival.count(hero, Torch.class)), 7){
			@Override
			protected void onClick() {
				hide();
				FireRecipes.feed(fire, hero);
			}
		};
		feed.enable(Survival.count(hero, Torch.class) > 0);
		pos = place(feed, pos, width);

		RenderedTextBlock make = PixelScene.renderTextBlock(Messages.get(this, "make"), 7);
		make.hardlight(TITLE_COLOR);
		make.setPos(0, pos + GAP);
		add(make);
		pos = make.bottom() + GAP * 2;

		for (final FireRecipes.Recipe r : FireRecipes.Recipe.values()){
			RedButton b = new RedButton(FireRecipes.label(r), 6){
				@Override
				protected void onClick() {
					hide();
					FireRecipes.make(fire, hero, r);
				}
			};
			b.leftJustify = true;
			b.multiline = true;
			b.enable(fire.lit() && FireRecipes.canMake(hero, r));
			pos = place(b, pos, width);
		}

		RedButton close = new RedButton(Messages.get(this, "close")){
			@Override
			protected void onClick() {
				hide();
			}
		};
		pos = place(close, pos + GAP, width);

		resize(width, (int) pos - (int) GAP);
	}

	private float place( RedButton b, float pos, int width ){
		b.setRect(0, pos, width, Math.max(BTN_H, b.reqHeight() + 2));
		add(b);
		buttons.add(b);
		return b.bottom() + GAP;
	}

	//COOK, FEED, then RECIPES + the recipe's ordinal (the test bot presses them like a player)
	public RedButton button( int index ){
		return index >= 0 && index < buttons.size() ? buttons.get(index) : null;
	}
}
