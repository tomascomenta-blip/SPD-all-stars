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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Shrine;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.TaintedFood;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.EdgeTransition;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.WndWorldMap;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.FinaleAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalParts;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//the surface of a biome: one huge map with no stairs. on the ash plains the portal opens on its
//south edge and the old slab road crosses it to the north, to a pass in the ash storm. the other
//biomes are reached by walking out through the passes on the edges. the two ways down are the
//cave (the skull, on the ash) to the tunnels and the abyss (down the rope); cliffs with rope
//ladders and ramps on the faces of the terraces are the only ways between heights
public class AshSurface extends AshLand implements SurfaceTiles.Land {

	public static final int STEPS = 1, SLIDE = 2;

	//per cell: height (2 bits), road, fog, ramp (2 bits), crossroads slab
	private static final int ALT = 3, ROAD = 4, OUT = 8, RAMP_SHIFT = 4, RAMP = 48, CROSS = 64;

	int[] land;
	int tileSeed;
	ArrayList<Landmark> marks = new ArrayList<>();
	int portal = -1, shrine = -1;
	//where each edge has its pass, -1 none (see Biome.pass)
	int[] passes = {-1, -1, -1, -1};

	private SurfaceTiles tiles;
	private int[] fogDepth;

	@Override
	public int layer() {
		return SURFACE;
	}

	static int pack( int alt, boolean road, boolean out, int ramp, boolean cross ){
		return alt | (road ? ROAD : 0) | (out ? OUT : 0) | (ramp << RAMP_SHIFT) | (cross ? CROSS : 0);
	}

	void setUp( int w, int h ){
		setSize(w, h);
	}

	@Override
	public int seed(){
		return tileSeed;
	}

	@Override
	public int alt( int x, int y ){
		x = Math.max(0, Math.min(width() - 1, x));
		y = Math.max(0, Math.min(height() - 1, y));
		return land[x + y * width()] & ALT;
	}

	@Override
	public boolean road( int cell ){
		return (land[cell] & ROAD) != 0;
	}

	public boolean out( int cell ){
		return (land[cell] & OUT) != 0;
	}

	@Override
	public int ramp( int cell ){
		return (land[cell] & RAMP) >> RAMP_SHIFT;
	}

	@Override
	public boolean cross( int cell ){
		return (land[cell] & CROSS) != 0;
	}

	@Override
	public boolean windy( int x, int y ){
		float n = (2 * LandNoise.value(tileSeed + 3, x, y, 5) + LandNoise.value(tileSeed + 7, x, y, 2.5f)) / 3f;
		return n > 0.62f;
	}

	@Override
	SurfaceTiles tiles(){
		if (tiles == null) tiles = SurfaceTiles.of(this);
		return tiles;
	}

