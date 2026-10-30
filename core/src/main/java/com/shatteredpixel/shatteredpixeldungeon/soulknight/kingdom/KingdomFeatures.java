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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKChampion;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKFloor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//kingdom stuff besides the mobs:
//- patrols: banner bearer + 2-3 knights following it. -25% dmg while the banner's up, they flee
//  for a few turns when the bearer dies
//- weapon racks, take a weapon but the alarm brings guards
//- spike tiles and arrow slits (SKFloor)
public class KingdomFeatures {

	public static boolean isKnight( Char ch ){
		return ch instanceof SKMob && ch.getClass().getName().contains(".kingdom.") && ch.alignment == Char.Alignment.ENEMY;
	}

	// *** the banner bearer ***

	public static class BannerBearer extends SKMob {

		{
			spriteClass = BannerSprite.class;

			HP = HT = 20;
			defenseSkill = 7;

			EXP = 6;
			maxLvl = 14;
		}

		@Override public int damageRoll() { return Random.NormalIntRange(1, 6); }
		@Override public int attackSkill( Char target ) { return 11; }

		@Override
		protected boolean act() {
			for (Mob m : Dungeon.level.mobs){
				if (m != this && isKnight(m) && Dungeon.level.distance(m.pos, pos) <= 4){
					Buff.prolong(m, Rallied.class, 2f);
				}
			}
			return super.act();
		}

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			Hero near = Party.nearestLiving(at);
			int fled = 0;
			for (Mob m : Dungeon.level.mobs){
				if (!isKnight(m) || Dungeon.level.distance(m.pos, at) > 6) continue;
				Rallied r = m.buff(Rallied.class);
				if (r != null) r.detach();
				if (near != null && !m.isImmune(Terror.class)){
					Buff.affect(m, Terror.class, 5f).object = near.id();
					fled++;
				}
			}
			if (fled > 0) GLog.p(Messages.get(BannerBearer.class, "fall"));
		}
	}

	//-25% dmg while the banner's near (SKMarks.damageFactor)
	public static class Rallied extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0xF07A84);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc");
		}
	}

	public static class WeaponRack extends SKProps.SKProp {

		{
			spriteClass = RackSprite.class;
			HP = HT = 1;
		}

		private boolean looted = false;

		public boolean looted(){
			return looted;
		}

		@Override
		public boolean heroShouldInteract() {
			return true;
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

		@Override
		public boolean interact( Char c ) {
			if (!(c instanceof Hero)) return true;
			if (looted){
				GLog.i(Messages.get(WeaponRack.class, "empty"));
				return true;
			}
			looted = true;
			Item weapon = Generator.randomWeapon();
			Dungeon.level.drop(weapon, c.pos).sprite.drop();
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			Sample.INSTANCE.play(Assets.Sounds.ALERT);
			GLog.w(Messages.get(WeaponRack.class, "alarm"));
			int guards = alarm();
			CoopLog.line("weapon rack looted at " + pos + " guards=" + guards);
			return true;
		}

		//2 or 3 guards come running. stealing is bad
		private int alarm(){
			ArrayList<Integer> spots = new ArrayList<>();
			for (int c = 0; c < Dungeon.level.length(); c++){
				int d = Dungeon.level.distance(c, pos);
				if (d < 3 || d > 6 || !Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
				spots.add(c);
			}
			Random.shuffle(spots);
			int n = Math.min(spots.size(), 2 + Random.Int(2));
			for (int i = 0; i < n; i++){
				Mob m = Random.Int(2) == 0 ? new KingdomMobs.Soldier() : new KingdomMobs.Swordsman();
				SKChampion.roll(m);
				m.pos = spots.get(i);
				m.state = m.HUNTING;
				GameScene.add(m);
				ScrollOfTeleportation.appearVFX(m);
			}
			return n;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("looted", looted);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			looted = bundle.getBoolean("looted");
		}
	}

	/* levelgen, runs after the mobs are placed */

	public static void decorate( Level level, ArrayList<Room> rooms ){
		boolean force = DevFlags.skKingdom();
		ArrayList<Room> spots = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && !r.isEntrance() && !r.isExit() && r.width() >= 6 && r.height() >= 6){
				spots.add(r);
			}
		}
		Random.shuffle(spots);
		SKFloor floor = SKFloor.getOrMake(level);
		int patrols = 0, racks = 0, spikeRooms = 0;

		//patrol + banner
		if (Random.Int(10) < 6 || force){
			for (Room r : spots){
				if (patrol(level, r)){
					spots.remove(r);
					patrols++;
					break;
				}
			}
		}
		//rack
		if (Random.Int(10) < 6 || force){
			for (Room r : spots){
				int c = SKProps.nearCenter(level, r);
				if (c == -1) continue;
				WeaponRack w = new WeaponRack();
				w.pos = c;
				level.mobs.add(w);
				spots.remove(r);
				racks++;
				break;
			}
		}
		//spikes in 1-2 rooms
		int spikes = 1 + Random.Int(2);
		for (Room r : spots){
			if (spikes <= 0) break;
			if (spikeRoom(level, r, floor)){
				spikes--;
				spikeRooms++;
			}
		}
		//arrow slits on walls looking down a room
		int slits = 2 + Random.Int(2);
		ArrayList<Integer> walls = new ArrayList<>();
		int w = level.width();
		for (int c = w; c < level.length() - w; c++){
			int t = level.map[c];
			if (t != Terrain.WALL && t != Terrain.WALL_DECO) continue;
			int below = c + w;
			if (!level.passable[below] || level.map[below] == Terrain.DOOR || level.map[below] == Terrain.OPEN_DOOR) continue;
			int open = 0;
			for (int k = 1; k <= 5; k++){
				int cell = c + k*w;
				if (cell >= level.length() || level.solid[cell]) break;
				open++;
			}
			if (open >= 4) walls.add(c);
		}
		Random.shuffle(walls);
		for (int c : walls){
			if (slits <= 0) break;
			boolean near = false;
			for (SKFloor.Slit s : floor.slits){
				if (level.distance(s.wall, c) < 6) near = true;
			}
			int entrance = level.entrance();
			if (near || (entrance >= 0 && level.distance(c, entrance) < 4)) continue;
			floor.slits.add(new SKFloor.Slit(c, 0, 1));
			slits--;
		}
		CoopLog.line("kingdom features depth=" + Dungeon.depth + " patrols=" + patrols + " racks=" + racks
				+ " spike rooms=" + spikeRooms + " slits=" + floor.slits.size());
	}

	private static boolean patrol( Level level, Room r ){
		int center = SKProps.nearCenter(level, r);
		if (center == -1) return false;
		ArrayList<Integer> beside = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = center + n;
			if (level.passable[c] && !level.solid[c] && !level.pit[c] && level.findMob(c) == null) beside.add(c);
		}
		if (beside.size() < 2) return false;
		BannerBearer b = new BannerBearer();
		b.pos = center;
		b.state = b.WANDERING;
		level.mobs.add(b);
		int escorts = Math.min(beside.size(), 2 + Random.Int(2));
		for (int i = 0; i < escorts; i++){
			SKMob m = Random.Int(2) == 0 ? new KingdomMobs.Soldier() : new KingdomMobs.Swordsman();
			SKChampion.roll(m);
			m.pos = beside.get(i);
			m.state = m.WANDERING;
			m.leader = b.id();
			level.mobs.add(m);
		}
		return true;
	}

	private static boolean spikeRoom( Level level, Room r, SKFloor floor ){
		int w = level.width();
		int placed = 0;
		for (int y = r.top + 1; y < r.bottom; y++){
			for (int x = r.left + 1; x < r.right; x++){
				if ((x + y) % 3 != 0) continue;
				int c = x + y*w;
				int t = level.map[c];
				if (t != Terrain.EMPTY && t != Terrain.EMPTY_SP && t != Terrain.EMPTY_DECO) continue;
				if (!level.passable[c] || level.findMob(c) != null || level.heaps.get(c) != null || level.traps.get(c) != null) continue;
				boolean nearDoor = false;
				for (int n : PathFinder.NEIGHBOURS9){
					int k = level.map[c + n];
					if (k == Terrain.DOOR || k == Terrain.OPEN_DOOR || k == Terrain.LOCKED_DOOR || k == Terrain.ENTRANCE || k == Terrain.EXIT) nearDoor = true;
				}
				if (nearDoor) continue;
				floor.put(level, c, SKFloor.SPIKES);
				placed++;
			}
		}
		return placed >= 4;
	}

	// *** sprites ***

	public static class BannerSprite extends SKMobSprite {
		public BannerSprite(){
			super();
			setup(SKAssets.BANNER_BEARER, 16, 16);
		}
	}

	public static class RackSprite extends SKMobSprite {
		public RackSprite(){
			super();
			setup(SKAssets.WEAPON_RACK, 16, 16);
		}
		@Override
		public void update() {
			if (ch instanceof WeaponRack) row(((WeaponRack) ch).looted() ? 1 : 0);
			super.update();
		}
	}
}
