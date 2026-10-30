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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKChampion;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Knight Kingdom mobs (floors 6-9). knights, slimes, wizards
public class KingdomMobs {

	//pistol soldier, shoots from range
	public static class Soldier extends SKMob {
		{
			spriteClass = SoldierSprite.class;
			HP = HT = 25;
			defenseSkill = 8;
			EXP = 6;
			maxLvl = 14;
			loot = Gold.class;
			lootChance = 0.3f;
		}
		@Override protected int shotRange() { return 5; }
		@Override public int damageRoll() { return Random.NormalIntRange(2, 9); }
		@Override public int attackSkill( Char target ) { return 13; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 4); }

		//under half hp it whistles for help, once
		private boolean called = false;

		@Override
		public void damage( int dmg, Object src ) {
			super.damage(dmg, src);
			if (!called && isAlive() && HP*2 <= HT){
				called = true;
				callHelp(this);
			}
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("called", called);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			called = bundle.getBoolean("called");
		}
	}

	//another soldier comes running, out of the heroes' sight if possible
	private static void callHelp( Mob caller ){
		ArrayList<Integer> hidden = new ArrayList<>();
		ArrayList<Integer> any = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++){
			int d = Dungeon.level.distance(c, caller.pos);
			if (d < 3 || d > 8 || !Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
			any.add(c);
			boolean seen = false;
			for (Hero h : Party.living()){
				if (h.fieldOfView != null && h.fieldOfView.length > c && h.fieldOfView[c]) seen = true;
			}
			if (!seen) hidden.add(c);
		}
		ArrayList<Integer> spots = hidden.isEmpty() ? any : hidden;
		if (caller.sprite != null && Dungeon.level.heroFOV[caller.pos]){
			caller.sprite.showStatus(CharSprite.WARNING, Messages.get(Soldier.class, "whistle"));
		}
		Sample.INSTANCE.play(Assets.Sounds.ALERT, 0.7f, 1.6f);
		if (spots.isEmpty()) return;
		Soldier s = new Soldier();
		s.called = true;
		SKChampion.roll(s);
		s.pos = spots.get(Random.Int(spots.size()));
		s.state = s.HUNTING;
		GameScene.add(s);
		ScrollOfTeleportation.appearVFX(s);
		CoopLog.line("soldier calls help at " + caller.pos + " -> " + s.pos);
	}

	//sword knight. plain and tough, nada especial
	public static class Swordsman extends SKMob {
		{
			spriteClass = SwordsmanSprite.class;
			HP = HT = 35;
			defenseSkill = 9;
			EXP = 7;
			maxLvl = 14;
			loot = Generator.Category.ARMOR;
			lootChance = 0.06f;
		}
		@Override public int damageRoll() { return Random.NormalIntRange(4, 11); }
		@Override public int attackSkill( Char target ) { return 13; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 5); }

		//raises its shield now and then, the next melee hit gets parried and answered right away
		private int guard = 0;

		public boolean guarding(){
			return guard > 0;
		}

		@Override protected int specialCooldown() { return 5; }

		@Override
		protected boolean act() {
			if (guard > 0 && --guard == 0 && sprite != null) sprite.remove(CharSprite.State.SHIELDED);
			return super.act();
		}

		@Override
		protected boolean special( Char enemy ) {
			if (!Dungeon.level.adjacent(pos, enemy.pos)) return false;
			guard = 3;
			if (sprite != null){
				sprite.add(CharSprite.State.SHIELDED);
				if (Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.WARNING, Messages.get(Swordsman.class, "guard"));
			}
			spend(TICK);
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (guard > 0 && dmg > 0 && src instanceof Char && Dungeon.level.adjacent(pos, ((Char) src).pos)){
				guard = 0;
				if (sprite != null){
					sprite.remove(CharSprite.State.SHIELDED);
					if (Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(Swordsman.class, "parry"));
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY);
				Char attacker = (Char) src;
				if (attacker.isAlive()) attack(attacker, 1f, 0, 2f);
				return;
			}
			super.damage(dmg, src);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("guard", guard);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			guard = bundle.getInt("guard");
		}
	}

	//musketeer: sometimes a burst of 3 fast bullets
	public static class Gunner extends SKMob {
		{
			spriteClass = GunnerSprite.class;
			HP = HT = 26;
			defenseSkill = 8;
			EXP = 7;
			maxLvl = 14;
			loot = Gold.class;
			lootChance = 0.3f;
		}
		@Override protected int shotRange() { return 4; }
		@Override protected int specialCooldown() { return 4; }
		@Override public int damageRoll() { return Random.NormalIntRange(2, 8); }
		@Override public int attackSkill( Char target ) { return 12; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 4); }

		//long burst: 3 volleys over 3 turns, each aimed where the target is NOW. stands still while fireing
		private int burst = 0;

		@Override
		protected boolean special( Char enemy ) {
			if (Dungeon.level.distance(pos, enemy.pos) > 6 || !fieldOfView[enemy.pos]) return false;
			burst = 3;
			volley(enemy);
			return true;
		}

		private void volley( Char target ){
			burst--;
			arc(3, 20, target.pos, 2f, ItemSpriteSheet.SHOT_BULLET, 3, 7);
			if (sprite != null){
				sprite.turnTo(pos, target.pos);
				if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			}
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW, 1f, 0.8f);
			spend(TICK);
		}

		@Override
		protected boolean act() {
			if (burst > 0){
				Char target = enemy;
				if (paralysed == 0 && state == HUNTING && target != null && target.isAlive()
						&& fieldOfView != null && fieldOfView[target.pos]){
					volley(target);
					return true;
				}
				burst = 0;
			}
			return super.act();
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("burst", burst);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			burst = bundle.getInt("burst");
		}
	}

	//elite rifleman. aims along a line for a turn, then a piercing shot hits everyone on it
	public static class Rifleman extends SKMob {
		{
			spriteClass = RiflemanSprite.class;
			HP = HT = 31;
			defenseSkill = 9;
			EXP = 8;
			maxLvl = 15;
			loot = Generator.Category.MISSILE;
			lootChance = 0.1f;
		}
		@Override protected int shotRange() { return 5; }
		@Override protected int specialCooldown() { return 5; }
		@Override public int damageRoll() { return Random.NormalIntRange(3, 9); }
		@Override public int attackSkill( Char target ) { return 14; }

		//laser sight follows the target for 2 turns (the marked line moves w/ it), then fires along the last one.
		//lose sight of the target and the shot is spoiled
		private int aimTurns = 0;
		private int aimTarget = -1;
		private int[] aimCells = new int[0];

		@Override
		protected boolean special( Char enemy ) {
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (d < 2 || d > 8 || !fieldOfView[enemy.pos]) return false;
			ArrayList<Integer> cells = line(enemy.pos, 8);
			if (!cells.contains(enemy.pos)) return false;
			aimTurns = 2;
			aimTarget = enemy.id();
			sight(cells, enemy.pos);
			if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.WARNING, Messages.get(Rifleman.class, "aim"));
			spend(TICK);
			return true;
		}

		private void sight( ArrayList<Integer> cells, int at ){
			aimCells = new int[cells.size()];
			for (int i = 0; i < aimCells.length; i++) aimCells[i] = cells.get(i);
			SKCombat.mark(cells, TICK);
			if (sprite != null) sprite.turnTo(pos, at);
		}

		@Override
		protected boolean act() {
			if (aimTurns > 0){
				Actor a = Actor.findById(aimTarget);
				Char target = a instanceof Char ? (Char) a : null;
				if (paralysed > 0 || state != HUNTING || target == null || !target.isAlive()
						|| fieldOfView == null || !fieldOfView[target.pos]){
					aimTurns = 0;
					if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.NEUTRAL, "?");
					return super.act();
				}
				aimTurns--;
				if (aimTurns > 0){
					sight(line(target.pos, 8), target.pos);
				} else {
					ArrayList<Integer> cells = new ArrayList<>();
					for (int c : aimCells) cells.add(c);
					release(0, cells, target.pos);
				}
				spend(TICK);
				return true;
			}
			return super.act();
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("aim_turns", aimTurns);
			bundle.put("aim_target", aimTarget);
			bundle.put("aim_cells", aimCells);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			aimTurns = bundle.getInt("aim_turns");
			aimTarget = bundle.contains("aim_target") ? bundle.getInt("aim_target") : -1;
			aimCells = bundle.contains("aim_cells") ? bundle.getIntArray("aim_cells") : new int[0];
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			if (sprite != null && sprite.parent != null && !cells.isEmpty()){
				sprite.parent.add(new Beam.LightRay(
						sprite.center(), DungeonTilemap.raisedTileCenterToWorld(cells.get(cells.size()-1))));
			}
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.RAY);
			for (Char ch : SKCombat.foesIn(this, cells)){
				ch.damage(SKCombat.specialDamage(this, ch, 8, 18), this);
			}
		}
	}

	//royal slime: bumps into you, pops into a bullet ring on death
	public static class RoyalSlime extends SKMob {
		{
			spriteClass = SlimeSprite.class;
			HP = HT = 31;
			defenseSkill = 5;
			EXP = 6;
			maxLvl = 14;
		}
		@Override public int damageRoll() { return Random.NormalIntRange(3, 8); }
		@Override public int attackSkill( Char target ) { return 12; }

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			if (Dungeon.level != null){
				if (Dungeon.level.heroFOV[at]) CellEmitter.center(at).burst(Speck.factory(Speck.BUBBLE), 6);
				SKBulletField field =
						SKBulletField.get();
				for (int i = 0; i < 8; i++){
					field.fire(this, at, i*45f, 1f, ItemSpriteSheet.SHOT_BUBBLE, 3, 6);
				}
				//splits into 2 small slimes, mitosis
				int born = 0;
				for (int n : PathFinder.NEIGHBOURS8){
					if (born >= 2) break;
					int c = at + n;
					if (!Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
					SmallSlime s = new SmallSlime();
					s.pos = c;
					s.state = s.HUNTING;
					GameScene.add(s);
					born++;
				}
			}
		}
	}

	//half a royal slime, no crown. doesn't split again (menos mal)
	public static class SmallSlime extends SKMob {
		{
			spriteClass = SmallSlimeSprite.class;
			HP = HT = 12;
			defenseSkill = 4;
			EXP = 2;
			maxLvl = 14;
		}
		@Override public int damageRoll() { return Random.NormalIntRange(2, 5); }
		@Override public int attackSkill( Char target ) { return 11; }

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			if (Dungeon.level != null){
				if (Dungeon.level.heroFOV[at]) CellEmitter.center(at).burst(Speck.factory(Speck.BUBBLE), 3);
				SKBulletField field =
						SKBulletField.get();
				for (int i = 0; i < 4; i++){
					field.fire(this, at, 45f + i*90f, 1f, ItemSpriteSheet.SHOT_BUBBLE, 1, 4);
				}
			}
		}
	}

	//court wizard: bullet rings, blinks away when cornered
	public static class Wizard extends SKMob {
		private boolean homingNext = false;

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("homing_next", homingNext);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			homingNext = bundle.getBoolean("homing_next");
		}

		{
			spriteClass = WizardSprite.class;
			HP = HT = 31;
			defenseSkill = 10;
			EXP = 8;
			maxLvl = 15;
			loot = Generator.Category.SCROLL;
			lootChance = 0.25f;
		}
		@Override protected int shotRange() { return 5; }
		@Override protected int specialCooldown() { return 5; }
		@Override public int damageRoll() { return Random.NormalIntRange(3, 9); }
		@Override public int attackSkill( Char target ) { return 13; }

		@Override
		protected boolean special( Char enemy ) {
			//cornered -> blink a few cells away. coward
			if (Dungeon.level.adjacent(pos, enemy.pos)){
				ArrayList<Integer> spots = new ArrayList<>();
				for (int c : SKCombat.area(pos, 4)){
					if (Dungeon.level.passable[c] && Actor.findChar(c) == null
							&& Dungeon.level.distance(c, enemy.pos) >= 3 && fieldOfView[c]){
						spots.add(c);
					}
				}
				if (!spots.isEmpty()){
					int to = spots.get(Random.Int(spots.size()));
					int from = pos;
					if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.WOOL), 6);
					if (sprite != null) sprite.place(to);
					move(to, false);
					//leaves an illusion behind that casts harmless bullets
					WizardIllusion fake = new WizardIllusion();
					fake.pos = from;
					fake.state = fake.HUNTING;
					GameScene.add(fake);
					if (Dungeon.level.heroFOV[to]) CellEmitter.get(to).burst(Speck.factory(Speck.WOOL), 6);
					Sample.INSTANCE.play(Assets.Sounds.PUFF);
					spend(TICK);
					return true;
				}
			}
			if (!fieldOfView[enemy.pos] || Dungeon.level.distance(pos, enemy.pos) > 6) return false;
			//every other spell: 4 slow homing bullets
			homingNext = !homingNext;
			if (homingNext) {
				int n = SKChampion.more(this, 4);
				float start = SKBulletField.angleTo(pos, enemy.pos);
				for (int i = 0; i < n; i++){
					SKBulletField.Bullet b =
							shoot(start + i*360f/n, 1f, ItemSpriteSheet.SHOT_ORB, 3, 8);
					b.behaviour = SKBulletField.HOMING;
					b.spread = 25;
					b.life = 7;
				}
			} else
			ring(8, enemy.pos, 1f, ItemSpriteSheet.SHOT_ORB, 3, 8);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.ZAP);
			spend(TICK);
			return true;
		}
	}

	//the wizard's illusion. harmless bullet rings, gone after a few turns or on the first hit
	public static class WizardIllusion extends SKMob {
		{
			spriteClass = IllusionSprite.class;
			HP = HT = 1;
			defenseSkill = 0;
			EXP = 0;
			maxLvl = -1;
		}
		private int life = 8;
		@Override protected int specialCooldown() { return 2; }
		@Override public int damageRoll() { return 0; }
		@Override public int attackSkill( Char target ) { return 0; }
		@Override protected boolean canAttack( Char enemy ) { return false; }

		@Override
		protected boolean act() {
			if (--life <= 0){
				if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.WOOL), 6);
				die(null);
				return true;
			}
			return super.act();
		}

		@Override
		protected boolean special( Char enemy ) {
			if (!fieldOfView[enemy.pos] || Dungeon.level.distance(pos, enemy.pos) > 6) return false;
			float start = SKBulletField.angleTo(pos, enemy.pos);
			for (int i = 0; i < 8; i++){
				shoot(start + i*45f, 1f, ItemSpriteSheet.SHOT_ORB, 1, 1).fake = true;
			}
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			spend(TICK);
			return true;
		}

		@Override
		public void die( Object cause ) {
			if (Dungeon.level != null && Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.WOOL), 4);
			super.die(cause);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("life", life);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			life = bundle.getInt("life");
		}
	}

	//dark knight (rare). heavy armor, lowers the lance and charges along a line
	public static class DarkKnight extends SKMob {
		{
			spriteClass = DarkKnightSprite.class;
			HP = HT = 84;
			defenseSkill = 12;
			EXP = 14;
			maxLvl = 18;
			loot = Generator.Category.WEAPON;
			lootChance = 0.5f;
		}
		@Override protected int specialCooldown() { return 5; }
		@Override public int damageRoll() { return Random.NormalIntRange(5, 16); }
		@Override public int attackSkill( Char target ) { return 16; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(2, 7); }

		@Override
		protected boolean special( Char enemy ) {
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (d < 2 || d > 5 || !fieldOfView[enemy.pos]) return false;
			ArrayList<Integer> cells = line(enemy.pos, 5);
			if (!cells.contains(enemy.pos)) return false;
			announce(0, cells, enemy.pos);
			return true;
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			charge(this, cells, 1.5f);
		}

		//under half hp it falls off the horse. next turn the horse keeps charging alone and the knight
		//fights on foot next to it
		private boolean falling = false;

		@Override
		public void damage( int dmg, Object src ) {
			super.damage(dmg, src);
			if (isAlive() && HP*2 <= HT) falling = true;
		}

		@Override
		protected boolean act() {
			if (falling && pendingKind == -1){
				int foot = -1;
				for (int n : PathFinder.NEIGHBOURS8){
					int c = pos + n;
					if (Dungeon.level.passable[c] && !Dungeon.level.pit[c] && Actor.findChar(c) == null){
						foot = c;
						break;
					}
				}
				if (foot != -1){
					dismount(foot);
					return true;
				}
			}
			return super.act();
		}

		private void dismount( int footCell ){
			int at = pos;
			int hp = HP;
			if (Dungeon.level.heroFOV[at]){
				CellEmitter.get(at).burst(Speck.factory(Speck.DUST), 8);
				Sample.INSTANCE.play(Assets.Sounds.TRAMPLE);
			}
			GLog.w(Messages.get(DarkKnight.class, "dismounted"));
			destroy();
			if (sprite != null) sprite.killAndErase();

			DarkHorse horse = new DarkHorse();
			horse.pos = at;
			horse.state = horse.HUNTING;
			GameScene.add(horse);
			DarkKnightFoot knight = new DarkKnightFoot();
			knight.HP = Math.min(knight.HT, Math.max(1, hp));
			knight.pos = footCell;
			knight.state = knight.HUNTING;
			GameScene.add(knight);
			CoopLog.line("dark knight dismounted at " + at);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("falling", falling);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			falling = bundle.getBoolean("falling");
		}
	}

	//the horse, solo. fast, keeps charging, nobody told it to stop
	public static class DarkHorse extends SKMob {
		{
			spriteClass = DarkHorseSprite.class;
			HP = HT = 35;
			defenseSkill = 12;
			baseSpeed = 1.5f;
			EXP = 5;
			maxLvl = 18;
		}
		@Override protected int specialCooldown() { return 4; }
		@Override public int damageRoll() { return Random.NormalIntRange(3, 10); }
		@Override public int attackSkill( Char target ) { return 15; }

		@Override
		protected boolean special( Char enemy ) {
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (d < 2 || d > 5 || !fieldOfView[enemy.pos]) return false;
			ArrayList<Integer> cells = line(enemy.pos, 5);
			if (!cells.contains(enemy.pos)) return false;
			announce(0, cells, enemy.pos);
			return true;
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			charge(this, cells, 1.3f);
		}
	}

	//dark knight on foot, sword
	public static class DarkKnightFoot extends SKMob {
		{
			spriteClass = KnightFootSprite.class;
			HP = HT = 54;
			defenseSkill = 12;
			EXP = 10;
			maxLvl = 18;
			loot = Generator.Category.WEAPON;
			lootChance = 0.5f;
		}
		@Override public int damageRoll() { return Random.NormalIntRange(5, 14); }
		@Override public int attackSkill( Char target ) { return 16; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(2, 6); }
	}

	//charge along marked cells: moves until someone's in the way, hits that one harder
	public static void charge( SKMob mob, ArrayList<Integer> cells, float dmgMulti ){
		int start = mob.pos;
		Char victim = null;
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (SKCombat.foes(mob, ch)) victim = ch;
				break;
			}
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c] || !mob.isAlive()) break;
			mob.move(c, false);
		}
		if (!mob.isAlive()) return;
		if (mob.pos != start && mob.sprite != null){
			mob.sprite.move(start, mob.pos);
			if (Dungeon.level.heroFOV[mob.pos]) Sample.INSTANCE.play(Assets.Sounds.TRAMPLE);
		}
		if (victim != null){
			mob.attack(victim, dmgMulti, 0, 2f);
		}
	}

	// *** sprites ***

	public static class SoldierSprite extends SKMobSprite {
		public SoldierSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_BULLET;
			setup(SKAssets.KNIGHT_SOLDIER, 16, 16);
		}
	}

	public static class SwordsmanSprite extends SKMobSprite {
		public SwordsmanSprite(){
			super();
			setup(SKAssets.KNIGHT_SWORD, 16, 16);
		}
	}

	public static class GunnerSprite extends SKMobSprite {
		public GunnerSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_BULLET;
			setup(SKAssets.KNIGHT_GUNNER, 16, 16);
		}
	}

	public static class RiflemanSprite extends SKMobSprite {
		public RiflemanSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_BLADE;
			setup(SKAssets.KNIGHT_RIFLE, 16, 16);
		}
	}

	public static class SlimeSprite extends SKMobSprite {
		public SlimeSprite(){
			super();
			setup(SKAssets.ROYAL_SLIME, 16, 14);
		}

		@Override
		public int blood() {
			return 0xFF7F9FE0;
		}
	}

	public static class WizardSprite extends SKMobSprite {
		public WizardSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_ORB;
			spinning = true;
			shotSound = Assets.Sounds.ZAP;
			setup(SKAssets.COURT_WIZARD, 16, 16);
		}
	}

	public static class SmallSlimeSprite extends SKMobSprite {
		public SmallSlimeSprite(){
			super();
			setup(SKAssets.SLIME_SMALL, 12, 12);
		}
	}

	public static class IllusionSprite extends WizardSprite {
		public IllusionSprite(){
			super();
			alpha(0.65f);
		}
	}

	public static class DarkHorseSprite extends SKMobSprite {
		public DarkHorseSprite(){
			super();
			setup(SKAssets.DARK_HORSE, 20, 16);
		}
	}

	public static class KnightFootSprite extends SKMobSprite {
		public KnightFootSprite(){
			super();
			setup(SKAssets.KNIGHT_FOOT, 16, 16);
		}
	}

	public static class DarkKnightSprite extends SKMobSprite {
		public DarkKnightSprite(){
			super();
			setup(SKAssets.DARK_KNIGHT, 24, 22);
		}
	}
}
