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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.TaintedFood;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.ChainDog;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.FleshGargoyle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.Lamplighter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.SootRats;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.BurningFist;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogEye;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

//how the inside of a furnace cathedral is laid out. always a cross: the portal and the narthex at
//the bottom, the nave with its pews and the aisles behind the pillars, the transept across, and
//the apse up top with the heart on its dais in a ring of molten metal. what changes from one to
//another: which end of the transept has the bell tower and which the crypt, what each chapel is
//(or whether it's there), which pews are broken, where the rubble fell
public class CathedralPlan {

	public static final int W = 41, H = 56;
	private static final int CX = 20;

	private static CathedralLevel l;
	private static boolean towerWest;

	static void build( CathedralLevel level ){
		l = level;
		//a cathedral of its own: the id of the one they walked into goes into the seed
		Random.pushGenerator(Random.Long() + 7919L * Cathedral.madeFor());
		try {
			lay();
		} finally {
			Random.popGenerator();
			l = null;
		}
	}

	private static int cell( int x, int y ){
		return x + y * W;
	}

	private static void put( int x, int y, int terrain, int zone ){
		if (x < 1 || y < 1 || x >= W - 1 || y >= H - 1) return;
		l.map[cell(x, y)] = terrain;
		l.zone[cell(x, y)] = zone;
	}

	private static void rect( int x0, int y0, int x1, int y1, int terrain, int zone ){
		for (int y = y0; y <= y1; y++){
			for (int x = x0; x <= x1; x++) put(x, y, terrain, zone);
		}
	}

	private static int at( int x, int y ){
		if (x < 0 || y < 0 || x >= W || y >= H) return Terrain.WALL;
		return l.map[cell(x, y)];
	}

	private static void low( int x, int y, int piece ){
		l.low[cell(x, y)] = piece;
	}

	private static void high( int x, int y, int piece ){
		l.high[cell(x, y)] = piece;
	}

	private static boolean floor( int x, int y ){
		int t = at(x, y);
		return t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMPTY_SP;
	}

	private static void lay(){
		l.setSize(W, H);
		l.zone = new int[W * H];
		l.low = new int[W * H];
		l.high = new int[W * H];
		Arrays.fill(l.map, Terrain.WALL);
		Arrays.fill(l.low, -1);
		Arrays.fill(l.high, -1);
		towerWest = Random.Int(2) == 0;
		gratesOut.clear();

		narthex();
		nave();
		transept();
		apse();
		tower(towerWest ? 2 : W - 10);
		crypt(towerWest ? W - 10 : 2);
		for (int slot = 0; slot < 3; slot++){
			chapel(true, slot);
			chapel(false, slot);
		}
		rubble();
		windows();
		l.customTiles.add(new CathedralDeco(false, W, H));
		l.customWalls.add(new CathedralDeco(true, W, H));

		int portal = cell(CX, 52);
		l.map[portal] = Terrain.ENTRANCE;
		l.transitions.add(new LevelTransition(l, portal, LevelTransition.Type.REGULAR_ENTRANCE,
				21, Continent.CITY, LevelTransition.Type.REGULAR_EXIT));
		CoopLog.line("cathedral: inside made for " + Cathedral.madeFor() + ", tower " + (towerWest ? "west" : "east"));
	}

	// *** the parts ***

	//the porch inside the portal, with the saints either side of the doors
	private static void narthex(){
		rect(10, 49, 30, 52, Terrain.EMPTY, CathedralLevel.NARTHEX);
		//the wall between it and the nave, open in the middle and into each aisle
		rect(9, 48, 31, 48, Terrain.WALL, CathedralLevel.OUT);
		rect(17, 48, 23, 48, Terrain.EMPTY_SP, CathedralLevel.NARTHEX);
		put(11, 48, Terrain.EMPTY, CathedralLevel.NARTHEX);
		put(29, 48, Terrain.EMPTY, CathedralLevel.NARTHEX);
		rect(19, 49, 21, 52, Terrain.EMPTY_SP, CathedralLevel.NARTHEX);
		for (int x : new int[]{12, 15, 25, 28}){
			put(x, 50, Terrain.STATUE, CathedralLevel.NARTHEX);
		}
		low(13, 51, CathedralDeco.CANDLES);
		low(27, 51, CathedralDeco.CANDLES);
		low(17, 52, CathedralDeco.WAX);
	}

