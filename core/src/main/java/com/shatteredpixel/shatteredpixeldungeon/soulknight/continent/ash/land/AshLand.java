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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.TaintedFood;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.Awakening;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.BonesFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral.Cathedral;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral.CathedralDoor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.CityFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.FleshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.FrostFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.IslesFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.GreatWorm;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.EdgeTransition;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.WndWorldMap;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

//what the three layers of a biome share: the dark, the music, how scarce things are, and the
//fauna coming back over time (who comes is the business of the fauna of each biome). the ash
//plains were the first, so they lend their name to the lot
public abstract class AshLand extends Level implements OpenWorld, WndWorldMap.Charted {

	{
		color1 = 0x5a3020;
		color2 = 0x2a1814;
	}

	//every this many turns, one more thing may come back
	private static final float COMEBACK = 60f;

	private float[] danger;
	private Comeback comeback;

	//the biome it belongs to, by zone (older saves had only the ash)
	private int zone = Continent.Z_ASH;

	public Biome biome(){
		return Biome.zone(zone);
	}

	@Override
	public void create() {
		zone = Biome.current().zone;
		color1 = biome().color1;
		color2 = biome().color2;
		super.create();
		viewDistance = Continent.BASE_SIGHT;
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.playTracks(biome().tracks, biome().chances, false);
	}

	@Override
	public String tilesTex() {
		return biome().underTiles;
	}

	//the ground drawn with a sheet of the biome, by the rule of the surface (null: the cave tiles)
	SurfaceTiles tiles(){
		return null;
	}

	String sheet(){
		return biome().sheet;
	}

	@Override
	public String waterTex() {
		return biome().underWater;
	}

	//the one who walks out through a pass: the others come in next to them on the other side
	@Override
	public boolean activateTransition( Hero hero, LevelTransition transition ) {
		//the continent is awake and this is the portal: the essence seals it behind them
		if (Awakening.closes(hero, transition)) return true;
		if (Awakening.blocks(hero, transition)) return false;
		//the doors of a fallen furnace cathedral
		if (transition instanceof CathedralDoor && !Cathedral.enter(hero, (CathedralDoor) transition)) return false;
		if (transition instanceof EdgeTransition){
			((EdgeTransition) transition).crossing(hero.pos, width());
		}
		return super.activateTransition(hero, transition);
	}

	@Override
	public float danger( int cell ){
		if (danger == null) danger = buildDanger();
		return cell >= 0 && cell < danger.length ? danger[cell] : 1f;
	}

	protected abstract float[] buildDanger();

	//shared by the three layers: 0 at the sources, growing with the walking distance
	protected float[] walkDanger( ArrayList<Integer> from, float base, float span ){
		float[] out = new float[length()];
		Arrays.fill(out, 1f);
		int[] dist = walkDistance(from);
		int max = 1;
		for (int d : dist) if (d > max) max = d;
		for (int i = 0; i < length(); i++){
			if (dist[i] >= 0) out[i] = Math.min(1f, base + span * dist[i] / (float)max);
		}
		return out;
	}

