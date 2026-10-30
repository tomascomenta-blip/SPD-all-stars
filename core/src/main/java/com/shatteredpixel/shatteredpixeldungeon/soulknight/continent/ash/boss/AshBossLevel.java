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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.boss;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//ash plains, last floor: the walking pyre's plain. a wide oval of ash with a river of black lava
//across it, four braziers, the pass to the entrance at the bottom (it caves in when the fight starts)
//and the way on at the top, shut until the pyre is dead.
//black lava is WATER underneath, but nobody walks into it by accident: it's off the paths like a trap
public class AshBossLevel extends Level {

	public static final int W = 27, H = 30;
	public static final int CX = 13, CY = 13;
	private static final float RX = 10.2f, RY = 8.2f;

	//row of the pass between the plain and the entrance
	public static final int PASS_Y = 22;

	//stomp lava: turns until it crusts over again (0 = not stomp lava)
	public int[] cooling;
	//turns of ash storm left
	public int storm = 0;
	//falling embers, marked and waiting
	public final ArrayList<Strike> strikes = new ArrayList<>();
	//bumped when the floor changes, for the visuals
	public int visualVersion = 0;

	{
		color1 = 0x3a3230;
		color2 = 0x8a4a22;
	}

	public static class Strike {
		public int kind;
		public float at;
		public int[] cells;
	}

	public static int cell( int x, int y ){
		return x + y * W;
	}

	public static boolean plainShape( int x, int y ){
		if (x <= 1 || y <= 1 || x >= W - 2 || y >= H - 2) return false;
		float dx = (x - CX) / RX, dy = (y - CY) / RY;
		return dx * dx + dy * dy <= 1f;
	}

	public boolean onPlain( int cell ){
		if (cell < 0 || cell >= length()) return false;
		return plainShape(cell % W, cell / W);
	}

	public static int pyreCell(){ return cell(CX, 8); }
	public static int exitCell(){ return cell(CX, 4); }
	public static int entranceCell(){ return cell(CX, 26); }
	public static int[] passCells(){ return new int[]{ cell(CX - 1, PASS_Y), cell(CX, PASS_Y), cell(CX + 1, PASS_Y) }; }
	public static int[] brazierCells(){
		return new int[]{ cell(8, 9), cell(18, 9), cell(8, 17), cell(18, 17) };
	}

	private static int riverY( int x ){
		return CY + Math.round(1.6f * (float) Math.sin((x - 3) * 0.45f));
	}

	@Override
	public void playLevelMusic() {
		AshPyre p = pyre(this);
		if (p != null && locked && p.fighting()){
			Music.INSTANCE.play(p.phase >= 4 ? Assets.Music.CAVES_BOSS_FINALE : Assets.Music.CAVES_BOSS, true);
		} else if (p != null){
			Music.INSTANCE.end();
		} else {
			Music.INSTANCE.playTracks(CavesLevel.CAVES_TRACK_LIST, CavesLevel.CAVES_TRACK_CHANCES, false);
		}
	}

