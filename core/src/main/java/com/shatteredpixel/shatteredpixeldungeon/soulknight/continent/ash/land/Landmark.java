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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Halo;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

//one of the big drawings laid on the surface: a way down (a cave, an abyss), a cliff with its
//rope ladder, or one of the landmarks you find your way by. x,y is the top left cell of its
//footprint; the drawing may stick out of it (up, or a bit to the left)
public class Landmark implements Bundlable {

	//the ash plains
	public static final int SKULL = 0, CHASM = 1, CHASM_LOW = 2, CLIFF = 3;
	public static final int RIBS = 4, TOWER = 5, TREE = 6, CAMP = 7;
	//the flesh forest
	public static final int F_CAVE = 8, F_CHASM = 9, F_TREE = 10, F_NEST = 11, F_CORPSE = 12, F_CAMP = 13;
	//the devoured city
	public static final int C_SEWER = 14, C_CHASM = 15, C_CLOCK = 16, C_THRONE = 17, C_WALL = 18, C_CAMP = 19;
	//the frost moor
	public static final int R_CAVE = 20, R_CHASM = 21, R_SHIP = 22, R_STATUES = 23, R_VILLAGE = 24, R_CAMP = 25;
	//the colossi graveyard
	public static final int B_THROAT = 26, B_PIT = 27, B_SKELETON = 28, B_ALTAR = 29, B_CAMP = 30;
	//the isles
	public static final int I_CHASM = 31, I_PORTAL = 32, I_LIGHTHOUSE = 33, I_WRECK = 34, I_THRONE = 35;
	//what is scattered around and only gets in the way: trees of flesh (1x1, 2x2 and 3x3, four
	//looks each) and the houses of the city
	public static final int F_TREES = 36, C_HOUSES = 48;

	//'.' walk, '#' blocked, 'B' a way down (or the ladder), 'L' blocked with something to take
	private static final String[] CAMP_STEPS = {"........", "........", "...L.###", ".##L.###", "######L.", ".L......"};
	private static final String[] CHASM_STEPS = {"..####..", "#BBBBBB#", "#BBBBBB#", "..###..."};

	private static class Piece {
		final String tex;
		final String[] steps;
		//where the drawing starts, in pixels from the footprint's corner
		int ox, oy;
		//light: x, y (pixels in the drawing), radius, color. how far away it can be seen, in cells
		int[] light;
		int beacon;
		//drawn whole over the heroes (the first drawing goes whole under them)
		String high;
		//rows of the footprint from this one down are drawn under the characters, the ones above
		//over them. -1: from the first row that isn't all walkable
		int split = -1;

		Piece( String tex, String... steps ){
			this.tex = tex;
			this.steps = steps;
		}

		Piece at( int ox, int oy ){
			this.ox = ox;
			this.oy = oy;
			return this;
		}

		Piece light( int x, int y, int radius, int color, int beacon ){
			light = new int[]{x, y, radius, color};
			this.beacon = beacon;
			return this;
		}

		Piece split( int split ){
			this.split = split;
			return this;
		}

		Piece high( String high ){
			this.high = high;
			return this;
		}

		//one frame of a strip of them (both drawings), named as the first of its kind
		int fw, frame, text = -1;

		Piece frame( int fw, int frame, int text ){
			this.fw = fw;
			this.frame = frame;
			this.text = text;
			return this;
		}

		boolean prop(){
			return text != -1;
		}

		int split(){
			if (split >= 0) return split;
			for (int r = 0; r < steps.length; r++){
				if (steps[r].replace(".", "").length() > 0) return r;
			}
			return steps.length;
		}
	}

	private static String flesh( String name ){
		return ContinentAssets.piece("flesh", name);
	}

	private static String city( String name ){
		return ContinentAssets.piece("city", name);
	}

	private static String frost( String name ){
		return ContinentAssets.piece("frost", name);
	}

	private static String bones( String name ){
		return ContinentAssets.piece("bones", name);
	}

	private static String isles( String name ){
		return ContinentAssets.piece("isles", name);
	}

