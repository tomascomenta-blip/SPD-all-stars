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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Undersea mobs (16-19): the crew and the experiments
public class UnderseaMobs {

	//infected crew: fast blade shot that bounces once and leaves bullets behind
	public static class InfectedCrew extends SKMob {
		{
			spriteClass = CrewSprite.class;
			HP = HT = 62;
			defenseSkill = 18;
			EXP = 13;
			maxLvl = 26;
			loot = Gold.class;
			lootChance = 0.3f;
		}
		@Override protected int shotRange() { return 5; }
		@Override protected int specialCooldown() { return 4; }
		@Override public int damageRoll() { return Random.NormalIntRange(14, 24); }
		@Override public int attackSkill( Char target ) { return 26; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 6); }

		@Override
		protected boolean special( Char enemy ) {
			if (!fieldOfView[enemy.pos] || Dungeon.level.distance(pos, enemy.pos) > 7) return false;
			SKBulletField.Bullet b = shoot(SKBulletField.angleTo(pos, enemy.pos), 2f, ItemSpriteSheet.SHOT_BLADE, 10, 19);
			b.behaviour = SKBulletField.TRAIL;
			b.period = 2;
			b.childImage = ItemSpriteSheet.SHOT_BUBBLE;
			b.bounces = 1;
			b.life = 8;
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			spend(TICK);
			return true;
		}