	public static void music( final String track ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.play(track, true);
			}
		});
	}

	@Override
	public String tilesTex() {
		return ContinentAssets.TILES_ASH;
	}

	@Override
	public String waterTex() {
		return ContinentAssets.WATER_ASH;
	}

	@Override
	public float waterSpeed() {
		return 2f;
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		Arrays.fill(map, Terrain.WALL);

		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				if (plainShape(x, y)) map[cell(x, y)] = Terrain.EMPTY;
			}
		}

		//the river, with two fords
		for (int x = 2; x < W - 2; x++){
			if (x == 8 || x == 9 || x == 17 || x == 18) continue;
			int y = riverY(x);
			for (int k = 0; k < 2; k++){
				if (plainShape(x, y + k)) map[cell(x, y + k)] = Terrain.WATER;
			}
		}
		//and two pools
		for (int c : new int[]{ cell(5, 17), cell(6, 17), cell(6, 18), cell(18, 6), cell(18, 7), cell(19, 7) }){
			if (onPlain(c)) map[c] = Terrain.WATER;
		}

		//basalt pebbles, live embers and a few clumps of basalt columns near the edge
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				int c = cell(x, y);
				if (map[c] != Terrain.EMPTY) continue;
				float dx = (x - CX) / RX, dy = (y - CY) / RY;
				float d = dx * dx + dy * dy;
				if (d > 0.62f && d < 0.93f && Random.Int(9) == 0 && clearOfLandmarks(c)){
					map[c] = Terrain.REGION_DECO;
				} else if (Random.Int(10) == 0){
					map[c] = Terrain.EMBERS;
				} else if (Random.Int(7) == 0){
					map[c] = Terrain.EMPTY_DECO;
				}
			}
		}

		//the way on, up north
		map[exitCell()] = Terrain.LOCKED_EXIT;
		for (int x = CX - 1; x <= CX + 1; x++) map[cell(x, 5)] = Terrain.EMPTY_SP;

		//the pass and the entrance hollow
		for (int c : passCells()) map[c] = Terrain.EMPTY;
		for (int x = CX - 1; x <= CX + 1; x++){
			map[cell(x, 21)] = Terrain.EMPTY;
			map[cell(x, PASS_Y + 1)] = Terrain.EMPTY;
		}
		for (int y = 24; y <= 27; y++){
			for (int x = CX - 3; x <= CX + 3; x++){
				if ((x == CX - 3 || x == CX + 3) && (y == 24 || y == 27)) continue;
				map[cell(x, y)] = Random.Int(6) == 0 ? Terrain.EMPTY_DECO : Terrain.EMPTY;
			}
		}
		map[entranceCell()] = Terrain.ENTRANCE;

		for (int c : brazierCells()) map[c] = Terrain.EMPTY;
		map[pyreCell()] = Terrain.EMBERS;

		transitions.add(new LevelTransition(this, entranceCell(), LevelTransition.Type.REGULAR_ENTRANCE));
		transitions.add(new LevelTransition(this, exitCell(), LevelTransition.Type.REGULAR_EXIT));

		cooling = new int[length()];
		return true;
	}

	private boolean clearOfLandmarks( int c ){
		for (int b : brazierCells()) if (distance(c, b) <= 2) return false;
		for (int p : passCells()) if (distance(c, p) <= 2) return false;
		return distance(c, exitCell()) > 2 && distance(c, pyreCell()) > 2;
	}

	//an ironlord's duel room gets glued under the map after it's built
	@Override
	public void appendRows( int n ) {
		super.appendRows(n);
		cooling = Arrays.copyOf(cooling, length());
	}

	// *** lava ***

	public boolean lava( int cell ){
		return cell >= 0 && cell < length() && map[cell] == Terrain.WATER;
	}

	@Override
	public void buildFlagMaps() {
		super.buildFlagMaps();
		for (int i = 0; i < length(); i++) lavaFlags(i);
	}

	@Override
	public void updateCellFlags( int cell ) {
		super.updateCellFlags(cell);
		lavaFlags(cell);
	}

	//off every path, and it doesn't put fires out
	private void lavaFlags( int i ){
		if (map[i] != Terrain.WATER) return;
		passable[i] = false;
		avoid[i] = true;
		water[i] = false;
	}

	//what the pyre walks on
	public boolean[] pyrePassable(){
		boolean[] pass = passable.clone();
		for (int i = 0; i < length(); i++){
			if (map[i] == Terrain.WATER) pass[i] = true;
		}
		return pass;
	}

	//black lava opens where it stomps. never under a brazier, nor on the way in or out
	public boolean canMelt( int cell ){
		if (!onPlain(cell) || solid[cell] || map[cell] == Terrain.WATER) return false;
		if (cell == exitCell() || distance(cell, exitCell()) <= 1) return false;
		for (int p : passCells()) if (distance(cell, p) <= 1) return false;
		for (int b : brazierCells()) if (b == cell) return false;
		return !(Actor.findChar(cell) instanceof PyreBrazier);
	}

	//never more than this much fresh lava at once, or the plain turns into islands
	public static final int MAX_MELT = 26;

	public void melt( int cell, int turns ){
		if (!canMelt(cell)) return;
		int melted = 0;
		for (int c : cooling) if (c > 0) melted++;
		if (melted >= MAX_MELT) return;
		set(cell, Terrain.WATER, this);
		cooling[cell] = turns;
		GameScene.updateMap(cell);
		if (heroFOV[cell]) CellEmitter.get(cell).burst(FlameParticle.FACTORY, 4);
		visualVersion++;
	}

	private void crust( int cell ){
		cooling[cell] = 0;
		set(cell, Terrain.EMBERS, this);
		GameScene.updateMap(cell);
		if (heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.STEAM), 3);
		visualVersion++;
	}

	//all of it goes cold when the pyre dies
	public void coolDown(){
		for (int i = 0; i < length(); i++){
			if (map[i] == Terrain.WATER) crust(i);
		}
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell(ch);
		if (lava(ch.pos)) scorch(ch);
	}

	public static class Lava implements Hero.Doom {
		@Override
		public void onDeath() {
			Dungeon.fail(this);
			GLog.n(Messages.get(AshBossLevel.class, "lava_death"));
		}
	}

	private static final Lava LAVA = new Lava();

	public void scorch( Char ch ){
		if (ch == null || !ch.isAlive() || ch.flying || ch.isImmune(Burning.class)) return;
		if (ch instanceof PyreBrazier) return;
		ch.damage(Math.max(1, Random.NormalIntRange(12, 20) - ch.drRoll() / 3), LAVA);
		if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch, 4f);
		if (ch.sprite != null && heroFOV[ch.pos]){
			CellEmitter.get(ch.pos).burst(FlameParticle.FACTORY, 6);
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.8f, 0.8f);
		}
	}

	//once a turn (Embers below)
	public void tick(){
		for (int i = 0; i < length(); i++){
			if (cooling[i] > 0 && --cooling[i] == 0 && map[i] == Terrain.WATER){
				if (Actor.findChar(i) == null) crust(i);
				else cooling[i] = 1;
			}
		}
		for (Hero h : Party.living()){
			if (lava(h.pos)) scorch(h);
		}
		for (Mob m : mobs.toArray(new Mob[0])){
			if (lava(m.pos)) scorch(m);
		}
		if (storm > 0 && --storm == 0){
			AshPyre p = pyre(this);
			if (p != null && p.fighting()) GLog.i(Messages.get(AshPyre.class, "storm_end"));
		}
		for (int i = 0; i < strikes.size(); i++){
			Strike s = strikes.get(i);
			if (s.at <= Actor.now() + 0.01f){
				strikes.remove(i--);
				AshPyre.strike(this, s);
			}
		}
	}

	//the floor's own clock: lava, storm, embers on time whatever the pyre is doing
	public static class Embers extends Blob {

		{
			actPriority = BLOB_PRIO;
		}

		@Override
		public boolean act() {
			spend(TICK);
			if (Dungeon.level instanceof AshBossLevel) ((AshBossLevel) Dungeon.level).tick();
			return true;
		}
	}

	// *** the fight ***

	public static AshPyre pyre( Level level ){
		for (Mob m : level.mobs){
			if (m instanceof AshPyre) return (AshPyre) m;
		}
		return null;
	}

	public ArrayList<PyreBrazier> braziers(){
		ArrayList<PyreBrazier> list = new ArrayList<>();
		for (Mob m : mobs){
			if (m instanceof PyreBrazier) list.add((PyreBrazier) m);
		}
		return list;
	}

	public int litBraziers(){
		int n = 0;
		for (PyreBrazier b : braziers()) if (b.lit) n++;
		return n;
	}

	@Override
	public void seal() {
		if (!locked){
			super.seal();
			for (int c : passCells()){
				set(c, Terrain.WALL, this);
				GameScene.updateMap(c);
				if (heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 8);
			}
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.7f);
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					PixelScene.shake(3, 0.6f);
				}
			});
			Dungeon.observe();
			CoopLog.line("pyre arena sealed t=" + (int) Actor.now());
		}
		gatherParty();
	}

	@Override
	public void unseal() {
		if (locked){
			super.unseal();
			for (int c : passCells()){
				set(c, Terrain.EMBERS, this);
				GameScene.updateMap(c);
				if (heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 8);
			}
			set(exitCell(), Terrain.EXIT, this);
			GameScene.updateMap(exitCell());
			if (heroFOV[exitCell()]) CellEmitter.get(exitCell()).burst(FlameParticle.FACTORY, 12);
			coolDown();
			storm = 0;
			strikes.clear();
			Dungeon.observe();
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.fadeOut(5f, new Callback() {
						@Override
						public void call() {
							Music.INSTANCE.playTracks(CavesLevel.CAVES_TRACK_LIST, CavesLevel.CAVES_TRACK_CHANCES, false);
						}
					});
				}
			});
		}
	}

	@Override
	protected void createMobs() {
		AshPyre p = new AshPyre();
		p.pos = pyreCell();
		mobs.add(p);
		for (int c : brazierCells()){
			PyreBrazier b = new PyreBrazier();
			b.pos = c;
			mobs.add(b);
		}
		Embers e = new Embers();
		e.seed(this, 0, 0);
		blobs.put(Embers.class, e);
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = entranceCell() + n;
			if (passable[c] && Actor.findChar(c) == null) cells.add(c);
		}
		return cells.isEmpty() ? -1 : Random.element(cells);
	}

	//free cells of the plain, far enough from the pyre and the braziers
	public ArrayList<Integer> freePlain( int around, int min, int max ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < length(); i++){
			if (!onPlain(i) || !passable[i] || Actor.findChar(i) != null) continue;
			int d = distance(i, around);
			if (d >= min && d <= max) cells.add(i);
		}
		return cells;
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		visuals.add(new LavaGlow(this));
		return visuals;
	}

	//embers rising off the black lava, render side only
	public static class LavaGlow extends Group {

		private final AshBossLevel level;
		private float t = 0;
		private final java.util.Random rnd = new java.util.Random();

		public LavaGlow( AshBossLevel level ){
			this.level = level;
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (t < 0.12f) return;
			t = 0;
			int tries = 6;
			while (tries-- > 0){
				int c = rnd.nextInt(level.length());
				if (level.map[c] != Terrain.WATER || !level.heroFOV[c]) continue;
				Emitter e = GameScene.emitter();
				if (e == null) return;
				float s = DungeonTilemap.SIZE;
				e.pos((c % level.width()) * s + 2, (c / level.width()) * s + 4, s - 4, s - 8);
				e.burst(FlameParticle.FACTORY, 1);
			}
		}
	}

	@Override
	public String tileName( int tile ) {
		switch (tile){
			case Terrain.WATER: return Messages.get(AshBossLevel.class, "lava_name");
			case Terrain.EMPTY: case Terrain.EMPTY_DECO: return Messages.get(AshBossLevel.class, "ash_name");
			case Terrain.EMBERS: return Messages.get(AshBossLevel.class, "embers_name");
			case Terrain.REGION_DECO: case Terrain.REGION_DECO_ALT: return Messages.get(AshBossLevel.class, "columns_name");
			default: return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile){
			case Terrain.WATER: return Messages.get(AshBossLevel.class, "lava_desc");
			case Terrain.EMPTY: case Terrain.EMPTY_DECO: return Messages.get(AshBossLevel.class, "ash_desc");
			case Terrain.EMBERS: return Messages.get(AshBossLevel.class, "embers_desc");
			case Terrain.REGION_DECO: case Terrain.REGION_DECO_ALT: return Messages.get(AshBossLevel.class, "columns_desc");
			case Terrain.LOCKED_EXIT: return Messages.get(AshBossLevel.class, "exit_locked_desc");
			default: return super.tileDesc(tile);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("cooling", cooling);
		bundle.put("storm", storm);
		bundle.put("visual", visualVersion);
		int[] kinds = new int[strikes.size()];
		float[] times = new float[strikes.size()];
		for (int i = 0; i < strikes.size(); i++){
			kinds[i] = strikes.get(i).kind;
			times[i] = strikes.get(i).at;
			bundle.put("strike_" + i, strikes.get(i).cells);
		}
		bundle.put("strike_kinds", kinds);
		bundle.put("strike_times", times);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		cooling = bundle.contains("cooling") ? bundle.getIntArray("cooling") : new int[length()];
		if (cooling.length != length()) cooling = Arrays.copyOf(cooling, length());
		storm = bundle.getInt("storm");
		visualVersion = bundle.getInt("visual");
		strikes.clear();
		if (bundle.contains("strike_kinds")){
			int[] kinds = bundle.getIntArray("strike_kinds");
			float[] times = bundle.getFloatArray("strike_times");
			for (int i = 0; i < kinds.length; i++){
				Strike s = new Strike();
				s.kind = kinds[i];
				s.at = times[i];
				s.cells = bundle.getIntArray("strike_" + i);
				strikes.add(s);
			}
		}
	}
}