	//the nave: pews in rows either side of the runner, the pillars, the aisles behind them
	private static void nave(){
		rect(14, 18, 26, 47, Terrain.EMPTY, CathedralLevel.NAVE);
		rect(10, 18, 12, 47, Terrain.EMPTY, CathedralLevel.AISLE);
		rect(28, 18, 30, 47, Terrain.EMPTY, CathedralLevel.AISLE);
		for (int y = 18; y <= 47; y++){
			boolean pillar = (y - 19) % 4 == 0 && y <= 43;
			put(13, y, pillar ? Terrain.WALL : Terrain.EMPTY, pillar ? CathedralLevel.OUT : CathedralLevel.AISLE);
			put(27, y, pillar ? Terrain.WALL : Terrain.EMPTY, pillar ? CathedralLevel.OUT : CathedralLevel.AISLE);
		}
		rect(19, 18, 21, 47, Terrain.EMPTY_SP, CathedralLevel.NAVE);
		//a row of pews on every other line, the last few rows near the door gone to the fire
		int lastRow = 43 - 2 * Random.Int(2);
		for (int y = 21; y <= lastRow; y += 2){
			pew(15, 18, y);
			pew(22, 25, y);
		}
		//what's left of a pillar here and there
		for (int k = Random.Int(2); k > 0; k--){
			int y = 19 + 4 * Random.Int(7);
			int x = Random.Int(2) == 0 ? 13 : 27;
			put(x, y, Terrain.EMPTY_DECO, CathedralLevel.AISLE);
			low(x, y, CathedralDeco.RUBBLE_B);
		}
		//chandeliers hanging over the runner
		for (int y = 24; y <= 44; y += 10) high(CX, y, CathedralDeco.CHANDELIER);
	}

	//a pew from x0 to x1: sometimes broken in two, sometimes smashed to pieces
	private static void pew( int x0, int x1, int y ){
		if (Random.Int(9) == 0){
			for (int x = x0; x <= x1; x++){
				if (Random.Int(2) == 0){
					put(x, y, Terrain.EMPTY_DECO, CathedralLevel.NAVE);
					low(x, y, CathedralDeco.PEW_BROKEN);
				}
			}
			return;
		}
		int gap = Random.Int(6) == 0 ? x0 + 1 + Random.Int(x1 - x0 - 1) : -1;
		for (int x = x0; x <= x1; x++){
			if (x == gap){
				put(x, y, Terrain.EMPTY_DECO, CathedralLevel.NAVE);
				low(x, y, CathedralDeco.PEW_BROKEN);
				continue;
			}
			put(x, y, Terrain.CUSTOM_DECO, CathedralLevel.NAVE);
			boolean left = x == x0 || x - 1 == gap, right = x == x1 || x + 1 == gap;
			low(x, y, left && right ? CathedralDeco.PEW_BROKEN : left ? CathedralDeco.PEW_L : right ? CathedralDeco.PEW_R : CathedralDeco.PEW_M);
		}
	}

	//the transept across, with a pool of molten metal and a grate at each end
	private static void transept(){
		rect(3, 11, 37, 17, Terrain.EMPTY, CathedralLevel.TRANSEPT);
		rect(19, 11, 21, 17, Terrain.EMPTY_SP, CathedralLevel.TRANSEPT);
		//the crossing: the bell that hangs from the vault
		high(CX, 13, CathedralDeco.CHAIN);
		for (int side = 0; side < 2; side++){
			int x0 = side == 0 ? 4 : W - 7;
			rect(x0, 15, x0 + 2, 16, Terrain.WATER, CathedralLevel.TRANSEPT);
			if (Random.Int(2) == 0) put(side == 0 ? x0 + 3 : x0 - 1, 16, Terrain.WATER, CathedralLevel.TRANSEPT);
		}
		grate(8, 13);
		grate(W - 9, 13);
		grate(15, 16);
		grate(W - 16, 16);
		low(16, 12, CathedralDeco.CANDLES);
		low(W - 17, 12, CathedralDeco.CANDLES);
	}

