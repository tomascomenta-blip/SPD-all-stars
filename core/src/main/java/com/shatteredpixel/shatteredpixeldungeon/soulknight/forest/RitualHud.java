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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forest;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

//how many forest ritual totems are broken. top of the screen on floors 1-5, until the priest wakes up
public class RitualHud extends Group {

	private final Image[] icons = new Image[RitualTotems.FLOORS];
	private final TextureFilm film;
	private final RenderedTextBlock text;
	private final float top;
	private int shownBroken = -1, shownPlaced = -1;

	public static boolean wanted(){
		return Dungeon.level instanceof ForestLevel && Dungeon.depth >= 1 && Dungeon.depth <= RitualTotems.FLOORS + 1
				&& RitualTotems.placed > 0;
	}

	public RitualHud( float top ){
		this.top = top;
		Image probe = new Image(PriestAssets.ICONS);
		film = new TextureFilm(probe.texture, 12, 16);
		for (int i = 0; i < icons.length; i++){
			icons[i] = new Image(PriestAssets.ICONS);
			icons[i].frame(film.get(PriestAssets.ICON_FLOOR));
			add(icons[i]);
		}
		text = PixelScene.renderTextBlock(6);
		add(text);
	}

	@Override
	public void update() {
		super.update();
		GoblinPriest p = Dungeon.level instanceof ForestBossLevel ? ForestBossLevel.priest(Dungeon.level) : null;
		boolean show = wanted() && (p == null || p.intro == 0) && !(Dungeon.level instanceof ForestBossLevel && p == null);
		visible = show;
		if (!show) return;
		if (RitualTotems.broken == shownBroken && RitualTotems.placed == shownPlaced) return;
		shownBroken = RitualTotems.broken;
		shownPlaced = RitualTotems.placed;

		for (int i = 0; i < icons.length; i++){
			icons[i].frame(film.get(i < RitualTotems.broken ? PriestAssets.ICON_FLOOR_BROKEN : PriestAssets.ICON_FLOOR));
		}
		String t;
		if (Dungeon.depth > RitualTotems.FLOORS){
			int left = RitualTotems.standing();
			t = left > 0 ? Messages.get(this, "left", left) : Messages.get(this, "none_left");
		} else {
			t = Messages.get(this, "count", RitualTotems.broken, RitualTotems.FLOORS);
		}
		text.text(t);
		float uiw = PixelScene.uiCamera.width;
		float w = icons.length * 13 + 4 + text.width();
		float x = (uiw - w) / 2f;
		for (int i = 0; i < icons.length; i++){
			icons[i].x = x + i * 13;
			icons[i].y = top;
		}
		text.setPos(x + icons.length * 13 + 4, top + (16 - text.height()) / 2f);
		PixelScene.align(text);
	}
}
