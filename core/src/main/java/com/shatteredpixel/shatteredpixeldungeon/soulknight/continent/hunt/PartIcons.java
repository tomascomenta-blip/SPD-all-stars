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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

//the parts of the big ones beyond the ash plains keep a sheet of their own (16 to a row, in the
//order of their kinds); the ash ones stay on items.png. their images are numbered far past it
public class PartIcons {

	public static final String SHEET = "sprites/colosos/partes_mutar.png";

	private static final int BASE = 10000, SIZE = 16, COLS = 16, ROWS = 2;

	private static TextureFilm film;

	public static int image( int kind ){
		return kind < BiomeParts.FIRST ? ItemSpriteSheet.MUTATION_PART + kind : BASE + kind - BiomeParts.FIRST;
	}

	public static boolean owns( int image ){
		return image >= BASE && image < BASE + COLS * ROWS;
	}

	private static TextureFilm film(){
		if (film == null){
			film = new TextureFilm(COLS * SIZE, ROWS * SIZE, SIZE, SIZE);
			for (int i = 0; i < COLS * ROWS; i++){
				int x = (i % COLS) * SIZE, y = (i / COLS) * SIZE;
				film.add(BASE + i, x, y, x + SIZE, y + SIZE);
			}
		}
		return film;
	}

	//ItemSprite.frame: puts the right sheet on the sprite and gives the film to cut it with
	public static TextureFilm use( Image sprite, int image ){
		boolean ours = owns(image);
		SmartTexture tx = TextureCache.get(ours ? SHEET : Assets.Sprites.ITEMS);
		if (sprite.texture != tx) sprite.texture(tx);
		return ours ? film() : ItemSpriteSheet.film;
	}
}
