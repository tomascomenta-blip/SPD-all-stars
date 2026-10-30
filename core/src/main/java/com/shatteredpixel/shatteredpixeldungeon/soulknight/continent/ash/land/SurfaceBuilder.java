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

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//lays out the surface of a biome. on the ash plains the slab road crosses it from the portal
//(south) to the pass in the ash storm (north); elsewhere it runs from one pass to the other, and
//a pass on the east or west edge gets a road of its own to it. around the road the ground is
//high; away from it it steps down in terraces to low and then deep ground, and the further down
//the worse. the faces of the terraces only show (and only have ramps) when they look south, so
//every lower patch gets at least one ramp on a face that joins it to the rest. all of it with
//Random, under the level's seed
class SurfaceBuilder {

	static final int W = 160, H = 128;

	private final AshSurface l;
	private final Biome biome;
	private final int[] alt = new int[W * H];
	private final boolean[] road = new boolean[W * H];
	private final boolean[] out = new boolean[W * H];
	private final boolean[] locked = new boolean[W * H];
	private final boolean[] taken = new boolean[W * H];
	private final boolean[] cross = new boolean[W * H];
	private final boolean[] open = new boolean[W * H];
	private final int[] ramp = new int[W * H];
	private float[] near;
	private int seed;

	private final ArrayList<Landmark> marks = new ArrayList<>();
	private int portal = -1, shrine = -1;
	private int[] wayX, wayY;
	//where each edge has its pass (x or y along it), -1 none
	private final int[] pass = new int[4];
	//a cell of the road everything has to be reached from
	private int home = -1;

	SurfaceBuilder( AshSurface level ){
		l = level;
		biome = level.biome();
	}

	private static int cell( int x, int y ){
		return x + y * W;
	}

	private static boolean in( int x, int y ){
		return x >= 0 && y >= 0 && x < W && y < H;
	}

	private int a( int x, int y ){
		return alt[cell(Math.max(0, Math.min(W - 1, x)), Math.max(0, Math.min(H - 1, y)))];
	}

	boolean build(){
		seed = Random.Int();
		l.tileSeed = Random.Int();

		for (int side = 0; side < 4; side++) pass[side] = biome.pass(side, W, H);

		margin();
		if (biome == Biome.ASH) mainRoad();
		else spine();
		for (int side = Biome.E; side <= Biome.W; side += 2){
			if (pass[side] != -1) sideRoad(side);
		}
		heights();
		passes();
		settle();

		boolean ash = biome == Biome.ASH;
		if (ash && !ribs()) return fail("ribs");
		if (ash && !tower()) return fail("tower");
		if (biome.cave != -1 && !skull(biome.cave)) return fail("cave");
		if (biome != Biome.ISLES && !shrine()) return fail("shrine");
		if (!somewhere(biome.chasm, 1, 2, 18, 34, 8, H / 2, 0)
				&& !somewhere(biome.chasm, 1, 1, 16, 40, 8, H - 20, 0)) return fail("chasm");
		if (ash){
			int side = Random.Int(2) == 0 ? -1 : 1;
			if (!somewhere(Landmark.TREE, 0, 1, 32, 999, H / 5, H * 4 / 5, side)
					&& !somewhere(Landmark.TREE, 1, 1, 16, 40, 12, H - 24, 0)) return fail("tree");
			if (!somewhere(Landmark.CAMP, 0, 1, 32, 999, H / 6, H * 5 / 6, -side)
					&& !somewhere(Landmark.CAMP, 1, 1, 16, 40, 12, H - 20, 0)) return fail("camp");
		}
		for (int[] m : biome.marks){
			if (!somewhere(m[0], m[1], 1, m[2], m[3], 12, H - 20, 0)
					&& !somewhere(m[0], 1, 1, 12, 44, 10, H - 18, 0)){
				CoopLog.line(biome.key + " surface: no room for landmark " + m[0]);
			}
		}
		settle();
		if (ash) cliffs();
		settle();
		props();

		walkable();
		connect();
		moreRamps();

		fill();
		return true;
	}

	private boolean fail( String what ){
		CoopLog.line(biome.key + " surface: no room for the " + what + ", again");
		return false;
	}

	// *** the edges: ash fog all around ***