	private static final Piece[] PIECES = {
			new Piece(ContinentAssets.SURFACE_SKULL, "######", "######", "######", "######", "##BB##")
					.light(48, 58, 22, 0xC83C14, 8).split(0),
			new Piece(ContinentAssets.SURFACE_CHASM, "..##B#..", "#BBBBBB#", "#BBBBBB#", "..###...")
					.light(66, 38, 30, 0x96280F, 0).split(0),
			new Piece(ContinentAssets.SURFACE_CHASM_LOW, "..##B#..", "#BBBBBB#", "#BBBBBB#", "..###...")
					.light(66, 38, 30, 0x96280F, 0).split(0),
			new Piece(ContinentAssets.SURFACE_CLIFF, "......", "####B#", "####B#", "####B#").split(0),
			new Piece(ContinentAssets.SURFACE_RIBS_LOW, "..............", "..............", "..............", "..............",
					"...#.......#..", "..#........#..", "..#........#..", "#.#........#.#")
					.light(113, 68, 30, 0xFFD070, 10).high(ContinentAssets.SURFACE_RIBS_HIGH),
			new Piece(ContinentAssets.SURFACE_TOWER, ".....", ".....", ".....", ".....", ".....", ".....", ".....", ".....",
					"#####", "#####").light(40, 14, 60, 0xFFA424, 26).split(10),
			new Piece(ContinentAssets.SURFACE_TREE, "........", "........", "........", "........", "........", "........",
					"........", "........", "...##...", ".######.").light(63, 110, 40, 0xF07C18, 12).split(8),
			new Piece(ContinentAssets.SURFACE_CAMP, CAMP_STEPS).light(64, 28, 24, 0xFFD070, 10).split(2),

			new Piece(flesh("boca_raices"), "......", "......", "#....#", ".BBBB.", "##BB##").light(48, 58, 20, 0x8CC83C, 8),
			new Piece(flesh("sima_carne"), "..##B#..", "#BBBBBB#", "#BBBBBB#", "..###...").light(66, 38, 30, 0x6A9A28, 0),
			new Piece(flesh("arbol_costilla"), "......", "......", "......", "......", "......", "......", "......", "..##..")
					.light(30, 30, 16, 0x9CE040, 12),
			new Piece(flesh("nido_ojos"), "....", ".##.", "####").light(32, 20, 10, 0xF0D040, 0),
			new Piece(flesh("cadaver_coloso"), "..........", "..........", ".########.", ".#########", ".########.", ".##.#.....")
					.light(26, 52, 16, 0x9CE040, 8),
			new Piece(flesh("campamento_perdido"), CAMP_STEPS).light(64, 28, 24, 0xFFD070, 10),

			new Piece(city("alcantarilla_mayor"), "......", "......", "##..##", "##BB##", "......").light(48, 42, 20, 0xA060E0, 8),
			new Piece(city("sima_ciudad"), CHASM_STEPS).light(66, 38, 30, 0x7040B0, 0),
			new Piece(city("torre_reloj"), ".....", ".....", ".....", ".....", ".....", ".....", ".....", ".....", ".####", ".####")
					.light(37, 56, 30, 0xB070F0, 22),
			new Piece(city("plaza_trono"), "........", "..####..", "..####..", "###..###", "........", "........")
					.at(0, -16).light(65, 26, 24, 0xB070F0, 12),
			new Piece(city("muralla"), "########", "########", "........").at(0, -48),
			new Piece(city("campamento"), CAMP_STEPS).light(64, 28, 24, 0xFFD070, 10),

			new Piece(frost("cueva_hielo"), "......", "......", ".#.###", "##BB##", ".#BB#.").light(46, 52, 20, 0x90D0FF, 8),
			new Piece(frost("grieta_glaciar"), CHASM_STEPS).light(66, 38, 30, 0x70B0E0, 0),
			new Piece(frost("barco_atrapado"), "........", "..###.##", ".#######", ".######.", "........")
					.at(-4, -24).light(24, 46, 22, 0xFFD070, 10),
			new Piece(frost("estatuas_hielo"), "....", "....", "####").at(0, -16),
			new Piece(frost("aldea_enterrada"), "........", "........", "........", "########", "#######.")
					.at(0, -8).light(28, 48, 18, 0xFF9A40, 10),
			new Piece(frost("campamento"), CAMP_STEPS).light(64, 28, 24, 0xFFD070, 10),

			new Piece(bones("garganta_craneo"), "######", "######", "######", "######", "##BB##")
					.light(48, 58, 20, 0xE8D070, 8).split(0),
			new Piece(bones("fosa_comun"), CHASM_STEPS).light(66, 38, 30, 0xC8B050, 0),
			new Piece(bones("esqueleto_mayor_bajo"), "................", "................", "................", "####..#..#......",
					"###..........###", "###..........###", "##.......#....#.", "................")
					.at(0, -8).light(15, 58, 14, 0xE8D070, 8).high(bones("esqueleto_mayor_alto")),
			new Piece(bones("altar_dientes"), ".....", "#####", ".###.").at(0, -16).light(42, 34, 20, 0xF0D060, 10),
			new Piece(bones("campamento"), CAMP_STEPS).light(64, 28, 24, 0xFFD070, 10),

			new Piece(isles("sima_marina"), CHASM_STEPS).light(66, 38, 30, 0x7040B0, 0),
			new Piece(isles("portal_yog"), "......", "......", "......", "......", "......", "..##..", ".#####")
					.at(-4, -8).light(52, 61, 50, 0x9050E0, 16),
			new Piece(isles("faro_muerto"), "....", "....", "....", "....", "....", "....", "....", ".###", "####")
					.at(-4, -8).light(36, 26, 20, 0x9050E0, 20),
			new Piece(isles("barco_hundido"), "........", "..###.##", ".#######", ".######.", "........")
					.at(-4, -24).light(24, 46, 22, 0xFFD070, 10),
			new Piece(isles("trono_yog"), "........", "........", "........", "........", "..#..#..", ".##..###", "########", "...#####")
					.at(-4, -24).light(70, 46, 40, 0x9050E0, 16),

			tree(1, 0), tree(1, 1), tree(1, 2), tree(1, 3),
			tree(2, 0), tree(2, 1), tree(2, 2), tree(2, 3),
			tree(3, 0), tree(3, 1), tree(3, 2), tree(3, 3),

			house("casa_2x2_a", -36, "##", "##"),
			house("casa_3x2_b", -39, "###", "###"),
			house("casa_3x3_c", -53, "###", "###", "###"),
			house("casa_2x2_d", -36, "##", "##")
	};