	private static final ArrayList<Integer> gratesOut = new ArrayList<>();

	private static void grate( int x, int y ){
		put(x, y, Terrain.EMPTY_DECO, l.zone[cell(x, y)] == CathedralLevel.OUT ? CathedralLevel.TRANSEPT : l.zone[cell(x, y)]);
		low(x, y, Random.Int(2) == 0 ? CathedralDeco.GRATE : CathedralDeco.GRATE_B);
		gratesOut.add(cell(x, y));
		l.grates = new int[gratesOut.size()];
		for (int i = 0; i < gratesOut.size(); i++) l.grates[i] = gratesOut.get(i);
	}

	//the apse, a cut-cornered chevet: the dais in its ring of molten metal, the way round behind it
	private static void apse(){
		rect(14, 2, 26, 2, Terrain.EMPTY, CathedralLevel.APSE);
		rect(12, 3, 28, 3, Terrain.EMPTY, CathedralLevel.APSE);
		rect(11, 4, 29, 10, Terrain.EMPTY, CathedralLevel.APSE);
		//the molten ring, crossed at the front and at each side
		rect(15, 3, 25, 9, Terrain.WATER, CathedralLevel.APSE);
		rect(16, 4, 24, 8, Terrain.EMPTY_SP, CathedralLevel.DAIS);
		rect(19, 9, 21, 10, Terrain.EMPTY_SP, CathedralLevel.DAIS);
		put(15, 6, Terrain.EMPTY_SP, CathedralLevel.DAIS);
		put(25, 6, Terrain.EMPTY_SP, CathedralLevel.DAIS);
		l.heart = cell(CX, 6);
		//candles round the dais, grates in the walk behind it
		low(17, 4, CathedralDeco.CANDLES);
		low(23, 4, CathedralDeco.CANDLES);
		low(17, 8, CathedralDeco.WAX);
		low(23, 8, CathedralDeco.WAX);
		grate(12, 5);
		grate(28, 5);
		grate(12, 9);
		grate(28, 9);
		high(13, 3, CathedralDeco.CHANDELIER);
		high(27, 3, CathedralDeco.CHANDELIER);
	}

	//the bell tower: two bells hanging from a beam, the ropes down to the floor
	private static void tower( int x0 ){
		rect(x0 + 1, 3, x0 + 6, 9, Terrain.EMPTY, CathedralLevel.TOWER);
		rect(x0 + 3, 10, x0 + 4, 10, Terrain.EMPTY, CathedralLevel.TOWER);
		for (int x = x0 + 1; x <= x0 + 6; x++){
			high(x, 4, x == x0 + 1 ? CathedralDeco.BEAM_L : x == x0 + 6 ? CathedralDeco.BEAM_R : CathedralDeco.BEAM_M);
		}
		low(x0 + 2, 8, CathedralDeco.ROPE);
		low(x0 + 5, 8, CathedralDeco.ROPE);
		if (Random.Int(2) == 0) low(x0 + 1 + Random.Int(6), 9, CathedralDeco.RUBBLE);
	}

	//bell cells, for populate
	private static int[] bells(){
		int x0 = towerWest ? 2 : W - 10;
		return new int[]{cell(x0 + 2, 5), cell(x0 + 5, 5)};
	}

