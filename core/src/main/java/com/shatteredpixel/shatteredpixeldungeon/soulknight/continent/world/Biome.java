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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.land.Landmark;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenAssets;
import com.watabou.utils.Random;

//one biome of the open world: its sheets, its pieces, its music and who lives across each edge.
//the six of them lie in a fan around the portal, and you get from one to the next on foot:
//
//    [frost moor] [ isles ] [colossi graveyard]
//    [flesh forest] [ ash ] [devoured city]
//                (portal)
public class Biome {

	public static final int N = 0, E = 1, S = 2, W = 3;

	public final int zone, branch;
	//for the texts and the sheets
	public final String key;
	//what lies across each edge (a zone, 0 for nothing)
	public final int[] next = new int[4];
	//its place in the fan, for the map
	public final int fanX, fanY;
	public final int fanColor;

	//the surface sheet (surface_ash.png layout), and whether its deep ground is water
	public String sheet;
	public boolean water;
	//how much higher (or lower) its ground lies, on the same terraces
	public float lift = 0;
	//the layers under it: the cave tiles (tiles_caves layout) and what fills their hollows
	public String underTiles, underWater;
	public boolean lava;
	public boolean depths = true;
	//their sheets, drawn by the rule of the surface (the layout of surface_ash.png)
	public String depthsSheet, abyssSheet;

	//the landmarks of its surface: the way down to the tunnels, the way down to the abyss and
	//the rest, each with the height it wants and how far from the road (min, max)
	public int cave = -1, chasm = -1;
	public int[][] marks = new int[0][];

	public int color1, color2;
	public String[] tracks;
	public float[] chances;

	private Biome( int zone, int branch, String key, int fanX, int fanY, int fanColor ){
		this.zone = zone;
		this.branch = branch;
		this.key = key;
		this.fanX = fanX;
		this.fanY = fanY;
		this.fanColor = fanColor;
	}

	private Biome sheet( String sheet, boolean water ){
		this.sheet = sheet;
		this.water = water;
		return this;
	}

	private Biome lift( float lift ){
		this.lift = lift;
		return this;
	}

	private Biome under( String tiles, String water, boolean lava ){
		underTiles = tiles;
		underWater = water;
		this.lava = lava;
		return this;
	}

	private Biome ways( int cave, int chasm ){
		this.cave = cave;
		this.chasm = chasm;
		depths = cave != -1;
		if (depths) depthsSheet = ContinentAssets.piece(key, "prof_" + key);
		abyssSheet = ContinentAssets.piece(key, "abyss_" + key);
		return this;
	}

	private Biome marks( int[]... marks ){
		this.marks = marks;
		return this;
	}

	private Biome look( int color1, int color2, String[] tracks, float[] chances ){
		this.color1 = color1;
		this.color2 = color2;
		this.tracks = tracks;
		this.chances = chances;
		return this;
	}

	private Biome edges( int n, int e, int s, int w ){
		next[N] = n;
		next[E] = e;
		next[S] = s;
		next[W] = w;
		return this;
	}

	//{kind, height, min road distance, max road distance}
	private static int[] mark( int kind, int height, int min, int max ){
		return new int[]{kind, height, min, max};
	}

	public static final Biome ASH = new Biome(Continent.Z_ASH, Continent.ASH, "ash", 1, 1, 0xFF8A7A6E)
			.sheet(ContinentAssets.SURFACE_ASH, false)
			.under(ContinentAssets.TILES_ASH, ContinentAssets.WATER_ASH, true)
			.ways(Landmark.SKULL, Landmark.CHASM_LOW)
			.look(0x5a3020, 0x2a1814, HallsLevel.HALLS_TRACK_LIST, HallsLevel.HALLS_TRACK_CHANCES)
			.edges(Continent.Z_ISLES, Continent.Z_CITY, 0, Continent.Z_FLESH);

	public static final Biome FLESH = new Biome(Continent.Z_FLESH, Continent.FLESH, "flesh", 0, 1, 0xFFB0505A)
			.sheet(ContinentAssets.surface("flesh"), false)
			.under(Assets.Environment.TILES_HALLS, Assets.Environment.WATER_HALLS, false)
			.ways(Landmark.F_CAVE, Landmark.F_CHASM)
			.marks(mark(Landmark.F_TREE, 2, 12, 30), mark(Landmark.F_CORPSE, 1, 16, 40),
					mark(Landmark.F_NEST, 1, 14, 40), mark(Landmark.F_CAMP, 2, 10, 30))
			.look(0x602028, 0x301014, CavesLevel.CAVES_TRACK_LIST, CavesLevel.CAVES_TRACK_CHANCES)
			.edges(Continent.Z_FROST, Continent.Z_ASH, 0, 0);

	public static final Biome CITY = new Biome(Continent.Z_CITY, Continent.CITY, "city", 2, 1, 0xFF8C6AA8)
			.sheet(ContinentAssets.surface("city"), true).lift(-0.25f)
			.under(Assets.Environment.TILES_CITY, Assets.Environment.WATER_CITY, false)
			.ways(Landmark.C_SEWER, Landmark.C_CHASM)
			.marks(mark(Landmark.C_CLOCK, 2, 4, 9), mark(Landmark.C_THRONE, 2, 10, 26),
					mark(Landmark.C_WALL, 2, 12, 30), mark(Landmark.C_CAMP, 1, 14, 36))
			.look(0x3c2850, 0x1c1228, CityLevel.CITY_TRACK_LIST, CityLevel.CITY_TRACK_CHANCES)
			.edges(Continent.Z_BONES, 0, 0, Continent.Z_ASH);

