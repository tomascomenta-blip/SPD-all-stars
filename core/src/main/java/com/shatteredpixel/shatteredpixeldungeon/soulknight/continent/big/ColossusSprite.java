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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.utils.PointF;

//sheets for things bigger than a cell. every frame is the same size and the footprint (size x size
//cells) starts at pixel (ox, oy) of the frame, facing right. facing left the frame is mirrored, so
//the footprint moves to the mirrored spot and the sprite is shifted to keep it on the same cells.
//sheets may be laid out in rows (TextureFilm numbers frames row by row)
public abstract class ColossusSprite extends ContinentMobSprite {

	protected int size = 3;
	protected int frameW, frameH, ox, oy;

	protected void setup( String tex, int size, int frameW, int frameH, int ox, int oy ){
		this.size = size;
		this.frameW = frameW;
		this.frameH = frameH;
		this.ox = ox;
		this.oy = oy;
		setup(tex, frameW, frameH);
	}

	//pixel offset of the footprint's left edge inside the frame, as it's drawn right now
	protected float footLeft(){
		return flipHorizontal ? frameW - ox - size * DungeonTilemap.SIZE : ox;
	}

	@Override
	public PointF worldToCamera( int cell ) {
		int csize = DungeonTilemap.SIZE;
		int w = Dungeon.level.width();
		int col = cell % w - size / 2, row = cell / w - size / 2;
		return new PointF(
				PixelScene.align(Camera.main, col * csize - footLeft()),
				PixelScene.align(Camera.main, row * csize - oy)
		);
	}

	@Override
	public void turnTo( int from, int to ) {
		boolean was = flipHorizontal;
		super.turnTo(from, to);
		if (was != flipHorizontal){
			//same footprint, mirrored frame
			float shift = 2 * ox + size * DungeonTilemap.SIZE - frameW;
			x += flipHorizontal ? shift : -shift;
		}
	}

	@Override
	public void link( Char ch ) {
		super.link(ch);
		if (ch instanceof Colossus) size = ((Colossus) ch).size;
	}

	//a whole white body that size is blinding: just a pale tint
	@Override
	public void flash() {
		ra = ba = ga = 0.3f;
		flashTime = 0.05f;
	}

	//for sorting against the small sprites: the top row of the patch, so whatever stands around
	//it (behind included) is drawn over it and never lost under the body
	public float sortY(){
		return y + oy + DungeonTilemap.SIZE;
	}
}
