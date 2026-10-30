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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.land;

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.AshLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.WndWorldMap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//under the surface of a biome: rock, black lava (or water, away from the ash) in the hollows and
//one way back up. drawn with the sheets of the biome by the rule of the surface: the rock is the
//high ground (its edges and its faces are what you see of it), the floor the low ground and the
//hollows the deep. older saves keep the cave tiles
public abstract class AshUnder extends AshLand {

	//the way back up to the surface
	protected int up = -1;
	//drawn with the sheet of the biome
	private boolean drawn = false;
	private SurfaceTiles tiles;

	private static final String CLEAR = "continent_clear_tiles";

	//cells of the map as the cave tiles see them
	protected static final int ROCK = Terrain.WALL, FLOOR = Terrain.EMPTY, LAVA = Terrain.WATER;

	@Override
	public void buildFlagMaps() {
		super.buildFlagMaps();
		if (!biome().lava) return;
		for (int i = 0; i < length(); i++){
			if (map[i] == Terrain.WATER) avoid[i] = true;
		}
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell(ch);
		if (biome().lava && map[ch.pos] == Terrain.WATER && !ch.flying && !Char.hasProp(ch, Char.Property.FIERY)
				&& ch.isAlive()){
			AshLevel.burnInLava(ch);
		}
	}

	//the cave of the biome above, or the skull on the ash
	protected String caveKey(){
		return biome() == Biome.ASH ? "skull" : "cave";
	}

	// *** drawn with the sheets ***

	private boolean rock( int cell ){
		int t = map[cell];
		return t == Terrain.WALL || t == Terrain.WALL_DECO;
	}

