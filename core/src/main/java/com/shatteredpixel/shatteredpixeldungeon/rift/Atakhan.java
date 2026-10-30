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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Atakhan, tags along with DM-300 on 15. drops blood roses
//  - his hits also take a small part of the target's current health
//  - Blood burst: 4 bolts of blood in 2 turns at heroes up to 6 cells away
//  - Rings of ruin: rings that grow from him (radius 1 to 6, one per turn, announced a turn before)
//  - Fragile and unworthy: his hits and bolts leave stacks that weaken the armor against him
//his stuff hurts DM-300 too and sometimes they start fighting each other (BossRivalry). best feature tbh
public class Atakhan extends RiftBoss {

	{
		spriteClass = Sprite.class;
		HP = HT = 220;
		defenseSkill = 15;
		EXP = 25;
		viewDistance = 10;

		HUNTING = new Hunting();
		state = HUNTING;

		immunities.add(Roots.class);
		immunities.add(Vertigo.class);
		immunities.add(Slow.class);
		immunities.add(Cripple.class);
	}

	public static final float CURRENT_HP = 0.03f;
	public static final int BURST_BOLTS = 4;      //over 2 turns
	public static final int BURST_RANGE = 6;
	public static final int BURST_COOLDOWN = 8;
	public static final int RING_MAX = 6;
	public static final int RING_COOLDOWN = 7;
	public static final float RING_CURRENT_HP = 0.08f;

