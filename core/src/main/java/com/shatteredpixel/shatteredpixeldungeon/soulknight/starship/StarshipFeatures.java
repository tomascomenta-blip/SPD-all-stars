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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
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
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//starship stuff besides the mobs:
//- alien teleporter, spits out an alien every few turns (3 max) until it's broken
//- varkolyn nest, same thing w/ varkolyns (4 max)
//- laser fences across some rooms, 2 turns on 2 off, + a console that turns them all off
//- blackouts: lights go out now and then, heroes see 2 cells for 10 turns
//- varkolyn eggs (StarshipMobs.Varkolyn)
public class StarshipFeatures {

	// *** spawners ***

	//base for the teleporter + nest. every 'period' turns while a hero is within 8 cells, a new mob next to it,
	//'max' alive at most
	public static abstract class Spawner extends SKProps.SKProp {

		//enemies, unlike crates: allies attack them and ranged weapons can aim at them
		{
			alignment = Alignment.ENEMY;
		}

		protected int timer = 3;
		protected int[] spawned = new int[0];

		protected abstract int period();
		protected abstract int max();
		protected abstract Mob make();

		protected boolean champions(){
			return true;
		}

		@Override
		protected boolean act() {
			spend(TICK);
			if (--timer > 0) return true;
			timer = period();
			boolean heroNear = false;
			for (Hero h : Party.living()){
				if (Dungeon.level.distance(h.pos, pos) <= 8) heroNear = true;
			}
			if (!heroNear) return true;
			ArrayList<Integer> alive = new ArrayList<>();
			for (int id : spawned){
				Actor a = Actor.findById(id);
				if (a instanceof Char && ((Char) a).isAlive()) alive.add(id);
			}
			if (alive.size() >= max()){
				spawned = toArray(alive);
				return true;
			}
			int cell = -1;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = pos + n;
				if (Dungeon.level.passable[c] && !Dungeon.level.pit[c] && Actor.findChar(c) == null){
					cell = c;
					break;
				}
			}
			if (cell == -1) return true;
			Mob m = make();
			if (m instanceof SKMob) ((SKMob) m).summoned();
			if (champions()) SKChampion.roll(m);
			m.pos = cell;
			m.state = m.HUNTING;
			GameScene.add(m);
			ScrollOfTeleportation.appearVFX(m);
			alive.add(m.id());
			spawned = toArray(alive);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			CoopLog.line(getClass().getSimpleName() + " brings " + m.getClass().getSimpleName() + " at " + cell);
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("spawn_timer", timer);
			bundle.put("spawned", spawned);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			timer = bundle.getInt("spawn_timer");
			spawned = bundle.contains("spawned") ? bundle.getIntArray("spawned") : new int[0];
		}
	}

	public static class Teleporter extends Spawner {

		{
			spriteClass = TeleporterSprite.class;
		}

		public Teleporter(){
			super();
			HP = HT = 15 + Dungeon.scalingDepth();
		}

		@Override protected int period() { return 5; }
		@Override protected int max() { return 3; }

		@Override
		protected Mob make() {
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.7f);
			return Random.Int(2) == 0 ? new StarshipMobs.AlienLaser() : new StarshipMobs.AlienSword();
		}
	}

	public static class VarkolynNest extends Spawner {

		{
			spriteClass = NestSprite.class;
		}

		public VarkolynNest(){
			super();
			HP = HT = 20 + Dungeon.scalingDepth();
		}

		@Override protected int period() { return 6; }
		@Override protected int max() { return 4; }

		@Override
		protected Mob make() {
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.6f, 1.5f);
			StarshipMobs.Varkolyn v = new StarshipMobs.Varkolyn();
			v.noEgg();
			return v;
		}
	}

	//varkolyn egg, hatches after a few turns unless someone breaks it. break it.
	public static class VarkolynEgg extends Mob {

		{
			spriteClass = EggSprite.class;
			defenseSkill = 0;
			EXP = 1;
			maxLvl = 20;
			properties.add(Property.IMMOVABLE);
			state = PASSIVE;
		}

		private int hatch = 10;

		public VarkolynEgg(){
			super();
			HP = HT = 5 + Dungeon.scalingDepth()/3;
		}

		public void hatchIn( int turns ){
			hatch = turns;
		}

		@Override
		protected boolean act() {
			spend(TICK);
			if (--hatch == 2 && sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			if (hatch > 0) return true;
			int at = pos;
			if (Dungeon.level.heroFOV[at]) CellEmitter.center(at).burst(Speck.factory(Speck.STEAM), 6);
			destroy();
			if (sprite != null) sprite.die();
			StarshipMobs.Varkolyn v = new StarshipMobs.Varkolyn();
			v.noEgg();
			v.summoned();
			v.pos = at;
			v.state = v.HUNTING;
			GameScene.add(v);
			CoopLog.line("varkolyn egg hatches at " + at);
			return true;
		}

		@Override
		protected boolean getCloser( int target ) {
			return false;
		}

		@Override
		protected boolean getFurther( int target ) {
			return false;
		}

		@Override
		public int attackSkill( Char target ) {
			return 0;
		}

		@Override
		protected boolean canAttack( Char enemy ) {
			return false;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("hatch", hatch);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			hatch = bundle.getInt("hatch");
		}
	}

	// *** laser fences ***

	public static class LaserPost extends SKProps.SKProp {

		{
			spriteClass = PostSprite.class;
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

	public static class Console extends SKProps.SKProp {

		{
			spriteClass = ConsoleSprite.class;
			HP = HT = 1;
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
			SKFloor f = SKFloor.get(Dungeon.level);
			if (f == null || f.fencesOff){
				GLog.i(Messages.get(Console.class, "already"));
				return true;
			}
			f.fencesOff = true;
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.5f, 1.5f);
			GLog.p(Messages.get(Console.class, "off"));
			CoopLog.line("console turns the fences off");
			return true;
		}
	}

	public static class Blackout extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = false;
		}

		public static final float DURATION = 10f;

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				target.viewDistance = Math.min(target.viewDistance, 2);
				return true;
			}
			return false;
		}

		@Override
		public void detach() {
			super.detach();
			if (Dungeon.level != null) target.viewDistance = Dungeon.level.viewDistance;
			if (target == Dungeon.hero) Dungeon.observe();
		}

		@Override
		public int icon() {
			return BuffIndicator.BLINDNESS;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0xC591EC);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - visualcooldown()) / DURATION);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	public static void blackout(){
		for (Hero h : Party.living()){
			Buff.prolong(h, Blackout.class, Blackout.DURATION);
		}
		Dungeon.observe();
		GameScene.updateFog();
		Sample.INSTANCE.play(Assets.Sounds.DEGRADE);
		GLog.w(Messages.get(StarshipFeatures.class, "blackout"));
		CoopLog.line("blackout depth=" + Dungeon.depth);
	}

	//---- levelgen, after the mobs ----

	public static void decorate( Level level, ArrayList<Room> rooms ){
		boolean force = DevFlags.skStarship();
		ArrayList<Room> spots = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && !r.isEntrance() && !r.isExit() && r.width() >= 6 && r.height() >= 6){
				spots.add(r);
			}
		}
		Random.shuffle(spots);
		int teleporters = 0, nests = 0;
		if (Random.Int(10) < 7 || force){
			for (Room r : spots){
				int c = SKProps.nearCenter(level, r);
				if (c == -1) continue;
				Teleporter t = new Teleporter();
				t.pos = c;
				level.mobs.add(t);
				spots.remove(r);
				teleporters++;
				break;
			}
		}
		if (Random.Int(10) < 6 || force){
			for (Room r : spots){
				int c = SKProps.nearCenter(level, r);
				if (c == -1) continue;
				VarkolynNest n = new VarkolynNest();
				n.pos = c;
				level.mobs.add(n);
				spots.remove(r);
				nests++;
				break;
			}
		}

		//laser fences across rooms + the console that turns them off
		SKFloor floor = SKFloor.getOrMake(level);
		int fences = 0;
		int wanted = 1 + Random.Int(2) + (force ? 1 : 0);
		for (Room r : new ArrayList<>(spots)){
			if (fences >= wanted) break;
			if (fence(level, r, floor)){
				spots.remove(r);
				fences++;
			}
		}
		if (fences > 0){
			for (Room r : spots){
				int c = SKProps.nearCenter(level, r);
				if (c == -1) continue;
				Console k = new Console();
				k.pos = c;
				level.mobs.add(k);
				break;
			}
		}
		floor.blackoutTimer = (force ? 15 : 50) + Random.Int(40);
		CoopLog.line("starship features depth=" + Dungeon.depth + " teleporters=" + teleporters + " nests=" + nests + " fences=" + fences);
	}

	//fence across a row of the room: a post at each end, beam between
	private static boolean fence( Level level, Room r, SKFloor floor ){
		int w = level.width();
		ArrayList<Integer> rows = new ArrayList<>();
		for (int y = r.top + 2; y < r.bottom - 1; y++) rows.add(y);
		Random.shuffle(rows);
		for (int y : rows){
			int left = r.left + 1 + y*w, right = r.right - 1 + y*w;
			if (right - left < 3) continue;
			boolean ok = true;
			for (int c = left; c <= right && ok; c++){
				int t = level.map[c];
				if (!level.passable[c] || level.pit[c] || level.findMob(c) != null || level.heaps.get(c) != null
						|| (t != Terrain.EMPTY && t != Terrain.EMPTY_SP && t != Terrain.EMPTY_DECO)) ok = false;
			}
			//no doors next to the posts (they'd block them)
			for (int p : new int[]{left, right}){
				for (int n : PathFinder.NEIGHBOURS8){
					int k = level.map[p + n];
					if (k == Terrain.DOOR || k == Terrain.OPEN_DOOR || k == Terrain.LOCKED_DOOR) ok = false;
				}
			}
			if (!ok) continue;
			LaserPost a = new LaserPost();
			a.pos = left;
			level.mobs.add(a);
			LaserPost b = new LaserPost();
			b.pos = right;
			level.mobs.add(b);
			for (int c = left + 1; c < right; c++) floor.put(level, c, SKFloor.LASER);
			return true;
		}
		return false;
	}

	private static int[] toArray( ArrayList<Integer> list ){
		int[] a = new int[list.size()];
		for (int i = 0; i < a.length; i++) a[i] = list.get(i);
		return a;
	}

	// *** sprites ***

	public static class TeleporterSprite extends SKMobSprite {
		public TeleporterSprite(){
			super();
			setup(SKAssets.ALIEN_TELEPORTER, 16, 16);
		}
		@Override
		public int blood() {
			return 0xFF7DFF8A;
		}
	}

	public static class NestSprite extends SKMobSprite {
		public NestSprite(){
			super();
			setup(SKAssets.VARKOLYN_NEST, 22, 18);
		}
		@Override
		public int blood() {
			return 0xFF7B3FA0;
		}
	}

	public static class EggSprite extends SKMobSprite {
		public EggSprite(){
			super();
			setup(SKAssets.VARKOLYN_EGG, 12, 12);
		}
		@Override
		public int blood() {
			return 0xFF7DFF8A;
		}
	}

	public static class PostSprite extends SKMobSprite {
		public PostSprite(){
			super();
			setup(SKAssets.LASER_POST, 16, 16);
		}
		@Override
		public void update() {
			SKFloor f = Dungeon.level == null ? null : SKFloor.get(Dungeon.level);
			row(f != null && f.fencesOn() ? 0 : 1);
			super.update();
		}
	}

	public static class ConsoleSprite extends SKMobSprite {
		public ConsoleSprite(){
			super();
			setup(SKAssets.CONSOLE, 16, 16);
		}
		@Override
		public void update() {
			SKFloor f = Dungeon.level == null ? null : SKFloor.get(Dungeon.level);
			row(f != null && f.fencesOff ? 1 : 0);
			super.update();
		}
	}
}