	//the sheets draw the rock as high ground: a wall one cell thick (floor above and below) would
	//look like floor, a lone column too, and the deep can't touch the high. those are opened up.
	//then the layers that draw it
	protected void useSheet(){
		int w = width();
		for (int i = w; i < length() - w; i++){
			if (map[i] == Terrain.REGION_DECO || map[i] == Terrain.REGION_DECO_ALT) map[i] = Terrain.EMPTY_DECO;
		}
		for (int pass = 0; pass < 2; pass++){
			for (int i = w; i < length() - w; i++){
				if (rock(i) && !rock(i - w) && !rock(i + w)) map[i] = FLOOR;
			}
		}
		for (int i = w; i < length() - w; i++){
			if (map[i] != LAVA) continue;
			int x = i % w;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					if (x + dx >= 0 && x + dx < w && rock(i + dx + dy * w)) map[i] = FLOOR;
				}
			}
		}
		drawn = true;
		customTiles.add(new SurfaceTiles.Ground());
		customTiles.add(new SurfaceTiles.Over());
		customTiles.add(new SurfaceTiles.Over2());
	}

	@Override
	SurfaceTiles tiles(){
		if (tiles == null) tiles = SurfaceTiles.of(new Rock());
		return tiles;
	}

	@Override
	String sheet(){
		return layer() == ABYSS ? biome().abyssSheet : biome().depthsSheet;
	}

	//under the sheet the cave tiles stay, see-through (the walls they'd raise over the heroes too)
	@Override
	public String tilesTex() {
		if (!drawn) return super.tilesTex();
		if (!TextureCache.contains(CLEAR)){
			SmartTexture tx = TextureCache.create(CLEAR, 256, 256);
			tx.filter(Texture.NEAREST, Texture.NEAREST);
			tx.bitmap.setBlending(Pixmap.Blending.None);
			tx.bitmap.setColor(0);
			tx.bitmap.fill();
		}
		return CLEAR;
	}

	//the cave as heights: deep where the hollows are, the rock high, but the row of rock right
	//over the floor is its face (one step down from the rock behind it)
	private class Rock implements SurfaceTiles.Land {

		@Override
		public int width() {
			return AshUnder.this.width();
		}

		@Override
		public int height() {
			return AshUnder.this.height();
		}

		@Override
		public int seed() {
			return up * 7919 + layer();
		}

		@Override
		public int alt( int x, int y ) {
			x = Math.max(0, Math.min(width() - 1, x));
			y = Math.max(0, Math.min(height() - 1, y));
			int c = x + y * width();
			if (map[c] == LAVA) return 0;
			if (!rock(c)) return 1;
			return y + 1 < height() && !rock(c + width()) ? 1 : 2;
		}

		@Override
		public boolean road( int cell ) {
			return false;
		}

		@Override
		public int ramp( int cell ) {
			return 0;
		}

		@Override
		public boolean cross( int cell ) {
			return false;
		}

		@Override
		public boolean windy( int x, int y ) {
			return false;
		}
	}

	//the way up, hanging out of the dark
	@Override
	public Group addVisuals() {
		super.addVisuals();
		if ((drawn || layer() == ABYSS) && up != -1){
			float s = DungeonTilemap.SIZE;
			Image rope = new Image(ContinentAssets.SURFACE_CLIFF, 64, 16, 16, 48);
			rope.x = (up % width()) * s;
			rope.y = (up / width()) * s + s - 48;
			visuals.add(rope);
		}
		return visuals;
	}

	//odds and ends of the cave: chipped floor, cracked rock
	protected void decorate(){
		for (int i = width(); i < length() - width(); i++){
			if (map[i] == FLOOR && Random.Int(10) == 0){
				map[i] = Terrain.EMPTY_DECO;
			} else if (map[i] == ROCK && Random.Int(8) == 0){
				for (int n : PathFinder.NEIGHBOURS4){
					int c = i + n;
					if (c >= 0 && c < length() && (map[c] == FLOOR || map[c] == Terrain.EMPTY_DECO)){
						map[i] = Terrain.WALL_DECO;
						break;
					}
				}
			}
		}
	}

	protected abstract String upKey();

	@Override
	public String tileName( int tile ) {
		switch (tile){
			case Terrain.WATER:          return biome().lava ? Messages.get(AshLevel.class, "lava_name") : super.tileName(tile);
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT: return Messages.get(AshLevel.class, "basalt_name");
			case Terrain.EMPTY:
			case Terrain.EMPTY_DECO:     return Messages.get(AshUnder.class, "floor_name");
			case Terrain.ENTRANCE:
			case Terrain.EMPTY_SP:       return Messages.get(AshUnder.class, upKey() + "_name");
			default:                     return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile){
			case Terrain.WATER:          return biome().lava ? Messages.get(AshLevel.class, "lava_desc") : super.tileDesc(tile);
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT: return Messages.get(AshLevel.class, "basalt_desc");
			case Terrain.EMPTY:
			case Terrain.EMPTY_DECO:     return Messages.get(AshUnder.class, "floor_desc");
			case Terrain.ENTRANCE:
			case Terrain.EMPTY_SP:       return Messages.get(AshUnder.class, upKey() + "_desc");
			default:                     return super.tileDesc(tile);
		}
	}

	@Override
	public int mapColor( int cell ) {
		switch (map[cell]){
			case Terrain.WALL: case Terrain.WALL_DECO: return 0xFF1E1614;
			case Terrain.WATER:            return biome().lava ? 0xFFB04418 : 0xFF28406A;
			case Terrain.ENTRANCE: case Terrain.EMPTY_SP: return 0xFFF0D090;
			case Terrain.REGION_DECO:      return 0xFF2E2420;
			default:                       return passable[cell] ? 0xFF5E4C44 : 0xFF2E2420;
		}
	}

	@Override
	public void marks( ArrayList<WndWorldMap.Mark> out ) {
		if (up != -1){
			out.add(new WndWorldMap.Mark(up % width() - 1, up / width() - 1, 3, 3, 0xFFFFE08A,
					Messages.get(AshUnder.class, upKey() + "_name")));
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("up", up);
		bundle.put("drawn", drawn);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		up = bundle.getInt("up");
		drawn = bundle.getBoolean("drawn");
		super.restoreFromBundle(bundle);
	}
}