	private int burstLeft = 0;
	private int burstCooldown = 3;
	private int ringCooldown = 5;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(8, 15);
	}

	@Override
	public int attackSkill( Char target ) {
		return 22;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 8);
	}

	public static int boltMin(){ return 3; }
	public static int boltMax(){ return 7; }
	public static int ringMin(){ return 5; }
	public static int ringMax(){ return 10; }

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		damage += Math.round(enemy.HP * CURRENT_HP);
		if (enemy.alignment != alignment) Fragile.add(enemy, 1);
		return damage;
	}

	@Override
	protected boolean act() {
		//the burst goes on whatever he is doing
		if (burstLeft > 0){
			if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
				fieldOfView = new boolean[Dungeon.level.length()];
			}
			Dungeon.level.updateFieldOfView(this, fieldOfView);
			return fireBolt();
		}
		if (burstCooldown > 0) burstCooldown--;
		if (ringCooldown > 0) ringCooldown--;
		return super.act();
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			//while fighting the other boss he just brawls
			if (enemyInFOV && buff(BossRivalry.class) == null){
				if (ringCooldown <= 0 && heroWithin(RING_MAX)) return castRings();
				if (burstCooldown <= 0 && !burstTargets().isEmpty()){
					burstLeft = BURST_BOLTS;
					burstCooldown = BURST_COOLDOWN;
					if (Dungeon.level.heroFOV[pos]){
						GLog.w(Messages.get(Atakhan.this, "burst_warn"));
						if (sprite != null) sprite.showStatus(CharSprite.WARNING, Messages.get(Atakhan.this, "burst_status"));
					}
					return fireBolt();
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private boolean heroWithin( int range ){
		for (Hero h : Party.living()){
			if (fieldOfView != null && fieldOfView[h.pos] && Dungeon.level.distance(pos, h.pos) <= range) return true;
		}
		return false;
	}

	// *** Blood burst ***

	private ArrayList<Hero> burstTargets(){
		ArrayList<Hero> list = new ArrayList<>();
		for (Hero h : Party.living()){
			if (h.invisible > 0 || fieldOfView == null || !fieldOfView[h.pos]) continue;
			if (Dungeon.level.distance(pos, h.pos) <= BURST_RANGE) list.add(h);
		}
		return list;
	}

	//one bolt every half turn
	private boolean fireBolt(){
		burstLeft--;
		spend(0.5f);
		ArrayList<Hero> targets = burstTargets();
		if (targets.isEmpty()){
			burstLeft = 0;
			return true;
		}
		Hero target = Random.element(targets);
		final int cell = new Ballistica(pos, target.pos, Ballistica.MAGIC_BOLT).collisionPos;

		if (sprite != null && sprite.parent != null && (sprite.visible || Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[cell])){
			sprite.zap(cell);
			MagicMissile.boltFromChar(sprite.parent, MagicMissile.BLOOD_CONE, sprite, cell, new Callback() {
				@Override
				public void call() {
					boltHit(cell);
					next();
				}
			});
			Sample.INSTANCE.play(Assets.Sounds.ZAP);
			return false;
		}
		boltHit(cell);
		return true;
	}

	private void boltHit( int cell ){
		Char ch = Actor.findChar(cell);
		if (ch == null || ch == this || !ch.isAlive() || !hurts(ch)) return;
		ch.damage(Random.NormalIntRange(boltMin(), boltMax()), new RiftHerald.Strike(this));
		if (ch.sprite != null && ch.sprite.visible) ch.sprite.burst(0xFFCC0000, 6);
		if (ch.isAlive() && ch.alignment != alignment) Fragile.add(ch, 1);
		checkHeroDeath(ch);
	}

	// *** Rings of ruin ***

	private boolean castRings(){
		ringCooldown = RING_COOLDOWN;
		RuinRings rings = Buff.append(this, RuinRings.class);
		rings.start(pos);
		if (Dungeon.level.heroFOV[pos]){
			GLog.w(Messages.get(this, "rings_warn"));
			for (Hero h : Party.living()) h.interrupt();
		}
		spend(TICK);
		if (sprite != null && (sprite.visible || Dungeon.level.heroFOV[pos])){
			sprite.zap(pos, new Callback() {
				@Override
				public void call() {
					sprite.idle();
					next();
				}
			});
			return false;
		}
		return true;
	}

	//cells of the ring at a distance from a center, in its line of sight
	public static ArrayList<Integer> ringCells( int center, int radius ){
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width();
		boolean[] sight = new boolean[Dungeon.level.length()];
		Point c = Dungeon.level.cellToPoint(center);
		ShadowCaster.castShadow(c.x, c.y, w, sight, Dungeon.level.losBlocking, RING_MAX + 1);
		for (int y = c.y - radius; y <= c.y + radius; y++){
			for (int x = c.x - radius; x <= c.x + radius; x++){
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
				int cell = x + y*w;
				if (!sight[cell] || Dungeon.level.solid[cell]) continue;
				if (Math.round(Dungeon.level.trueDistance(center, cell)) == radius) cells.add(cell);
			}
		}
		return cells;
	}

	// *** helpers ***

	//his attacks hurt the party and its allies, and also the other boss of the floor
	private boolean hurts( Char ch ){
		//(not neutral things such as DM-300's pylons)
		return ch.alignment == Alignment.ALLY
				|| (ch != this && ch.alignment == alignment && Char.hasProp(ch, Property.BOSS));
	}

	private void checkHeroDeath( Char ch ){
		if (ch instanceof Hero && !ch.isAlive()){
			Dungeon.fail(this);
			GLog.n(Messages.capitalize(Messages.get(Char.class, "kill", name())));
		}
	}

	@Override
	protected Item riftLoot() {
		return new BloodRose();
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", Math.round(CURRENT_HP*100), BURST_BOLTS, BURST_RANGE,
				boltMin(), boltMax(), RING_MAX, ringMin(), ringMax(), Math.round(RING_CURRENT_HP*100), Fragile.MAX);
	}

	private static final String BURST_LEFT = "burst_left";
	private static final String BURST_CD = "burst_cd";
	private static final String RING_CD = "ring_cd";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(BURST_LEFT, burstLeft);
		bundle.put(BURST_CD, burstCooldown);
		bundle.put(RING_CD, ringCooldown);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		burstLeft = bundle.getInt(BURST_LEFT);
		burstCooldown = bundle.getInt(BURST_CD);
		ringCooldown = bundle.getInt(RING_CD);
	}

	//the rings of one cast: each turn the announced ring goes off and the next one is announced
	public static class RuinRings extends Buff {

		{
			type = buffType.NEUTRAL;
		}

		public int center = -1;
		public int radius = 0;   //last ring that went off

		void start( int center ){
			this.center = center;
			radius = 0;
			announce(1);
			spend(TICK);
		}

		private void announce( int r ){
			for (int c : ringCells(center, r)){
				GameScene.targetedCell(c, 1);
			}
		}

		@Override
		public boolean act() {
			if (!(target instanceof Atakhan) || !target.isAlive() || center < 0){
				detach();
				return true;
			}
			Atakhan boss = (Atakhan) target;
			radius++;
			boolean seen = false;
			ArrayList<Integer> cells = ringCells(center, radius);
			for (int c : cells){
				if (Dungeon.level.heroFOV[c]){
					seen = true;
					CellEmitter.get(c).burst(BloodParticle.BURST, 4);
				}
			}
			RiftHerald.Strike strike = new RiftHerald.Strike(boss);
			for (Char ch : Actor.chars()){
				if (ch == boss || !ch.isAlive() || !boss.hurts(ch) || !cells.contains(ch.pos)) continue;
				ch.damage(Random.NormalIntRange(ringMin(), ringMax()) + Math.round(ch.HP * RING_CURRENT_HP), strike);
				boss.checkHeroDeath(ch);
			}
			if (seen) Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.6f, 0.8f);

			if (radius >= RING_MAX){
				detach();
			} else {
				announce(radius + 1);
				spend(TICK);
			}
			return true;
		}

		private static final String CENTER = "center";
		private static final String RADIUS = "radius";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(CENTER, center);
			bundle.put(RADIUS, radius);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			center = bundle.getInt(CENTER);
			radius = bundle.getInt(RADIUS);
		}
	}

	//"Fragile and unworthy": stacks that weaken the armor against Atakhan, renewed by every new stack
	public static class Fragile extends Buff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		public static final int MAX = 10;
		public static final float DURATION = 10f;

		public int stacks = 0;
		public float left = DURATION;

		public static void add( Char ch, int amount ){
			Fragile f = Buff.affect(ch, Fragile.class);
			f.stacks = Math.min(MAX, f.stacks + amount);
			f.left = DURATION;
		}

		public static int stacks( Char ch ){
			Fragile f = ch.buff(Fragile.class);
			return f == null ? 0 : f.stacks;
		}

		//armor ignored by his hits: one point per stack, up to the armor of the target
		public static int armorIgnored( Char ch ){
			int s = stacks(ch);
			if (s <= 0) return 0;
			if (ch instanceof Hero){
				Hero h = (Hero) ch;
				int armor = h.belongings.armor() != null ? h.belongings.armor().DRMax() : 0;
				return Math.min(s, armor);
			}
			return s/2;
		}

		@Override
		public boolean act() {
			left -= TICK;
			if (left <= 0){
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.VULNERABLE;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.25f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - left) / DURATION);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(stacks);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", stacks, MAX, dispTurns(left));
		}

		private static final String STACKS = "stacks";
		private static final String LEFT = "left";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(STACKS, stacks);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			stacks = bundle.getInt(STACKS);
			left = bundle.getFloat(LEFT);
		}
	}

	public static class Sprite extends RiftHerald.Sprite {

		@Override
		protected String sheet() {
			return Assets.Sprites.ATAKHAN;
		}

		@Override
		public int blood() {
			return 0xFFCC1111;
		}
	}
}