	//the trunk under the heroes, the crown over them
	private static Piece tree( int size, int look ){
		int[] w = {40, 62, 84}, ox = {12, 15, 18}, oy = {36, 46, 56};
		String[] steps = new String[size];
		for (int i = 0; i < size; i++) steps[i] = "###".substring(0, size);
		String name = "arbol_" + size + "x" + size;
		return new Piece(flesh(name + "_tronco"), steps).at(-ox[size - 1], -oy[size - 1])
				.high(flesh(name + "_copa")).frame(w[size - 1], look, F_TREES);
	}

	//the walls under the heroes, the roof over them
	private static Piece house( String name, int oy, String... steps ){
		return new Piece(city(name + "_base"), steps).at(0, oy).high(city(name + "_techo")).frame(0, 0, C_HOUSES);
	}

	public static int props( int first ){
		return first == F_TREES ? 12 : 4;
	}

	//scattered, not worth a mark on the map
	public boolean prop(){
		return PIECES[kind].prop();
	}

	public int kind, x, y;

	public Landmark(){
	}

	public Landmark( int kind, int x, int y ){
		this.kind = kind;
		this.x = x;
		this.y = y;
	}

	public static int width( int kind ){
		return PIECES[kind].steps[0].length();
	}

	public static int height( int kind ){
		return PIECES[kind].steps.length;
	}

	public int width(){
		return width(kind);
	}

	public int height(){
		return height(kind);
	}

	public boolean inside( int cx, int cy ){
		return cx >= x && cy >= y && cx < x + width() && cy < y + height();
	}

	//'.', '#', 'B' or 'L' for a cell of the map, 0 outside
	public char step( int cx, int cy ){
		if (!inside(cx, cy)) return 0;
		return PIECES[kind].steps[cy - y].charAt(cx - x);
	}

	//the drawing is what you see there: the ground under it doesn't name the cell
	public boolean covers( int cx, int cy ){
		char s = step(cx, cy);
		return s == '#' || s == 'L' || s == 'B';
	}

	//the cells that lead down, as {left, top, right, bottom} in the map, null if none
	public int[] ways(){
		int l = 99, t = 99, r = -1, b = -1;
		String[] steps = PIECES[kind].steps;
		for (int row = 0; row < steps.length; row++){
			for (int col = 0; col < steps[row].length(); col++){
				if (steps[row].charAt(col) != 'B') continue;
				l = Math.min(l, col);
				t = Math.min(t, row);
				r = Math.max(r, col);
				b = Math.max(b, row);
			}
		}
		return r == -1 ? null : new int[]{x + l, y + t, x + r, y + b};
	}

