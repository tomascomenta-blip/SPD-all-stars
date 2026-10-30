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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//"the broken entrance": the meadow outside the dungeon, under a cracked sky.
//oval of grass and old flagstones, the dungeon's mouth at the bottom, and the top of the map is
//all sky, which is where he is. never explored, never respawns, no stairs
public class RiftLevel extends Level {

	public static final int W = 27, H = 31;
	public static final int CX = 13, CY = 19;

	//act being fought, for the floor/visuals. 0 intro, 1-5 the worlds, 6 the end of time, 7 dead
	public int act = 0;

	//melted clock drips, turns left per cell
	public int[] puddles;
	//time bubbles: cell, radius, turns, kind (0 slow, 1 fast)
	public ArrayList<int[]> bubbles = new ArrayList<>();

	{
		color1 = 0x2a3a5a;
		color2 = 0x6a5a3a;
	}

	public static int cell( int x, int y ){
		return x + y * W;
	}

	public static boolean floorShape( int x, int y ){
		if (x <= 1 || y <= 1 || x >= W - 2 || y >= H - 2) return false;
		float dx = (x - CX) / 10.6f, dy = (y - CY) / 8.6f;
		return dx*dx + dy*dy <= 1f;
	}

	public boolean floor( int cell ){
		if (cell < 0 || cell >= length()) return false;
		return floorShape(cell % W, cell / W);
	}

	public static int arrivalCell(){ return cell(CX, 26); }
	public static int mouthCell(){ return cell(CX, 28); }
	public static int farCell(){ return cell(CX, 3); }
	public static int coreCell(){ return cell(CX, 17); }
	public static int[] portalCells(){
		return new int[]{ cell(CX, 12), cell(5, 18), cell(21, 18) };
	}

	@Override
	public void playLevelMusic() {
		if (act >= 7){
			Music.INSTANCE.play(Assets.Music.THEME_FINALE, true);
		} else if (act == 6){
			Music.INSTANCE.play(Assets.Music.HALLS_BOSS_FINALE, true);
		} else {
			Music.INSTANCE.play(Assets.Music.HALLS_BOSS, true);
		}
	}

	@Override
	public String tilesTex() {
		return FinaleAssets.TILES;
	}

