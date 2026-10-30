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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.AshLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//inside the furnace cathedral: a church that is a furnace too, and alive. the portal at the bottom,
//the nave full of pews where the faithful melted into the wood, chapels off the aisles, the bell
//tower and the crypt at either end of the transept, and up in the apse, on the altar, the heart
//of the furnace. the longer the party stays the hotter it burns, and once the thing gets up out
//there the whole inside shakes and comes down on them (Cathedral keeps the clock)
public class CathedralLevel extends Level {

	{
		color1 = 0x3c2850;
		color2 = 0x8a3a18;
	}

	//what each cell is part of (CathedralPlan), for the monsters and the names
	public static final int OUT = 0, NARTHEX = 1, NAVE = 2, AISLE = 3, CHAPEL = 4, TRANSEPT = 5,
			APSE = 6, TOWER = 7, CRYPT = 8, DAIS = 9;

	int[] zone;
	//the pieces drawn over the floor (low, under everyone) and over the heads (high), -1 for none
	int[] low, high;
	//the middle of the heart's patch and the furnace grates the stokers crawl out of
	int heart = -1;
	int[] grates = new int[0];

	private Kiln kiln;

	public int zone( int cell ){
		return zone == null || cell < 0 || cell >= zone.length ? OUT : zone[cell];
	}

	public int heartCell(){
		return heart;
	}

	public int[] grates(){
		return grates;
	}

	@Override
	public void playLevelMusic() {
		if (Cathedral.risen()) Music.INSTANCE.play(Assets.Music.CITY_BOSS_FINALE, true);
		else Music.INSTANCE.play(Assets.Music.CITY_TENSE, true);
	}

	@Override
	public String tilesTex() {
		return CathedralAssets.TILES;
	}

	@Override
	public String waterTex() {
		return CathedralAssets.MOLTEN;
	}

	@Override
	protected boolean build() {
		CathedralPlan.build(this);
		return true;
	}

	@Override
	public void create() {
		super.create();
		viewDistance = Cathedral.GLOW_SIGHT;
	}

	// *** the ground ***

	@Override
	public void buildFlagMaps() {
		super.buildFlagMaps();
		for (int i = 0; i < length(); i++) hot(i);
	}

	@Override
	public void updateCellFlags( int cell ) {
		super.updateCellFlags(cell);
		hot(cell);
	}

