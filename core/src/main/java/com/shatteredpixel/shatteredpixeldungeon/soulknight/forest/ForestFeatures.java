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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forest;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKChampion;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKFloor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//forest stuff besides the mobs:
//- goblin camps: campfire, war drum, a few goblins sleeping around. a goblin by the drum that's
//  fighting beats it and every goblin within 8 cells shows up
//- totems shielding nearby goblins until you break them
//- brambles + poison mushrooms (SKFloor)
//- boars, in pairs (Boar)
public class ForestFeatures {

	public static boolean isGoblin( Char ch ){
		return ch instanceof GoblinSpearman || ch instanceof GoblinGunner || ch instanceof GoblinGiant;
	}

	// *** campfire. just decoration, can't be broken ***

	public static class Campfire extends SKProps.SKProp {

		{
			spriteClass = CampfireSprite.class;
			HP = HT = 1;
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}
	}

	// *** the war drum ***

	public static class GoblinDrum extends SKProps.SKProp {

		{
			spriteClass = DrumSprite.class;
		}

		private int cooldown = 0;

		public GoblinDrum(){
			super();
			HP = HT = 6 + Dungeon.scalingDepth();
		}

		@Override
		protected boolean act() {
			spend(TICK);
			if (cooldown > 0){
				cooldown--;
				return true;
			}
			Mob drummer = null;
			for (Mob m : Dungeon.level.mobs){
				if (isGoblin(m) && m.state == m.HUNTING && Dungeon.level.distance(m.pos, pos) <= 3){
					drummer = m;
					break;
				}
			}
			if (drummer == null) return true;
			Hero target = Party.nearestLiving(drummer.pos);
			if (target == null) return true;
			cooldown = 25;
			int called = 0;
			for (Mob m : Dungeon.level.mobs){
				if ((isGoblin(m) || m instanceof Boar) && m != drummer && m.state != m.HUNTING
						&& Dungeon.level.distance(m.pos, pos) <= 8){
					m.beckon(target.pos);
					called++;
				}
			}
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			if (Dungeon.level.heroFOV[pos]){
				sprite.showStatus(CharSprite.WARNING, Messages.get(GoblinDrum.class, "beat"));
				CellEmitter.center(pos).burst(Speck.factory(Speck.SCREAM), 3);
			}
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.8f, 0.8f);
			GLog.w(Messages.get(GoblinDrum.class, "heard"));
			CoopLog.line("goblin drum beats at " + pos + " called=" + called);
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("drum_cooldown", cooldown);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			cooldown = bundle.getInt("drum_cooldown");
		}
	}

	public static class GoblinTotem extends SKProps.SKProp {

		{
			spriteClass = TotemSprite.class;
		}

		private int timer = 3;

		public GoblinTotem(){
			super();
			HP = HT = 10 + Dungeon.scalingDepth();
		}

		@Override
		protected boolean act() {
			spend(TICK);
			if (--timer > 0) return true;
			timer = 3;
			int shield = 2 + Dungeon.scalingDepth()/2;
			boolean any = false;
			for (Mob m : Dungeon.level.mobs){
				if (!isGoblin(m) || !m.isAlive() || Dungeon.level.distance(m.pos, pos) > 3) continue;
				Barrier b = m.buff(Barrier.class);
				if (b != null && b.shielding() >= shield) continue;
				Buff.affect(m, Barrier.class).setShield(shield);
				any = true;
			}
			if (any){
				if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
				if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.CHARMS, 0.5f, 1.2f);
			}
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("totem_timer", timer);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			timer = bundle.getInt("totem_timer");
		}
	}

	// placing it all at levelgen, after the mobs

	public static void decorate( Level level, ArrayList<Room> rooms ){
		int depth = Dungeon.depth;
		ArrayList<Room> spots = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && !r.isEntrance() && !r.isExit() && r.width() >= 7 && r.height() >= 7){
				spots.add(r);
			}
		}
		Random.shuffle(spots);

		//goblin camp on most floors, no en todos
		int camps = 0;
		if (Random.Int(10) < 6 || DevFlags.skForest()){
			for (Room r : spots){
				if (camp(level, r, depth)){
					spots.remove(r);
					camps++;
					break;
				}
			}
		}

		//this floor's ritual totem, room farthest from the entrance
		RitualTotems.place(level, rooms);

		//totems from floor 2 on
		int totems = depth >= 2 ? 1 + Random.Int(2) : 0;
		for (Room r : spots){
			if (totems <= 0) break;
			int c = SKProps.nearCenter(level, r);
			if (c == -1) continue;
			GoblinTotem t = new GoblinTotem();
			t.pos = c;
			level.mobs.add(t);
			totems--;
		}

		//brambles + shrooms
		SKFloor floor = SKFloor.getOrMake(level);
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int c = 0; c < level.length(); c++){
			if (floorFree(level, c, floor)) candidates.add(c);
		}
		Random.shuffle(candidates);
		int clusters = 2 + Random.Int(3);
		for (int c : candidates){
			if (clusters <= 0) break;
			if (!floorFree(level, c, floor)) continue;
			int size = 3 + Random.Int(4);
			floor.put(level, c, SKFloor.BRAMBLE);
			for (int k = 0; k < size; k++){
				int n = c + PathFinder.NEIGHBOURS8[Random.Int(8)];
				if (floorFree(level, n, floor)) floor.put(level, n, SKFloor.BRAMBLE);
			}
			clusters--;
		}
		int shrooms = 3 + Random.Int(3);
		for (int c : candidates){
			if (shrooms <= 0) break;
			if (!floorFree(level, c, floor)) continue;
			floor.put(level, c, SKFloor.MUSHROOM);
			shrooms--;
		}
		StringBuilder sizes = new StringBuilder();
		for (Room r : rooms) if (r instanceof StandardRoom) sizes.append(r.width()).append('x').append(r.height()).append(r.isEntrance() ? "E" : "").append(r.isExit() ? "X" : "").append(' ');
		CoopLog.line("forest features depth=" + depth + " camps=" + camps + " floor cells=" + floor.cells().size() + " rooms: " + sizes);
	}

	//brambles/mushrooms only grow in rooms. never on doors or stairs, or next to them
	private static boolean floorFree( Level level, int c, SKFloor floor ){
		if (c < level.width() || c >= level.length() - level.width()) return false;
		if (floor.kindAt(c) != 0 || !level.passable[c] || level.pit[c] || level.water[c]) return false;
		int t = level.map[c];
		if (t != Terrain.EMPTY && t != Terrain.GRASS && t != Terrain.EMPTY_DECO && t != Terrain.HIGH_GRASS && t != Terrain.FURROWED_GRASS) return false;
		if (level.heaps.get(c) != null || level.traps.get(c) != null || level.plants.get(c) != null) return false;
		for (int n : PathFinder.NEIGHBOURS9){
			int k = level.map[c + n];
			if (k == Terrain.DOOR || k == Terrain.OPEN_DOOR || k == Terrain.LOCKED_DOOR || k == Terrain.ENTRANCE || k == Terrain.EXIT) return false;
		}
		int entrance = level.entrance();
		if (entrance >= 0 && level.distance(c, entrance) < 3) return false;
		//not in corridors, needs some open space around
		int open = 0;
		for (int n : PathFinder.NEIGHBOURS8) if (level.passable[c + n]) open++;
		return open >= 6;
	}

	private static boolean camp( Level level, Room r, int depth ){
		int center = SKProps.nearCenter(level, r);
		if (center == -1) return false;
		int drumCell = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = center + n;
			if (level.passable[c] && !level.solid[c] && !level.pit[c] && level.findMob(c) == null && level.heaps.get(c) == null){
				drumCell = c;
				break;
			}
		}
		if (drumCell == -1) return false;
		Campfire fire = new Campfire();
		fire.pos = center;
		level.mobs.add(fire);
		GoblinDrum drum = new GoblinDrum();
		drum.pos = drumCell;
		level.mobs.add(drum);

		ArrayList<Integer> beds = new ArrayList<>();
		for (int y = -2; y <= 2; y++){
			for (int x = -2; x <= 2; x++){
				int c = center + x + y*level.width();
				if (c == center || c == drumCell || c < 0 || c >= level.length()) continue;
				if (level.passable[c] && !level.solid[c] && !level.pit[c] && level.findMob(c) == null
						&& r.inside(level.cellToPoint(c))) beds.add(c);
			}
		}
		Random.shuffle(beds);
		int goblins = Math.min(beds.size(), 2 + Random.Int(2));
		for (int i = 0; i < goblins; i++){
			Mob m;
			int roll = Random.Int(depth >= 4 ? 4 : (depth >= 3 ? 3 : 2));
			switch (roll){
				case 0: default: m = new GoblinSpearman(); break;
				case 1: m = new GoblinGunner(); break;
				case 2: m = new GoblinElite(); break;
				case 3: m = new GoblinShaman(); break;
			}
			SKChampion.roll(m);
			m.pos = beds.get(i);
			m.state = m.SLEEPING;
			level.mobs.add(m);
		}
		return true;
	}

	//some lone boars get a partner next to them. max 2 pairs or the first floors turn into a boar farm
	public static void pairBoars( Level level ){
		int pairs = 0;
		for (Mob m : new ArrayList<>(level.mobs)){
			if (!(m instanceof Boar) || ((Boar) m).partner() != -1) continue;
			if (pairs >= 2 || Random.Int(10) >= 4) continue;
			pairs++;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = m.pos + n;
				if (level.passable[c] && !level.solid[c] && !level.pit[c] && level.findMob(c) == null){
					Boar b = new Boar();
					SKChampion.roll(b);
					b.pos = c;
					b.state = m.state == m.SLEEPING ? b.SLEEPING : b.WANDERING;
					level.mobs.add(b);
					((Boar) m).pairWith(b);
					break;
				}
			}
		}
	}

	// *** sprites ***

	public static class CampfireSprite extends SKMobSprite {
		public CampfireSprite(){
			super();
			setup(SKAssets.CAMPFIRE, 16, 16);
		}
		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch != null) emitter().pour(FlameParticle.FACTORY, 0.15f);
		}
	}

	public static class DrumSprite extends SKMobSprite {
		public DrumSprite(){
			super();
			setup(SKAssets.GOBLIN_DRUM, 16, 16);
		}
		@Override
		public int blood() {
			return 0xFF8A5A2B;
		}
	}

	public static class TotemSprite extends SKMobSprite {
		public TotemSprite(){
			super();
			setup(SKAssets.GOBLIN_TOTEM, 16, 22);
		}
		@Override
		public int blood() {
			return 0xFF8A5A2B;
		}
	}
}