	public static final Biome FROST = new Biome(Continent.Z_FROST, Continent.FROST, "frost", 0, 0, 0xFFA8C8E0)
			.sheet(ContinentAssets.surface("frost"), false)
			.under(ForgottenAssets.TILES_ICE, ForgottenAssets.WATER_ICE, false)
			.ways(Landmark.R_CAVE, Landmark.R_CHASM)
			.marks(mark(Landmark.R_SHIP, 1, 14, 40), mark(Landmark.R_STATUES, 2, 6, 20),
					mark(Landmark.R_VILLAGE, 2, 10, 30), mark(Landmark.R_CAMP, 1, 14, 36))
			.look(0x405870, 0x182430, PrisonLevel.PRISON_TRACK_LIST, PrisonLevel.PRISON_TRACK_CHANCES)
			.edges(0, Continent.Z_ISLES, Continent.Z_FLESH, 0);

	public static final Biome BONES = new Biome(Continent.Z_BONES, Continent.BONES, "bones", 2, 0, 0xFFD8CC9C)
			.sheet(ContinentAssets.surface("bones"), false)
			.under(Assets.Environment.TILES_CAVES, Assets.Environment.WATER_CAVES, false)
			.ways(Landmark.B_THROAT, Landmark.B_PIT)
			.marks(mark(Landmark.B_SKELETON, 2, 10, 30), mark(Landmark.B_ALTAR, 2, 6, 20),
					mark(Landmark.B_CAMP, 1, 14, 36))
			.look(0x5a5040, 0x28241c, HallsLevel.HALLS_TRACK_LIST, HallsLevel.HALLS_TRACK_CHANCES)
			.edges(0, 0, Continent.Z_CITY, Continent.Z_ISLES);

	//no tunnels under the islands, only the abyss under the sea, and no essence: yog waits there
	public static final Biome ISLES = new Biome(Continent.Z_ISLES, Continent.ISLES, "isles", 1, 0, 0xFF6A5AA0)
			.sheet(ContinentAssets.surface("isles"), true).lift(-0.7f)
			.under(SKAssets.TILES_UNDERSEA, SKAssets.WATER_UNDERSEA, false)
			.ways(-1, Landmark.I_CHASM)
			.marks(mark(Landmark.I_THRONE, 2, 10, 30), mark(Landmark.I_LIGHTHOUSE, 2, 6, 18),
					mark(Landmark.I_WRECK, 1, 12, 36), mark(Landmark.I_PORTAL, 2, 12, 30))
			.look(0x302848, 0x141022, SewerLevel.SEWER_TRACK_LIST, SewerLevel.SEWER_TRACK_CHANCES)
			.edges(0, Continent.Z_BONES, Continent.Z_ASH, Continent.Z_FROST);

	public static final Biome[] ALL = {ASH, FLESH, CITY, FROST, BONES, ISLES};

	//where two biomes meet, the ground of one creeps over the edge of the other. a sheet like the
	//road's (16 + 16 tiles) drawn on the cells of the base biome next to the other one
	public static class Blend {
		public final Biome base, over;
		public final String sheet;

		Blend( Biome base, Biome over, String sheet ){
			this.base = base;
			this.over = over;
			this.sheet = sheet;
		}
	}

	private static final Blend[] BLENDS = {
			new Blend(FLESH, ASH, ContinentAssets.piece("flesh", "blend_ash")),
			new Blend(CITY, ASH, ContinentAssets.piece("city", "blend_ash")),
			new Blend(ISLES, ASH, ContinentAssets.piece("isles", "blend_ash")),
			new Blend(ISLES, BONES, ContinentAssets.piece("isles", "blend_bones"))
	};

	//the sheet for the edge between these two, null if there's none
	public static Blend blend( Biome a, Biome b ){
		for (Blend bl : BLENDS){
			if (bl.base == a && bl.over == b || bl.base == b && bl.over == a) return bl;
		}
		return null;
	}

	public static Biome zone( int zone ){
		for (Biome b : ALL) if (b.zone == zone) return b;
		return ASH;
	}

	public static Biome branch( int branch ){
		for (Biome b : ALL) if (b.branch == branch) return b;
		return ASH;
	}

	public static Biome current(){
		return branch(Dungeon.branch);
	}

	public static int opposite( int side ){
		return (side + 2) % 4;
	}

	public boolean hasEdge( int side ){
		return next[side] != 0;
	}

	// *** the passes between biomes ***

	public static final int PASS = 8;

	//where the pass of an edge is, along it: the same for the biomes at both sides of it and for
	//every device of a run (it comes from the seed). x for the north and south edges, y for the
	//east and west ones
	public int pass( int side, int w, int h ){
		int other = next[side];
		if (other == 0) return -1;
		int a = Math.min(zone, other), b = Math.max(zone, other);
		Random.pushGenerator(Dungeon.seed + 0x2B1D + a * 31 + b * 977);
		int at = side == N || side == S ? Random.IntRange(w / 4, w * 3 / 4) : Random.IntRange(h / 4, h * 3 / 4);
		Random.popGenerator();
		return at;
	}
}