	//the crypt, down a few steps: sarcophagi in two rows, bones in the corners
	private static void crypt( int x0 ){
		rect(x0 + 1, 3, x0 + 6, 9, Terrain.EMPTY, CathedralLevel.CRYPT);
		rect(x0 + 3, 10, x0 + 4, 10, Terrain.EMPTY_DECO, CathedralLevel.CRYPT);
		low(x0 + 3, 10, CathedralDeco.STEPS);
		low(x0 + 4, 10, CathedralDeco.STEPS);
		for (int sx : new int[]{x0 + 2, x0 + 5}){
			for (int sy : new int[]{4, 7}){
				put(sx, sy, Terrain.CUSTOM_DECO, CathedralLevel.CRYPT);
				put(sx, sy + 1, Terrain.CUSTOM_DECO, CathedralLevel.CRYPT);
				low(sx, sy, CathedralDeco.SARCO_T);
				low(sx, sy + 1, CathedralDeco.SARCO_B);
			}
		}
		low(x0 + 1, 3, CathedralDeco.BONES);
		low(x0 + 6, 3, CathedralDeco.SKULLS);
		low(x0 + 1, 9, CathedralDeco.URN);
		low(x0 + 6, 9, CathedralDeco.BONES);
	}

	private static int[] cryptCells(){
		int x0 = towerWest ? W - 10 : 2;
		return new int[]{cell(x0 + 3, 4), cell(x0 + 4, 8), cell(x0 + 1, 6)};
	}

	private static final int ALTAR = 0, OSSUARY = 1, FONT_CHAPEL = 2, FALLEN = 3, WALLED = 4;

	//the chapels off each aisle, three a side, each one something different (or bricked up)
	private static final int[][] chapelKind = new int[2][3];

	private static void chapel( boolean west, int slot ){
		int y0 = 20 + 9 * slot, y1 = y0 + 5;
		int x0 = west ? 3 : W - 9, x1 = x0 + 5;
		int wall = west ? 9 : W - 10;
		int kind = Random.chances(new float[]{3, 2, 2, 2, 1});
		chapelKind[west ? 0 : 1][slot] = kind;
		if (kind == WALLED) return;
		rect(x0, y0, x1, y1, Terrain.EMPTY, CathedralLevel.CHAPEL);
		put(wall, y0 + 2, Terrain.EMPTY_DECO, CathedralLevel.CHAPEL);
		put(wall, y0 + 3, Terrain.EMPTY_DECO, CathedralLevel.CHAPEL);
		//the far end, away from the aisle
		int far = west ? x0 : x1, step = west ? 1 : -1;
		switch (kind){
			case ALTAR:
				put(far, y0 + 2, Terrain.CUSTOM_DECO, CathedralLevel.CHAPEL);
				put(far, y0 + 3, Terrain.CUSTOM_DECO, CathedralLevel.CHAPEL);
				low(far, y0 + 2, CathedralDeco.ALTAR_T);
				low(far, y0 + 3, CathedralDeco.ALTAR_B);
				low(far + step, y0 + 1, CathedralDeco.CANDLES);
				low(far + step, y0 + 4, CathedralDeco.CANDLES);
				put(far, y0, Terrain.STATUE, CathedralLevel.CHAPEL);
				put(far, y1, Terrain.STATUE, CathedralLevel.CHAPEL);
				break;
			case OSSUARY:
				for (int y = y0; y <= y1; y++){
					low(far, y, y % 2 == 0 ? CathedralDeco.BONES : CathedralDeco.SKULLS);
				}
				low(far + step, y0, CathedralDeco.URN);
				low(far + step, y1, CathedralDeco.BONES);
				break;
			case FONT_CHAPEL:
				rect(x0 + 2, y0 + 2, x0 + 3, y0 + 3, Terrain.WATER, CathedralLevel.CHAPEL);
				low(far, y0 + 1, CathedralDeco.WAX);
				put(far, y0 + 4, Terrain.STATUE, CathedralLevel.CHAPEL);
				break;
			case FALLEN:
				for (int i = 0; i < 6; i++){
					int x = x0 + Random.Int(6), y = y0 + Random.Int(6);
					if (Random.Int(3) == 0 && x != wall - step){
						put(x, y, Terrain.WALL, CathedralLevel.OUT);
					} else {
						put(x, y, Terrain.EMPTY_DECO, CathedralLevel.CHAPEL);
						low(x, y, Random.Int(2) == 0 ? CathedralDeco.RUBBLE : CathedralDeco.RUBBLE_B);
					}
				}
				//the way in stays clear
				put(wall - step, y0 + 2, Terrain.EMPTY, CathedralLevel.CHAPEL);
				put(wall - step, y0 + 3, Terrain.EMPTY, CathedralLevel.CHAPEL);
				break;
		}
	}

