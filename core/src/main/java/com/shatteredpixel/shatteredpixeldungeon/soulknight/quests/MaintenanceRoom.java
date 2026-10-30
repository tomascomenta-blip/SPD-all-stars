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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.quests;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.watabou.utils.Point;

//maintenance bay, M-4 sleeps on its dock here. one door, dock on the wall across from it,
//core in the middle, ship terminal by the door
public class MaintenanceRoom extends SpecialRoom {

	@Override
	public int minWidth() { return 11; }
	public int maxWidth() { return 11; }
	public int minHeight() { return 9; }
	public int maxHeight() { return 9; }

	@Override
	public void paint( Level level ) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY_SP);
		Painter.fill(level, this, 2, Terrain.EMPTY);
		Door d = entrance();
		d.set(Door.Type.REGULAR);
	}

	private Point inward(){
		Door d = entrance();
		if (d.y == top) return new Point(0, 1);
		if (d.x == right) return new Point(-1, 0);
		if (d.y == bottom) return new Point(0, -1);
		return new Point(1, 0);
	}

	//middle of the far side
	public int dock( Level level ){
		Point in = inward();
		Point c = center();
		int reach = in.x != 0 ? (width() / 2 - 2) : (height() / 2 - 2);
		return level.pointToCell(new Point(c.x + in.x * reach, c.y + in.y * reach));
	}

	public int terminalCell( Level level ){
		Door d = entrance();
		Point in = inward();
		Point side = new Point(in.y, in.x);
		int c = level.pointToCell(new Point(d.x + in.x + side.x, d.y + in.y + side.y));
		if (!level.passable[c]) c = level.pointToCell(new Point(d.x + in.x - side.x, d.y + in.y - side.y));
		return c;
	}
}
