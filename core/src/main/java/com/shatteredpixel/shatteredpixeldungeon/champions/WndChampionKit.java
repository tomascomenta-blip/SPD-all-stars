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

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;

//kit window. plain buttons on purpose: coop sends the click over and replays it, so button order must match
public class WndChampionKit extends Window {

	private static final int WIDTH = 130;
	private static final int ROW = 20;

	public WndChampionKit( final ChampionKit kit, final Hero hero ){
		super();

		IconTitle title = new IconTitle(new ItemSprite(kit), Messages.titleCase(kit.name()));
		title.setRect(0, 0, WIDTH, 0);
		add(title);

		float top = title.bottom() + 3;

		for (final ChampionAbility a : kit.abilities()){

			HeroIcon icon = new HeroIcon(a.icon());
			icon.x = 0;
			icon.y = top + (ROW - icon.height()) / 2f;
			add(icon);

			String state;
			if (!a.unlocked(hero)){
				state = Messages.get(WndChampionKit.class, "locked", a.unlockLevel());
			} else if (a.onCooldown(hero)){
				state = Messages.get(WndChampionKit.class, "turns", Math.round(Math.max(1, a.cooldownLeft(hero))));
			} else if (!a.ready(hero)){
				state = a.notReadyText(hero);
			} else {
				state = Messages.get(WndChampionKit.class, "ready");
			}

			RedButton btn = new RedButton(a.slot() + "  " + Messages.titleCase(a.name()) + "  (" + state + ")", 7){
				@Override
				protected void onClick() {
					hide();
					kit.cast(hero, a);
				}
			};
			btn.enable(a.canUse(hero));
			if (!a.canUse(hero)) icon.alpha(0.4f);
			btn.setRect(18, top, WIDTH - 18 - 18, ROW);
			add(btn);

			IconButton info = new IconButton(Icons.INFO.get()){
				@Override
				protected void onClick() {
					GameScene.show(new WndTitledMessage(new HeroIcon(a.icon()),
							Messages.titleCase(a.name()), a.fullDesc(hero)));
				}
			};
			info.setRect(WIDTH - 16, top + (ROW - 16) / 2f, 16, 16);
			add(info);

			top += ROW + 2;
		}

		//forger's forge button
		final String extraLabel = kit.extraLabel(hero);
		if (extraLabel != null){
			HeroIcon icon = new HeroIcon(kit.extraIcon());
			icon.x = 0;
			icon.y = top + (ROW - icon.height()) / 2f;
			add(icon);

			RedButton btn = new RedButton(extraLabel, 7){
				@Override
				protected void onClick() {
					hide();
					kit.extraAction(hero);
				}
			};
			btn.enable(kit.extraEnabled(hero));
			if (!kit.extraEnabled(hero)) icon.alpha(0.4f);
			btn.setRect(18, top, WIDTH - 18 - 18, ROW);
			add(btn);

			IconButton info = new IconButton(Icons.INFO.get()){
				@Override
				protected void onClick() {
					GameScene.show(new WndTitledMessage(new HeroIcon(kit.extraIcon()), extraLabel, kit.extraInfo(hero)));
				}
			};
			info.setRect(WIDTH - 16, top + (ROW - 16) / 2f, 16, 16);
			add(info);

			top += ROW + 2;
		}

		RenderedTextBlock hint = PixelScene.renderTextBlock(Messages.get(WndChampionKit.class, "hint"), 6);
		hint.maxWidth(WIDTH);
		hint.setPos(0, top + 1);
		add(hint);
		top = hint.bottom();

		resize(WIDTH, (int) top);

		//phones: a bit lower, near the toolbar. thumbs
		if (SPDSettings.interfaceSize() != 2){
			offset(0, (int) (GameScene.uiCamera.height / 2 - 30 - height / 2));
		}
	}
}
