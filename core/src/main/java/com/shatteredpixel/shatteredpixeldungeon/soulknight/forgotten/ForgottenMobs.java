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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomMobs;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//forgotten land zoo. ice age stuff up top, dinos down below, + the man-eating plants
//(all in one file bc i kept losing them across 15 files)
public class ForgottenMobs {

	public static ArrayList<Class<? extends Mob>> rotation( int depth ){
		switch (depth){
			case 21: default:
				return new ArrayList<>(Arrays.asList(
						FrostWolf.class, FrostWolf.class, FrostWolf.class,
						Yeti.class, Sabertooth.class, Yeti.class));
			case 22:
				return new ArrayList<>(Arrays.asList(
						FrostWolf.class, FrostWolf.class, Yeti.class, Yeti.class,
						Sabertooth.class, WoollyRhino.class, Sloth.class, Mammoth.class));
			case 23:
				return new ArrayList<>(Arrays.asList(
						Yeti.class, Mammoth.class, WoollyRhino.class, Sloth.class,
						Raptor.class, Raptor.class, Dilophosaurus.class, Compy.class));
			case 24: case 25:
				return new ArrayList<>(Arrays.asList(
						Raptor.class, Raptor.class, Triceratops.class, Pterodactyl.class,
						Dilophosaurus.class, Ankylosaurus.class, Compy.class, Spinosaurus.class));
		}
	}

	//shared stats, floors 21-25
	public static abstract class Beast extends SKMob {
		{
			maxLvl = 30;
			lootChance = 0.1f;
		}
		protected int minDmg = 16, maxDmg = 24, acc = 38, armor = 8;

		@Override
		public int damageRoll() {
			return Random.NormalIntRange(minDmg, maxDmg);
		}

		@Override
		public int attackSkill( Char target ) {
			return acc;
		}

		@Override
		public int drRoll() {
			return super.drRoll() + Random.NormalIntRange(0, armor);
		}

		//charge along a line, telegraphed 1 turn before
		protected boolean chargeAt( Char enemy, int maxDist, int kind ){
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (d < 2 || d > maxDist) return false;
			ArrayList<Integer> line = line(enemy.pos, maxDist);
			if (!line.contains(enemy.pos)) return false;
			announce(kind, line, enemy.pos);
			return true;
		}
	}