		//kraken tentacle grows where it dies
		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			if (Dungeon.level != null) UnderseaFeatures.tentacle(at);
		}
	}

	//kraken minion: 3 bubbles that bounce off walls
	public static class KrakenMinion extends SKMob {
		{
			spriteClass = KrakenSprite.class;
			HP = HT = 65;
			defenseSkill = 16;
			EXP = 13;
			maxLvl = 26;
			loot = Generator.Category.POTION;
			lootChance = 0.2f;
		}
		@Override protected int shotRange() { return 4; }
		@Override protected int specialCooldown() { return 4; }
		@Override public int damageRoll() { return Random.NormalIntRange(12, 20); }
		@Override public int attackSkill( Char target ) { return 25; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 7); }

		//every other spell it calls a tentacle next to the target (marked a turn before), and under half hp
		//it takes over the nearest hero's mind, once
		private boolean callNext = false;
		private boolean controlled = false;
		private static final int TENTACLE = 1;

		@Override
		public void damage( int dmg, Object src ) {
			super.damage(dmg, src);
			if (!controlled && isAlive() && HP*2 <= HT){
				Hero h =
						Party.nearestLiving(pos);
				if (h != null && h.isAlive() && Dungeon.level.distance(h.pos, pos) <= 6){
					controlled = true;
					Buff.prolong(h,
							UnderseaFeatures.MindControl.class, UnderseaFeatures.MindControl.DURATION);
					if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
					if (h.sprite != null) h.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
					Sample.INSTANCE.play(Assets.Sounds.CHARMS, 1f, 0.6f);
					GLog.n(
							Messages.get(KrakenMinion.class, "control", Party.name(h)));
					CoopLog.line("mind control on hero " + h.pos);
				}
			}
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			if (kind == TENTACLE && !cells.isEmpty()) UnderseaFeatures.tentacle(cells.get(0));
		}

		private int tentacleSpot( Char enemy ){
			ArrayList<Integer> water = new ArrayList<>(), any = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8){
				int c = enemy.pos + n;
				if (!Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
				any.add(c);
				if (Dungeon.level.water[c]) water.add(c);
			}
			ArrayList<Integer> from = water.isEmpty() ? any : water;
			return from.isEmpty() ? -1 : from.get(Random.Int(from.size()));
		}

		@Override
		protected boolean special( Char enemy ) {
			if (!fieldOfView[enemy.pos] || Dungeon.level.distance(pos, enemy.pos) > 7) return false;
			callNext = !callNext;
			if (callNext){
				int c = tentacleSpot(enemy);
				if (c != -1){
					ArrayList<Integer> mark = new ArrayList<>();
					mark.add(c);
					announce(TENTACLE, mark, enemy.pos);
					return true;
				}
			}
			float aim = SKBulletField.angleTo(pos, enemy.pos);
			for (int i = -1; i <= 1; i++){
				SKBulletField.Bullet b = shoot(aim + 15*i, 2f, ItemSpriteSheet.SHOT_BUBBLE, 8, 16);
				b.bounces = 2;
				b.life = 10;
			}
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 1.3f);
			spend(TICK);
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("call_next", callNext);
			bundle.put("controlled", controlled);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			callNext = bundle.getBoolean("call_next");
			controlled = bundle.getBoolean("controlled");
		}
	}

	//subject 001: hides in a bubble that shoots bullets around. pop it and it vanishes, reappears behind
	//its prey, spikes come out a turn later
	public static class Subject001 extends SKMob {
		{
			spriteClass = Subject001Sprite.class;
			HP = HT = 66;
			defenseSkill = 17;
			EXP = 14;
			maxLvl = 26;
		}

		private boolean bubble = true;
		private int popped = 0; //turns out of its bubble (after 4 it makes a new one)
		private static final int SPIKES = 0;

		@Override
		protected boolean act() {
			if (!bubble && ++popped >= 4 && pendingKind == -1){
				bubble = true;
				popped = 0;
				if (sprite != null && Dungeon.level.heroFOV[pos]){
					CellEmitter.center(pos).burst(Speck.factory(Speck.BUBBLE), 8);
					sprite.showStatus(CharSprite.WARNING, Messages.get(Subject001.class, "rebubble"));
				}
			}
			return super.act();
		}

		public boolean inBubble(){
			return bubble;
		}

		@Override protected int specialCooldown() { return 3; }
		@Override public int damageRoll() { return Random.NormalIntRange(9, 17); }
		@Override public int attackSkill( Char target ) { return 26; }

		@Override
		public void damage( int dmg, Object src ) {
			if (bubble && isAlive()){
				//bubble eats the first hit
				bubble = false;
				if (sprite != null){
					sprite.showStatus(CharSprite.WARNING, Messages.get(this, "pop"));
					if (sprite.visible) CellEmitter.center(pos).burst(Speck.factory(Speck.BUBBLE), 10);
				}
				Sample.INSTANCE.play(Assets.Sounds.WATER);
				cooldown = 0;
				return;
			}
			super.damage(dmg, src);
		}

		@Override
		protected boolean special( Char enemy ) {
			if (!fieldOfView[enemy.pos]) return false;
			if (bubble){
				ring(16, enemy.pos, 1f, ItemSpriteSheet.SHOT_BUBBLE, 4, 10);
				spend(TICK);
				return true;
			}
			//reappears behind its prey. nothing personal
			int best = -1;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = enemy.pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null
						&& (best == -1 || Dungeon.level.distance(c, pos) > Dungeon.level.distance(best, pos))){
					best = c;
				}
			}
			if (best == -1) return false;
			if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.BUBBLE), 6);
			if (sprite != null) sprite.place(best);
			move(best, false);
			announce(SPIKES, SKCombat.area(pos, 1), enemy.pos);
			return true;
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.HIT_STAB);
			for (Char ch : SKCombat.foesIn(this, cells)){
				ch.damage(SKCombat.specialDamage(this, ch, 11, 22), this);
			}
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("bubble", bubble);
			bundle.put("popped", popped);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			bubble = bundle.getBoolean("bubble");
			popped = bundle.getInt("popped");
		}
	}

	//subject 002: slow and chill in its bubble. hurt it badly and the bubble bursts into bullet clusters
	public static class Subject002 extends SKMob {
		{
			spriteClass = Subject002Sprite.class;
			HP = HT = 124;
			defenseSkill = 12;
			baseSpeed = 0.75f;
			EXP = 15;
			maxLvl = 26;
			loot = Generator.Category.SEED;
			lootChance = 0.3f;
		}

		private boolean burst = false;

		public boolean inBubble(){
			return !burst;
		}

		@Override public int damageRoll() { return Random.NormalIntRange(20, 30); }
		@Override public int attackSkill( Char target ) { return 24; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(2, 12); }

		//every few turns in a fight it electrifies the water around it (3 cells)
		@Override protected int specialCooldown() { return 4; }

		@Override
		protected boolean special( Char enemy ) {
			if (Dungeon.level.distance(pos, enemy.pos) > 4) return false;
			ArrayList<Integer> cells = new ArrayList<>();
			for (int c : SKCombat.area(pos, 3)){
				if (Dungeon.level.water[c] || c == pos) cells.add(c);
			}
			boolean any = false;
			for (Char ch : SKCombat.foesIn(this, cells)){
				if (!Dungeon.level.water[ch.pos] && !Dungeon.level.adjacent(ch.pos, pos)) continue;
				ch.damage(SKCombat.specialDamage(this, ch, 11, 21), this);
				any = true;
				if (ch.sprite != null && sprite != null && sprite.parent != null){
					ArrayList<Lightning.Arc> arcs = new ArrayList<>();
					arcs.add(new Lightning.Arc(sprite.center(), ch.sprite.center()));
					sprite.parent.add(new Lightning(arcs, null));
				}
			}
			if (!any) return false;
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			spend(TICK);
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
			super.damage(dmg, src);
			if (!burst && isAlive() && HP*2 <= HT){
				burst = true;
				if (sprite != null && sprite.visible) CellEmitter.center(pos).burst(Speck.factory(Speck.BUBBLE), 12);
				for (int cluster = 0; cluster < 4; cluster++){
					SKBulletField.Bullet b = shoot(cluster*90f + 45f, 1f, ItemSpriteSheet.SHOT_BIG_ORB, 9, 18);
					b.behaviour = SKBulletField.SPLIT;
					b.timer = 2;
					b.children = 6;
					b.spread = 360;
					b.childImage = ItemSpriteSheet.SHOT_BUBBLE;
					b.childSpeed = SKBulletField.UNIT;
				}
			}
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("burst", burst);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			burst = bundle.getBoolean("burst");
		}
	}

	//subject 004 (rare). huge clawed thing, breathes fans of red bullets
	public static class Subject004 extends SKMob {
		{
			spriteClass = Subject004Sprite.class;
			HP = HT = 170;
			defenseSkill = 20;
			EXP = 25;
			maxLvl = 30;
			loot = Generator.Category.ARMOR;
			lootChance = 0.5f;
		}
		@Override protected int specialCooldown() { return 3; }
		@Override public int damageRoll() { return Random.NormalIntRange(22, 34); }
		@Override public int attackSkill( Char target ) { return 30; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(3, 11); }

		//shell blocks most of what comes from the front, from behind it takes double
		@Override
		public void damage( int dmg, Object src ) {
			Char from = src instanceof Char ? (Char) src
					: (src instanceof SKBulletField.Shot ? ((SKBulletField.Shot) src).owner
					: (src instanceof Item ? Dungeon.hero : null));
			if (dmg > 0 && from != null && enemy != null && from.pos != pos && enemy.pos != pos){
				int w = Dungeon.level.width();
				int ex = enemy.pos % w - pos % w, ey = enemy.pos / w - pos / w;
				int ax = from.pos % w - pos % w, ay = from.pos / w - pos / w;
				double cos = (ex*ax + ey*ay) / Math.sqrt((double)(ex*ex + ey*ey) * (ax*ax + ay*ay));
				if (cos >= 0.5){
					dmg = Math.max(1, Math.round(dmg * 0.3f));
					if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(Subject004.class, "shell"));
				} else if (cos <= -0.5){
					dmg *= 2;
					if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Subject004.class, "back"));
				}
			}
			super.damage(dmg, src);
		}

		@Override
		protected boolean special( Char enemy ) {
			if (!fieldOfView[enemy.pos] || Dungeon.level.distance(pos, enemy.pos) > 7) return false;
			arc(7, 90, enemy.pos, 1.5f, ItemSpriteSheet.SHOT_ORB, 9, 18);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			spend(TICK);
			return true;
		}
	}

	// *** sprites ***

	public static class CrewSprite extends SKMobSprite {
		public CrewSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_BULLET;
			setup(SKAssets.INFECTED_CREW, 16, 16);
		}
	}

	public static class KrakenSprite extends SKMobSprite {
		public KrakenSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_BUBBLE;
			spinning = true;
			setup(SKAssets.KRAKEN_MINION, 16, 16);
		}
	}

	//row 0 in the bubble, row 1 popped
	public static class Subject001Sprite extends SKMobSprite {
		public Subject001Sprite(){
			super();
			setup(SKAssets.SUBJECT_001, 16, 16);
		}
		@Override
		public void update() {
			if (ch instanceof Subject001) row(((Subject001) ch).inBubble() ? 0 : 1);
			super.update();
		}
	}

	public static class Subject002Sprite extends SKMobSprite {
		public Subject002Sprite(){
			super();
			setup(SKAssets.SUBJECT_002, 18, 16);
		}
		@Override
		public void update() {
			if (ch instanceof Subject002) row(((Subject002) ch).inBubble() ? 0 : 1);
			super.update();
		}
	}

	public static class Subject004Sprite extends SKMobSprite {
		public Subject004Sprite(){
			super();
			setup(SKAssets.SUBJECT_004, 24, 20);
		}
	}
}
