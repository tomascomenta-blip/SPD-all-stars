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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaFeatures;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//floor 20, branch 2 = the actual undersea boss. sub sinks -> you wake up on a ship in a storm
//heading straight for the kraken's whirlpool. fun
//this class is just STATE (ship hp, holes, mast, distance...). Kraken runs the fight,
//KrakenVisuals/KrakenHud draw it. don't put logic in here pls
public class KrakenLevel extends Level {

	{
		color1 = 0x2a5672;
		color2 = 0x0a1622;
	}

	public static final int W = 41, H = 50;
	//ship top-left in the map
	public static final int SX = 15, SY = 20;
	public static final int SHIP_W = 11, SHIP_H = 25;

	//bow points up. B bowsprit . deck # rail C harpoon cannon M mast H helm L ladder
	public static final String[] SHIP = {
			"     B     ",
			"    #.#    ",
			"   #...#   ",
			"  #..C..#  ",
			" #.......# ",
			"#.........#",
			"#.........#",
			"#.........#",
			"#.........#",
			"#.........#",
			"#.........#",
			"#....M....#",
			"#.........#",
			"#.........#",
			"#.........#",
			"#.........#",
			"#.........#",
			"#.........#",
			"#.........#",
			"#.........#",
			"#....H....#",
			"#.........#",
			"#.........#",
			" #...L...# ",
			"  #######  ",
	};

	public static final int MAST_ROW = 11;

	public static final int INTRO    = 0;
	public static final int TENTACLE = 1; //ship's stuck while a tentacle is up
	public static final int SAILING  = 2;
	public static final int ARRIVED  = 3; //hit the eye!!
	public static final int PUSHED   = 4; //got shoved back
	public static final int WON      = 5;
	public static final int SUNK     = 6;

	public int stage = INTRO;
	public int assault = 1;
	public int distance = 12, distanceMax = 12;
	public int shipHP = 150, shipHT = 150;
	public ArrayList<Integer> holes = new ArrayList<>();
	public int mast = 0;         //0 standing, 1 burning, 2 fallen
	public int mastTimer = 0;
	public int mastSide = 1;     //which side the gap is on once it falls
	public int countdown = -1, countdownMax = -1;
	public int returnPos = -1;   //where to put everyone back on floor 20
	public boolean eyeOpen = false;

	//telegraphed tentacle, for the visuals. -1 = none
	public int riseRow = -1, riseSide = 1, riseType = 1;

	//static handoff from KrakenCall -> build(). ugly but works
	public static int pendingReturnPos = -1;

	@Override
	public void playLevelMusic() {
		if (stage == WON){
			Music.INSTANCE.play(Assets.Music.THEME_FINALE, true);
		} else {
			Music.INSTANCE.play(Assets.Music.HALLS_BOSS, true);
		}
	}

	@Override
	public String tilesTex() {
		return SKAssets.TILES_UNDERSEA;
	}

	@Override
	public String waterTex() {
		return KrakenAssets.WATER_STORM;
	}

	//sea scroll speed, px/sec. a ojo
	@Override
	public float waterSpeed() {
		switch (stage){
			case SAILING:  return Kraken.helmManned() ? 70f : 42f;
			case PUSHED:   return -90f;
			case ARRIVED:  return 10f;
			case WON:      return 3f;
			default:       return 16f;
		}
	}

	public static int cell( int cx, int cy ){
		return (SX + cx) + (SY + cy) * W;
	}

	public static int shipX( int cell ){
		return cell % W - SX;
	}

	public static int shipY( int cell ){
		return cell / W - SY;
	}

	//plan char at cell, ' ' = not ship
	public static char plan( int cell ){
		int x = shipX(cell), y = shipY(cell);
		if (x < 0 || y < 0 || x >= SHIP_W || y >= SHIP_H) return ' ';
		return SHIP[y].charAt(x);
	}

	public static int bowsprit(){ return cell(5, 0); }
	public static int eyeCell(){ return cell(5, -1); }
	public static int cannonCell(){ return cell(5, 3); }
	public static int mastCell(){ return cell(5, MAST_ROW); }
	public static int helmCell(){ return cell(5, 20); }
	public static int helmsmanCell(){ return cell(5, 21); }
	public static int ladderCell(){ return cell(5, 23); }
	public static int arrivalCell(){ return cell(5, 16); }
	//kraken parks here while the ship is far
	public static int farCell(){ return (W/2) + 2*W; }

