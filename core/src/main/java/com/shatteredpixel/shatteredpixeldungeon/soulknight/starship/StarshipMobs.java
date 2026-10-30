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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Starship mobs (floors 11-14). aliens + varkolyns
public class StarshipMobs {

	//purple poison cloud where it dies (varkolyns, tentacles)
	static void poisonPool( int cell, int amount ){
		if (Dungeon.level != null && cell >= 0) GameScene.add(Blob.seed(cell, amount, ToxicGas.class));
	}

	//laser along marked cells, hits everyone on them
	static void laser( SKMob mob, ArrayList<Integer> cells, int min, int max ){
		if (mob.sprite != null && mob.sprite.parent != null && !cells.isEmpty()){
			mob.sprite.parent.add(new Beam.DeathRay(mob.sprite.center(),
					DungeonTilemap.raisedTileCenterToWorld(cells.get(cells.size()-1))));
		}
		if (Dungeon.level.heroFOV[mob.pos]) Sample.INSTANCE.play(Assets.Sounds.RAY);
		for (Char ch : SKCombat.foesIn(mob, cells)){
			ch.damage(SKCombat.specialDamage(mob, ch, min, max), mob);
		}
	}

	//varkolyn. small purple alien, bites, leaves poison when it dies
	public static class Varkolyn extends SKMob {
		{
			spriteClass = VarkolynSprite.class;
			HP = HT = 39;
			defenseSkill = 14;
			EXP = 9;
			maxLvl = 20;
			loot = Gold.class;
			lootChance = 0.3f;
		}
		@Override public int damageRoll() { return Random.NormalIntRange(8, 22); }
		@Override public int attackSkill( Char target ) { return 18; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 5); }

		//leaves an egg where it dies, hatches unless broken. not for the ones born from an egg or a nest, and max
		//6 eggs per floor, otherwise it goes exponential and the floor never ends
		private boolean egg = true;