	//fallen masonry in the nave and the transept
	private static void rubble(){
		int n = 6 + Random.Int(5);
		for (int i = 0; i < n * 10 && n > 0; i++){
			int x = 3 + Random.Int(W - 6), y = 11 + Random.Int(37);
			int c = cell(x, y);
			if (l.map[c] != Terrain.EMPTY || l.low[c] != -1) continue;
			int z = l.zone[c];
			if (z != CathedralLevel.NAVE && z != CathedralLevel.AISLE && z != CathedralLevel.TRANSEPT) continue;
			l.map[c] = Terrain.EMPTY_DECO;
			low(x, y, Random.Int(3) == 0 ? CathedralDeco.RUBBLE_B : CathedralDeco.RUBBLE);
			n--;
		}
	}

	//the stained glass, lit from behind by the furnace, and the coloured light it throws down
	private static void windows(){
		for (int y = 1; y < H - 2; y++){
			for (int x = 1; x < W - 1; x++){
				if (at(x, y) != Terrain.WALL || !floor(x, y + 1)) continue;
				//not on the pillars
				if (!(at(x - 1, y) == Terrain.WALL || at(x + 1, y) == Terrain.WALL)) continue;
				int z = l.zone[cell(x, y + 1)];
				float chance = z == CathedralLevel.APSE || z == CathedralLevel.DAIS ? 0.5f : z == CathedralLevel.CHAPEL ? 0.45f : z == CathedralLevel.TRANSEPT ? 0.35f : 0.2f;
				if (Random.Float() >= chance) continue;
				l.map[cell(x, y)] = Terrain.WALL_DECO;
				if (l.low[cell(x, y + 1)] == -1 && Random.Int(3) != 0) low(x, y + 1, CathedralDeco.LIGHT + Random.Int(3));
			}
		}
	}

	// *** who lives in there ***

	static void populate( CathedralLevel level ){
		l = level;
		try {
			people();
		} finally {
			l = null;
		}
	}

	private static boolean free( int c, boolean[] taken ){
		return c >= 0 && c < l.length() && l.passable[c] && !l.avoid[c] && !taken[c]
				&& l.map[c] != Terrain.ENTRANCE && l.zone[c] != CathedralLevel.OUT;
	}

	private static void add( Mob m, int c, boolean[] taken ){
		m.pos = c;
		taken[c] = true;
		l.mobs.add(m);
	}

	private static int somewhere( int zone, boolean[] taken ){
		for (int tries = 0; tries < 200; tries++){
			int c = Random.Int(l.length());
			if (l.zone[c] == zone && free(c, taken)) return c;
		}
		return -1;
	}

	private static void people(){
		boolean[] taken = new boolean[l.length()];
		//the heart and the ground round it
		FurnaceHeart heart = new FurnaceHeart();
		add(heart, l.heart, taken);
		for (int dy = -2; dy <= 2; dy++){
			for (int dx = -2; dx <= 2; dx++) taken[l.heart + dx + dy * W] = true;
		}
		//the faithful, melted into their pews: on the row behind each one
		ArrayList<Integer> seats = new ArrayList<>();
		for (int i = 0; i < l.length(); i++){
			if (l.zone[i] == CathedralLevel.NAVE && l.map[i] == Terrain.EMPTY && i - W >= 0 && l.map[i - W] == Terrain.CUSTOM_DECO) seats.add(i);
		}
		Random.shuffle(seats);
		int faithful = Math.min(seats.size(), 9 + Random.Int(4));
		for (int i = 0; i < faithful; i++){
			MeltedFaithful f = new MeltedFaithful();
			f.seat();
			add(f, seats.get(i), taken);
		}
		//the bells: two in the tower, one hanging over the crossing
		for (int c : bells()) add(new DeafBell(), c, taken);
		add(new DeafBell(), cell(CX, 14), taken);
		//stokers at the grates of the apse
		for (int i = 0; i < 2; i++){
			int g = l.grates[l.grates.length - 1 - i];
			if (free(g, taken)) add(new Stoker(), g, taken);
		}
		//and what came in from the city
		place(new Lamplighter(), CathedralLevel.AISLE, taken);
		place(new FleshGargoyle(), CathedralLevel.TRANSEPT, taken);
		place(new FleshGargoyle(), CathedralLevel.AISLE, taken);
		place(new YogEye(), CathedralLevel.AISLE, taken);
		place(new YogEye(), CathedralLevel.APSE, taken);
		place(new BurningFist(), CathedralLevel.APSE, taken);
		place(new YogLarva(), CathedralLevel.CRYPT, taken);
		place(new ChainDog(), CathedralLevel.CRYPT, taken);
		SootRats rats = new SootRats();
		rats.swarm = 3 + Random.Int(2);
		if (!place(rats, CathedralLevel.CHAPEL, taken)) place(rats, CathedralLevel.NARTHEX, taken);
		CoopLog.line("cathedral: " + l.mobs.size() + " inside, " + faithful + " faithful in the pews");
	}

