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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Elder Dragon, crashes the dwarf king's party on 20. drops the execute sword
//  Take flight: when it arrives, it throws back everyone close to it.
//  Ancestral breath: every few turns it marks a cone, and breathes on it the next turn.
//  Execution: its hits and its breath leave the ancestral mark (see ElderMark).
public class ElderDragon extends RiftBoss {

	{
		spriteClass = ElderDragonSprite.class;

		HP = HT = 280;
		defenseSkill = 24;
		EXP = 40;
		viewDistance = 8;

		flying = true;
		properties.add(Property.LARGE);
		immunities.add(Burning.class);
		immunities.add(ElderBurn.class);
	}

	public static final int FLIGHT_RADIUS = 3;
	public static final int BREATH_RANGE = 5;
	public static final float BREATH_DEGREES = 60;
	public static final int BREATH_MIN = 12;
	public static final int BREATH_MAX = 18;
	public static final int BURN_POWER = 3;
	public static final int BURN_TURNS = 4;

	private boolean tookFlight = false;
	//the first breath comes right after taking flight, while the heroes are still thrown back
	private int breathCooldown = 1;
	//cells marked for the breath, null when it isn't drawing breath
	private int[] breathCells = null;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(12, 20);
	}

	@Override
	public int attackSkill( Char target ) {
		return 30;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 10);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.alignment == Alignment.ALLY) ElderMark.mark(enemy);
		return damage;
	}

	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView(this, fieldOfView);

		if (paralysed <= 0){
			if (!tookFlight){
				tookFlight = true;
				takeFlight();
				spend(TICK);
				return true;
			}
			if (breathCells != null){
				breathe();
				spend(TICK);
				return true;
			}
			if (breathCooldown > 0) breathCooldown--;
			if (breathCooldown <= 0 && drawBreath()){
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	//Take flight: throws back the heroes and their allies around it
	private void takeFlight(){
		if (Dungeon.level.heroFOV[pos]){
			GLog.w(Messages.get(this, "flight"));
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.8f);
			PixelScene.shake(2, 0.6f);
			CellEmitter.center(pos).burst(PurpleParticle.BURST, 30);
		}
		pushAway(this, FLIGHT_RADIUS);
	}

	//purely visual zap: the callback only returns the sprite to idle, nothing waits for it
	static void gesture( final CharSprite s, int cell ){
		s.zap(cell, new Callback() {
			@Override
			public void call() {
				s.idle();
			}
		});
	}

	//pushes every character of the other side within a radius out to radius+2 cells
	static void pushAway( Char from, int radius ){
		ArrayList<Char> targets = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch == from || !ch.isAlive() || ch.alignment == from.alignment) continue;
			if (Dungeon.level.distance(from.pos, ch.pos) <= radius) targets.add(ch);
		}
		for (Char ch : targets){
			int power = radius + 2 - Dungeon.level.distance(from.pos, ch.pos);
			Ballistica line = new Ballistica(from.pos, ch.pos, Ballistica.WONT_STOP);
			int idx = line.path.indexOf(ch.pos);
			if (idx < 0 || power <= 0) continue;
			int dest = line.path.get(Math.min(idx + power, line.path.size() - 1));
			if (dest == ch.pos) continue;
			Ballistica trajectory = new Ballistica(ch.pos, dest, Ballistica.MAGIC_BOLT);
			WandOfBlastWave.throwChar(ch, trajectory, power, false, false, from);
		}
	}

	private Char breathTarget(){
		if (canBreatheAt(enemy)) return enemy;
		Char best = null;
		for (Char ch : Actor.chars()){
			if (ch.alignment == alignment || !canBreatheAt(ch)) continue;
			if (best == null || Dungeon.level.distance(pos, ch.pos) < Dungeon.level.distance(pos, best.pos)){
				best = ch;
			}
		}
		return best;
	}

	private boolean canBreatheAt( Char ch ){
		return ch != null && ch != this && ch.isAlive() && ch.invisible <= 0
				&& Actor.chars().contains(ch)
				&& fieldOfView[ch.pos] && Dungeon.level.distance(pos, ch.pos) <= BREATH_RANGE;
	}

	//marks the cone of the breath, which comes on its next turn
	private boolean drawBreath(){
		Char target = breathTarget();
		if (target == null) return false;

		Ballistica aim = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		ConeAOE cone = new ConeAOE(aim, BREATH_RANGE, BREATH_DEGREES,
				Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : cone.cells){
			if (c != pos && !Dungeon.level.solid[c]) cells.add(c);
		}
		if (cells.isEmpty()) return false;

		breathCells = new int[cells.size()];
		for (int i = 0; i < cells.size(); i++){
			breathCells[i] = cells.get(i);
			GameScene.targetedCell(breathCells[i], TICK);
		}
		if (sprite != null) sprite.turnTo(pos, target.pos);
		if (Dungeon.level.heroFOV[pos]){
			GLog.w(Messages.get(this, "breath_warn"));
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.8f);
		}
		return true;
	}

	private void breathe(){
		int[] cells = breathCells;
		breathCells = null;
		breathCooldown = Random.NormalIntRange(4, 5);

		if (sprite != null && cells.length > 0) gesture(sprite, cells[cells.length/2]);
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.8f);

		ArrayList<Char> hit = new ArrayList<>();
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				CellEmitter.get(c).burst(PurpleParticle.BURST, 5);
				CellEmitter.get(c).burst(FlameParticle.FACTORY, 2);
			}
			Char ch = Actor.findChar(c);
			if (ch != null && ch != this && ch.isAlive() && !hit.contains(ch)) hit.add(ch);
		}
		//it doesn't care who is in the way: the other boss of the floor burns too
		for (Char ch : hit){
			ch.damage(Random.NormalIntRange(BREATH_MIN, BREATH_MAX), this);
			if (ch.isAlive()){
				ElderBurn.apply(ch, BURN_POWER, BURN_TURNS);
				if (ch.alignment == Alignment.ALLY) ElderMark.mark(ch);
			} else if (ch instanceof Hero){
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "breath_kill"));
			}
		}
	}

	@Override
	protected Item riftLoot() {
		ElderBlade blade = new ElderBlade();
		blade.level(3);
		blade.identify();
		return blade;
	}

	private static final String TOOK_FLIGHT = "took_flight";
	private static final String BREATH_CD = "breath_cd";
	private static final String BREATH_CELLS = "breath_cells";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(TOOK_FLIGHT, tookFlight);
		bundle.put(BREATH_CD, breathCooldown);
		if (breathCells != null) bundle.put(BREATH_CELLS, breathCells);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		tookFlight = bundle.getBoolean(TOOK_FLIGHT);
		breathCooldown = bundle.getInt(BREATH_CD);
		breathCells = bundle.contains(BREATH_CELLS) ? bundle.getIntArray(BREATH_CELLS) : null;
	}
}
