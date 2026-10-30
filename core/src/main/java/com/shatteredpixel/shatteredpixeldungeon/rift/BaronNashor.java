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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Baron Nashor, joins Yog on 25. reward: hand of baron (allies +50%).
//never leaves its pit, hits from 4 cells and spits corruption at whoever has the fewest stacks
//(so it spreads it around, rude)
//  Recoil: when it arrives, it throws back everyone close to it.
//  Acid pools: three pools in a cone, marked one turn before; they hurt and slow.
//  Tentacle: marks the 3x3 square under a hero, and the next turn hurts and stuns there.
public class BaronNashor extends RiftBoss {

	{
		spriteClass = BaronNashorSprite.class;

		HP = HT = 400;
		defenseSkill = 15;
		EXP = 50;
		viewDistance = 8;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.DEMONIC);
		immunities.add(BaronAcid.class);
		immunities.add(BaronCorruption.class);
		immunities.add(Slow.class);
	}

	public static final int RANGE = 4;
	public static final int RECOIL_RADIUS = 3;

	public static final int SPIT_MIN = 3;
	public static final int SPIT_MAX = 6;

	public static final int ACID_RANGE = 6;
	public static final float ACID_DEGREES = 60;
	public static final int ACID_POOLS = 3;
	public static final int ACID_MIN = 10;
	public static final int ACID_MAX = 16;
	public static final int ACID_COOLDOWN = 9;

	public static final int TENTACLE_RANGE = 8;
	public static final int TENTACLE_MIN = 15;
	public static final int TENTACLE_MAX = 22;
	public static final int TENTACLE_COOLDOWN = 7;

	private boolean recoiled = false;
	private int acidCooldown = 5;
	private int tentacleCooldown = 3;
	//cells marked for the next acid or tentacle, null when nothing is coming
	private int[] acidCells = null;
	private int[] tentacleCells = null;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(16, 26);
	}

	@Override
	public int attackSkill( Char target ) {
		return 38;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 12);
	}

	// *** it never moves ***

	@Override
	protected boolean getCloser( int target ) {
		return false;
	}

	@Override
	protected boolean getFurther( int target ) {
		return false;
	}

	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView(this, fieldOfView);
		//it never wanders off: with nobody in sight it just waits in its pit
		if (state != HUNTING) state = HUNTING;

		if (paralysed <= 0){
			if (!recoiled){
				recoiled = true;
				recoil();
				spend(TICK);
				return true;
			}
			//what was marked last turn lands now, and the turn goes on
			if (acidCells != null) releaseAcid();
			if (tentacleCells != null) releaseTentacle();

			if (acidCooldown > 0) acidCooldown--;
			if (tentacleCooldown > 0) tentacleCooldown--;
			if (tentacleCooldown <= 0 && markTentacle()){
				tentacleCooldown = TENTACLE_COOLDOWN;
				spend(TICK);
				return true;
			}
			if (acidCooldown <= 0 && markAcid()){
				acidCooldown = ACID_COOLDOWN;
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	//Recoil: throws back the heroes and their allies around the pit
	private void recoil(){
		if (Dungeon.level.heroFOV[pos]){
			GLog.w(Messages.get(this, "recoil"));
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
			PixelScene.shake(3, 0.7f);
			CellEmitter.center(pos).burst(PurpleParticle.BURST, 30);
		}
		ElderDragon.pushAway(this, RECOIL_RADIUS);
	}

	// *** basic attack: bite next to it, spit up to 4 cells away ***

	@Override
	protected boolean canAttack( Char enemy ) {
		return Dungeon.level.distance(pos, enemy.pos) <= RANGE
				&& new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (Dungeon.level.adjacent(pos, enemy.pos)){
			return super.doAttack(enemy);
		}
		if (sprite != null && (sprite.visible || enemy.sprite.visible)){
			sprite.zap(enemy.pos);
			return false;
		} else {
			spit();
			return true;
		}
	}

	public void onZapComplete() {
		spit();
		next();
	}

	private void spit(){
		attack(enemy);
		Invisibility.dispel(this);
		spend(attackDelay());
	}

	//every attack lowers the armor of its target by the void corruption on it,
	//and spits a glob of corruption at the hero with the fewest stacks
	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
		boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti);
		corrosion();
		return result;
	}

	private void corrosion(){
		Hero target = null;
		int fewest = Integer.MAX_VALUE;
		for (Hero h : Party.living()){
			if (fieldOfView == null || !fieldOfView[h.pos]) continue;
			int s = BaronCorruption.stacks(h);
			if (s < fewest){
				fewest = s;
				target = h;
			}
		}
		if (target == null) return;

		if (sprite != null && sprite.parent != null && (sprite.visible || target.sprite.visible)){
			MagicMissile.boltFromChar(sprite.parent, MagicMissile.SHADOW, sprite, target.pos, null);
		}
		target.damage(Random.NormalIntRange(SPIT_MIN, SPIT_MAX), this);
		if (target.isAlive()){
			BaronCorruption.addStack(target);
			if (target.sprite != null && target.sprite.visible){
				target.sprite.emitter().burst(ShadowParticle.UP, 5);
			}
		} else {
			Dungeon.fail(this);
			GLog.n(Messages.get(this, "spit_kill"));
		}
	}

	// *** abilities ***

	//heroes first; their allies only when no hero is in reach
	private Char abilityTarget( int range ){
		ArrayList<Char> options = new ArrayList<>();
		for (Hero h : Party.living()){
			if (fieldOfView[h.pos] && h.invisible <= 0 && Dungeon.level.distance(pos, h.pos) <= range) options.add(h);
		}
		if (options.isEmpty()){
			for (Char ch : Actor.chars()){
				if (ch instanceof Hero || ch == this || !ch.isAlive() || ch.alignment == alignment) continue;
				if (fieldOfView[ch.pos] && ch.invisible <= 0 && Dungeon.level.distance(pos, ch.pos) <= range) options.add(ch);
			}
		}
		return options.isEmpty() ? null : Random.element(options);
	}

	private void addCell( ArrayList<Integer> cells, int c ){
		if (Dungeon.level.insideMap(c) && !Dungeon.level.solid[c] && c != pos && !cells.contains(c)) cells.add(c);
	}

	//shows the cells where something lands next turn
	private static int[] markCells( ArrayList<Integer> cells ){
		int[] result = new int[cells.size()];
		for (int i = 0; i < result.length; i++){
			result[i] = cells.get(i);
			GameScene.targetedCell(result[i], TICK);
		}
		return result;
	}

	private boolean markTentacle(){
		Char target = abilityTarget(TENTACLE_RANGE);
		if (target == null) return false;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS9){
			addCell(cells, target.pos + n);
		}
		tentacleCells = markCells(cells);
		if (sprite != null) sprite.turnTo(pos, target.pos);
		if (Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[target.pos]){
			GLog.w(Messages.get(this, "tentacle_warn"));
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.6f);
			for (int c : tentacleCells){
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(ShadowParticle.UP, 2);
			}
		}
		return true;
	}

	private void releaseTentacle(){
		int[] cells = tentacleCells;
		tentacleCells = null;
		boolean visible = false;
		ArrayList<Char> hit = new ArrayList<>();
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				visible = true;
				CellEmitter.get(c).burst(ShadowParticle.UP, 8);
				CellEmitter.get(c).burst(PurpleParticle.BURST, 4);
			}
			Char ch = Actor.findChar(c);
			if (ch != null && ch != this && ch.isAlive() && !hit.contains(ch)) hit.add(ch);
		}
		if (visible){
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.6f);
			PixelScene.shake(2, 0.4f);
		}
		//the tentacle hits whatever stands there, the other boss of the floor too
		for (Char ch : hit){
			ch.damage(Random.NormalIntRange(TENTACLE_MIN, TENTACLE_MAX), this);
			if (ch.isAlive()){
				Buff.prolong(ch, Paralysis.class, 1f);
			} else if (ch instanceof Hero){
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "tentacle_kill"));
			}
		}
	}

	private boolean markAcid(){
		Char target = abilityTarget(ACID_RANGE);
		if (target == null) return false;

		Ballistica aim = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		ConeAOE cone = new ConeAOE(aim, ACID_RANGE, ACID_DEGREES,
				Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		ArrayList<Integer> options = new ArrayList<>();
		for (int c : cone.cells){
			if (c != pos && !Dungeon.level.solid[c] && Dungeon.level.distance(pos, c) >= 2) options.add(c);
		}
		//one pool under the target, the others somewhere else in the cone
		ArrayList<Integer> centers = new ArrayList<>();
		centers.add(target.pos);
		pickPools(centers, options);
		//a cone squeezed by walls (or a target right next to the pit): the rest land around the target
		if (centers.size() < ACID_POOLS){
			ArrayList<Integer> around = new ArrayList<>();
			for (int c = 0; c < Dungeon.level.length(); c++){
				int d = Dungeon.level.distance(target.pos, c);
				if (d >= 2 && d <= 3 && fieldOfView[c] && !Dungeon.level.solid[c] && c != pos) around.add(c);
			}
			pickPools(centers, around);
		}
		ArrayList<Integer> cells = new ArrayList<>();
		for (int center : centers){
			addCell(cells, center);
			for (int n : PathFinder.NEIGHBOURS4){
				addCell(cells, center + n);
			}
		}
		acidCells = markCells(cells);
		if (sprite != null) sprite.turnTo(pos, target.pos);
		if (Dungeon.level.heroFOV[pos]){
			GLog.w(Messages.get(this, "acid_warn"));
			Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 0.7f);
		}
		return true;
	}

	private void pickPools( ArrayList<Integer> centers, ArrayList<Integer> options ){
		while (centers.size() < ACID_POOLS && !options.isEmpty()){
			int c = options.remove(Random.Int(options.size()));
			boolean apart = true;
			for (int other : centers){
				if (Dungeon.level.distance(c, other) < 2) apart = false;
			}
			if (apart) centers.add(c);
		}
	}

	private void releaseAcid(){
		int[] cells = acidCells;
		acidCells = null;
		if (sprite != null && cells.length > 0) ElderDragon.gesture(sprite, cells[0]);
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 0.9f);

		ArrayList<Char> hit = new ArrayList<>();
		BaronAcid acid = null;
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(PoisonParticle.SPLASH, 6);
			Char ch = Actor.findChar(c);
			if (ch != null && ch != this && ch.isAlive() && !hit.contains(ch)) hit.add(ch);
			acid = Blob.seed(c, BaronAcid.TURNS, BaronAcid.class);
		}
		if (acid != null) GameScene.add(acid);
		//the acid burns whatever stands there, the other boss of the floor too
		for (Char ch : hit){
			ch.damage(Random.NormalIntRange(ACID_MIN, ACID_MAX), this);
			if (ch.isAlive()){
				Buff.prolong(ch, Slow.class, BaronAcid.TURNS);
			} else if (ch instanceof Hero){
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "acid_kill"));
			}
		}
	}

	// *** reward: the Hand of Baron for every hero still standing ***

	@Override
	protected void rewardParty() {
		for (Hero h : Party.living()){
			Buff.affect(h, HandOfBaron.class);
			if (h.sprite != null && h.sprite.visible) h.sprite.emitter().burst(PurpleParticle.BURST, 20);
		}
		GLog.p(Messages.get(this, "reward"));
		HandOfBaron.empowerAllies();
	}

	private static final String RECOILED = "recoiled";
	private static final String ACID_CD = "acid_cd";
	private static final String TENTACLE_CD = "tentacle_cd";
	private static final String ACID_CELLS = "acid_cells";
	private static final String TENTACLE_CELLS = "tentacle_cells";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(RECOILED, recoiled);
		bundle.put(ACID_CD, acidCooldown);
		bundle.put(TENTACLE_CD, tentacleCooldown);
		if (acidCells != null) bundle.put(ACID_CELLS, acidCells);
		if (tentacleCells != null) bundle.put(TENTACLE_CELLS, tentacleCells);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		recoiled = bundle.getBoolean(RECOILED);
		acidCooldown = bundle.getInt(ACID_CD);
		tentacleCooldown = bundle.getInt(TENTACLE_CD);
		acidCells = bundle.contains(ACID_CELLS) ? bundle.getIntArray(ACID_CELLS) : null;
		tentacleCells = bundle.contains(TENTACLE_CELLS) ? bundle.getIntArray(TENTACLE_CELLS) : null;
	}
}