		public void noEgg(){
			egg = false;
		}

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			poisonPool(at, 15);
			if (egg && Dungeon.level != null && eggs() < 6 && Actor.findChar(at) == null){
				StarshipFeatures.VarkolynEgg e = new StarshipFeatures.VarkolynEgg();
				e.pos = at;
				GameScene.add(e);
			}
		}

		private static int eggs(){
			int n = 0;
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof StarshipFeatures.VarkolynEgg) n++;
			}
			return n;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("egg", egg);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			egg = !bundle.contains("egg") || bundle.getBoolean("egg");
		}
	}

	//laser alien: aims for a turn, then the laser goes through the whole line
	public static class AlienLaser extends SKMob {
		{
			spriteClass = AlienLaserSprite.class;
			HP = HT = 45;
			defenseSkill = 14;
			EXP = 10;
			maxLvl = 20;
			loot = Generator.Category.WAND;
			lootChance = 0.05f;
		}
		@Override protected int specialCooldown() { return 4; }
		@Override public int damageRoll() { return Random.NormalIntRange(10, 17); }
		@Override public int attackSkill( Char target ) { return 18; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 6); }

		@Override
		protected boolean special( Char enemy ) {
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (d < 2 || d > 7 || !fieldOfView[enemy.pos]) return false;
			ArrayList<Integer> cells = line(enemy.pos, 8);
			if (!cells.contains(enemy.pos)) return false;
			announce(0, cells, enemy.pos);
			return true;
		}

		//2 lasers in a row, the 2nd one aimed where the target moved
		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			laser(this, cells, 15, 30);
			if (kind == 0 && enemy != null && enemy.isAlive() && fieldOfView != null && fieldOfView[enemy.pos]){
				ArrayList<Integer> next = line(enemy.pos, 8);
				if (next.contains(enemy.pos)) announce(1, next, enemy.pos);
			}
		}
	}

	//sword alien. tough, hits hard, no le dejes acercarse
	public static class AlienSword extends SKMob {
		{
			spriteClass = AlienSwordSprite.class;
			HP = HT = 53;
			defenseSkill = 12;
			EXP = 10;
			maxLvl = 20;
			loot = Generator.Category.WEAPON;
			lootChance = 0.06f;
		}
		@Override public int damageRoll() { return Random.NormalIntRange(10, 27); }
		@Override public int attackSkill( Char target ) { return 21; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 9); }

		//cloaked, invisible untill it attacks, hides again after a few turns
		private boolean cloaked = false;
		private int sinceAttack = 0;

		public boolean cloaked(){
			return cloaked;
		}

		private void cloak( boolean on ){
			if (on == cloaked) return;
			cloaked = on;
			if (on) invisible++; else invisible--;
			if (sprite != null && Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 4);
		}

		@Override
		protected boolean act() {
			if (!cloaked && ++sinceAttack >= 6) cloak(true);
			return super.act();
		}

		@Override
		public int attackProc( Char enemy, int damage ) {
			cloak(false);
			sinceAttack = 0;
			return super.attackProc(enemy, damage);
		}

		@Override
		public void damage( int dmg, Object src ) {
			super.damage(dmg, src);
			if (isAlive()){
				cloak(false);
				sinceAttack = 0;
			}
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("cloaked", cloaked);
			bundle.put("since_attack", sinceAttack);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			cloaked = bundle.getBoolean("cloaked");
			if (cloaked) invisible++;
			sinceAttack = bundle.getInt("since_attack");
		}
	}

	//ufo. fast, hard to hit, shoots, sometimes a laser fan. I hate this one
	public static class Ufo extends SKMob {
		{
			spriteClass = UfoSprite.class;
			HP = HT = 34;
			defenseSkill = 20;
			baseSpeed = 1.5f;
			flying = true;
			EXP = 10;
			maxLvl = 20;
			properties.add(Property.INORGANIC);
		}
		@Override protected int shotRange() { return 5; }
		@Override protected int specialCooldown() { return 4; }
		@Override public int damageRoll() { return Random.NormalIntRange(4, 11); }
		@Override public int attackSkill( Char target ) { return 18; }
		//flies over chasms/water and backs off when you get close
		@Override protected boolean keepsDistance() { return true; }

		@Override
		protected boolean special( Char enemy ) {
			if (!fieldOfView[enemy.pos] || Dungeon.level.distance(pos, enemy.pos) > 6) return false;
			arc(3, 30, enemy.pos, 2.5f, ItemSpriteSheet.SHOT_LASER, 4, 9);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.ZAP, 1f, 1.4f);
			spend(TICK);
			return true;
		}
	}

	//tentacle. never moves, throws spikes all around, poison on death
	public static class Tentacle extends SKMob {
		{
			spriteClass = TentacleSprite.class;
			HP = HT = 58;
			defenseSkill = 12;
			EXP = 8;
			maxLvl = 20;
			properties.add(Property.IMMOVABLE);
		}
		@Override protected int specialCooldown() { return 3; }
		@Override public int damageRoll() { return Random.NormalIntRange(10, 20); }
		@Override public int attackSkill( Char target ) { return 16; }
		@Override protected boolean getCloser( int target ) { return false; }
		@Override protected boolean getFurther( int target ) { return false; }

		//every other time it sinks into the floor and pops out near its target (crack marked a turn before)
		private boolean burrowNext = false;
		private static final int EMERGE = 1;

		@Override
		protected boolean special( Char enemy ) {
			if (!fieldOfView[enemy.pos] || Dungeon.level.distance(pos, enemy.pos) > 6) return false;
			burrowNext = !burrowNext;
			if (burrowNext && Dungeon.level.distance(pos, enemy.pos) > 2){
				ArrayList<Integer> spots = new ArrayList<>();
				for (int c : SKCombat.area(enemy.pos, 2)){
					if (Dungeon.level.distance(c, enemy.pos) == 2 && Dungeon.level.passable[c] && !Dungeon.level.pit[c]
							&& Actor.findChar(c) == null) spots.add(c);
				}
				if (!spots.isEmpty()){
					int to = spots.get(Random.Int(spots.size()));
					java.util.ArrayList<Integer> mark = new java.util.ArrayList<>();
					mark.add(to);
					announce(EMERGE, mark, to);
					return true;
				}
			}
			ring(8, enemy.pos, 1f, ItemSpriteSheet.SHOT_SPIKE, 5, 13);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			spend(TICK);
			return true;
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			if (kind != EMERGE || cells.isEmpty()) return;
			int to = cells.get(0);
			if (Actor.findChar(to) != null || !Dungeon.level.passable[to]) return;
			if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 4);
			if (sprite != null) sprite.place(to);
			move(to, false);
			if (Dungeon.level.heroFOV[to]){
				CellEmitter.get(to).burst(Speck.factory(Speck.ROCK), 6);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.7f, 1.3f);
			}
		}

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			poisonPool(at, 15);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("burrow_next", burrowNext);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			burrowNext = bundle.getBoolean("burrow_next");
		}
	}

	//alien battery: fixed turret, big bullet that splits into many, or a laser
	public static class Battery extends SKMob {
		{
			spriteClass = BatterySprite.class;
			HP = HT = 67;
			defenseSkill = 11;
			EXP = 9;
			maxLvl = 20;
			properties.add(Property.IMMOVABLE);
			properties.add(Property.INORGANIC);
			loot = Generator.Category.WAND;
			lootChance = 0.08f;
		}
		@Override protected int specialCooldown() { return 3; }
		@Override public int damageRoll() { return Random.NormalIntRange(8, 19); }
		@Override public int attackSkill( Char target ) { return 16; }
		@Override protected boolean getCloser( int target ) { return false; }
		@Override protected boolean getFurther( int target ) { return false; }

		@Override
		protected boolean special( Char enemy ) {
			if (!fieldOfView[enemy.pos] || Dungeon.level.distance(pos, enemy.pos) > 7) return false;
			if (Random.Int(2) == 0){
				ArrayList<Integer> cells = line(enemy.pos, 8);
				if (cells.contains(enemy.pos)){
					announce(0, cells, enemy.pos);
					return true;
				}
			}
			SKBulletField.Bullet b = shoot(SKBulletField.angleTo(pos, enemy.pos), 1f, ItemSpriteSheet.SHOT_BIG_ORB, 8, 16);
			b.behaviour = SKBulletField.SPLIT;
			b.timer = 2;
			b.children = 12;
			b.spread = 360;
			b.childImage = ItemSpriteSheet.SHOT_SPIKE;
			b.childSpeed = SKBulletField.UNIT;
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			spend(TICK);
			return true;
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			laser(this, cells, 12, 24);
		}

		//spike ring when destroyed, petty
		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			if (Dungeon.level == null) return;
			if (Dungeon.level.heroFOV[at]){
				CellEmitter.center(at).burst(BlastParticle.FACTORY, 10);
				Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.7f);
			}
			SKBulletField field = SKBulletField.get();
			for (int i = 0; i < 8; i++){
				field.fire(this, at, i*45f, 1.5f, ItemSpriteSheet.SHOT_SPIKE, 6, 14);
			}
		}
	}

	// *** sprites ***

	public static class VarkolynSprite extends SKMobSprite {
		public VarkolynSprite(){
			super();
			setup(SKAssets.VARKOLYN, 18, 14);
		}
		@Override
		public int blood() {
			return 0xFF9944CC;
		}
	}

	public static class AlienLaserSprite extends SKMobSprite {
		public AlienLaserSprite(){
			super();
			setup(SKAssets.ALIEN_LASER, 16, 16);
		}
	}

	public static class AlienSwordSprite extends SKMobSprite {
		public AlienSwordSprite(){
			super();
			setup(SKAssets.ALIEN_SWORD, 16, 16);
		}
	}

	public static class UfoSprite extends SKMobSprite {
		public UfoSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_LASER;
			shotSound = Assets.Sounds.ZAP;
			setup(SKAssets.UFO, 18, 15);
		}
		@Override
		public int blood() {
			return 0xFFBBBBBB;
		}
	}

	public static class TentacleSprite extends SKMobSprite {
		public TentacleSprite(){
			super();
			setup(SKAssets.TENTACLE, 16, 18);
		}
		@Override
		public int blood() {
			return 0xFF9944CC;
		}
	}

	public static class BatterySprite extends SKMobSprite {
		public BatterySprite(){
			super();
			setup(SKAssets.ALIEN_TURRET, 16, 16);
		}
		@Override
		public int blood() {
			return 0xFFBBBBBB;
		}
	}
}