	@Override
	public String waterTex() {
		return SKAssets.WATER_FOREST;
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		Arrays.fill(map, Terrain.CHASM);
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				if (!floorShape(x, y)) continue;
				int c = cell(x, y);
				//old flagstones on a cross, grass everywhere else
				boolean path = Math.abs(x - CX) <= 1 || (y == CY && Math.abs(x - CX) <= 7);
				map[c] = path ? Terrain.EMPTY_SP : (Random.Int(5) == 0 ? Terrain.GRASS : Terrain.EMPTY);
			}
		}
		map[cell(CX, 27)] = Terrain.EMPTY_SP;
		map[mouthCell()] = Terrain.ENTRANCE;
		transitions.add(new LevelTransition(this, arrivalCell(), LevelTransition.Type.BRANCH_ENTRANCE,
				1, 0, LevelTransition.Type.BRANCH_EXIT));
		puddles = new int[length()];
		viewDistance = 20;
		return true;
	}

	//nothing to take, the way out is the portal he leaves behind
	@Override
	public LevelTransition getTransition( int cell ) {
		return null;
	}

	@Override
	public void cleanWalls() {
		super.cleanWalls();
		for (int i = 0; i < length(); i++){
			visited[i] = true;
		}
	}

	@Override
	public void buildFlagMaps() {
		super.buildFlagMaps();
		for (int i = 0; i < length(); i++) flags(i);
	}

	@Override
	public void updateCellFlags( int cell ) {
		super.updateCellFlags(cell);
		flags(cell);
	}

	//the sky is a big hole but you can't fall in it, it just isn't there
	private void flags( int i ){
		boolean f = floor(i) || i == mouthCell();
		int x = i % W, y = i / W;
		//the outer ring stops the shadow caster, it doesn't check the map bounds
		boolean edge = x == 0 || y == 0 || x == W - 1 || y == H - 1;
		passable[i] = f && i != mouthCell();
		avoid[i] = false;
		solid[i] = edge;
		losBlocking[i] = edge;
		pit[i] = false;
		flamable[i] = f && map[i] == Terrain.GRASS;
		water[i] = false;
		openSpace[i] = f;
	}

	@Override
	public boolean invalidHeroPos( int tile ) {
		return !floor(tile);
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
		ThalVekhorr t = new ThalVekhorr();
		t.pos = farCell();
		mobs.add(t);
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
	}

	public ArrayList<Integer> floorCells(){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < length(); i++){
			if (floor(i)) cells.add(i);
		}
		return cells;
	}

	public int freeFloorNear( int cell, int maxDist ){
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int i : floorCells()){
			if (Actor.findChar(i) != null) continue;
			int d = distance(i, cell);
			if (d <= maxDist && d < bestD){
				best = i;
				bestD = d;
			}
		}
		return best;
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		ArrayList<Integer> cells = floorCells();
		for (int i = 0; i < 60 && !cells.isEmpty(); i++){
			int c = cells.get(Random.Int(cells.size()));
			if (Actor.findChar(c) == null) return c;
		}
		return -1;
	}

	// *** drips and bubbles ***

	public boolean puddle( int cell ){
		return puddles != null && cell >= 0 && cell < puddles.length && puddles[cell] > 0;
	}

	public void addPuddle( int cell, int turns ){
		if (floor(cell)) puddles[cell] = Math.max(puddles[cell], turns);
	}

	//-1 none, else the kind
	public int bubbleAt( int cell ){
		for (int[] b : bubbles){
			if (distance(cell, b[0]) <= b[1]) return b[3];
		}
		return -1;
	}

	public void tickField(){
		for (int i = 0; i < puddles.length; i++){
			if (puddles[i] > 0) puddles[i]--;
		}
		for (int i = bubbles.size() - 1; i >= 0; i--){
			if (--bubbles.get(i)[2] <= 0) bubbles.remove(i);
		}
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell(ch);
		if (puddle(ch.pos)){
			puddles[ch.pos] = 0;
			ThalVekhorr.steppedInPuddle(ch);
		}
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		visuals.add(new ThalVisuals.Low(this));
		return visuals;
	}

	@Override
	public Group addWallVisuals() {
		super.addWallVisuals();
		wallVisuals.add(new ThalVisuals.High(this));
		return wallVisuals;
	}

	@Override
	public String tileName( int tile ) {
		if (tile == Terrain.CHASM) return Messages.get(RiftLevel.class, "sky_name");
		if (tile == Terrain.ENTRANCE) return Messages.get(RiftLevel.class, "mouth_name");
		return Messages.get(RiftLevel.class, "floor_name");
	}

	@Override
	public String tileDesc( int tile ) {
		if (tile == Terrain.CHASM) return Messages.get(RiftLevel.class, "sky_desc");
		if (tile == Terrain.ENTRANCE) return Messages.get(RiftLevel.class, "mouth_desc");
		return Messages.get(RiftLevel.class, "floor_desc");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("act", act);
		bundle.put("puddles", puddles);
		int[] flat = new int[bubbles.size() * 4];
		for (int i = 0; i < bubbles.size(); i++){
			System.arraycopy(bubbles.get(i), 0, flat, i * 4, 4);
		}
		bundle.put("bubbles", flat);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		act = bundle.getInt("act");
		puddles = bundle.contains("puddles") ? bundle.getIntArray("puddles") : new int[length()];
		bubbles.clear();
		if (bundle.contains("bubbles")){
			int[] flat = bundle.getIntArray("bubbles");
			for (int i = 0; i + 3 < flat.length; i += 4){
				bubbles.add(new int[]{flat[i], flat[i+1], flat[i+2], flat[i+3]});
			}
		}
		viewDistance = 20;
	}
}
