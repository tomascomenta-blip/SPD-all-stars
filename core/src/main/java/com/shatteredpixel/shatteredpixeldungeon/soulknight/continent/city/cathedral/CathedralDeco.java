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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Image;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

//the furniture of the inside, drawn over the tiles from the pieces of one sheet (16x16, 16 a row):
//the low layer under everyone (pews, altars, grates, bones), the high one over their heads (the
//bell beams, chains and chandeliers). which piece goes where is the level's (low[] and high[])
public class CathedralDeco extends CustomTilemap {

	static final int PEW_L = 0, PEW_M = 1, PEW_R = 2, PEW_BROKEN = 3, ALTAR_T = 4, ALTAR_B = 5,
			CANDLES = 6, GRATE = 7, GRATE_B = 8, STEPS = 9, WAX = 10, BONES = 11, SKULLS = 12, URN = 13,
			ROPE = 14, SARCO_T = 16, SARCO_B = 17, RUBBLE = 18, RUBBLE_B = 19, LIGHT = 20;
	static final int BEAM_L = 32, BEAM_M = 33, BEAM_R = 34, CHAIN = 35, CHANDELIER = 36;

	public boolean high = false;

	{
		texture = CathedralAssets.DECO;
	}

	public CathedralDeco(){
	}

	CathedralDeco( boolean high, int w, int h ){
		this.high = high;
		setRect(0, 0, w, h);
	}

	private int[] pieces(){
		if (!(Dungeon.level instanceof CathedralLevel)) return null;
		CathedralLevel l = (CathedralLevel) Dungeon.level;
		return high ? l.high : l.low;
	}

	@Override
	public Tilemap create() {
		Tilemap v = super.create();
		int[] p = pieces();
		int[] data = new int[tileW * tileH];
		for (int i = 0; i < data.length; i++) data[i] = p == null || i >= p.length ? -1 : p[i];
		v.map(data, tileW);
		return v;
	}

	//the pieces with a name of their own show up when the cell is examined
	@Override
	public Image image( int tileX, int tileY ) {
		return key(tileX, tileY) == null ? null : super.image(tileX, tileY);
	}

	@Override
	public String name( int tileX, int tileY ) {
		String k = key(tileX, tileY);
		return k == null ? null : Messages.get(CathedralLevel.class, k + "_name");
	}

	@Override
	public String desc( int tileX, int tileY ) {
		String k = key(tileX, tileY);
		return k == null ? null : Messages.get(CathedralLevel.class, k + "_desc");
	}

	private String key( int tileX, int tileY ){
		int[] p = pieces();
		if (p == null || high) return null;
		int i = tileX + tileY * tileW;
		if (i < 0 || i >= p.length) return null;
		switch (p[i]){
			case GRATE: case GRATE_B:      return "grate";
			case SARCO_T: case SARCO_B:    return "sarcophagus";
			case ALTAR_T: case ALTAR_B:    return "altar";
			case BONES: case SKULLS: case URN: return "bones";
			case STEPS:                    return "steps";
			case PEW_L: case PEW_M: case PEW_R: return "pew";
			case PEW_BROKEN:               return "broken_pew";
			case RUBBLE: case RUBBLE_B:    return "rubble";
			default:                       return null;
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("high", high);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		high = bundle.getBoolean("high");
	}
}