	public static boolean fallenMastCell( int cell, int side ){
		if (shipY(cell) != MAST_ROW) return false;
		int x = shipX(cell);
		return side > 0 ? x >= 1 && x <= 8 : x >= 2 && x <= 9;
	}

	//walkable deck right now
	public boolean deck( int cell ){
		char c = plan(cell);
		if (c != '.' && c != 'B' && c != 'L') return false;
		return !(mast == 2 && fallenMastCell(cell, mastSide));
	}

	public static boolean onShip( int cell ){
		return plan(cell) != ' ';
	}

	public ArrayList<Integer> deckCells(){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < length(); i++) if (deck(i)) cells.add(i);
		return cells;
	}

	public ArrayList<Integer> rowCells( int row ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int x = 0; x < SHIP_W; x++){
			int c = cell(x, row);
			if (deck(c)) cells.add(c);
		}
		return cells;
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		Arrays.fill(map, Terrain.WATER);
		//map border, never visible. water tiles peek at neighbours so it needs something there
		for (int i = 0; i < length(); i++){
			int x = i % W, y = i / W;
			if (x == 0 || y == 0 || x == W - 1 || y == H - 1) map[i] = Terrain.CHASM;
		}
		for (int y = 0; y < SHIP_H; y++){
			for (int x = 0; x < SHIP_W; x++){
				char c = SHIP[y].charAt(x);
				if (c != ' ' && c != '#') map[cell(x, y)] = Terrain.EMPTY;
			}
		}
		LevelTransition arrival = new LevelTransition(this, arrivalCell(), LevelTransition.Type.BRANCH_ENTRANCE,
				20, 0, LevelTransition.Type.BRANCH_EXIT);
		transitions.add(arrival);
		returnPos = pendingReturnPos;
		viewDistance = 20;
		return true;
	}

	//no stairs. you leave through the bowsprit once it's dead (occupyCell)
	@Override
	public LevelTransition getTransition( int cell ) {
		return null;
	}

	//everything's visible, nothing to explore
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

	private void flags( int i ){
		int x = i % W, y = i / W;
		if (x == 0 || y == 0 || x == W - 1 || y == H - 1) return;
		boolean d = deck(i);
		passable[i] = d;
		avoid[i] = false;
		solid[i] = false;
		losBlocking[i] = false;
		pit[i] = false;
		flamable[i] = false;
		water[i] = !onShip(i);
		openSpace[i] = d;
	}

	//ink blinds heroes+allies, same as undersea
	@Override
	public void updateFieldOfView( Char c, boolean[] fieldOfView ) {
		Blob ink =
				blobs.get(UnderseaFeatures.InkCloud.class);
		if (ink == null || ink.volume <= 0 || c.alignment != Char.Alignment.ALLY){
			super.updateFieldOfView(c, fieldOfView);
			return;
		}
		boolean[] saved = losBlocking.clone();
		for (int i = 0; i < losBlocking.length; i++){
			if (ink.cur[i] > 0) losBlocking[i] = true;
		}
		try {
			super.updateFieldOfView(c, fieldOfView);
		} finally {
			System.arraycopy(saved, 0, losBlocking, 0, saved.length);
		}
		for (int i = 0; i < fieldOfView.length; i++){
			if (fieldOfView[i] && ink.cur[i] > 0 && distance(i, c.pos) > 1) fieldOfView[i] = false;
		}
	}

	@Override
	public boolean invalidHeroPos( int tile ) {
		return !deck(tile);
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
		Kraken k = new Kraken();
		k.pos = farCell();
		mobs.add(k);

		HarpoonCannon cannon = new HarpoonCannon();
		cannon.pos = cannonCell();
		mobs.add(cannon);

		//powder barrels. kaboom
		int[][] spots = {{2, 7}, {8, 9}, {2, 15}, {8, 18}};
		for (int[] s : spots){
			SKProps.ExplosiveBarrel b = new SKProps.ExplosiveBarrel();
			b.pos = cell(s[0], s[1]);
			mobs.add(b);
		}
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		ArrayList<Integer> cells = deckCells();
		for (int i = 0; i < 40 && !cells.isEmpty(); i++){
			int c = cells.get(Random.Int(cells.size()));
			if (Actor.findChar(c) == null) return c;
		}
		return -1;
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell(ch);
		//kraken dead + someone steps on the bowsprit = whole party back to floor 20
		if (stage == WON && ch instanceof Hero && Party.isPartyHero(ch) && ch.pos == bowsprit() && returnPos >= 0){
			leave();
		}
	}

	private boolean leaving = false;

	private void leave(){
		if (leaving) return;
		leaving = true;
		Level.beforeTransition();
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = 20;
		InterlevelScene.returnBranch = 0;
		InterlevelScene.returnPos = returnPos;
		Game.switchScene(InterlevelScene.class);
	}

	public boolean solo(){
		return Party.living().size() <= 1;
	}

	//solo takes less ship dmg
	public void damageShip( int amount ){
		if (stage == WON || stage == SUNK || amount <= 0) return;
		if (solo()) amount = Math.max(1, Math.round(amount * 0.6f));
		shipHP -= amount;
		KrakenVisuals.shipHit(amount);
		if (shipHP <= 0){
			shipHP = 0;
			Kraken.shipLost(false);
		}
	}

	public void repairShip( int amount ){
		if (stage == WON || stage == SUNK) return;
		shipHP = Math.min(shipHT, shipHP + amount);
	}

	public void addHole( int cell ){
		if (!deck(cell) || holes.contains(cell)) return;
		holes.add(cell);
		KrakenVisuals.holeOpened(cell);
	}

	public boolean removeHole( int cell ){
		return holes.remove((Integer) cell);
	}

	//closest hole within dist, -1 if none
	public int holeNear( int cell, int dist ){
		int best = -1;
		for (int h : holes){
			int d = distance(cell, h);
			if (d <= dist && (best == -1 || d < distance(cell, best))) best = h;
		}
		return best;
	}

	public int freeDeckNear( int cell ){
		if (deck(cell) && Actor.findChar(cell) == null) return cell;
		int best = -1;
		for (int i = 0; i < length(); i++){
			if (!deck(i) || Actor.findChar(i) != null) continue;
			if (best == -1 || trueDistance(cell, i) < trueDistance(cell, best)) best = i;
		}
		return best;
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		KrakenVisuals.Low low = new KrakenVisuals.Low(this);
		visuals.add(low);
		return visuals;
	}

	@Override
	public Group addWallVisuals() {
		super.addWallVisuals();
		wallVisuals.add(new KrakenVisuals.High(this));
		return wallVisuals;
	}

	@Override
	public String tileName( int tile ) {
		return tile == Terrain.WATER ? Messages.get(KrakenLevel.class, "sea_name")
				: Messages.get(KrakenLevel.class, "deck_name");
	}

	@Override
	public String tileDesc( int tile ) {
		return tile == Terrain.WATER ? Messages.get(KrakenLevel.class, "sea_desc")
				: Messages.get(KrakenLevel.class, "deck_desc");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("stage", stage);
		bundle.put("assault", assault);
		bundle.put("distance", distance);
		bundle.put("distance_max", distanceMax);
		bundle.put("ship_hp", shipHP);
		bundle.put("ship_ht", shipHT);
		int[] h = new int[holes.size()];
		for (int i = 0; i < h.length; i++) h[i] = holes.get(i);
		bundle.put("holes", h);
		bundle.put("mast", mast);
		bundle.put("mast_timer", mastTimer);
		bundle.put("mast_side", mastSide);
		bundle.put("countdown", countdown);
		bundle.put("countdown_max", countdownMax);
		bundle.put("return_pos", returnPos);
		bundle.put("eye_open", eyeOpen);
		bundle.put("rise_row", riseRow);
		bundle.put("rise_side", riseSide);
		bundle.put("rise_type", riseType);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		stage = bundle.getInt("stage");
		assault = bundle.getInt("assault");
		distance = bundle.getInt("distance");
		distanceMax = bundle.getInt("distance_max");
		shipHP = bundle.getInt("ship_hp");
		shipHT = bundle.getInt("ship_ht");
		holes.clear();
		for (int c : bundle.getIntArray("holes")) holes.add(c);
		mast = bundle.getInt("mast");
		mastTimer = bundle.getInt("mast_timer");
		mastSide = bundle.getInt("mast_side");
		countdown = bundle.getInt("countdown");
		countdownMax = bundle.getInt("countdown_max");
		returnPos = bundle.getInt("return_pos");
		eyeOpen = bundle.getBoolean("eye_open");
		riseRow = bundle.getInt("rise_row");
		riseSide = bundle.getInt("rise_side");
		riseType = bundle.getInt("rise_type");
		viewDistance = 20;
	}
}