	//cell (in the map) of its light, -1 if it has none
	public int lightCell( int mapWidth ){
		Piece p = PIECES[kind];
		if (p.light == null) return -1;
		return (x + Math.floorDiv(p.ox + p.light[0], 16)) + (y + Math.floorDiv(p.oy + p.light[1], 16)) * mapWidth;
	}

	public int beacon(){
		return PIECES[kind].beacon;
	}

	public String name(){
		Piece p = PIECES[kind];
		return Messages.get(Landmark.class, "name_" + (p.prop() ? p.text : kind));
	}

	public String desc(){
		Piece p = PIECES[kind];
		return Messages.get(Landmark.class, "desc_" + (p.prop() ? p.text : kind));
	}

	//the part of the drawing for this one: {x, y, w, h} in its texture
	private static int[] rect( Piece p, String tex ){
		Image whole = new Image(tex);
		int w = p.fw > 0 ? p.fw : (int)whole.width;
		return new int[]{p.fw > 0 ? p.frame * p.fw : 0, 0, w, (int)whole.height};
	}

	// *** drawing ***

	private static final int SIZE = DungeonTilemap.SIZE;

	//under the characters
	public void addLow( Group g ){
		Piece p = PIECES[kind];
		if (kind == TOWER) return;
		int px = x * SIZE + p.ox, py = y * SIZE + p.oy;
		Image whole = new Image(p.tex);
		if (p.high != null){
			int[] r = rect(p, p.tex);
			Image im = new Image(p.tex, r[0], r[1], r[2], r[3]);
			im.x = px;
			im.y = py;
			g.add(im);
			return;
		}
		int iw = (int)whole.width, ih = (int)whole.height;
		int cut = Math.max(0, Math.min(ih, p.split() * SIZE - p.oy));
		if (cut < ih){
			Image im = new Image(p.tex, 0, cut, iw, ih - cut);
			im.x = px;
			im.y = py + cut;
			g.add(im);
		}
	}

	//over the characters
	public void addHigh( Group g ){
		Piece p = PIECES[kind];
		int px = x * SIZE + p.ox, py = y * SIZE + p.oy;
		if (p.high != null){
			int[] r = rect(p, p.high);
			Image im = new Image(p.high, r[0], r[1], r[2], r[3]);
			im.x = px;
			im.y = py;
			g.add(im);
		} else if (kind == TOWER){
			g.add(new Fire(px, py));
			Image im = new Image(p.tex);
			im.x = px;
			im.y = py;
			g.add(im);
		} else {
			Image whole = new Image(p.tex);
			int iw = (int)whole.width, ih = (int)whole.height;
			int cut = Math.max(0, Math.min(ih, p.split() * SIZE - p.oy));
			if (cut > 0){
				Image im = new Image(p.tex, 0, 0, iw, cut);
				im.x = px;
				im.y = py;
				g.add(im);
			}
		}
		int[] l = p.light;
		if (l != null){
			g.add(new Glow(l[2], l[3], kind == TOWER ? 0.45f : 0.3f).point(px + l[0], py + l[1]));
		}
	}

	//the fire on top of the watchtower that never goes out: 4 frames of 80x48
	private static class Fire extends Image {

		private final TextureFilm film;
		private float t = 0;

		Fire( float px, float py ){
			super(ContinentAssets.SURFACE_TOWER_FIRE);
			film = new TextureFilm(texture, 80, 48);
			frame(film.get(0));
			x = px;
			y = py;
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			frame(film.get((int)(t * 7) % 4));
		}
	}

	//a slow breathing light
	private static class Glow extends Halo {

		private final float base;
		private float t;

		Glow( float radius, int color, float brightness ){
			super(radius, color, brightness);
			base = brightness;
			t = (radius * 13) % 7;
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			alpha(base * (0.85f + 0.15f * (float)Math.sin(t * 2.1f)));
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		bundle.put("kind", kind);
		bundle.put("x", x);
		bundle.put("y", y);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		kind = bundle.getInt("kind");
		x = bundle.getInt("x");
		y = bundle.getInt("y");
	}
}
