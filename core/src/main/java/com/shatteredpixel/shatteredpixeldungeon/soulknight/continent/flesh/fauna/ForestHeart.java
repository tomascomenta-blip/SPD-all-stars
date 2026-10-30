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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EarthParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//heart of the forest, guardian of the flesh forest: a heart the size of a hill that grew into a tree,
//walking on eight roots. it beats: the beat shakes the ground around it (marked), the crown lets go
//spores over whoever it hears, and its roots break out of the ground in a line towards the far ones.
//the knot of veins at the base of the aorta beats harder than the rest: that's where it hurts
public class ForestHeart extends FleshColossus {

	{
		size = 15;
		spriteClass = HeartSprite.class;

		HP = HT = 9000;
		defenseSkill = 0;
		EXP = 400;
		baseSpeed = 0.4f;

		minDmg = 60; maxDmg = 80; acc = 66;
		armorMin = 26; armorMax = 46;

		properties.add(Property.MINIBOSS);
	}

	private static final int BEAT = 0, SPORES = 1, ROOTS = 2;
	private static final int ROOT_REACH = 12;

	//the knot at the base of the aorta, on top of the heart
	@Override
	public int[][] weakSpots() {
		return new int[][]{{8, 0}, {8, 1}};
	}

	//the bark plates over the ventricles
	@Override
	public int[][] climbable() {
		return new int[][]{{1, 4, 10}, {2, 3, 11}, {3, 3, 11}, {4, 3, 11}, {5, 4, 10}, {6, 5, 9}};
	}

	@Override
	protected int eyes() {
		return 2;
	}

	//it feels every step on its roots
	@Override
	protected int ears() {
		return 8;
	}

	@Override
	protected int roam() {
		return 18;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		int close = SKCombat.foesIn(this, ring(3)).size();
		if (close >= 2 || close == 1 && Random.Int(3) > 0){
			//a beat: four cells out, three turns to get away
			warn(BEAT, ring(3), enemy.pos, 3f);
			return true;
		}
		if (d > ROOT_REACH) return false;
		if (Random.Int(2) == 0){
			LinkedHashSet<Integer> cells = new LinkedHashSet<>();
			for (Hero h : Party.living()){
				if (reach(h.pos) <= ROOT_REACH) cells.addAll(square(h.pos, 1));
			}
			if (cells.isEmpty()) return false;
			warn(SPORES, cells, enemy.pos, 2f);
			return true;
		}
		ArrayList<Integer> line = roots(enemy.pos);
		if (line.size() < 2) return false;
		warn(ROOTS, line, enemy.pos, 2f);
		return true;
	}

	//roots coming up from under its body towards a cell, three wide
	private ArrayList<Integer> roots( int aim ){
		ArrayList<Integer> out = new ArrayList<>();
		int from = edgeToward(aim);
		Ballistica b = new Ballistica(from, aim, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		if (b.dist < 1) return out;
		int w = Dungeon.level.width();
		for (int c : b.subPath(1, Math.min(b.dist + 1, ROOT_REACH + 1))){
			for (int k : new int[]{c, c - 1, c + 1, c - w, c + w}){
				if (k < 0 || k >= Dungeon.level.length() || covers(k) || Dungeon.level.solid[k] || out.contains(k)) continue;
				out.add(k);
			}
		}
		return out;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		if (kind == BEAT){
			Senses.noise(pos, Senses.STAMPEDE);
			if (seen()){
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
				Camera.main.shake(4, 0.6f);
				for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(BloodParticle.BURST, 2);
			}
			for (Char ch : SKCombat.foesIn(this, cells)){
				blast(ch, 120, 160);
				knock(ch, 3);
				if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 3f);
			}
		} else if (kind == SPORES){
			Senses.noise(pos, Senses.FIGHT);
			if (seen()) Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 0.7f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.STENCH), 2);
			for (Char ch : SKCombat.foesIn(this, cells)){
				blast(ch, 50, 70);
				if (ch.isAlive()) Buff.affect(ch, Poison.class).set(8);
			}
		} else {
			Senses.noise(aim, Senses.CRASH);
			if (seen()) Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.8f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(EarthParticle.FACTORY, 4);
			for (Char ch : SKCombat.foesIn(this, cells)){
				blast(ch, 80, 110);
				if (ch.isAlive()) Buff.prolong(ch, Roots.class, 3f);
			}
		}
	}

	@Override
	public void notice() {
		super.notice();
		if (enemy != null && seen()) GLog.w(Messages.get(this, "beat"));
	}

	//6 it shrinks, 7 the beat, 8 spores from the crown
	public static class HeartSprite extends Sheet {

		public HeartSprite(){
			super();
			layout(FleshAssets.HEART, 15, 304, 384, 32, 128, 9, 10);
			idle = loop(3, 0, 1, 0, 0, 1, 0);
			run = loop(4, 2, 3, 4, 5);
		}

		@Override
		protected PointF standingSpot() {
			return new PointF(166, 148);
		}

		@Override
		protected PointF lyingSpot() {
			return new PointF(166, 186);
		}

		@Override
		public void windup( int kind ) {
			play(loop(2, kind == SPORES ? 8 : 6, kind == SPORES ? 8 : 6, 0));
		}

		@Override
		public void strike( int kind ) {
			once(kind == SPORES ? clip(6, 8, 8, 8, 0) : clip(8, 7, 7, 7, 0));
		}
	}
}
