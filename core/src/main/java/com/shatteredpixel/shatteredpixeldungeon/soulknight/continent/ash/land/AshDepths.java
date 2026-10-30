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

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the tunnels under a biome, down through its cave (the skull's mouth, on the ash plains): a maze
//of narrow tunnels, knots of rock and the odd hollow with black lava, or water away from the
//ash. the further from the stairs, the worse
public class AshDepths extends AshUnder {

	private static final int W = 120, H = 96, STEP = 4, EDGE = 4;

	@Override
	public int layer() {
		return DEPTHS;
	}

	@Override
	protected String upKey() {
		return caveKey();
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		int cols = (W - 2 * EDGE) / STEP + 1, rows = (H - 2 * EDGE) / STEP + 1;
		int nodes = cols * rows;

		//a maze over the knots (every corridor between two of them), then some walls knocked
		//down so there's more than one way around
		boolean[] right = new boolean[nodes], down = new boolean[nodes];
		boolean[] seen = new boolean[nodes];
		int[] stack = new int[nodes];
		int sp = 0;
		int start = Random.Int(nodes);
		seen[start] = true;
		stack[sp++] = start;
		while (sp > 0){
			int n = stack[sp - 1];
			int i = n % cols, j = n / cols;
			ArrayList<Integer> next = new ArrayList<>();
			if (i > 0 && !seen[n - 1]) next.add(n - 1);
			if (i < cols - 1 && !seen[n + 1]) next.add(n + 1);
			if (j > 0 && !seen[n - cols]) next.add(n - cols);
			if (j < rows - 1 && !seen[n + cols]) next.add(n + cols);
			if (next.isEmpty()){
				sp--;
				continue;
			}
			int m = Random.element(next);
			link(right, down, cols, n, m);
			seen[m] = true;
			stack[sp++] = m;
		}
		for (int n = 0; n < nodes; n++){
			if (n % cols < cols - 1 && !right[n] && Random.Int(8) == 0) right[n] = true;
			if (n / cols < rows - 1 && !down[n] && Random.Int(8) == 0) down[n] = true;
		}

		for (int n = 0; n < nodes; n++){
			int x = EDGE + (n % cols) * STEP, y = EDGE + (n / cols) * STEP;
			carve(x, y);
			if (right[n]) tunnel(x, y, x + STEP, y, true);
			if (down[n]) tunnel(x, y, x, y + STEP, false);
		}

		//hollows on some knots, a few with lava where the tunnels don't pass
		for (int n = 0; n < nodes; n++){
			int x = EDGE + (n % cols) * STEP, y = EDGE + (n / cols) * STEP;
			float r = Random.Float();
			if (r < 0.06f) hollow(x, y, 2);
			else if (r < 0.085f) hollow(x, y, 3);
		}

		erode();
		decorate();
		useSheet();

		//the stairs up into the skull, the knot nearest the middle of the top
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int n = 0; n < nodes; n++){
			int x = EDGE + (n % cols) * STEP, y = EDGE + (n / cols) * STEP;
			int d = Math.abs(x - W / 2) + Math.abs(y - EDGE) * 3;
			if (d < bestD){
				bestD = d;
				best = x + y * W;
			}
		}
		up = best;
		map[up] = Terrain.ENTRANCE;
		CoopLog.line(biome().key + " depths: stairs up at " + (up % W) + "," + (up / W));
		for (int i : new int[]{up - 1, up + 1, up - W, up + W}){
			if (map[i] == LAVA || map[i] == Terrain.REGION_DECO) map[i] = FLOOR;
		}
		transitions.add(new LevelTransition(this, up, LevelTransition.Type.REGULAR_ENTRANCE,
				21, biome().branch, LevelTransition.Type.REGULAR_EXIT));
		return true;
	}

	private static void link( boolean[] right, boolean[] down, int cols, int a, int b ){
		int lo = Math.min(a, b), hi = Math.max(a, b);
		if (hi - lo == 1) right[lo] = true;
		else down[lo] = true;
	}

	private void carve( int x, int y ){
		if (x > 1 && y > 1 && x < W - 2 && y < H - 2) map[x + y * W] = FLOOR;
	}

	//one cell wide, sometimes two, sometimes with a kink halfway
	private void tunnel( int x0, int y0, int x1, int y1, boolean across ){
		int kink = Random.Int(4) == 0 ? (Random.Int(2) == 0 ? -1 : 1) : 0;
		boolean wide = Random.Int(9) == 0;
		int len = across ? x1 - x0 : y1 - y0;
		for (int k = 0; k <= len; k++){
			int off = (k > 0 && k < len) ? kink : 0;
			int x = across ? x0 + k : x0 + off;
			int y = across ? y0 + off : y0 + k;
			carve(x, y);
			if (off != 0){
				//join the kink to the straight part
				carve(across ? x : x0, across ? y0 : y);
			}
			if (wide) carve(across ? x : x + 1, across ? y + 1 : y);
		}
	}

	private void hollow( int cx, int cy, int r ){
		for (int y = cy - r; y <= cy + r; y++){
			for (int x = cx - r; x <= cx + r; x++){
				if (Math.abs(x - cx) == r && Math.abs(y - cy) == r) continue;
				carve(x, y);
			}
		}
		if (r >= 2 && Random.Int(3) == 0){
			//a puddle of black lava off to one side
			int lx = cx + (Random.Int(2) == 0 ? -1 : 1) * (r - 1), ly = cy + (Random.Int(2) == 0 ? -1 : 1) * (r - 1);
			for (int y = ly - 1; y <= ly; y++){
				for (int x = lx - 1; x <= lx; x++){
					if (x != cx && y != cy && map[x + y * W] == FLOOR) map[x + y * W] = LAVA;
				}
			}
		} else if (r == 3 && Random.Int(2) == 0){
			map[cx - 1 + (cy - 1) * W] = Terrain.REGION_DECO;
			map[cx + 1 + (cy + 1) * W] = Terrain.REGION_DECO;
		}
	}

	//rock with floor almost all around falls in, so the maze looks dug and not drawn
	private void erode(){
		int[] next = map.clone();
		for (int y = 2; y < H - 2; y++){
			for (int x = 2; x < W - 2; x++){
				int c = x + y * W;
				if (map[c] != ROCK) continue;
				int open = 0;
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						if (map[c + dx + dy * W] != ROCK) open++;
					}
				}
				if (open >= 6 && Random.Int(2) == 0) next[c] = FLOOR;
			}
		}
		map = next;
	}

	@Override
	protected float[] buildDanger() {
		return walkDanger(list(up), 0.15f, 0.85f);
	}

	@Override
	protected void createItems() {
		ArrayList<Integer> cells = dropCells(0.3f);
		int n = 4 + Random.Int(3);
		for (int i = 0; i < n; i++) dropAround(scrap(), cells);
		dropAround(new Torch(), cells);
		dropAround(new Gold().random(), cells);
		ArrayList<Integer> far = dropCells(0.7f);
		for (int i = 0; i < 2 && !far.isEmpty(); i++){
			Item it = scrap();
			if (it == null) continue;
			Heap h = drop(it, far.remove(Random.Int(far.size())));
			h.type = Heap.Type.SKELETON;
		}
	}
}