	private static boolean place( Mob m, int zone, boolean[] taken ){
		int c = somewhere(zone, taken);
		if (c == -1) return false;
		add(m, c, taken);
		return true;
	}

	//what traps and summons ask for
	static Mob straggler(){
		switch (Random.Int(3)){
			case 0:  return new Lamplighter();
			case 1:  return new YogEye();
			default: return new Stoker();
		}
	}

	// *** what's left lying about ***

	static void items( CathedralLevel level ){
		l = level;
		try {
			loot();
		} finally {
			l = null;
		}
	}

	private static void loot(){
		//reliquaries in the crypt
		Generator.Category[] good = {Generator.Category.RING, Generator.Category.WAND, Generator.Category.ARTIFACT};
		int[] crypt = cryptCells();
		for (int i = 0; i < crypt.length; i++){
			int c = crypt[i];
			if (!l.passable[c]) continue;
			Item it = i == 0 ? Generator.random(good[Random.Int(good.length)]) : Generator.random();
			if (it == null) continue;
			Heap h = l.drop(it, c);
			h.type = Heap.Type.CHEST;
		}
		//an offering on each altar, bones that kept something
		for (int i = 0; i < l.length(); i++){
			if (l.zone[i] != CathedralLevel.CHAPEL || !l.passable[i] || l.heaps.get(i) != null) continue;
			if (l.low[i] == CathedralDeco.CANDLES && Random.Int(2) == 0){
				l.drop(Generator.random(), i);
			} else if (l.low[i] == CathedralDeco.URN && Random.Int(2) == 0){
				Heap h = l.drop(Generator.random(), i);
				h.type = Heap.Type.SKELETON;
			}
		}
		boolean[] none = new boolean[l.length()];
		int[] zones = {CathedralLevel.NAVE, CathedralLevel.AISLE, CathedralLevel.TRANSEPT};
		for (int i = 0; i < 2; i++){
			int c = somewhere(zones[Random.Int(zones.length)], none);
			if (c != -1 && l.heaps.get(c) == null) l.drop(new Gold().random(), c);
		}
		int t = somewhere(CathedralLevel.AISLE, none);
		if (t != -1 && l.heaps.get(t) == null) l.drop(new Torch(), t);
		int f = somewhere(CathedralLevel.CHAPEL, none);
		if (f != -1 && l.heaps.get(f) == null) l.drop(TaintedFood.random(), f);
	}

	// *** waking them ***

	//the heat stirs the faithful out of their pews, n of them (the nearest to the party first)
	static void stir( CathedralLevel level, int n ){
		ArrayList<MeltedFaithful> seated = new ArrayList<>();
		for (Mob m : level.mobs){
			if (m instanceof MeltedFaithful && ((MeltedFaithful) m).seated() && m.isAlive()) seated.add((MeltedFaithful) m);
		}
		Collections.sort(seated, new Comparator<Char>() {
			@Override
			public int compare( Char a, Char b ) {
				return a.id() - b.id();
			}
		});
		for (int i = 0; i < Math.min(n, seated.size()); i++) seated.get(i).rise();
	}
}