	//how far into the fog a cell is: 0 inside the plains
	int fog( int cell ){
		if (fogDepth == null){
			fogDepth = new int[length()];
			int[] queue = new int[length()];
			int head = 0, tail = 0;
			for (int i = 0; i < length(); i++){
				if (out(i)) fogDepth[i] = -1;
				else queue[tail++] = i;
			}
			while (head < tail){
				int c = queue[head++];
				int x = c % width(), y = c / width();
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int nx = x + dx, ny = y + dy;
						if (nx < 0 || ny < 0 || nx >= width() || ny >= height()) continue;
						int n = nx + ny * width();
						if (fogDepth[n] == -1){
							fogDepth[n] = fogDepth[c] + 1;
							queue[tail++] = n;
						}
					}
				}
			}
		}
		return Math.max(0, fogDepth[cell]);
	}

	public Landmark landmarkAt( int cell ){
		int x = cell % width(), y = cell / width();
		for (Landmark m : marks){
			if (m.inside(x, y)) return m;
		}
		return null;
	}

	public Landmark find( int kind ){
		for (Landmark m : marks){
			if (m.kind == kind) return m;
		}
		return null;
	}

	// *** building it ***

	@Override
	protected boolean build() {
		if (!new SurfaceBuilder(this).build()) return false;
		tiles = null;
		fogDepth = null;

		Biome b = biome();
		if (portal != -1){
			map[portal] = Terrain.ENTRANCE;
			transitions.add(new LevelTransition(this, portal, LevelTransition.Type.REGULAR_ENTRANCE));
		}

		Landmark cave = b.cave == -1 ? null : find(b.cave);
		if (cave != null){
			//the mouth: the row of ways down at the bottom of the drawing
			int[] box = cave.ways();
			int mouth = box[0] + box[3] * width();
			for (int x = box[0]; x <= box[2]; x++){
				if (cave.step(x, box[3]) == 'B') map[x + box[3] * width()] = Terrain.EXIT;
			}
			while (map[mouth] != Terrain.EXIT) mouth++;
			LevelTransition down = new LevelTransition(this, mouth, LevelTransition.Type.REGULAR_EXIT,
					22, b.branch, LevelTransition.Type.REGULAR_ENTRANCE);
			down.set(box[0], box[1], box[2], box[3]);
			transitions.add(down);
		}

		Landmark chasm = find(b.chasm);
		if (chasm == null && b == Biome.ASH) chasm = find(Landmark.CHASM);
		//they come back up by the stake with the rope, right below the hole
		int stake = chasm.x + 6 + (chasm.y + 3) * width();
		LevelTransition abyss = new LevelTransition(this, stake, LevelTransition.Type.BRANCH_EXIT,
				23, b.branch, LevelTransition.Type.BRANCH_ENTRANCE);
		abyss.set(chasm.x + 1, chasm.y + 1, chasm.x + 6, chasm.y + 2);
		transitions.add(abyss);
		if (chasm.step(chasm.x + 4, chasm.y) == 'B'){
			LevelTransition lip = new LevelTransition(this, stake, LevelTransition.Type.BRANCH_EXIT,
					23, b.branch, LevelTransition.Type.BRANCH_ENTRANCE);
			lip.set(chasm.x + 4, chasm.y, chasm.x + 4, chasm.y);
			transitions.add(lip);
		}

		//the passes to the other biomes go last, so the ways down are found first
		for (int side = 0; side < 4; side++){
			if (passes[side] == -1) continue;
			transitions.add(new EdgeTransition(this, side, passes[side], 5, Biome.zone(b.next[side])));
		}

		StringBuilder where = new StringBuilder(b.key + " surface: portal " + (portal % width()) + "," + (portal / width())
				+ ", shrine " + (shrine % width()) + "," + (shrine / width())
				+ ", passes " + Arrays.toString(passes));
		for (Landmark m : marks) where.append(", ").append(m.kind).append("@").append(m.x).append(",").append(m.y);
		CoopLog.line(where.toString());

		customTiles.add(new SurfaceTiles.Ground());
		for (int side = 0; side < 4; side++){
			if (passes[side] == -1 || Biome.blend(b, Biome.zone(b.next[side])) == null) continue;
			customTiles.add(new SurfaceTiles.Border(side, false));
			customTiles.add(new SurfaceTiles.Border(side, true));
		}
		customTiles.add(new SurfaceTiles.Over());
		customTiles.add(new SurfaceTiles.Over2());
		customTiles.add(new SurfaceTiles.Fog());
		return true;
	}

	@Override
	protected void createMobs() {
		if (shrine != -1){
			Shrine.ShrineRoom plaza = new Shrine.ShrineRoom();
			int x = shrine % width(), y = shrine / width();
			plaza.set(x - 2, y - 2, x + 2, y + 2);
			Shrine.place(this, plaza, Continent.source(biome().zone, Continent.SHRINE));
		}
		super.createMobs();
	}

	private Landmark camp(){
		for (int kind : new int[]{Landmark.CAMP, Landmark.F_CAMP, Landmark.C_CAMP, Landmark.R_CAMP, Landmark.B_CAMP}){
			Landmark m = find(kind);
			if (m != null) return m;
		}
		return null;
	}

	@Override
	protected void createItems() {
		//what the others left in their camp, in front of the barrel, the crates and the chest
		Landmark camp = camp();
		if (camp != null){
			int w = width();
			drop(Random.Int(3) == 0 ? new TaintedFood.TaintedRation() : scrap(), camp.x + 4 + (camp.y + 3) * w);
			drop(new Torch().quantity(2), camp.x + 7 + (camp.y + 4) * w);
			Item best = scrap();
			drop(best != null ? best : new Gold().random(), camp.x + 2 + (camp.y + 5) * w);
		}

		//scraps lying around, more of them away from the road
		ArrayList<Integer> slopes = dropCells(0.35f);
		int n = 4 + Random.Int(3);
		for (int i = 0; i < n; i++) dropAround(scrap(), slopes);
		if (Random.Int(2) == 0) dropAround(new Torch(), slopes);
		dropAround(new Gold().random(), slopes);
		dropAround(new Gold().random(), slopes);

		//and some of the ones who came before
		ArrayList<Integer> deep = dropCells(0.6f);
		for (int i = 0; i < 3 && !deep.isEmpty(); i++){
			int cell = deep.remove(Random.Int(deep.size()));
			Item it = Random.Int(2) == 0 ? new Torch() : scrap();
			if (it == null) continue;
			Heap h = drop(it, cell);
			h.type = Heap.Type.SKELETON;
		}
	}

	// *** danger ***

	@Override
	protected float[] buildDanger() {
		float[] out = new float[length()];
		boolean[] main = new boolean[length()];
		for (int i = 0; i < length(); i++) main[i] = road(i) && alt(i % width(), i / width()) == 2;
		int[] dist = new int[length()];
		Arrays.fill(dist, -1);
		int[] queue = new int[length()];
		int head = 0, tail = 0;
		for (int i = 0; i < length(); i++){
			if (main[i]){
				dist[i] = 0;
				queue[tail++] = i;
			}
		}
		while (head < tail){
			int c = queue[head++];
			int x = c % width(), y = c / width();
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int nx = x + dx, ny = y + dy;
					if (nx < 0 || ny < 0 || nx >= width() || ny >= height()) continue;
					int n = nx + ny * width();
					if (dist[n] == -1){
						dist[n] = dist[c] + 1;
						queue[tail++] = n;
					}
				}
			}
		}
		for (int i = 0; i < length(); i++){
			if (out(i)){
				out[i] = 1f;
				continue;
			}
			int h = alt(i % width(), i / width());
			float base = h == 2 ? 0.05f : h == 1 ? 0.4f : 0.7f;
			out[i] = main[i] ? 0 : Math.min(1f, base + 0.3f * Math.min(dist[i], 30) / 30f);
		}
		return out;
	}

	// *** sight: the big lights are seen from far away ***

	@Override
	public void updateFieldOfView( Char c, boolean[] fieldOfView ) {
		super.updateFieldOfView(c, fieldOfView);
		if (!(c instanceof Hero) || !c.isAlive()) return;
		for (Landmark m : marks){
			int light = m.lightCell(width());
			if (light == -1 || m.beacon() <= 0) continue;
			int lx = light % width(), ly = light / width();
			if (distance(c.pos, light) <= m.beacon()){
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int x = lx + dx, y = ly + dy;
						if (x > 0 && y > 0 && x < width() - 1 && y < height() - 1){
							fieldOfView[x + y * width()] = true;
							visited[x + y * width()] = true;
						}
					}
				}
			}
			//the fog is only redrawn around the heroes, and these are further away
			GameScene.updateFog(light, 2);
		}
	}

	// *** drawing ***

	@Override
	public Group addVisuals() {
		super.addVisuals();
		for (Landmark m : marks) m.addLow(visuals);
		visuals.add(new PortalVisual(portal, width()));
		return visuals;
	}

	@Override
	public Group addWallVisuals() {
		super.addWallVisuals();
		for (Landmark m : marks) m.addHigh(wallVisuals);
		if (biome() != Biome.ASH) return wallVisuals;
		//the ash storm over the north, where the road goes into it
		float s = DungeonTilemap.SIZE;
		for (int x = 0; x < width(); x++){
			int c = x + 2 * width();
			if (!road(c) || road(c - 1)) continue;
			for (int k = -4; k <= 5; k++){
				Emitter e = new Emitter();
				e.pos((x + k * 2) * s, 2 * s, 2 * s, 5 * s);
				e.pour(AshWind.FACTORY, 0.06f);
				wallVisuals.add(e);
			}
		}
		return wallVisuals;
	}

	//flakes of ash the wind drags sideways, thick where the storm is
	public static class AshWind extends PixelParticle {

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((AshWind) emitter.recycle(AshWind.class)).reset(x, y);
			}
		};

		public AshWind(){
			super();
			color(Random.Visual.Int(3) == 0 ? 0x9A928A : 0x5E5852);
			lifespan = 2.4f;
		}

		public void reset( float x, float y ){
			revive();
			this.x = x;
			this.y = y;
			left = lifespan;
			size(Random.Visual.Int(2) == 0 ? 1 : 2);
			speed.set(Random.Visual.Float(28, 52), Random.Visual.Float(-4, 6));
		}

		@Override
		public void update() {
			super.update();
			float p = left / lifespan;
			am = (p > 0.8f ? (1 - p) * 5 : p) * 0.85f;
		}
	}

	//the way back: the same portal they came through, still open
	private static class PortalVisual extends Image {

		private final TextureFilm film;
		private float t = 0;

		PortalVisual( int cell, int w ){
			super(FinaleAssets.PORTAL);
			film = new TextureFilm(texture, 32, 48);
			frame(film.get(0));
			hardlight(ThalParts.worldColor(6));
			x = (cell % w) * DungeonTilemap.SIZE;
			y = (cell / w) * DungeonTilemap.SIZE + DungeonTilemap.SIZE - 48;
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			frame(film.get((int)(t * 8) % 4));
		}
	}

	// *** names ***

	private Landmark covering( int cell ){
		Landmark m = landmarkAt(cell);
		return m != null && m.covers(cell % width(), cell / width()) ? m : null;
	}

	public String cellName( int cell ){
		Landmark m = covering(cell);
		if (m != null) return m.name();
		EdgeTransition pass = passAt(cell);
		if (pass != null) return Messages.get(AshSurface.class, "pass_name", Continent.place(Biome.branch(pass.destBranch), SURFACE));
		return Messages.get(AshSurface.class, prefix(cell) + "_name");
	}

	public String cellDesc( int cell ){
		Landmark m = covering(cell);
		if (m != null) return m.desc();
		if (passAt(cell) != null) return Messages.get(AshSurface.class, "pass_desc");
		return Messages.get(AshSurface.class, prefix(cell) + "_desc");
	}

	private EdgeTransition passAt( int cell ){
		for (LevelTransition t : transitions){
			if (t instanceof EdgeTransition && t.inside(cellToPoint(cell))) return (EdgeTransition) t;
		}
		return null;
	}

	//the ash keeps its own words for everything; the other biomes have their own for their
	//ground and borrow the plain ones for the rest
	private String prefix( int cell ){
		String kind = kindOf(cell);
		Biome b = biome();
		if (b == Biome.ASH) return kind;
		switch (kind){
			case "high": case "low": case "deep": case "road": case "fog":
				return b.key + "_" + kind;
			default:
				return "land_" + kind;
		}
	}

	private String kindOf( int cell ){
		int x = cell % width(), y = cell / width();
		if (cell == portal) return "portal";
		if (out(cell)) return y < height() / 4 && biome() == Biome.ASH ? "storm" : "fog";
		if (ramp(cell) == STEPS) return "steps";
		if (ramp(cell) == SLIDE) return "slide";
		SurfaceTiles t = tiles();
		if (t.face(x, y)) return "face";
		int h = alt(x, y);
		if (h == 0 && biome().water) return "deep";
		if (map[cell] == Terrain.CUSTOM_DECO) return "rim";
		if (road(cell)) return "road";
		return h == 2 ? "high" : h == 1 ? "low" : "deep";
	}

	// *** the world map ***

	@Override
	public int mapColor( int cell ) {
		int x = cell % width(), y = cell / width();
		if (out(cell)) return 0xFF100C0C;
		Landmark m = landmarkAt(cell);
		if (m != null && m.covers(x, y)){
			if (m.prop()) return 0xFF2E2626;
			return m.step(x, y) == 'B' ? (m.kind == Landmark.CLIFF ? 0xFF8A6A40 : 0xFF000000) : 0xFFA89C8C;
		}
		if (ramp(cell) != 0) return 0xFF9C8A68;
		if (road(cell)) return 0xFFC8B48C;
		if (tiles().face(x, y)) return 0xFF161010;
		int h = alt(x, y);
		if (biome() != Biome.ASH) return tint(h, map[cell] == Terrain.CUSTOM_DECO);
		int c = h == 2 ? 0xFF6E6560 : h == 1 ? 0xFF4C4440 : 0xFF3A2622;
		if (map[cell] == Terrain.CUSTOM_DECO) c = h == 2 ? 0xFF5A524E : 0xFF3C3532;
		return c;
	}

	//the ground of the other biomes on the map: their own color, darker the lower
	private int tint( int h, boolean blocked ){
		Biome b = biome();
		if (h == 0 && b.water) return 0xFF141C2C;
		int c = b.fanColor;
		float f = (h == 2 ? 0.55f : h == 1 ? 0.4f : 0.27f) * (blocked ? 0.8f : 1f);
		int r = (int)(((c >> 16) & 0xFF) * f), g = (int)(((c >> 8) & 0xFF) * f), bl = (int)((c & 0xFF) * f);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

	@Override
	public void marks( ArrayList<WndWorldMap.Mark> out ) {
		int w = width();
		if (portal != -1){
			out.add(new WndWorldMap.Mark(portal % w - 1, portal / w - 2, 3, 3, 0xFFFFE08A, Messages.get(this, "portal_name")));
		}
		for (LevelTransition t : transitions){
			if (!(t instanceof EdgeTransition)) continue;
			String to = Continent.place(Biome.branch(t.destBranch), SURFACE);
			out.add(new WndWorldMap.Mark(t.left, t.top, t.width(), t.height(), 0xFF9AD0FF,
					Messages.get(AshSurface.class, "pass_name", to)));
		}
		if (shrine != -1){
			out.add(new WndWorldMap.Mark(shrine % w - 2, shrine / w - 2, 5, 5, 0xFFFFF4C0, Messages.get(Shrine.class, "name")));
		}
		for (Landmark m : marks){
			if (m.prop()) continue;
			out.add(new WndWorldMap.Mark(m.x, m.y, m.width(), m.height(), markColor(m), m.name()));
		}
	}

	private static final int[] MARK_COLORS = {
			0xFFE0402A, 0xFFB050E0, 0xFFB050E0, 0xFFC09060,
			0xFFE8DCC0, 0xFFFF9A2A, 0xFFF06A20, 0xFFE8C060
	};

	private int markColor( Landmark m ){
		if (m.kind < MARK_COLORS.length) return MARK_COLORS[m.kind];
		Biome b = biome();
		if (m.kind == b.cave) return 0xFFE0402A;
		if (m.kind == b.chasm) return 0xFFB050E0;
		return 0xFFE8C060;
	}

	// *** saving ***

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("land", land);
		bundle.put("tile_seed", tileSeed);
		bundle.put("landmarks", marks);
		bundle.put("portal", portal);
		bundle.put("shrine", shrine);
		bundle.put("passes", passes);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		land = bundle.getIntArray("land");
		tileSeed = bundle.getInt("tile_seed");
		marks = new ArrayList<>();
		for (Bundlable b : bundle.getCollection("landmarks")){
			marks.add((Landmark) b);
		}
		portal = bundle.getInt("portal");
		shrine = bundle.getInt("shrine");
		if (bundle.contains("passes")) passes = bundle.getIntArray("passes");
		super.restoreFromBundle(bundle);
	}
}
