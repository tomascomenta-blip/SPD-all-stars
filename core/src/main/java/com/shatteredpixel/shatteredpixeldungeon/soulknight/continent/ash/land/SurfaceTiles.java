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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Image;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Bundle;

import java.util.Arrays;

//the ground of the surface, drawn with the sheet of its biome (16x8 tiles, the layout of
//surface_ash.png for all of them). the rule is the one of its artist: every cell has a height
//(2 high, 1 low, 0 deep) and the tile comes from the height of the cell and of its neighbours,
//the road and the ramps
public class SurfaceTiles {

	//slab road: ROAD + mask (1 N, 2 E, 4 S, 8 W = that side gives onto ash)
	static final int ROAD = 16;
	static final int[] ROAD_WHOLE = {32, 33, 34, 35};
	static final int CROSS = 36;
	static final int ROAD_NE = 37, ROAD_NW = 38, ROAD_SE = 39, ROAD_SW = 40;

	//the two slope sets: high to low, low to deep
	static final int HIGH_LOW = 48, LOW_DEEP = 80;
	static final int RIM = 0, RIM_N = 7, RIM_E = 8, RIM_W = 9;
	static final int INNER_NE = 10, INNER_NW = 11;
	static final int FACE = 12, FACE_ROOTS = 21, FACE_EMBER = 22;
	static final int STEPS = 23, SLIDE = 24;
	static final int FOOT = 25, FOOT_B = 26, SHADE = 27, FOOT_SHADE = 28, SHADE_CORNER = 29;

	private static final int[][] FLOOR_HIGH = {{0, 9}, {1, 9}, {112, 9}, {113, 9}, {114, 9}, {115, 9}, {116, 9}, {117, 9},
			{2, 8}, {3, 8}, {4, 3}, {5, 1}, {6, 3}, {7, 6}};
	private static final int[][] FLOOR_WIND = {{0, 10}, {1, 10}, {2, 30}, {3, 30}, {4, 3}, {7, 3}};
	private static final int[][] FLOOR_LOW = {{8, 12}, {118, 12}, {119, 12}, {120, 12}, {121, 12}, {9, 14}, {10, 6}, {11, 1}};
	private static final int[][] FLOOR_DEEP = {{12, 12}, {122, 12}, {123, 12}, {124, 12}, {125, 12}, {13, 6}, {14, 8}, {15, 5}};

	//what the rule needs to know of a map: its heights, and the road and the ramps on it
	public interface Land {
		int width();
		int height();
		int seed();
		int alt( int x, int y );
		boolean road( int cell );
		int ramp( int cell );
		boolean cross( int cell );
		boolean windy( int x, int y );
	}

	private final Land l;
	private final int w, h, seed;

	public final int[] ground;
	public final int[] over1, over2;

