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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;

import java.util.ArrayList;

//the queen's lair: a room on the boss floor w/ its door walled up, nobody walks in.
//she ends up here after the flight and drags the heroes + her brood in for the last stand. door opens
//when she dies. single connection, so it's a dead end and walling it up never cuts a path (the whole point).
//airlock to space in the back wall (never the door wall), console by the door, handholds on the floor
public class VarkolynLair extends StandardRoom {

	public int doorCell = -1;
	public int airlockCenter = -1;   //middle one of the 3 airlock wall cells
	public int console = -1;
	public int[] handholds = new int[0];

	@Override
	public float[] sizeCatProbs() {
		return new float[]{0, 1, 0};
	}

	@Override
	public int maxConnections( int direction ) {
		if (direction == TOP) return 0;
		return 1;
	}

	@Override
	public boolean canConnect( Point p ) {
		return super.canConnect(p) && p.y != top;
	}

	@Override
	public boolean canMerge( Level l, Room other, Point p, int mergeTerrain ) {
		return false;
	}

	@Override
	public void paint( Level level ) {
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY_SP );
		for (Room.Door door : connected.values()) {
			door.set( Room.Door.Type.WALL );
			doorCell = level.pointToCell(door);
		}

		//airlock in the back wall, console, handholds
		int w = level.width();
		int cx = (left + right) / 2, cy = (top + bottom) / 2;
		if (width() >= 5){
			airlockCenter = cx + top * w;
			for (int dx = -1; dx <= 1; dx++) Painter.set(level, cx + dx, top, Terrain.WALL);
		}
		int consoleCell = cx + (bottom - 1) * w;
		if (consoleCell == doorCell + w || consoleCell == doorCell - w || consoleCell == doorCell + 1 || consoleCell == doorCell - 1){
			consoleCell = cx + 1 + (bottom - 1) * w;
		}
		console = consoleCell;
		HiveProps.AirlockConsole c = new HiveProps.AirlockConsole();
		c.pos = consoleCell;
		level.mobs.add(c);

		ArrayList<Integer> holds = new ArrayList<>();
		int[][] spots = {{left + 1, cy}, {right - 1, cy}, {cx - 2, bottom - 1}, {cx + 2, bottom - 1}};
		for (int[] p : spots){
			int cell = p[0] + p[1] * w;
			if (p[0] <= left || p[0] >= right || p[1] <= top + 1 || p[1] >= bottom) continue;
			if (cell == consoleCell || holds.contains(cell)) continue;
			holds.add(cell);
		}
		handholds = new int[holds.size()];
		for (int i = 0; i < handholds.length; i++) handholds[i] = holds.get(i);
	}

	public int airlockFaceCenter( Level level ){
		return airlockCenter + level.width();
	}

	//the 3 floor cells right in front of the airlock
	public boolean isFace( Level level, int cell ){
		if (airlockCenter == -1) return false;
		int f = airlockFaceCenter(level);
		return cell == f || cell == f - 1 || cell == f + 1;
	}

	public boolean isHandhold( Level level, int cell ){
		for (int h : handholds) if (h == cell) return true;
		return false;
	}

	@Override
	public boolean canPlaceWater( Point p ) {
		return false;
	}

	@Override
	public boolean canPlaceGrass( Point p ) {
		return false;
	}

	@Override
	public boolean canPlaceTrap( Point p ) {
		return false;
	}

	@Override
	public boolean canPlaceItem( Point p, Level l ) {
		return false;
	}

	@Override
	public boolean canPlaceCharacter( Point p, Level l ) {
		return false;
	}

	//cells inside the lair
	public ArrayList<Integer> cells( Level level ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int y = top + 1; y < bottom; y++){
			for (int x = left + 1; x < right; x++){
				cells.add(x + y*level.width());
			}
		}
		return cells;
	}

	public static VarkolynLair find(){
		if (!(Dungeon.level instanceof RegularLevel) || ((RegularLevel) Dungeon.level).rooms() == null) return null;
		for (Room r : ((RegularLevel) Dungeon.level).rooms()){
			if (r instanceof VarkolynLair) return (VarkolynLair) r;
		}
		return null;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("door_cell", doorCell);
		bundle.put("airlock", airlockCenter);
		bundle.put("console", console);
		bundle.put("handholds", handholds);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		doorCell = bundle.contains("door_cell") ? bundle.getInt("door_cell") : -1;
		airlockCenter = bundle.contains("airlock") ? bundle.getInt("airlock") : -1;
		console = bundle.contains("console") ? bundle.getInt("console") : -1;
		handholds = bundle.contains("handholds") ? bundle.getIntArray("handholds") : new int[0];
	}
}
