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
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;

//coliseum for the herald's tourney (kingdom, floor 8). sand ring, one gate, royal box on the wall facing the gate.
//TourneyVisuals draws the crowd and the dark knight watching from the box
public class ColiseumRoom extends SpecialRoom {

	//gate wall: 0 top 1 right 2 bottom 3 left
	public int gateSide = 2;

	@Override
	public int minWidth() { return 13; }
	public int maxWidth() { return 13; }
	public int minHeight() { return 11; }
	public int maxHeight() { return 11; }

	@Override
	public void paint( Level level ) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY_SP);
		//darker trodden sand along the walls
		Painter.fill(level, this, 1, Terrain.EMPTY);
		Painter.fill(level, this, 2, Terrain.EMPTY_SP);

		Door gate = entrance();
		gate.set(Door.Type.REGULAR);
		if (gate.y == top) gateSide = 0;
		else if (gate.x == right) gateSide = 1;
		else if (gate.y == bottom) gateSide = 2;
		else gateSide = 3;

		//4 pillars, corners of the ring. pure decoration
		Painter.set(level, left + 3, top + 3, Terrain.STATUE_SP);
		Painter.set(level, right - 3, top + 3, Terrain.STATUE_SP);
		Painter.set(level, left + 3, bottom - 3, Terrain.STATUE_SP);
		Painter.set(level, right - 3, bottom - 3, Terrain.STATUE_SP);
	}

	public int gateCell( Level level ){
		Door d = entrance();
		return d == null ? -1 : level.pointToCell(d);
	}

	//herald stands inside, next to the gate
	public int heraldCell( Level level ){
		Point in = inward(1);
		Door d = entrance();
		int c = level.pointToCell(new Point(d.x + in.x, d.y + in.y));
		//one step aside so the gate stays free
		Point side = new Point(in.y, in.x);
		int s = level.pointToCell(new Point(d.x + in.x + side.x, d.y + in.y + side.y));
		return level.passable[s] ? s : c;
	}

	//champions come out at the far end
	public int foeCell( Level level ){
		Point mid = center();
		Point in = inward(1);
		return level.pointToCell(new Point(mid.x + in.x * 3, mid.y + in.y * 3));
	}

	//1 step in from the gate
	private Point inward( int n ){
		switch (gateSide){
			case 0: return new Point(0, n);
			case 1: return new Point(-n, 0);
			case 3: return new Point(n, 0);
			default: return new Point(0, -n);
		}
	}

	//walls not included
	public boolean inRing( Level level, int cell ){
		Point p = level.cellToPoint(cell);
		return p.x > left && p.x < right && p.y > top && p.y < bottom;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("gate_side", gateSide);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		gateSide = bundle.getInt("gate_side");
	}
}