	private SurfaceTiles( Land level ){
		l = level;
		w = level.width();
		h = level.height();
		seed = level.seed();
		ground = new int[w * h];
		over1 = new int[w * h];
		over2 = new int[w * h];
		Arrays.fill(over1, -1);
		Arrays.fill(over2, -1);
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				tile(x, y);
			}
		}
		if (l instanceof AshSurface){
			for (int side = 0; side < 4; side++) border((AshSurface) l, side);
		}
	}

	static SurfaceTiles of( Land level ){
		return new SurfaceTiles(level);
	}

	private int e( int x, int y ){
		return l.alt(x, y);
	}

	boolean face( int x, int y ){
		if (x < 0 || y <= 0 || x >= w || y >= h) return false;
		return e(x, y - 1) > e(x, y);
	}

	private static int set( int hi ){
		return hi == 2 ? HIGH_LOW : LOW_DEEP;
	}

	private boolean road( int x, int y ){
		return x >= 0 && y >= 0 && x < w && y < h && l.road(x + y * w);
	}

	private float roll( int x, int y, int salt ){
		return LandNoise.roll(seed, x + y * w, salt);
	}

	private int pick( int[][] options, float r ){
		int total = 0;
		for (int[] o : options) total += o[1];
		float left = r * total;
		for (int[] o : options){
			left -= o[1];
			if (left < 0) return o[0];
		}
		return options[options.length - 1][0];
	}

	private void over( int cell, int tile ){
		if (over1[cell] == -1) over1[cell] = tile;
		else if (over2[cell] == -1) over2[cell] = tile;
	}

	private void tile( int x, int y ){
		int cell = x + y * w;
		int hgt = e(x, y);

		//the face of a step: the cell under something higher
		if (face(x, y)){
			int hi = e(x, y - 1);
			int b = set(hi);
			int ramp = l.ramp(cell);
			if (ramp != 0){
				ground[cell] = b + (ramp == AshSurface.STEPS ? STEPS : SLIDE);
				return;
			}
			int lc = side(x - 1, y, hgt, hi), rc = side(x + 1, y, hgt, hi);
			int t = b + FACE + 3 * lc + rc;
			if (lc == 0 && rc == 0){
				float r = roll(x, y, 1);
				if (r < 0.12f) t = b + FACE_ROOTS;
				else if (r < 0.2f && hi == 1) t = b + FACE_EMBER;
				else if (r < 0.14f && hi == 2) t = b + FACE_EMBER;
			}
			ground[cell] = t;
			return;
		}

		//the rim of something high: a lower cell to the north, east or west
		int m = (y > 0 && e(x, y - 1) < hgt ? 1 : 0)
				+ (x < w - 1 && e(x + 1, y) < hgt ? 2 : 0)
				+ (x > 0 && e(x - 1, y) < hgt ? 4 : 0);
		if (m != 0){
			int b = set(hgt);
			int t = b + RIM + m - 1;
			if ((m == 1 || m == 2 || m == 4) && roll(x, y, 2) < 0.45f){
				t = b + (m == 1 ? RIM_N : m == 2 ? RIM_E : RIM_W);
			}
			ground[cell] = t;
			return;
		}

		//flat ground: the foot of a face, shade from the west, or just ash / road
		boolean foot = hgt < 2 && face(x, y - 1);
		boolean shade = hgt < 2 && x > 0 && (e(x - 1, y) > hgt || face(x - 1, y));
		boolean corner = hgt < 2 && !shade && !foot && x > 0 && y > 0
				&& (e(x - 1, y - 1) > hgt || face(x - 1, y - 1));
		if (foot || shade || corner){
			int b = set(hgt + 1);
			int t;
			if (foot && shade) t = b + FOOT_SHADE;
			else if (foot) t = b + (roll(x, y, 3) < 0.5f ? FOOT : FOOT_B);
			else if (shade) t = b + SHADE;
			else t = b + SHADE_CORNER;
			ground[cell] = t;
		} else if (road(x, y)){
			int mk = (road(x, y - 1) ? 0 : 1) + (road(x + 1, y) ? 0 : 2)
					+ (road(x, y + 1) ? 0 : 4) + (road(x - 1, y) ? 0 : 8);
			int t = ROAD + mk;
			if (roll(x, y, 4) < 0.5f){
				switch (mk){
					case 1: t = 41; break;
					case 2: t = 42; break;
					case 4: t = 43; break;
					case 8: t = 44; break;
					case 5: t = 45; break;
					case 10: t = 46; break;
				}
			}
			if (mk == 0 && roll(x, y, 5) < 0.32f){
				t = ROAD_WHOLE[(int)(roll(x, y, 6) * 4)];
			}
			if (l.cross(cell)) t = CROSS;
			ground[cell] = t;
			if (road(x + 1, y) && road(x, y - 1) && !road(x + 1, y - 1)) over(cell, ROAD_NE);
			if (road(x - 1, y) && road(x, y - 1) && !road(x - 1, y - 1)) over(cell, ROAD_NW);
			if (road(x + 1, y) && road(x, y + 1) && !road(x + 1, y + 1)) over(cell, ROAD_SE);
			if (road(x - 1, y) && road(x, y + 1) && !road(x - 1, y + 1)) over(cell, ROAD_SW);
		} else {
			int[][] opts = hgt == 2 ? FLOOR_HIGH : hgt == 1 ? FLOOR_LOW : FLOOR_DEEP;
			if (hgt == 2 && l.windy(x, y)) opts = FLOOR_WIND;
			ground[cell] = pick(opts, roll(x, y, 7));
		}

		//inner corners of the high ground (only the diagonal is lower)
		if (hgt > 0 && y > 0){
			int b = set(hgt);
			if (x < w - 1 && e(x, y - 1) >= hgt && e(x + 1, y) >= hgt && e(x + 1, y - 1) < hgt) over(cell, b + INNER_NE);
			if (x > 0 && e(x, y - 1) >= hgt && e(x - 1, y) >= hgt && e(x - 1, y - 1) < hgt) over(cell, b + INNER_NW);
		}
	}

	// *** where two biomes meet ***

	//by the pass to a biome with a blend sheet, the last cells before the edge are the ground of
	//the other biome. the cells of the base biome of the sheet that touch the other one get its
	//tiles by mask, like the road (+1 N, +2 E, +4 S, +8 W where the other one is), and some of the
	//row behind them a loose mix. foreign is drawn with the sheet of the other biome
	public final int[][] foreign = new int[4][], blend = new int[4][];

	private static final int DEPTH = 3, MIX = 24;

	private void border( AshSurface l, int side ){
		int p = l.passes[side];
		if (p == -1) return;
		Biome here = l.biome(), there = Biome.zone(here.next[side]);
		Biome.Blend sheet = Biome.blend(here, there);
		if (sheet == null) return;

		boolean[] strip = new boolean[w * h];
		boolean[] near = new boolean[w * h];
		for (int a = p - 6; a <= p + 7; a++){
			for (int d = 0; d < 12; d++){
				int c = along(side, a, d);
				if (c == -1 || l.out(c) || l.road(c)) continue;
				near[c] = true;
				if (d < DEPTH && a > p - 6 && a < p + 7) strip[c] = true;
			}
		}
		//on the base biome's map the strip is the other ground; on the other's, the strip is the base
		boolean baseHere = sheet.base == here;
		int[] f = new int[w * h], b = new int[w * h];
		Arrays.fill(f, -1);
		Arrays.fill(b, -1);
		for (int c = 0; c < w * h; c++){
			if (!near[c]) continue;
			int x = c % w, y = c / w;
			boolean base = baseHere ? !strip[c] : strip[c];
			if (!base){
				if (strip[c]) f[c] = pick(FLOOR_HIGH, roll(x, y, 11));
				continue;
			}
			int m = (over(strip, near, baseHere, x, y - 1) ? 1 : 0) + (over(strip, near, baseHere, x + 1, y) ? 2 : 0)
					+ (over(strip, near, baseHere, x, y + 1) ? 4 : 0) + (over(strip, near, baseHere, x - 1, y) ? 8 : 0);
			if (m != 0){
				int t = m;
				if ((m == 1 || m == 2 || m == 4 || m == 8) && roll(x, y, 12) < 0.5f){
					t = 16 + (m == 1 ? 0 : m == 2 ? 1 : m == 4 ? 2 : 3);
				}
				b[c] = t;
			} else if (touching(strip, near, baseHere, x, y) && roll(x, y, 13) < 0.5f){
				b[c] = MIX + (int)(roll(x, y, 14) * 4);
			} else if (strip[c]){
				f[c] = pick(FLOOR_HIGH, roll(x, y, 11));
			}
		}
		foreign[side] = f;
		blend[side] = b;
	}

	//cell at distance d from the edge of that side, a along it; -1 off the map
	private int along( int side, int a, int d ){
		int x, y;
		switch (side){
			case Biome.N: x = a; y = d; break;
			case Biome.S: x = a; y = h - 1 - d; break;
			case Biome.E: x = w - 1 - d; y = a; break;
			default:      x = d; y = a; break;
		}
		return x < 0 || y < 0 || x >= w || y >= h ? -1 : x + y * w;
	}

	//is that cell the ground that creeps over the base?
	private boolean over( boolean[] strip, boolean[] near, boolean baseHere, int x, int y ){
		if (x < 0 || y < 0 || x >= w || y >= h) return false;
		int c = x + y * w;
		return baseHere ? strip[c] : near[c] && !strip[c];
	}

	//the row behind: a diagonal or a cell two away is the other ground
	private boolean touching( boolean[] strip, boolean[] near, boolean baseHere, int x, int y ){
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++){
				if (over(strip, near, baseHere, x + dx, y + dy)) return true;
			}
		}
		return false;
	}

	//how a face ends on that side: 0 it goes on, 1 it ends in the open, 2 it runs into something high
	private int side( int nx, int y, int hgt, int hi ){
		if (nx < 0 || nx >= w) return 0;
		if (face(nx, y) && e(nx, y) == hgt && e(nx, y - 1) == hi) return 0;
		if (e(nx, y) > hgt) return 2;
		return 1;
	}

	// *** the layers in the scene ***

	//all three cover the whole map. the ground names every cell, the other two only draw
	public static abstract class Layer extends CustomTilemap {

		protected abstract int[] data( AshLand level );

		//drawn with the sheet of the biome
		protected boolean sheet(){
			return true;
		}

		@Override
		public Tilemap create() {
			AshLand level = (AshLand) Dungeon.level;
			if (sheet()) texture = level.sheet();
			setRect(0, 0, level.width(), level.height());
			Tilemap v = super.create();
			v.map(data(level), tileW);
			return v;
		}
	}

	public static class Ground extends Layer {

		@Override
		protected int[] data( AshLand level ) {
			return level.tiles().ground;
		}

		@Override
		public String name( int tileX, int tileY ) {
			if (!(Dungeon.level instanceof AshSurface)) return null;
			return ((AshSurface) Dungeon.level).cellName(tileX + tileY * tileW);
		}

		@Override
		public String desc( int tileX, int tileY ) {
			if (!(Dungeon.level instanceof AshSurface)) return null;
			return ((AshSurface) Dungeon.level).cellDesc(tileX + tileY * tileW);
		}
	}

	public static class Over extends Layer {

		@Override
		protected int[] data( AshLand level ) {
			return level.tiles().over1;
		}

		@Override
		public Image image( int tileX, int tileY ) {
			return null;
		}
	}

	public static class Over2 extends Over {

		@Override
		protected int[] data( AshLand level ) {
			return level.tiles().over2;
		}
	}

	//the ground by a pass: the other biome's (foreign) or the blend between the two
	public static class Border extends Layer {

		private int side;
		private boolean mix;

		public Border(){
		}

		public Border( int side, boolean mix ){
			this.side = side;
			this.mix = mix;
		}

		@Override
		protected boolean sheet() {
			return false;
		}

		@Override
		public Tilemap create() {
			AshLand level = (AshLand) Dungeon.level;
			Biome here = level.biome(), there = Biome.zone(here.next[side]);
			Biome.Blend b = Biome.blend(here, there);
			texture = mix && b != null ? b.sheet : there.sheet;
			return super.create();
		}

		@Override
		protected int[] data( AshLand level ) {
			int[] d = mix ? level.tiles().blend[side] : level.tiles().foreign[side];
			if (d != null) return d;
			d = new int[level.length()];
			Arrays.fill(d, -1);
			return d;
		}

		@Override
		public Image image( int tileX, int tileY ) {
			return null;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("side", side);
			bundle.put("mix", mix);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			side = bundle.getInt("side");
			mix = bundle.getBoolean("mix");
		}
	}

	//the ash fog around the plains: black, thicker the further out
	public static class Fog extends Layer {

		private static final String KEY = "ash_land_fog";

		@Override
		protected boolean sheet() {
			return false;
		}

		@Override
		public Tilemap create() {
			if (!TextureCache.contains(KEY)){
				SmartTexture tx = TextureCache.create(KEY, 64, 16);
				tx.filter(Texture.NEAREST, Texture.NEAREST);
				float[] alpha = {0.5f, 0.74f, 0.9f, 1f};
				for (int i = 0; i < 4; i++){
					int a = Math.round(alpha[i] * 255);
					tx.bitmap.setColor((0x0A0608 << 8) | a);
					tx.bitmap.fillRectangle(i * 16, 0, 16, 16);
				}
			}
			texture = KEY;
			return super.create();
		}

		@Override
		protected int[] data( AshLand level ) {
			int[] d = new int[level.length()];
			for (int i = 0; i < d.length; i++){
				int f = ((AshSurface) level).fog(i);
				d[i] = f == 0 ? -1 : Math.min(4, f) - 1;
			}
			return d;
		}

		@Override
		public Image image( int tileX, int tileY ) {
			return null;
		}
	}
}
