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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.exit.ExitRoom;
import com.watabou.utils.Point;
import com.watabou.utils.Reflection;

//boss room for the new worlds. wide, some cover, boss in the middle
public class SKArena extends StandardRoom {

	public static final int HALL     = 0; //kingdom, corner blocks + banners
	public static final int OPEN     = 1; //starship, zero cover. good luck
	public static final int FLOODED  = 2; //undersea, pillars in water

	public int style = HALL;
	public Class<? extends Mob> bossClass;

	public SKArena(){
	}

	public SKArena( int style, Class<? extends Mob> bossClass ){
		this.style = style;
		this.bossClass = bossClass;
	}

	@Override
	public float[] sizeCatProbs() {
		return new float[]{0, 1, 0};
	}

	@Override
	public boolean canMerge( Level l, Room other, Point p, int mergeTerrain ) {
		return false;
	}

	@Override
	public void paint( Level level ) {
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );
		for (Room.Door door : connected.values()) {
			door.set( Room.Door.Type.REGULAR );
		}

		Point c = center();
		switch (style){
			case HALL: default:
				Painter.fill( level, left + 2, top + 2, 2, 2, Terrain.WALL );
				Painter.fill( level, right - 3, bottom - 3, 2, 2, Terrain.WALL );
				Painter.set( level, c.x - 2, c.y, Terrain.STATUE );
				Painter.set( level, c.x + 2, c.y, Terrain.STATUE );
				Painter.fill( level, left + 1, top + 1, width() - 2, 1, Terrain.EMPTY_SP );
				break;
			case OPEN:
				Painter.fill( level, this, 2, Terrain.EMPTY_SP );
				Painter.set( level, left, c.y, Terrain.WALL_DECO );
				Painter.set( level, right, c.y, Terrain.WALL_DECO );
				break;
			case FLOODED:
				Painter.fill( level, this, 1, Terrain.WATER );
				Painter.fill( level, this, 3, Terrain.EMPTY );
				Painter.set( level, left + 3, top + 3, Terrain.WALL );
				Painter.set( level, right - 3, top + 3, Terrain.WALL );
				Painter.set( level, left + 3, bottom - 3, Terrain.WALL );
				Painter.set( level, right - 3, bottom - 3, Terrain.WALL );
				break;
		}

		if (bossClass != null){
			Mob boss = Reflection.newInstance(bossClass);
			boss.pos = level.pointToCell(c);
			level.mobs.add( boss );
		}
	}

	@Override
	public boolean canPlaceWater( Point p ) {
		return false;
	}

	@Override
	public boolean canPlaceTrap( Point p ) {
		return false;
	}

	@Override
	public boolean canPlaceGrass( Point p ) {
		return false;
	}

	//locked till the key drops
	public static class LockedExit extends ExitRoom {

		@Override
		public int minWidth() {
			return Math.max(super.minWidth(), 7);
		}

		@Override
		public int minHeight() {
			return Math.max(super.minHeight(), 7);
		}

		@Override
		public void paint( Level level ) {
			Painter.fill( level, this, Terrain.WALL );
			Painter.fill( level, this, 1, Terrain.EMPTY );
			for (Room.Door door : connected.values()) {
				door.set( Room.Door.Type.REGULAR );
			}
			Point c = center();
			Painter.fill( level, c.x-1, c.y-1, 3, 3, Terrain.EMPTY_SP );
			int exitCell = level.pointToCell(c);
			Painter.set( level, exitCell, Terrain.LOCKED_EXIT );
			level.transitions.add(new LevelTransition(level, exitCell, LevelTransition.Type.REGULAR_EXIT));
		}
	}
}
