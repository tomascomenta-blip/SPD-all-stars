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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;

//same deal as the subclass window. in co-op the other players keep a detached copy and replay the
//clicks, so the buttons can't depend on who's looking (only info buttons may)
public class WndChooseSpecialization extends Window {

	private static final int WIDTH = 130;
	private static final float GAP = 2;

	public WndChooseSpecialization( final ThalHeart heart, final Hero hero ){
		super();

		IconTitle titlebar = new IconTitle();
		titlebar.icon(new ItemSprite(heart.image(), null));
		titlebar.label(heart.name());
		titlebar.setRect(0, 0, WIDTH, 0);
		add(titlebar);

		RenderedTextBlock message = PixelScene.renderTextBlock(6);
		message.text(Messages.get(this, "message"), WIDTH);
		message.setPos(titlebar.left(), titlebar.bottom() + GAP);
		add(message);

		float pos = message.bottom() + 3 * GAP;

		for (final Specialization spec : Specialization.values()){
			RedButton btn = new RedButton(spec.shortDesc(), 6){
				@Override
				protected void onClick() {
					GameScene.show(new WndOptions(new HeroIcon(spec.icon()),
							Messages.titleCase(spec.title()),
							Messages.get(WndChooseSpecialization.class, "are_you_sure"),
							Messages.get(WndChooseSpecialization.class, "yes"),
							Messages.get(WndChooseSpecialization.class, "no")){
						@Override
						protected void onSelect( int index ) {
							hide();
							//isOpen, not parent: the copy kept for another player's hero has no parent
							if (index == 0 && WndChooseSpecialization.this.isOpen()){
								WndChooseSpecialization.this.hide();
								heart.choose(spec, hero);
							}
						}
					});
				}
			};
			btn.leftJustify = true;
			btn.multiline = true;
			btn.setSize(WIDTH - 20, btn.reqHeight() + 2);
			btn.setRect(0, pos, WIDTH - 20, btn.reqHeight() + 2);
			add(btn);

			IconButton info = new IconButton(Icons.get(Icons.INFO)){
				@Override
				protected void onClick() {
					GameScene.show(new WndInfoSpecialization(spec));
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

	//the power of a path and its row of talents
	public static class WndInfoSpecialization extends WndTitledMessage {

		public WndInfoSpecialization( Specialization spec ){
			super(new HeroIcon(spec.icon()), Messages.titleCase(spec.title()), spec.desc());

			ArrayList<LinkedHashMap<Talent, Integer>> talents = new ArrayList<>();
			Talent.initSpecTalents(spec, talents);

			TalentsPane.TalentTierPane pane = new TalentsPane.TalentTierPane(talents.get(4), 5, TalentButton.Mode.INFO);
			pane.title.text(Messages.titleCase(Messages.get(WndChooseSpecialization.class, "talents")));
			pane.setRect(0, height + 5, width, pane.height());
			add(pane);
			resize(width, (int) pane.bottom());
		}

		@Override
		protected float targetHeight() {
			return super.targetHeight() - 40;
		}
	}
}
