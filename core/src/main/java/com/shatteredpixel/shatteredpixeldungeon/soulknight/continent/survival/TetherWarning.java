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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

//on screen while the local hero is alone in the dark: a frame that throbs and one line of text.
//only reads Tether, never touches the game
public class TetherWarning extends Group {

	private static final int EDGE = 4;
	private static final int COLOR = 0xFF8A3AC8;

	private final ColorBlock top, bottom, left, right;
	private final RenderedTextBlock text;
	private float t = 0;

	public TetherWarning(){
		float w = PixelScene.uiCamera.width, h = PixelScene.uiCamera.height;

		top = bar(0, 0, w, EDGE);
		bottom = bar(0, h - EDGE, w, EDGE);
		left = bar(0, 0, EDGE, h);
		right = bar(w - EDGE, 0, EDGE, h);

		text = PixelScene.renderTextBlock(Messages.get(Tether.class, "banner"), 8);
		text.hardlight(0xD8A8FF);
		text.setPos((w - text.width()) / 2f, h * 0.24f);
		PixelScene.align(text);
		add(text);

		visible = false;
	}

	private ColorBlock bar( float x, float y, float w, float h ){
		ColorBlock b = new ColorBlock(w, h, COLOR);
		b.x = x;
		b.y = y;
		add(b);
		return b;
	}

	@Override
	public void update() {
		super.update();
		boolean on = Tether.lost(Dungeon.local());
		if (!on){
			visible = false;
			t = 0;
			return;
		}
		visible = true;
		t += Game.elapsed;
		float a = 0.45f + 0.35f * (float) Math.sin(t * 4);
		top.alpha(a);
		bottom.alpha(a);
		left.alpha(a);
		right.alpha(a);
		text.alpha(0.75f + 0.25f * (float) Math.sin(t * 4));
	}
}