	private void margin(){
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				int m = 5 + (int)(4 * LandNoise.value(seed + 11, x, y, 9));
				out[cell(x, y)] = x < m || y < m || x >= W - m || y >= H - m;
			}
		}
	}

	// *** the road ***

	//from the portal to the north in seven stretches: up, across, up. the fourth goes straight on
	//(the ribs of a colossus are laid over it)
	private void mainRoad(){
		int n = 7;
		wayX = new int[n + 1];
		wayY = new int[n + 1];
		int sy = H - 14, ny = 2;
		wayX[0] = W / 2 + Random.IntRange(-14, 14);
		wayY[0] = sy;
		for (int k = 1; k <= n; k++){
			wayY[k] = sy - k * (sy - ny) / n;
			if (k == 4 || k == n){
				wayX[k] = wayX[k - 1];
			} else {
				wayX[k] = Math.max(30, Math.min(W - 32, wayX[k - 1] + Random.IntRange(-26, 26)));
			}
			//the last two stretches go straight into the pass in the storm
			if (k == n - 1 && pass[Biome.N] != -1) wayX[k] = pass[Biome.N];
		}
		for (int k = 0; k < n; k++){
			stretch(wayX[k], wayY[k], wayX[k + 1], wayY[k + 1]);
		}
		if (pass[Biome.N] != -1) stretch(wayX[n], ny, wayX[n], 0);
		//a little square where the portal opens
		brush(wayX[0] - 1, sy + 1);
		brush(wayX[0] + 1, sy + 1);
		portal = cell(wayX[0], sy + 2);
		home = portal;
	}

	//anywhere else, from the south pass (or a dead end) to the north pass (or a dead end), long
	//enough that the roads from the east and west passes run into it
	private void spine(){
		boolean south = pass[Biome.S] != -1, north = pass[Biome.N] != -1;
		int sy = south ? H - 1 : H - 20 - Random.Int(6);
		int ny = north ? 0 : 18 + Random.Int(6);
		for (int side = Biome.E; side <= Biome.W; side += 2){
			if (pass[side] == -1) continue;
			sy = Math.max(sy, Math.min(H - 12, pass[side] + 8));
			ny = Math.min(ny, Math.max(10, pass[side] - 8));
		}
		int n = 6;
		wayX = new int[n + 1];
		wayY = new int[n + 1];
		wayX[0] = south ? pass[Biome.S] : W / 2 + Random.IntRange(-20, 20);
		wayY[0] = sy;
		for (int k = 1; k <= n; k++){
			wayY[k] = sy - k * (sy - ny) / n;
			if (k == 1 || k == n){
				wayX[k] = wayX[k - 1];
			} else {
				wayX[k] = Math.max(30, Math.min(W - 32, wayX[k - 1] + Random.IntRange(-26, 26)));
			}
			if (k >= n - 1 && north) wayX[k] = pass[Biome.N];
		}
		for (int k = 0; k < n; k++){
			stretch(wayX[k], wayY[k], wayX[k + 1], wayY[k + 1]);
		}
		home = cell(wayX[n / 2], wayY[n / 2]);
	}

	//from the pass on the east or west edge, across until it meets a road, with a jog now and then
	private void sideRoad( int side ){
		int dir = side == Biome.W ? 1 : -1;
		int x = side == Biome.W ? 0 : W - 2, y = pass[side];
		int run = 0, jog = 12 + Random.Int(12);
		boolean[] mine = new boolean[W * H];
		for (int steps = 0; steps < W; steps++){
			int from = side == Biome.W ? x : W - 2 - x;
			if (touches(x, y, mine)){
				cross[cell(x, y)] = true;
				return;
			}
			own(x, y, mine);
			x += dir;
			if (++run >= jog && from > 12){
				int ty = Math.max(14, Math.min(H - 16, y + Random.IntRange(-8, 8)));
				int sy = ty > y ? 1 : -1;
				while (y != ty){
					if (touches(x, y, mine)){
						cross[cell(x, y)] = true;
						return;
					}
					own(x, y, mine);
					y += sy;
				}
				run = 0;
				jog = 12 + Random.Int(12);
			}
		}
	}

	private void own( int x, int y, boolean[] mine ){
		for (int dy = 0; dy < 2; dy++){
			for (int dx = 0; dx < 2; dx++){
				if (in(x + dx, y + dy) && !road[cell(x + dx, y + dy)]) mine[cell(x + dx, y + dy)] = true;
			}
		}
		brush(x, y);
	}

	//some other road under the next piece of this one
	private boolean touches( int x, int y, boolean[] mine ){
		for (int dy = 0; dy < 2; dy++){
			for (int dx = 0; dx < 2; dx++){
				int c = cell(Math.max(0, Math.min(W - 1, x + dx)), Math.max(0, Math.min(H - 1, y + dy)));
				if (road[c] && !mine[c]) return true;
			}
		}
		return false;
	}

	//no fog over the passes, and high ground in them, so the road goes out of the map clean
	private void passes(){
		for (int side = 0; side < 4; side++){
			int p = pass[side];
			if (p == -1) continue;
			for (int a = p - 6; a <= p + 7; a++){
				for (int d = 0; d < 13; d++){
					int x, y;
					switch (side){
						case Biome.N: x = a; y = d; break;
						case Biome.S: x = a; y = H - 1 - d; break;
						case Biome.E: x = W - 1 - d; y = a; break;
						default:      x = d; y = a; break;
					}
					if (!in(x, y)) continue;
					int c = cell(x, y);
					out[c] = false;
					alt[c] = 2;
					locked[c] = true;
					taken[c] = true;
				}
			}
		}
	}

	private void stretch( int x0, int y0, int x1, int y1 ){
		int mid = (y0 + y1) / 2;
		for (int y = y0; y >= mid; y--) brush(x0, y);
		int dx = x1 > x0 ? 1 : -1;
		for (int x = x0; x != x1; x += dx) brush(x, mid);
		for (int y = mid; y >= y1; y--) brush(x1, y);
	}

	private void brush( int x, int y ){
		for (int dy = 0; dy < 2; dy++){
			for (int dx = 0; dx < 2; dx++){
				if (in(x + dx, y + dy)) road[cell(x + dx, y + dy)] = true;
			}
		}
	}

	// *** the heights ***

	//the ground drops away from the road, but not evenly: lobes of high ground, hollows, a mesa
	//here and there. it comes in blocks of 4x3, so the faces of the terraces run straight a while
	private void heights(){
		near = distance(road);
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				int c = cell(x, y);
				int bx = Math.min(W - 1, (x / 4) * 4 + 2), by = Math.min(H - 1, (y / 3) * 3 + 1);
				float e = 2.4f + biome.lift - near[cell(bx, by)] / 22f
						+ 2f * LandNoise.wobble(seed + 3, bx, by, 26) + 0.8f * LandNoise.wobble(seed + 9, bx, by, 9);
				if (near[c] <= 3.5f){
					alt[c] = 2;
					locked[c] = true;
				} else {
					alt[c] = e > 1.5f ? 2 : e > 0.5f ? 1 : 0;
				}
			}
		}
	}

	//chamfer distance to the marked cells
	private static float[] distance( boolean[] from ){
		float[] d = new float[W * H];
		for (int i = 0; i < d.length; i++) d[i] = from[i] ? 0 : 9999;
		float diag = 1.41f;
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				int c = cell(x, y);
				if (x > 0) d[c] = Math.min(d[c], d[c - 1] + 1);
				if (y > 0){
					d[c] = Math.min(d[c], d[c - W] + 1);
					if (x > 0) d[c] = Math.min(d[c], d[c - W - 1] + diag);
					if (x < W - 1) d[c] = Math.min(d[c], d[c - W + 1] + diag);
				}
			}
		}
		for (int y = H - 1; y >= 0; y--){
			for (int x = W - 1; x >= 0; x--){
				int c = cell(x, y);
				if (x < W - 1) d[c] = Math.min(d[c], d[c + 1] + 1);
				if (y < H - 1){
					d[c] = Math.min(d[c], d[c + W] + 1);
					if (x < W - 1) d[c] = Math.min(d[c], d[c + W + 1] + diag);
					if (x > 0) d[c] = Math.min(d[c], d[c + W - 1] + diag);
				}
			}
		}
		return d;
	}

	//cleans the heights so the tiles can draw them: no lonely bumps, no strips one cell thick,
	//and deep ash never closer than 4 cells to high ash (the ash goes down one step at a time,
	//and two faces one right under the other look like a glitch)
	private void settle(){
		for (int pass = 0; pass < 8; pass++){
			boolean changed = false;
			if (pass < 2) changed |= majority();
			changed |= strips();
			changed |= blobs();
			changed |= stepByStep();
			if (!changed) break;
		}
		stepByStep();
	}

	private boolean majority(){
		boolean changed = false;
		int[] next = alt.clone();
		for (int y = 1; y < H - 1; y++){
			for (int x = 1; x < W - 1; x++){
				int c = cell(x, y);
				if (locked[c]) continue;
				int[] count = new int[3];
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						if (dx != 0 || dy != 0) count[alt[cell(x + dx, y + dy)]]++;
					}
				}
				for (int v = 0; v < 3; v++){
					if (count[v] >= 6 && v != alt[c]){
						next[c] = v;
						changed = true;
					}
				}
			}
		}
		System.arraycopy(next, 0, alt, 0, alt.length);
		return changed;
	}

	private boolean strips(){
		boolean changed = false;
		for (int y = 1; y < H - 1; y++){
			for (int x = 1; x < W - 1; x++){
				int c = cell(x, y);
				if (locked[c]) continue;
				int n = alt[c - W], s = alt[c + W], e = alt[c + 1], w = alt[c - 1];
				if (n == s && n != alt[c]){
					alt[c] = n;
					changed = true;
				} else if (e == w && e != alt[c]){
					alt[c] = e;
					changed = true;
				}
			}
		}
		return changed;
	}

	//patches of less than 12 cells take the height around them
	private boolean blobs(){
		boolean changed = false;
		boolean[] seen = new boolean[W * H];
		int[] queue = new int[W * H];
		for (int start = 0; start < W * H; start++){
			if (seen[start]) continue;
			int v = alt[start];
			int head = 0, tail = 0;
			queue[tail++] = start;
			seen[start] = true;
			boolean fixed = false;
			int[] around = new int[3];
			while (head < tail){
				int c = queue[head++];
				if (locked[c]) fixed = true;
				int x = c % W, y = c / W;
				int[] ns = {x > 0 ? c - 1 : -1, x < W - 1 ? c + 1 : -1, y > 0 ? c - W : -1, y < H - 1 ? c + W : -1};
				for (int n : ns){
					if (n < 0) continue;
					if (alt[n] != v){
						around[alt[n]]++;
					} else if (!seen[n]){
						seen[n] = true;
						queue[tail++] = n;
					}
				}
			}
			if (tail < 12 && !fixed){
				int best = v;
				for (int k = 0; k < 3; k++){
					if (k != v && (best == v || around[k] > around[best])) best = k;
				}
				if (best != v && around[best] > 0){
					for (int i = 0; i < tail; i++) alt[queue[i]] = best;
					changed = true;
				}
			}
		}
		return changed;
	}

	private boolean stepByStep(){
		boolean changed = false;
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				int c = cell(x, y);
				if (alt[c] != 0 || locked[c]) continue;
				search:
				for (int dy = -3; dy <= 3; dy++){
					for (int dx = -3; dx <= 3; dx++){
						if (in(x + dx, y + dy) && alt[cell(x + dx, y + dy)] == 2){
							alt[c] = 1;
							changed = true;
							break search;
						}
					}
				}
			}
		}
		return changed;
	}

	// *** what is laid on it ***

	private boolean fits( int x0, int y0, int w, int h, int border, int want ){
		for (int y = y0 - border; y < y0 + h + border; y++){
			for (int x = x0 - border; x < x0 + w + border; x++){
				if (!in(x, y)) return false;
				int c = cell(x, y);
				if (out[c] || road[c] || taken[c] || alt[c] != want) return false;
			}
		}
		return true;
	}

	private boolean clear( int x0, int y0, int w, int h, int border ){
		for (int y = y0 - border; y < y0 + h + border; y++){
			for (int x = x0 - border; x < x0 + w + border; x++){
				if (!in(x, y)) return false;
				int c = cell(x, y);
				if (out[c] || road[c] || taken[c]) return false;
			}
		}
		return true;
	}

	private void force( int x0, int y0, int w, int h, int border, int want ){
		for (int y = y0 - border; y < y0 + h + border; y++){
			for (int x = x0 - border; x < x0 + w + border; x++){
				if (!in(x, y)) continue;
				int c = cell(x, y);
				alt[c] = want;
				locked[c] = true;
				taken[c] = true;
			}
		}
	}

	//top left corners where the middle of a w x h thing is between minNear and maxNear cells
	//from the road, side -1 west half, 1 east half, 0 anywhere; shuffled
	private ArrayList<Integer> spots( int w, int h, float minNear, float maxNear, int yMin, int yMax, int side ){
		ArrayList<Integer> out = new ArrayList<>();
		for (int y = Math.max(2, yMin); y <= Math.min(H - 2, yMax) - h; y += 2){
			for (int x = 2; x < W - w - 2; x += 2){
				int mx = x + w / 2;
				if (side != 0 && (side < 0) != (mx < W / 2)) continue;
				float d = near[cell(mx, y + h / 2)];
				if (d >= minNear && d <= maxNear) out.add(cell(x, y));
			}
		}
		Random.shuffle(out);
		return out;
	}

	//on ground of that height if there's some, otherwise the ground is made so
	private boolean somewhere( int kind, int want, int border, float minNear, float maxNear, int yMin, int yMax, int side ){
		int w = Landmark.width(kind), h = Landmark.height(kind);
		ArrayList<Integer> spots = spots(w, h, minNear, maxNear, yMin, yMax, side);
		int at = -1;
		for (int c : spots){
			if (fits(c % W, c / W, w, h, border, want)){
				at = c;
				break;
			}
		}
		if (at == -1){
			for (int c : spots){
				if (clear(c % W, c / W, w, h, border + 2)){
					at = c;
					break;
				}
			}
		}
		if (at == -1) return false;
		force(at % W, at / W, w, h, border, want);
		if (kind == Landmark.CHASM_LOW && want == 2) kind = Landmark.CHASM;
		marks.add(new Landmark(kind, at % W, at / W));
		return true;
	}

	//the ribs of a colossus over the straight stretch of road, the road through the tunnel
	private boolean ribs(){
		int x = wayX[3], y = (wayY[3] + wayY[4]) / 2;
		int x0 = x - 6, y0 = y - 4;
		int w = Landmark.width(Landmark.RIBS), h = Landmark.height(Landmark.RIBS);
		for (int yy = y0; yy < y0 + h; yy++){
			if (!road[cell(x, yy)] || !road[cell(x + 1, yy)]) return false;
		}
		if (!in(x0 - 2, y0 - 2) || !in(x0 + w + 2, y0 + h + 2)) return false;
		force(x0, y0, w, h, 2, 2);
		marks.add(new Landmark(Landmark.RIBS, x0, y0));
		return true;
	}

	//the watchtower, right by the road on the first stretch: the first thing they'll see
	private boolean tower(){
		int w = Landmark.width(Landmark.TOWER), h = Landmark.height(Landmark.TOWER);
		for (int c : spots(w, h, 4, 8, wayY[0] - 22, wayY[0] - 2, 0)){
			int x0 = c % W, y0 = c / W;
			if (clear(x0, y0, w, h, 1)){
				force(x0, y0, w, h, 1, 2);
				marks.add(new Landmark(Landmark.TOWER, x0, y0));
				return true;
			}
		}
		return false;
	}

	//the skull (or the cave of the biome), on the high ground a bit off the road, with a branch
	//of road to its mouth
	private boolean skull( int kind ){
		int w = Landmark.width(kind), h = Landmark.height(kind);
		for (int c : spots(w, h, 9, 16, H / 4, H * 3 / 5, 0)){
			int x0 = c % W, y0 = c / W;
			if (!fits(x0, y0, w, h, 1, 2)) continue;
			if (branch(x0 + 2, y0 + h)){
				force(x0, y0, w, h, 0, 2);
				marks.add(new Landmark(kind, x0, y0));
				return true;
			}
		}
		return false;
	}

	//2 wide: two steps down from (x,y), then sideways until it meets the main road
	private boolean branch( int x, int y ){
		int row = y + 1;
		int dir = 0, reach = 999;
		for (int d = -1; d <= 1; d += 2){
			for (int k = 1; k < 60; k++){
				int xx = x + d * k;
				if (!in(xx, row)) break;
				if (road[cell(xx, row)] || road[cell(xx, row + 1)]){
					if (k < reach){
						reach = k;
						dir = d;
					}
					break;
				}
			}
		}
		if (dir == 0) return false;
		ArrayList<Integer> path = new ArrayList<>();
		path.add(cell(x, y));
		//each piece of road is 2 cells wide, from its x to the right
		int pieces = dir > 0 ? reach - 1 : reach;
		for (int k = 0; k < pieces; k++){
			path.add(cell(x + dir * k, row));
		}
		for (int p : path){
			int px = p % W, py = p / W;
			for (int dy = -1; dy <= 2; dy++){
				for (int dx = -1; dx <= 2; dx++){
					if (!in(px + dx, py + dy)) return false;
					int c = cell(px + dx, py + dy);
					if (out[c] || alt[c] != 2 || taken[c]) return false;
				}
			}
		}
		for (int p : path){
			brush(p % W, p / W);
			for (int dy = -1; dy <= 2; dy++){
				for (int dx = -1; dx <= 2; dx++){
					locked[cell(p % W + dx, p / W + dy)] = true;
				}
			}
		}
		int jx = x + dir * reach;
		cross[cell(jx, road[cell(jx, row)] ? row : row + 1)] = true;
		return true;
	}

	//the besieged shrine: a square of slabs down on the low ash
	private boolean shrine(){
		ArrayList<Integer> spots = spots(5, 5, 17, 30, H * 2 / 5, H * 6 / 7, 0);
		int at = -1;
		for (int c : spots){
			if (fits(c % W, c / W, 5, 5, 2, 1)){
				at = c;
				break;
			}
		}
		if (at == -1){
			for (int c : spots){
				if (clear(c % W, c / W, 5, 5, 4)){
					at = c;
					break;
				}
			}
		}
		if (at == -1) return false;
		int x0 = at % W, y0 = at / W;
		force(x0, y0, 5, 5, 2, 1);
		for (int y = y0; y < y0 + 5; y++){
			for (int x = x0; x < x0 + 5; x++){
				road[cell(x, y)] = true;
			}
		}
		shrine = cell(x0 + 2, y0 + 2);
		return true;
	}

	//a cliff with a rope ladder, straight from the high ground down to a pocket of deep ash. it
	//goes where there's a strip of high ground to stand on; what's under it is made to fit: a
	//patch of low ash around the pocket, so the high and the deep never touch
	private void cliffs(){
		ArrayList<Integer> spots = new ArrayList<>();
		for (int y = 14; y < H - 24; y++){
			for (int x = 10; x < W - 16; x++){
				spots.add(cell(x, y));
			}
		}
		Random.shuffle(spots);
		int placed = 0;
		for (int c : spots){
			if (placed >= 2) break;
			if (cliffFits(c % W, c / W)){
				cliff(c % W, c / W);
				placed++;
			}
		}
	}

	//the trees of the forest and the houses of the city, here and there on flat ground, never on
	//the road nor in a pass
	private void props(){
		int first = biome == Biome.FLESH ? Landmark.F_TREES : biome == Biome.CITY ? Landmark.C_HOUSES : -1;
		if (first == -1) return;
		int want = biome == Biome.FLESH ? 80 : 55;
		int placed = 0;
		for (int tries = 0; tries < want * 40 && placed < want; tries++){
			int kind = first + Random.Int(Landmark.props(first));
			int w = Landmark.width(kind), h = Landmark.height(kind);
			int x = Random.IntRange(4, W - w - 5), y = Random.IntRange(4, H - h - 5);
			if (near[cell(x + w / 2, y + h / 2)] < 4) continue;
			int a = alt[cell(x, y)];
			if (a == 0 && biome.water) continue;
			if (!fits(x, y, w, h, 1, a)) continue;
			for (int yy = y; yy < y + h; yy++){
				for (int xx = x; xx < x + w; xx++){
					taken[cell(xx, yy)] = true;
					locked[cell(xx, yy)] = true;
				}
			}
			marks.add(new Landmark(kind, x, y));
			placed++;
		}
	}

	//x0,y = where the face starts (the cliff's top row is y - 1)
	private boolean cliffFits( int x0, int y ){
		if (near[cell(x0 + 3, y)] < 8) return false;
		for (int yy = y - 2; yy < y; yy++){
			for (int x = x0 - 1; x < x0 + 7; x++){
				int c = cell(x, yy);
				if (alt[c] != 2 || road[c] || taken[c]) return false;
			}
		}
		for (int yy = y; yy <= y + 12; yy++){
			for (int x = x0 - 4; x <= x0 + 10; x++){
				if (!in(x, yy)) return false;
				int c = cell(x, yy);
				if (road[c] || taken[c] || out[c] || locked[c]) return false;
			}
		}
		for (Landmark m : marks){
			if (m.kind == Landmark.CLIFF && Math.abs(m.x - x0) < 40) return false;
		}
		return true;
	}

	private void cliff( int x0, int y ){
		for (int yy = y; yy <= y + 12; yy++){
			for (int x = x0 - 4; x <= x0 + 10; x++){
				alt[cell(x, yy)] = 1;
				locked[cell(x, yy)] = true;
			}
		}
		for (int x = x0; x < x0 + 6; x++){
			alt[cell(x, y + 2)] = 0;
		}
		//the pocket it falls into, a bit wider the further down
		int depth = 5 + Random.Int(3);
		for (int k = 0; k < depth; k++){
			int grow = Math.min(1, k / 2);
			for (int x = x0 - 1 - grow; x < x0 + 7 + grow; x++){
				alt[cell(x, y + 3 + k)] = 0;
			}
		}
		for (int yy = y - 1; yy < y + 3; yy++){
			for (int x = x0; x < x0 + 6; x++){
				locked[cell(x, yy)] = true;
				taken[cell(x, yy)] = true;
			}
		}
		marks.add(new Landmark(Landmark.CLIFF, x0, y - 1));
	}

	// *** where you can walk ***

	private boolean face( int x, int y ){
		return in(x, y) && y > 0 && a(x, y - 1) > a(x, y);
	}

	//the very edge of something high can't be stood on: the drop is right there
	private boolean edge( int x, int y ){
		int h = a(x, y);
		if (h == 0) return false;
		return a(x, y - 1) < h || a(x + 1, y) < h || a(x - 1, y) < h
				|| a(x + 1, y - 1) < h || a(x - 1, y - 1) < h;
	}

	private void walkable(){
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				int c = cell(x, y);
				open[c] = !out[c] && !(face(x, y) && ramp[c] == 0) && !edge(x, y)
						&& x > 0 && y > 0 && x < W - 1 && y < H - 1
						&& !(biome.water && alt[c] == 0);
			}
		}
		for (Landmark m : marks){
			for (int y = m.y; y < m.y + m.height(); y++){
				for (int x = m.x; x < m.x + m.width(); x++){
					char s = m.step(x, y);
					if (s == '#' || s == 'L') open[cell(x, y)] = false;
					else if (s == 'B') open[cell(x, y)] = true;
				}
			}
		}
		if (portal != -1) open[portal] = true;
		open[home] = true;
	}

	private int[] label(){
		int[] comp = new int[W * H];
		Arrays.fill(comp, -1);
		int[] queue = new int[W * H];
		int n = 0;
		for (int start = 0; start < W * H; start++){
			if (!open[start] || comp[start] != -1) continue;
			int head = 0, tail = 0;
			queue[tail++] = start;
			comp[start] = n;
			while (head < tail){
				int c = queue[head++];
				int x = c % W, y = c / W;
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						if (!in(x + dx, y + dy)) continue;
						int nc = cell(x + dx, y + dy);
						if (open[nc] && comp[nc] == -1){
							comp[nc] = n;
							queue[tail++] = nc;
						}
					}
				}
			}
			n++;
		}
		return comp;
	}

	//a face where a ramp can go: straight (the step goes on at both sides), not under anything,
	//with ground to stand on above and below
	private boolean rampable( int x, int y ){
		if (!face(x, y) || ramp[cell(x, y)] != 0 || taken[cell(x, y)]) return false;
		int lo = a(x, y), hi = a(x, y - 1);
		for (int dx = -1; dx <= 1; dx += 2){
			if (!face(x + dx, y) || a(x + dx, y) != lo || a(x + dx, y - 1) != hi) return false;
		}
		return open[cell(x, y - 1)] && open[cell(x, y + 1)];
	}

	private int rampKind( int c ){
		return road[c - W] || road[c - 2 * W] ? AshSurface.STEPS : AshSurface.SLIDE;
	}

	private void connect(){
		int bays = 0;
		for (int round = 0; round < 80; round++){
			int[] comp = label();
			int main = comp[home];
			int count = 0;
			for (int c : comp) count = Math.max(count, c + 1);
			ArrayList<ArrayList<Integer>> options = new ArrayList<>();
			for (int i = 0; i < count; i++) options.add(new ArrayList<Integer>());
			boolean any = false;
			for (int y = 2; y < H - 2; y++){
				for (int x = 1; x < W - 1; x++){
					if (!rampable(x, y)) continue;
					int up = comp[cell(x, y - 1)], down = comp[cell(x, y + 1)];
					if (up == down) continue;
					if (up == main) options.get(down).add(cell(x, y));
					else if (down == main) options.get(up).add(cell(x, y));
					else continue;
					any = true;
				}
			}
			if (any){
				for (ArrayList<Integer> o : options){
					if (o.isEmpty()) continue;
					int c = o.get(Random.Int(o.size()));
					ramp[c] = rampKind(c);
					open[c] = true;
				}
			} else if (bay(comp, main, count)){
				walkable();
				bays++;
			} else {
				break;
			}
		}
		//whatever is still cut off is just seen from above
		int[] comp = label();
		int main = comp[home];
		int lost = 0;
		for (int c = 0; c < W * H; c++){
			if (open[c] && comp[c] != main){
				open[c] = false;
				lost++;
			}
		}
		int ramps = 0;
		for (int r : ramp) if (r != 0) ramps++;
		CoopLog.line(biome.key + " surface: " + ramps + " ramps, " + bays + " bays, " + lost + " cells out of reach");
	}

	//when no face joins a lower patch to the rest (the higher ground is beside it, not above
	//it), a little bay of its height is dug into the higher ground, with a slope at the back
	private boolean bay( int[] comp, int main, int count ){
		int[] size = new int[count];
		for (int c : comp) if (c >= 0) size[c]++;
		for (int tries = 0; tries < count; tries++){
			int best = -1;
			for (int k = 0; k < count; k++){
				if (k != main && size[k] >= 20 && (best == -1 || size[k] > size[best])) best = k;
			}
			if (best == -1) return false;
			ArrayList<Integer> cells = new ArrayList<>();
			for (int c = 0; c < W * H; c++) if (comp[c] == best) cells.add(c);
			Random.shuffle(cells);
			for (int c : cells){
				for (int dir = -1; dir <= 1; dir += 2){
					if (digBay(comp, main, c % W, c / W, dir)) return true;
				}
			}
			size[best] = 0;
		}
		return false;
	}

	//(cx,cy) is a cell of the cut off patch, the higher ground is at its side dir (-1 west, 1 east)
	private boolean digBay( int[] comp, int main, int cx, int cy, int dir ){
		int lo = a(cx, cy), hi = lo + 1;
		if (hi > 2 || a(cx + dir, cy) != hi) return false;
		int x0 = dir < 0 ? cx - 5 : cx + 1, x1 = x0 + 4;
		int rx = x0 + 2, top = cy - 1;
		if (x0 < 4 || x1 >= W - 4 || top < 4 || cy + 3 >= H - 4) return false;
		for (int y = top; y <= cy + 2; y++){
			for (int x = x0; x <= x1; x++){
				int c = cell(x, y);
				if (alt[c] != hi || locked[c] || taken[c] || road[c] || out[c] || ramp[c] != 0) return false;
			}
		}
		for (int y = top - 2; y < top; y++){
			for (int x = rx - 1; x <= rx + 1; x++){
				if (alt[cell(x, y)] != hi) return false;
			}
		}
		if (comp[cell(rx, top - 1)] != main) return false;
		if (comp[cell(cx, cy)] == main || comp[cell(cx, cy + 1)] != comp[cell(cx, cy)]) return false;
		if (lo == 0){
			for (int y = top - 3; y <= cy + 5; y++){
				for (int x = x0 - 3; x <= x1 + 3; x++){
					if (in(x, y) && alt[cell(x, y)] == 2) return false;
				}
			}
		}
		for (int y = top; y <= cy + 2; y++){
			for (int x = x0; x <= x1; x++){
				alt[cell(x, y)] = lo;
				locked[cell(x, y)] = true;
			}
		}
		ramp[cell(rx, top)] = AshSurface.SLIDE;
		return true;
	}

	//a few more ramps, so there's more than one way up from every hollow
	private void moreRamps(){
		ArrayList<Integer> faces = new ArrayList<>();
		for (int y = 2; y < H - 2; y++){
			for (int x = 1; x < W - 1; x++){
				if (rampable(x, y)) faces.add(cell(x, y));
			}
		}
		Random.shuffle(faces);
		ArrayList<Integer> ramps = new ArrayList<>();
		for (int c = 0; c < W * H; c++){
			if (ramp[c] != 0) ramps.add(c);
		}
		int added = 0;
		for (int c : faces){
			if (added >= 24) break;
			boolean crowded = false;
			for (int r : ramps){
				if (Math.abs(r % W - c % W) < 10 && Math.abs(r / W - c / W) < 10) crowded = true;
			}
			if (crowded) continue;
			ramp[c] = rampKind(c);
			open[c] = true;
			ramps.add(c);
			added++;
		}
	}

	// *** into the level ***

	private void fill(){
		//-Dspd.test.landdump=1: the layout in the log, one character per cell
		if (DevFlags.landDump()){
			StringBuilder sb = new StringBuilder();
			for (int y = 0; y < H; y++){
				sb.append("|");
				for (int x = 0; x < W; x++){
					int c = cell(x, y);
					char ch;
					if (out[c]) ch = ' ';
					else if (ramp[c] != 0) ch = 'R';
					else if (road[c]) ch = '=';
					else if (face(x, y)) ch = '_';
					else if (!open[c]) ch = '#';
					else ch = (char)('0' + alt[c]);
					sb.append(ch);
				}
			}
			CoopLog.line("DUMP" + sb);
		}
		l.setUp(W, H);
		int[] land = new int[W * H];
		for (int c = 0; c < W * H; c++){
			land[c] = AshSurface.pack(alt[c], road[c], out[c], ramp[c], cross[c]);
		}
		l.land = land;
		l.marks = new ArrayList<>(marks);
		l.portal = portal;
		l.shrine = shrine;
		l.passes = pass.clone();
		for (int c = 0; c < W * H; c++){
			l.map[c] = open[c] ? Terrain.EMPTY : Terrain.CUSTOM_DECO;
		}
	}
}