	// ===== ice age =====
	//big, slow, charges + knockdown
	public static class Mammoth extends Beast {
		{
			spriteClass = MammothSprite.class;
			HP = HT = 150;
			defenseSkill = 16;
			baseSpeed = 0.8f;
			EXP = 22;
			minDmg = 22; maxDmg = 32; acc = 38; armor = 12;
			properties.add(Property.LARGE);
			immunities.add(Chill.class);
			immunities.add(Frost.class);
		}
		@Override protected int specialCooldown() { return 5; }
		@Override protected boolean special( Char enemy ) { return chargeAt(enemy, 5, 0); }
		@Override protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			KingdomMobs.charge(this, cells, 1.5f);
			Char victim = Actor.findChar(aim);
			if (victim != null && victim.isAlive() && Dungeon.level.adjacent(pos, victim.pos)) Buff.prolong(victim, Paralysis.class, 1f);
		}
	}

	//hides, pounces on whoever wanders off alone
	public static class Sabertooth extends Beast {
		{
			spriteClass = SabertoothSprite.class;
			HP = HT = 95;
			defenseSkill = 30;
			EXP = 18;
			minDmg = 22; maxDmg = 32; acc = 44; armor = 6;
		}
		@Override protected int specialCooldown() { return 4; }
		@Override protected boolean special( Char enemy ) {
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (d < 2 || d > 4) return false;
			//"alone" = no other hero nearby
			for (Hero h : Party.living()){
				if (h != enemy && Dungeon.level.distance(h.pos, enemy.pos) <= 2) return false;
			}
			ArrayList<Integer> cells = new ArrayList<>();
			cells.add(enemy.pos);
			announce(0, cells, enemy.pos);
			if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(this, "crouch"));
			return true;
		}
		@Override protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			Char victim = Actor.findChar(aim);
			int land = -1;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = aim + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null && (land == -1
						|| Dungeon.level.distance(c, pos) < Dungeon.level.distance(land, pos))) land = c;
			}
			if (land != -1){
				int from = pos;
				move(land, false);
				if (sprite != null) sprite.jump(from, land, 0, 0.15f, null);
			}
			if (victim != null && victim.isAlive() && Dungeon.level.adjacent(pos, victim.pos)){
				attack(victim, 1.5f, 0, 2f);
				if (victim.isAlive()) Buff.affect(victim, Bleeding.class).set(8);
			}
		}
	}

	//one howl and the whole floor's wolves show up. have fun
	public static class FrostWolf extends Beast {
		{
			spriteClass = FrostWolfSprite.class;
			HP = HT = 75;
			defenseSkill = 26;
			EXP = 12;
			minDmg = 18; maxDmg = 27; acc = 38; armor = 6;
			immunities.add(Chill.class);
		}
		private boolean howled = false;
		@Override public void notice() {
			super.notice();
			if (!howled && enemy != null){
				howled = true;
				if (Dungeon.level.heroFOV[pos]){
					GLog.w(Messages.get(this, "howl"));
					Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 1.5f);
				}
				for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
					if (m instanceof FrostWolf && m != this && m.state != m.HUNTING) m.beckon(enemy.pos);
				}
			}
		}
		@Override public int attackProc( Char enemy, int damage ) {
			if (Random.Int(3) == 0) Buff.prolong(enemy, Chill.class, 2f);
			return super.attackProc(enemy, damage);
		}
		@Override public void storeInBundle( Bundle b ) { super.storeInBundle(b); b.put("howled", howled); }
		@Override public void restoreFromBundle( Bundle b ) { super.restoreFromBundle(b); howled = b.getBoolean("howled"); }
	}

	//snowballs, slow
	public static class Yeti extends Beast {
		{
			spriteClass = YetiSprite.class;
			HP = HT = 125;
			defenseSkill = 20;
			EXP = 18;
			minDmg = 22; maxDmg = 31; acc = 38; armor = 8;
			immunities.add(Chill.class);
			immunities.add(Frost.class);
		}
		@Override protected int shotRange() { return 4; }
		@Override public int attackProc( Char enemy, int damage ) {
			if (!Dungeon.level.adjacent(pos, enemy.pos)){
				Buff.prolong(enemy, Slow.class, 2f);
				Buff.prolong(enemy, Chill.class, 3f);
			}
			return super.attackProc(enemy, damage);
		}
	}

	//chill until you poke it. then it isn't
	public static class Sloth extends Beast {
		{
			spriteClass = SlothSprite.class;
			HP = HT = 170;
			defenseSkill = 8;
			baseSpeed = 0.7f;
			EXP = 22;
			minDmg = 24; maxDmg = 34; acc = 36; armor = 10;
			state = PASSIVE;
			properties.add(Property.LARGE);
		}
		@Override public void damage( int dmg, Object src ) {
			if (state == PASSIVE && src instanceof Char){
				state = HUNTING;
				aggro((Char) src);
				if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(this, "angry"));
			}
			super.damage(dmg, src);
		}
		@Override protected int specialCooldown() { return 4; }
		@Override protected boolean special( Char enemy ) {
			if (Dungeon.level.distance(pos, enemy.pos) > 1) return false;
			announce(0, SKCombat.area(pos, 1), enemy.pos);
			return true;
		}
		@Override protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			PixelScene.shake(2, 0.4f);
			for (Char ch : SKCombat.foesIn(this, cells)){
				ch.damage(SKCombat.specialDamage(this, ch, 26, 38), this);
				if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 3f);
			}
		}
	}

	//charges, breaks thin ice on the way
	public static class WoollyRhino extends Beast {
		{
			spriteClass = RhinoSprite.class;
			HP = HT = 130;
			defenseSkill = 14;
			EXP = 20;
			minDmg = 22; maxDmg = 32; acc = 38; armor = 12;
			immunities.add(Chill.class);
		}
		@Override protected int specialCooldown() { return 4; }
		@Override protected boolean special( Char enemy ) { return chargeAt(enemy, 6, 0); }
		@Override protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			ForgottenFloor f = ForgottenFloor.get(Dungeon.level);
			if (f != null && Dungeon.level instanceof ForgottenLevel){
				for (int c : cells){
					if (f.kindAt(c) == ForgottenFloor.THIN_ICE || f.kindAt(c) == ForgottenFloor.CRACKED){
						f.put(Dungeon.level, c, 0);
						Dungeon.level.set(c, Terrain.WATER);
						GameScene.updateMap(c);
						ForgottenVisuals.splash(c);
					}
				}
			}
			KingdomMobs.charge(this, cells, 1.6f);
		}
	}

	// ===== dinos =====
	//go for wounded heroes, run away when hurt + alone. smart little things
	public static class Raptor extends Beast {
		{
			spriteClass = RaptorSprite.class;
			HP = HT = 80;
			defenseSkill = 34;
			baseSpeed = 1.25f;
			EXP = 14;
			minDmg = 20; maxDmg = 29; acc = 45; armor = 7;
		}
		@Override protected int specialCooldown() { return 3; }
		@Override protected boolean special( Char enemy ) {
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (d < 2 || d > 3 || enemy.HP > enemy.HT / 2) return false;
			ArrayList<Integer> cells = new ArrayList<>();
			cells.add(enemy.pos);
			announce(0, cells, enemy.pos);
			return true;
		}
		@Override protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			int land = -1;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = aim + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null && (land == -1
						|| Dungeon.level.distance(c, pos) < Dungeon.level.distance(land, pos))) land = c;
			}
			if (land == -1) return;
			int from = pos;
			move(land, false);
			if (sprite != null) sprite.jump(from, land, 0, 0.12f, null);
			Char victim = Actor.findChar(aim);
			if (victim != null && victim.isAlive() && Dungeon.level.adjacent(pos, victim.pos)) attack(victim, 1.3f, 0, 2f);
		}
		@Override protected boolean act() {
			if (state == HUNTING && HP < HT / 3){
				boolean pack = false;
				for (Mob m : Dungeon.level.mobs){
					if (m instanceof Raptor && m != this && Dungeon.level.distance(m.pos, pos) <= 4) pack = true;
				}
				if (!pack) state = FLEEING;
			}
			return super.act();
		}
	}

	//frill blocks most hits from the front. go around it
	public static class Triceratops extends Beast {
		{
			spriteClass = TriceratopsSprite.class;
			HP = HT = 175;
			defenseSkill = 10;
			EXP = 24;
			minDmg = 28; maxDmg = 38; acc = 38; armor = 10;
			properties.add(Property.LARGE);
		}
		@Override public void damage( int dmg, Object src ) {
			//whatever it's facing hits the frill
			if (src instanceof Char && src == enemy && state == HUNTING){
				dmg = Math.max(1, Math.round(dmg * 0.45f));
				if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "frill"));
			}
			super.damage(dmg, src);
		}
		@Override protected int specialCooldown() { return 5; }
		@Override protected boolean special( Char enemy ) { return chargeAt(enemy, 5, 0); }
		@Override protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			KingdomMobs.charge(this, cells, 1.4f);
			Char victim = Actor.findChar(aim);
			if (victim != null && victim.isAlive() && Dungeon.level.adjacent(pos, victim.pos)){
				int dx = Integer.signum(victim.pos % Dungeon.level.width() - pos % Dungeon.level.width());
				int dy = Integer.signum(victim.pos / Dungeon.level.width() - pos / Dungeon.level.width());
				push(victim, dx + dy * Dungeon.level.width(), 2);
			}
		}
	}

	static void push( Char ch, int step, int n ){
		for (int i = 0; i < n; i++){
			int next = ch.pos + step;
			if (next < 0 || next >= Dungeon.level.length() || !Dungeon.level.passable[next] || Actor.findChar(next) != null) return;
			int from = ch.pos;
			ch.pos = next;
			Actor.add(new Pushing(ch, from, next));
			Dungeon.level.occupyCell(ch);
		}
	}

	//flies, grabs a hero, drops them somewhere else. rude
	public static class Pterodactyl extends Beast {
		{
			spriteClass = PterodactylSprite.class;
			HP = HT = 70;
			defenseSkill = 40;
			flying = true;
			baseSpeed = 1.3f;
			EXP = 14;
			minDmg = 18; maxDmg = 26; acc = 42; armor = 4;
		}
		@Override protected int specialCooldown() { return 5; }
		@Override protected boolean special( Char enemy ) {
			if (!Dungeon.level.adjacent(pos, enemy.pos) || enemy.properties().contains(Property.LARGE)) return false;
			int drop = -1;
			for (int tries = 0; tries < 20; tries++){
				int c = enemy.pos + Random.IntRange(-3, 3) + Random.IntRange(-3, 3) * Dungeon.level.width();
				if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null
						&& Dungeon.level.distance(c, enemy.pos) >= 2){
					drop = c;
					break;
				}
			}
			if (drop == -1) return false;
			ArrayList<Integer> cells = new ArrayList<>();
			cells.add(drop);
			announce(0, cells, enemy.pos);
			return true;
		}
		@Override protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			Char victim = Actor.findChar(aim);
			if (victim == null || !victim.isAlive() || !Dungeon.level.adjacent(pos, victim.pos) || cells.isEmpty()) return;
			int drop = cells.get(0);
			if (Actor.findChar(drop) != null || !Dungeon.level.passable[drop]) return;
			int from = victim.pos;
			victim.pos = drop;
			Actor.add(new Pushing(victim, from, drop));
			Dungeon.level.occupyCell(victim);
			victim.damage(SKCombat.specialDamage(this, victim, 11, 20), this);
			if (victim instanceof Hero) GLog.w(Messages.get(this, "dropped"));
		}
	}

	//spit = blind
	public static class Dilophosaurus extends Beast {
		{
			spriteClass = DilophoSprite.class;
			HP = HT = 95;
			defenseSkill = 24;
			EXP = 16;
			minDmg = 22; maxDmg = 30; acc = 40; armor = 6;
		}
		@Override protected int shotRange() { return 4; }
		@Override public int attackProc( Char enemy, int damage ) {
			if (!Dungeon.level.adjacent(pos, enemy.pos)){
				Buff.prolong(enemy, Blindness.class, 2f);
				Buff.affect(enemy, Poison.class).set(4);
			}
			return super.attackProc(enemy, damage);
		}
	}

	//tank w/ a tail club
	public static class Ankylosaurus extends Beast {
		{
			spriteClass = AnkyloSprite.class;
			HP = HT = 140;
			defenseSkill = 6;
			baseSpeed = 0.85f;
			EXP = 22;
			minDmg = 24; maxDmg = 34; acc = 36; armor = 22;
			properties.add(Property.LARGE);
		}
		@Override public int attackProc( Char enemy, int damage ) {
			if (Random.Int(3) == 0){
				Buff.prolong(enemy, Paralysis.class, 1f);
				if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
			}
			return super.attackProc(enemy, damage);
		}
	}

	//tiny, packs, steal your food. the worst
	public static class Compy extends Beast {
		{
			spriteClass = CompySprite.class;
			HP = HT = 26;
			defenseSkill = 40;
			baseSpeed = 2f;
			EXP = 3;
			minDmg = 6; maxDmg = 12; acc = 40; armor = 0;
		}
		private Item stolen = null;
		@Override public int attackProc( Char enemy, int damage ) {
			if (stolen == null && enemy instanceof Hero && Random.Int(3) == 0){
				Food food = ((Hero) enemy).belongings.getItem(Food.class);
				if (food != null){
					stolen = food.detach(((Hero) enemy).belongings.backpack);
					if (stolen != null){
						GLog.w(Messages.get(this, "stole", stolen.name()));
						state = FLEEING;
					}
				}
			}
			return super.attackProc(enemy, damage);
		}
		@Override public void die( Object cause ) {
			if (stolen != null) Dungeon.level.drop(stolen, pos).sprite.drop();
			stolen = null;
			super.die(cause);
		}
		@Override public void storeInBundle( Bundle b ) { super.storeInBundle(b); if (stolen != null) b.put("stolen", stolen); }
		@Override public void restoreFromBundle( Bundle b ) { super.restoreFromBundle(b); stolen = b.contains("stolen") ? (Item) b.get("stolen") : null; }
	}

	//hides underwater, bites whoever gets near the shore. tiburón con patas
	public static class Spinosaurus extends Beast {
		{
			spriteClass = SpinoSprite.class;
			HP = HT = 185;
			defenseSkill = 14;
			EXP = 26;
			minDmg = 28; maxDmg = 38; acc = 40; armor = 10;
			properties.add(Property.LARGE);
		}
		public boolean submerged(){
			if (!Dungeon.level.water[pos]) return false;
			for (Hero h : Party.living()){
				if (Dungeon.level.adjacent(h.pos, pos)) return false;
			}
			return true;
		}
		@Override public boolean isInvulnerable( Class effect ) {
			return submerged() || super.isInvulnerable(effect);
		}
		@Override protected boolean act() {
			invisible = submerged() ? 1 : 0;
			if (sprite instanceof SpinoSprite) ((SpinoSprite) sprite).under(invisible > 0);
			return super.act();
		}
	}

	//can't move, eats you if you get close
	public static class CarnivorePlant extends Beast {
		{
			spriteClass = PlantSprite.class;
			HP = HT = 90;
			defenseSkill = 0;
			EXP = 15;
			minDmg = 20; maxDmg = 28; acc = 40; armor = 6;
			properties.add(Property.IMMOVABLE);
			state = WANDERING;
		}
		@Override protected boolean getCloser( int target ) { return false; }
		@Override protected boolean getFurther( int target ) { return false; }
		@Override public int attackProc( Char enemy, int damage ) {
			if (Random.Int(3) == 0){
				Buff.prolong(enemy, Roots.class, 2f);
				Buff.affect(enemy, Bleeding.class).set(5);
				if (enemy instanceof Hero) GLog.w(Messages.get(this, "swallow"));
			}
			return super.attackProc(enemy, damage);
		}
	}

	//sprites

	public static class MammothSprite extends SKMobSprite {
		public MammothSprite(){ super(); setup(ForgottenAssets.MAMMOTH, 32, 28); }
	}
	public static class SabertoothSprite extends SKMobSprite {
		public SabertoothSprite(){ super(); setup(ForgottenAssets.SABERTOOTH, 24, 16); }
	}
	public static class FrostWolfSprite extends SKMobSprite {
		public FrostWolfSprite(){ super(); setup(ForgottenAssets.FROST_WOLF, 18, 14); }
	}
	public static class YetiSprite extends SKMobSprite {
		public YetiSprite(){ super(); shot = ItemSpriteSheet.SHOT_BUBBLE; setup(ForgottenAssets.YETI, 20, 24); }
	}
	public static class SlothSprite extends SKMobSprite {
		public SlothSprite(){ super(); setup(ForgottenAssets.SLOTH, 28, 28); }
	}
	public static class RhinoSprite extends SKMobSprite {
		public RhinoSprite(){ super(); setup(ForgottenAssets.RHINO, 30, 20); }
	}
	public static class RaptorSprite extends SKMobSprite {
		public RaptorSprite(){ super(); setup(ForgottenAssets.RAPTOR, 20, 16); }
	}
	public static class TriceratopsSprite extends SKMobSprite {
		public TriceratopsSprite(){ super(); setup(ForgottenAssets.TRICERATOPS, 32, 22); }
	}
	public static class PterodactylSprite extends SKMobSprite {
		public PterodactylSprite(){ super(); setup(ForgottenAssets.PTERODACTYL, 26, 18); }
	}
	public static class DilophoSprite extends SKMobSprite {
		public DilophoSprite(){ super(); shot = ItemSpriteSheet.SHOT_PLAGUE; setup(ForgottenAssets.DILOPHO, 20, 20); }
	}
	public static class AnkyloSprite extends SKMobSprite {
		public AnkyloSprite(){ super(); setup(ForgottenAssets.ANKYLO, 30, 18); }
	}
	public static class CompySprite extends SKMobSprite {
		public CompySprite(){ super(); setup(ForgottenAssets.COMPY, 12, 10); }
	}
	public static class SpinoSprite extends SKMobSprite {
		public SpinoSprite(){ super(); setup(ForgottenAssets.SPINO, 34, 26); }
		void under( boolean u ){ row(u ? 1 : 0); }
	}
	public static class PlantSprite extends SKMobSprite {
		public PlantSprite(){ super(); setup(ForgottenAssets.PLANT, 24, 28); }
	}
}
