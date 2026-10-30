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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.SacredEssence;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.WndWorldMap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Group;
import com.watabou.noosa.Halo;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//the abyss of a biome, down the rope from its chasm: a cavern so big the colossi walk in it,
//halls joined by wide galleries, lakes of black lava (of water, away from the ash). at its
//farthest point lies the heart of the abyss, and some runs a drop of sacred essence with it
public class AshAbyss extends AshUnder {

	private static final int W = 140, H = 110;

	//the deepest point, where the essence of the abyss lies (if this run it has one)
	public int heart = -1;

	@Override
	public int layer() {
		return ABYSS;
	}

	@Override
	protected String upKey() {
		return "rope";
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		int seed = Random.Int();

		//big halls that may run into each other, so it reads as one cavern and not as rooms
		ArrayList<int[]> halls = new ArrayList<>();
		for (int tries = 0; tries < 600 && halls.size() < 8; tries++){
			int r = Random.IntRange(11, 18);
			int x = Random.IntRange(r + 5, W - r - 6), y = Random.IntRange(r + 5, H - r - 6);
			boolean room = true;
			for (int[] h : halls){
				if (Math.hypot(h[0] - x, h[1] - y) < (h[2] + r) * 0.8f) room = false;
			}
			if (room) halls.add(new int[]{x, y, r});
		}
		if (halls.size() < 5) return false;

		boolean[] floor = new boolean[W * H];
		for (int[] h : halls){
			for (int y = h[1] - h[2] - 4; y <= h[1] + h[2] + 4; y++){
				for (int x = h[0] - h[2] - 4; x <= h[0] + h[2] + 4; x++){
					if (x < 3 || y < 3 || x >= W - 3 || y >= H - 3) continue;
					double d = Math.hypot(x - h[0], y - h[1]);
					if (d < h[2] + 3f * LandNoise.wobble(seed, x, y, 6)) floor[x + y * W] = true;
				}
			}
		}

		//galleries: a tree joining the halls by distance, plus two more
		int n = halls.size();
		boolean[] joined = new boolean[n];
		joined[0] = true;
		for (int k = 1; k < n; k++){
			int ba = -1, bb = -1;
			double bd = Double.MAX_VALUE;
			for (int a = 0; a < n; a++){
				if (!joined[a]) continue;
				for (int b = 0; b < n; b++){
					if (joined[b]) continue;
					double d = Math.hypot(halls.get(a)[0] - halls.get(b)[0], halls.get(a)[1] - halls.get(b)[1]);
					if (d < bd){
						bd = d;
						ba = a;
						bb = b;
					}
				}
			}
			joined[bb] = true;
			gallery(floor, halls.get(ba), halls.get(bb));
		}
		for (int k = 0; k < 2; k++){
			gallery(floor, Random.element(halls), Random.element(halls));
		}

		//smoothed twice so the walls look worn
		for (int pass = 0; pass < 2; pass++){
			boolean[] next = floor.clone();
			for (int y = 2; y < H - 2; y++){
				for (int x = 2; x < W - 2; x++){
					int rock = 0;
					for (int dy = -1; dy <= 1; dy++){
						for (int dx = -1; dx <= 1; dx++){
							if (!floor[x + dx + (y + dy) * W]) rock++;
						}
					}
					next[x + y * W] = rock < 5;
				}
			}
			floor = next;
		}
		keepLargest(floor);

		for (int i = 0; i < W * H; i++) map[i] = floor[i] ? FLOOR : ROCK;

		//the rope hangs in the hall nearest the top
		int[] top = halls.get(0);
		for (int[] h : halls) if (h[1] - h[2] < top[1] - top[2]) top = h;
		up = nearestFloor(top[0], top[1] - top[2] + 3);
		if (up == -1) return false;

		//lakes of black lava off to one side of some halls, never in the hall of the rope
		for (int[] h : halls){
			if (h == top || Random.Int(3) != 0) continue;
			double side = Random.Float() * Math.PI * 2;
			int lx = h[0] + (int)Math.round(Math.cos(side) * h[2] * 0.55f);
			int ly = h[1] + (int)Math.round(Math.sin(side) * h[2] * 0.55f);
			float lr = h[2] * 0.3f;
			for (int y = ly - h[2]; y <= ly + h[2]; y++){
				for (int x = lx - h[2]; x <= lx + h[2]; x++){
					if (x < 2 || y < 2 || x >= W - 2 || y >= H - 2) continue;
					int c = x + y * W;
					if (map[c] == FLOOR && Math.hypot(x - lx, y - ly) < lr + 2f * LandNoise.wobble(seed + 5, x, y, 4)){
						map[c] = LAVA;
					}
				}
			}
		}

		//a few basalt columns near the walls of the halls, the middle stays open
		for (int[] h : halls){
			int cols = 2 + Random.Int(3);
			for (int k = 0; k < cols; k++){
				double ang = Random.Float() * Math.PI * 2;
				double rad = h[2] * (0.6f + 0.25f * Random.Float());
				int x = h[0] + (int)Math.round(Math.cos(ang) * rad), y = h[1] + (int)Math.round(Math.sin(ang) * rad);
				int c = x + y * W;
				if (c > W && c < W * H - W && map[c] == FLOOR) map[c] = Terrain.REGION_DECO;
			}
		}

		decorate();
		useSheet();
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -1; dx <= 1; dx++){
				int c = up + dx + dy * W;
				if (map[c] == LAVA || map[c] == Terrain.REGION_DECO) map[c] = FLOOR;
			}
		}
		map[up] = Terrain.EMPTY_SP;
		CoopLog.line(biome().key + " abyss: rope at " + (up % W) + "," + (up / W) + ", " + halls.size() + " halls");
		transitions.add(new LevelTransition(this, up, LevelTransition.Type.BRANCH_ENTRANCE,
				21, biome().branch, LevelTransition.Type.BRANCH_EXIT));
		return true;
	}

	//a wide gallery that bends a little, from one hall to another
	private void gallery( boolean[] floor, int[] a, int[] b ){
		if (a == b) return;
		int r = 3 + Random.Int(2);
		double mx = (a[0] + b[0]) / 2.0, my = (a[1] + b[1]) / 2.0;
		double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
		double bend = Random.IntRange(-10, 10);
		mx += -(b[1] - a[1]) / len * bend;
		my += (b[0] - a[0]) / len * bend;
		int steps = (int)(len * 2) + 1;
		for (int s = 0; s <= steps; s++){
			double t = s / (double)steps;
			double x = (1 - t) * (1 - t) * a[0] + 2 * (1 - t) * t * mx + t * t * b[0];
			double y = (1 - t) * (1 - t) * a[1] + 2 * (1 - t) * t * my + t * t * b[1];
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (dx * dx + dy * dy > r * r + 1) continue;
					int cx = (int)Math.round(x) + dx, cy = (int)Math.round(y) + dy;
					if (cx >= 3 && cy >= 3 && cx < W - 3 && cy < H - 3) floor[cx + cy * W] = true;
				}
			}
		}
	}

	private void keepLargest( boolean[] floor ){
		int[] comp = new int[W * H];
		Arrays.fill(comp, -1);
		int[] queue = new int[W * H];
		int best = -1, bestSize = 0, n = 0;
		for (int s = 0; s < W * H; s++){
			if (!floor[s] || comp[s] != -1) continue;
			int head = 0, tail = 0;
			queue[tail++] = s;
			comp[s] = n;
			while (head < tail){
				int c = queue[head++];
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int nc = c + dx + dy * W;
						if (nc >= 0 && nc < W * H && floor[nc] && comp[nc] == -1){
							comp[nc] = n;
							queue[tail++] = nc;
						}
					}
				}
			}
			if (tail > bestSize){
				bestSize = tail;
				best = n;
			}
			n++;
		}
		for (int i = 0; i < W * H; i++) floor[i] = floor[i] && comp[i] == best;
	}

	private int nearestFloor( int x, int y ){
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int i = 0; i < W * H; i++){
			if (map[i] != FLOOR) continue;
			int d = Math.abs(i % W - x) + Math.abs(i / W - y);
			if (d < bestD){
				bestD = d;
				best = i;
			}
		}
		return best;
	}

	@Override
	protected float[] buildDanger() {
		return walkDanger(list(up), 0.5f, 0.5f);
	}

	@Override
	protected void createItems() {
		//the heart: the walkable cell farthest from the rope
		int[] dist = walkDistance(list(up));
		int far = 0;
		for (int i = 0; i < length(); i++){
			if (dist[i] > dist[far] && map[i] == FLOOR) far = i;
		}
		heart = far;
		//the isles have none: what waits down there is something else
		int source = Continent.source(biome().zone, Continent.ABYSS);
		boolean essence = source >= 0 && Continent.essenceAt(source);
		CoopLog.line(biome().key + " abyss: heart at " + (heart % W) + "," + (heart / W)
				+ (essence ? ", with essence" : ", dry"));
		if (essence){
			drop(new SacredEssence(source), heart);
		} else {
			drop(new Gold().random(), heart);
		}

		ArrayList<Integer> cells = dropCells(0.6f);
		int n = 5 + Random.Int(3);
		for (int i = 0; i < n; i++) dropAround(scrap(), cells);
		dropAround(new Torch(), cells);
		for (int i = 0; i < 3 && !cells.isEmpty(); i++){
			Item it = scrap();
			if (it == null) continue;
			Heap h = drop(it, cells.remove(Random.Int(cells.size())));
			h.type = Heap.Type.SKELETON;
		}
	}

	@Override
	public Group addWallVisuals() {
		super.addWallVisuals();
		float s = DungeonTilemap.SIZE;
		if (heart != -1){
			wallVisuals.add(new Halo(26, 0xA02810, 0.35f).point((heart % width() + 0.5f) * s, (heart / width() + 0.5f) * s));
		}
		return wallVisuals;
	}

	@Override
	public void marks( ArrayList<WndWorldMap.Mark> out ) {
		super.marks(out);
		if (heart != -1){
			out.add(new WndWorldMap.Mark(heart % width() - 2, heart / width() - 2, 5, 5, 0xFFE04020,
					Messages.get(AshAbyss.class, "heart")));
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("heart", heart);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		heart = bundle.getInt("heart");
		super.restoreFromBundle(bundle);
	}
}
