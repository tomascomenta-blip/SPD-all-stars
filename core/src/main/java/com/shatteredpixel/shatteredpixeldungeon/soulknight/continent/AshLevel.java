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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Mutations;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogTongue;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//the ash plains, floors 27-30 (branch 4, depths 21-24). the caves painter makes wide open messy
//ground, the "water" is black lava, it's dark, food's bad and the yogs are everywhere.
//the third floor has the besieged shrine
public class AshLevel extends CavesLevel {

	{
		color1 = 0x5a3020;
		color2 = 0x2a1814;
	}

	public static final int SHRINE_DEPTH = 23;

	@Override
	public void create() {
		super.create();
		viewDistance = Continent.BASE_SIGHT;
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.playTracks(HallsLevel.HALLS_TRACK_LIST, HallsLevel.HALLS_TRACK_CHANCES, false);
	}

	@Override
	public String tilesTex() {
		return ContinentAssets.TILES_ASH;
	}

	@Override
	public String waterTex() {
		return ContinentAssets.WATER_ASH;
	}

	//no dwarves out here
	@Override
	protected ArrayList<Room> questRooms( ArrayList<Room> rooms ){
		if (Dungeon.depth == SHRINE_DEPTH) rooms.add(new Shrine.ShrineRoom());
		return rooms;
	}

	@Override
	protected void placeQuests() {
	}

	@Override
	protected int specialRooms( boolean forceMax ) {
		return forceMax ? 2 : 1 + Random.Int(2);
	}

	@Override
	public Mob createMob() {
		ArrayList<Class<? extends Mob>> rot = ContinentMobs.rotation(Dungeon.depth, Dungeon.branch);
		Mob m = Reflection.newInstance(Random.element(rot));
		return m;
	}

	@Override
	protected void createMobs() {
		super.createMobs();
		//tongues wait in their puddles, away from the stairs
		int tongues = Dungeon.depth - 20;
		for (int i = 0; i < tongues; i++){
			int cell = randomRespawnCell(null);
			if (cell == -1 || distance(cell, entrance()) < 6 || findMob(cell) != null) continue;
			YogTongue t = new YogTongue();
			t.pos = cell;
			mobs.add(t);
		}
		for (Room r : rooms){
			if (r instanceof Shrine.ShrineRoom) Shrine.place(this, (Shrine.ShrineRoom) r, Continent.SHRINE_ASH);
		}
	}

	//scarce: fewer things, bad food, one torch if you're lucky
	@Override
	protected void createItems() {
		int n = 2 + Random.chances(new float[]{3, 2, 1});
		for (int i = 0; i < n; i++){
			Item it = Generator.random();
			if (it == null) continue;
			if (it instanceof Food) it = Random.Int(4) == 0 ? it : TaintedFood.random();
			drop(it, randomDropCell());
		}
		if (Random.Int(3) != 0) drop(new Torch(), randomDropCell());
		drop(new Gold().random(), randomDropCell());
		for (Item it : new ArrayList<>(itemsToSpawn)){
			drop(it, randomDropCell());
		}
		itemsToSpawn.clear();
	}

	// *** black lava ***

	@Override
	public void buildFlagMaps() {
		super.buildFlagMaps();
		for (int i = 0; i < length(); i++){
			if (map[i] == Terrain.WATER) avoid[i] = true;
		}
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell(ch);
		if (map[ch.pos] == Terrain.WATER && !ch.flying && !Char.hasProp(ch, Char.Property.FIERY)
				&& ch.isAlive()){
			burnInLava(ch);
		}
	}

	public static void burnInLava( Char ch ){
		if (Mutations.lavaproof(ch)) return;
		int dmg = Random.NormalIntRange(6, 12) + ch.HT / 25;
		ch.damage(dmg, AshLevel.class);
		Buff.prolong(ch, Cripple.class, 2f);
		if (ch.sprite != null) CellEmitter.get(ch.pos).burst(FlameParticle.FACTORY, 6);
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.6f);
	}

	@Override
	public void updateFieldOfView( Char c, boolean[] fieldOfView ) {
		super.updateFieldOfView(c, fieldOfView);
		Continent.glowSight(this, c, fieldOfView);
	}

	@Override
	public String tileName( int tile ) {
		switch (tile){
			case Terrain.WATER:        return Messages.get(AshLevel.class, "lava_name");
			case Terrain.GRASS:        return Messages.get(AshLevel.class, "grass_name");
			case Terrain.HIGH_GRASS:   return Messages.get(AshLevel.class, "high_grass_name");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT: return Messages.get(AshLevel.class, "basalt_name");
			case Terrain.EMPTY:
			case Terrain.EMPTY_DECO:   return Messages.get(AshLevel.class, "floor_name");
			default:                   return super.tileName(tile);
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile){
			case Terrain.WATER:        return Messages.get(AshLevel.class, "lava_desc");
			case Terrain.GRASS:        return Messages.get(AshLevel.class, "grass_desc");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT: return Messages.get(AshLevel.class, "basalt_desc");
			case Terrain.EMPTY:
			case Terrain.EMPTY_DECO:   return Messages.get(AshLevel.class, "floor_desc");
			default:                   return super.tileDesc(tile);
		}
	}
}
