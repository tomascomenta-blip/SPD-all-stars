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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

//while the local hero is up on a big one: how much grip is left, in the middle of the screen a bit
//below the hero. red and a sweating hand when it's nearly gone
public class GripBar extends Group {

	private static final int FILL_X = 4, FILL_Y = 3, FILL_W = 58, FILL_H = 6;

	private final Image hand, frame, fill;
	private float t;

	public GripBar(){
		hand = new Image(Hunt.GRIP_HAND);
		hand.frame(0, 0, 16, 16);
		add(hand);
		fill = new Image(Hunt.GRIP_FILL);
		add(fill);
		frame = new Image(Hunt.GRIP_FRAME);
		add(frame);
		visible = false;
	}

	@Override
	public void update() {
		super.update();
		Hero me = Dungeon.local();
		Climbing b = me == null ? null : me.buff(Climbing.class);
		visible = b != null;
		if (b == null) return;
		t += Game.elapsed;
		float k = Math.max(0, Math.min(1, b.grip / Climbing.MAX));
		boolean low = k < 0.3f;
		float w = PixelScene.uiCamera.width, h = PixelScene.uiCamera.height;
		float x0 = (w - 66 - 18) / 2f, y0 = h * 0.62f;
		hand.x = x0;
		hand.y = y0 - 2 + (low ? (float) Math.sin(t * 40) : 0);
		hand.frame(low ? 16 : 0, 0, 16, 16);
		frame.x = x0 + 18;
		frame.y = y0;
		fill.x = frame.x + FILL_X;
		int fw = Math.max(0, Math.round(FILL_W * k));
		fill.frame(0, low ? FILL_H : 0, Math.max(1, fw), FILL_H);
		fill.y = frame.y + FILL_Y;
		fill.visible = fw > 0;
	}
}