	//molten metal and live embers: the monsters keep off them (and so does the hero, walking)
	private void hot( int i ){
		if (map[i] == Terrain.WATER || map[i] == Terrain.EMBERS) avoid[i] = true;
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell(ch);
		if (!ch.isAlive() || ch.flying || Char.hasProp(ch, Char.Property.FIERY)) return;
		if (map[ch.pos] == Terrain.WATER){
			AshLevel.burnInLava(ch);
			if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch, 4f);
		} else if (map[ch.pos] == Terrain.EMBERS){
			ch.damage(Cathedral.fire(4 + 3 * Cathedral.heat()), this);
			if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch, 2f + Cathedral.heat() / 2f);
			if (heroFOV[ch.pos]) CellEmitter.get(ch.pos).burst(FlameParticle.FACTORY, 4);
		}
	}

	@Override
	public boolean invalidHeroPos( int tile ) {
		return !passable[tile] || zone(tile) == OUT;
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		for (int i = 0; i < 80; i++){
			int c = Random.Int(length());
			if (passable[c] && !avoid[c] && zone(c) != OUT && Actor.findChar(c) == null) return c;
		}
		return -1;
	}

	// *** the doors ***

	//the portal from inside: out by the doorstep, right next to it
	@Override
	public boolean activateTransition( Hero hero, LevelTransition transition ) {
		if (locked) return false;
		Cathedral.leave(Cathedral.DOOR);
		return true;
	}

	// *** who and what is inside ***

	@Override
	protected void createMobs() {
		CathedralPlan.populate(this);
	}

	@Override
	public Mob createMob() {
		return CathedralPlan.straggler();
	}

	@Override
	protected void createItems() {
		CathedralPlan.items(this);
	}

	@Override
	public Actor addRespawner() {
		if (kiln == null) kiln = new Kiln();
		Actor.add(kiln);
		return kiln;
	}

	// *** heat ***

	//pending: the ground about to catch fire and the masonry about to fall, marked a turn or two before
	private static final int EMBER = 0, DEBRIS = 1;

	//the furnace burns hotter: more ground turns to live embers (marked first) and the faithful stir
	void hotter( int heat ){
		GLog.w(Messages.get(Cathedral.class, "hotter_" + Math.min(heat, Cathedral.MAX_HEAT)));
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.7f);
		CathedralScreen.heat();
		ArrayList<Integer> cells = new ArrayList<>();
		int n = 8 + 5 * heat;
		for (int tries = 0; tries < 400 && cells.size() < n; tries++){
			int c = Random.Int(length());
			if (!emberable(c) || cells.contains(c)) continue;
			cells.add(c);
			//they come in patches
			for (int k : PathFinder.NEIGHBOURS4){
				if (Random.Int(3) == 0 && emberable(c + k) && !cells.contains(c + k)) cells.add(c + k);
			}
		}
		pend(EMBER, cells, 2);
		CathedralPlan.stir(this, 2 + heat);
	}

	private boolean emberable( int c ){
		if (c < 0 || c >= length() || zone(c) == OUT || zone(c) == NARTHEX) return false;
		int t = map[c];
		if (t != Terrain.EMPTY && t != Terrain.EMPTY_DECO && t != Terrain.EMPTY_SP) return false;
		return distance(c, entrance()) > 3;
	}

	//the heart's cage cracked: the dais round it catches fire, all but the cells right against it
	void scorchDais( FurnaceHeart h ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < length(); i++){
			if (zone(i) == DAIS && !h.covers(i) && h.reach(i) > 1 && emberable(i)) cells.add(i);
		}
		pend(EMBER, cells, 2);
	}

	//it got up out there
	void risen(){
		GLog.n(Messages.get(Cathedral.class, "risen"));
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
		CathedralScreen.risen();
		Music.INSTANCE.play(Assets.Music.CITY_BOSS_FINALE, true);
		CathedralPlan.stir(this, 99);
		Senses.noise(heart, Senses.STAMPEDE);
	}

	//masonry coming down: round every hero and somewhere else, marked two turns before
	void debris(){
		ArrayList<Integer> cells = new ArrayList<>();
		ArrayList<Integer> centers = new ArrayList<>();
		for (Hero h : Party.living()){
			centers.add(near(h.pos, 2));
		}
		for (int i = 0; i < 2; i++) centers.add(randomRespawnCell(null));
		for (int c : centers){
			if (c < 0) continue;
			for (int k : PathFinder.NEIGHBOURS9){
				int x = c + k;
				if (x >= 0 && x < length() && !solid[x] && zone(x) != OUT && !cells.contains(x)) cells.add(x);
			}
		}
		pend(DEBRIS, cells, 2);
		CathedralScreen.tremor();
	}

	private int near( int cell, int r ){
		ArrayList<Integer> options = new ArrayList<>();
		for (int c : SKCombat.area(cell, r)){
			if (passable[c]) options.add(c);
		}
		return options.isEmpty() ? cell : Random.element(options);
	}

	private void pend( int kind, ArrayList<Integer> cells, int turns ){
		if (cells.isEmpty() || kiln == null) return;
		float delay = SKCombat.announceDelay(turns);
		SKCombat.mark(cells, delay);
		kiln.add(kind, cells, (int) Math.ceil(delay));
	}

	//a pending thing comes down
	private void land( int kind, int[] cells ){
		if (kind == EMBER){
			for (int c : cells){
				if (!emberable(c)) continue;
				set(c, Terrain.EMBERS, this);
				GameScene.updateMap(c);
				if (heroFOV[c]) CellEmitter.get(c).burst(FlameParticle.FACTORY, 3);
				Char ch = Actor.findChar(c);
				if (ch != null) occupyCell(ch);
			}
			return;
		}
		boolean seen = false;
		for (int c : cells){
			if (heroFOV[c]){
				seen = true;
				CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
			}
		}
		if (seen) Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f, 1.1f);
		ArrayList<Char> hit = new ArrayList<>();
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null && ch.isAlive() && !hit.contains(ch) && !(ch instanceof FurnaceHeart)) hit.add(ch);
		}
		for (Char ch : hit){
			int before = ch.HP;
			ch.damage(Math.max(1, Random.NormalIntRange(40, 70) - ch.drRoll() / 2), this);
			CoopLog.line("cathedral: masonry hits " + ch.getClass().getSimpleName() + " for " + (before - ch.HP));
		}
	}

	//the turns of the furnace: Cathedral's clock, and what's pending landing
	public static class Kiln extends Actor {

		{
			actPriority = BUFF_PRIO;
		}

		private final ArrayList<int[]> pending = new ArrayList<>();

		void add( int kind, ArrayList<Integer> cells, int turns ){
			int[] p = new int[cells.size() + 2];
			p[0] = kind;
			p[1] = turns;
			for (int i = 0; i < cells.size(); i++) p[i + 2] = cells.get(i);
			pending.add(p);
		}

		@Override
		protected boolean act() {
			if (Dungeon.level instanceof CathedralLevel){
				CathedralLevel l = (CathedralLevel) Dungeon.level;
				for (int[] p : pending.toArray(new int[0][])){
					if (--p[1] > 0) continue;
					pending.remove(p);
					int[] cells = new int[p.length - 2];
					System.arraycopy(p, 2, cells, 0, cells.length);
					l.land(p[0], cells);
				}
				Cathedral.tick(l);
			}
			spend(TICK);
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("count", pending.size());
			for (int i = 0; i < pending.size(); i++) bundle.put("p" + i, pending.get(i));
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			pending.clear();
			int n = bundle.getInt("count");
			for (int i = 0; i < n; i++) pending.add(bundle.getIntArray("p" + i));
		}
	}

	// *** looks ***

	@Override
	public Group addVisuals() {
		super.addVisuals();
		visuals.add(new CathedralScreen.Glow(this));
		return visuals;
	}

	@Override
	public String tileName( int tile ) {
		switch (tile){
			case Terrain.WATER:       return Messages.get(CathedralLevel.class, "molten_name");
			case Terrain.EMBERS:      return Messages.get(CathedralLevel.class, "embers_name");
			case Terrain.CUSTOM_DECO: return Messages.get(CathedralLevel.class, "pew_name");
			case Terrain.STATUE:      return Messages.get(CathedralLevel.class, "saint_name");
			case Terrain.WALL_DECO:   return Messages.get(CathedralLevel.class, "window_name");
			case Terrain.ENTRANCE:    return Messages.get(CathedralLevel.class, "portal_name");
			case Terrain.EMPTY_SP:    return Messages.get(CathedralLevel.class, "runner_name");
			default:                  return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile){
			case Terrain.WATER:       return Messages.get(CathedralLevel.class, "molten_desc");
			case Terrain.EMBERS:      return Messages.get(CathedralLevel.class, "embers_desc");
			case Terrain.CUSTOM_DECO: return Messages.get(CathedralLevel.class, "pew_desc");
			case Terrain.STATUE:      return Messages.get(CathedralLevel.class, "saint_desc");
			case Terrain.WALL_DECO:   return Messages.get(CathedralLevel.class, "window_desc");
			case Terrain.ENTRANCE:    return Messages.get(CathedralLevel.class, "portal_desc");
			case Terrain.EMPTY_SP:    return Messages.get(CathedralLevel.class, "runner_desc");
			default:                  return super.tileDesc(tile);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("zone", zone);
		bundle.put("low", low);
		bundle.put("high", high);
		bundle.put("heart", heart);
		bundle.put("grates", grates);
		if (kiln != null) bundle.put("kiln", kiln);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		zone = bundle.getIntArray("zone");
		low = bundle.getIntArray("low");
		high = bundle.getIntArray("high");
		heart = bundle.getInt("heart");
		grates = bundle.getIntArray("grates");
		if (bundle.contains("kiln")) kiln = (Kiln) bundle.get("kiln");
		viewDistance = Cathedral.GLOW_SIGHT;
	}
}