	//breadth first over passable cells, -1 where it doesn't reach
	protected int[] walkDistance( ArrayList<Integer> from ){
		int[] dist = new int[length()];
		Arrays.fill(dist, -1);
		int[] queue = new int[length()];
		int head = 0, tail = 0;
		for (int c : from){
			if (c >= 0 && c < length() && dist[c] == -1){
				dist[c] = 0;
				queue[tail++] = c;
			}
		}
		int w = width();
		while (head < tail){
			int c = queue[head++];
			int x = c % w, y = c / w;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = -1; dx <= 1; dx++){
					int nx = x + dx, ny = y + dy;
					if (nx < 0 || ny < 0 || nx >= w || ny >= height()) continue;
					int n = nx + ny * w;
					if (dist[n] != -1 || !passable[n]) continue;
					dist[n] = dist[c] + 1;
					queue[tail++] = n;
				}
			}
		}
		return dist;
	}

	// *** who lives here ***

	@Override
	protected void createMobs() {
		switch (zone){
			case Continent.Z_FLESH: FleshFauna.populate(this, layer()); break;
			case Continent.Z_CITY:  CityFauna.populate(this, layer()); break;
			case Continent.Z_FROST: FrostFauna.populate(this, layer()); break;
			case Continent.Z_BONES: BonesFauna.populate(this, layer()); break;
			case Continent.Z_ISLES: IslesFauna.populate(this, layer()); break;
			default:                AshFauna.populate(this, layer()); break;
		}
	}

	//traps, summons and the like still ask the level for "a mob of here"
	@Override
	public Mob createMob() {
		Mob m = zone == Continent.Z_ASH ? AshFauna.respawn(this, layer()) : null;
		if (m == null){
			m = Reflection.newInstance(Random.element(ContinentMobs.rotation(21, Continent.ASH)));
		}
		return m;
	}

	@Override
	public Actor addRespawner() {
		if (comeback == null){
			comeback = new Comeback();
			Actor.addDelayed(comeback, COMEBACK);
		} else {
			Actor.add(comeback);
		}
		return comeback;
	}

	void comeBack(){
		Mob m;
		switch (zone){
			case Continent.Z_FLESH: m = FleshFauna.respawn(this, layer()); break;
			case Continent.Z_CITY:  m = CityFauna.respawn(this, layer()); break;
			//the moor, the graveyard and the isles bring theirs back on their own (the blind worm too)
			case Continent.Z_FROST: FrostFauna.respawn(this, layer()); return;
			case Continent.Z_BONES: BonesFauna.respawn(this, layer()); return;
			case Continent.Z_ISLES: IslesFauna.respawn(this, layer()); return;
			default: m = AshFauna.respawn(this, layer()); break;
		}
		//the blind worm lives under all of it
		GreatWorm.maybeEmerge(this);
		if (m == null) return;
		if (m.pos <= 0 || m.pos >= length() || !passable[m.pos] || Actor.findChar(m.pos) != null){
			m.pos = farCell(m, 12);
		}
		if (m.pos == -1) return;
		GameScene.add(m);
	}

	//a free cell no hero is close to
	public int farCell( Char ch, int minDist ){
		ArrayList<Hero> heroes = new ArrayList<>();
		if (Party.active()) heroes.addAll(Party.living());
		else if (Dungeon.hero != null) heroes.add(Dungeon.hero);
		for (int tries = 0; tries < 60; tries++){
			int cell = Random.Int(length());
			if (!passable[cell] || Actor.findChar(cell) != null) continue;
			if (Char.hasProp(ch, Char.Property.LARGE) && !openSpace[cell]) continue;
			boolean near = false;
			for (Hero h : heroes){
				if (distance(h.pos, cell) < minDist) near = true;
			}
			if (!near) return cell;
		}
		return -1;
	}

	public static class Comeback extends Actor {

		{
			actPriority = BUFF_PRIO;
		}

		@Override
		protected boolean act() {
			if (Dungeon.level instanceof AshLand){
				((AshLand) Dungeon.level).comeBack();
			}
			spend(Awakening.comeback(COMEBACK));
			return true;
		}
	}

	// *** what lies around ***

	//scarce: rotten food mostly, now and then a torch
	protected Item scrap(){
		Item it = Generator.random();
		if (it instanceof Food && Random.Int(4) != 0) it = TaintedFood.random();
		return it;
	}

	protected void dropAround( Item item, ArrayList<Integer> cells ){
		if (item == null || cells.isEmpty()) return;
		int cell = cells.remove(Random.Int(cells.size()));
		drop(item, cell);
	}

	//free cells with at least that much danger, in a fixed order
	protected ArrayList<Integer> dropCells( float minDanger ){
		ArrayList<Integer> out = new ArrayList<>();
		for (int i = 0; i < length(); i++){
			if (passable[i] && !avoid[i] && !onTransition(i) && heaps.get(i) == null
					&& danger(i) >= minDanger){
				out.add(i);
			}
		}
		return out;
	}

	//as getTransition(cell), but it works while the level is still being made
	protected boolean onTransition( int cell ){
		Point p = cellToPoint(cell);
		for (LevelTransition t : transitions){
			if (t.inside(p)) return true;
		}
		return false;
	}

	@Override
	public void updateFieldOfView( Char c, boolean[] fieldOfView ) {
		super.updateFieldOfView(c, fieldOfView);
		Continent.glowSight(this, c, fieldOfView);
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		if (DevFlags.landShots() != null) visuals.add(new LandShots());
		if (DevFlags.landHop()) visuals.add(new LandHop());
		return visuals;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("zone", zone);
		if (comeback != null) bundle.put("comeback", comeback);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		if (bundle.contains("zone")) zone = bundle.getInt("zone");
		super.restoreFromBundle(bundle);
		color1 = biome().color1;
		color2 = biome().color2;
		if (bundle.contains("comeback")) comeback = (Comeback) bundle.get("comeback");
	}

	protected static ArrayList<Integer> list( Integer... cells ){
		ArrayList<Integer> out = new ArrayList<>();
		Collections.addAll(out, cells);
		return out;
	}
}
