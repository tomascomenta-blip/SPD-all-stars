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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//beaked rex, a hunter of the ash plains: carbon down, a bare neck, a hooked beak of bone. it finds
//you by your light and your noise, roars when it does, and then either lowers its head and charges
//down a marked lane, or breathes in (the throat glows) and spits a cone of fire. both give time
public class BeakRex extends AshColossus {

	{
		size = 5;
		spriteClass = RexSprite.class;

		HP = HT = 820;
		defenseSkill = 18;
		EXP = 55;

		minDmg = 44; maxDmg = 58; acc = 62;
		armorMin = 8; armorMax = 20;

		properties.add(Property.FIERY);
	}

	private static final int CHARGE = 0, FIRE = 1;
	private static final int CHARGE_RANGE = 10, CONE = 7;

	private boolean roared = false;

	//the bare neck
	//beaten without being killed (knocked down) it takes the collar
	@Override
	public boolean tameable() {
		return true;
	}

	@Override
	public String tameBlock( Hero h ) {
		return down > 0 ? null : Messages.get(this, "standing");
	}

	@Override
	public int[][] weakSpots() {
		return new int[][]{{4, 0}, {4, 1}};
	}

	@Override
	protected int eyes() {
		return 3;
	}

	@Override
	protected int ears() {
		return 4;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	@Override
	public void notice() {
		super.notice();
		if (roared || enemy == null) return;
		roared = true;
		Senses.noise(pos, Senses.ROAR);
		if (sprite instanceof RexSprite) ((RexSprite) sprite).roar();
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.5f);
			GLog.w(Messages.get(this, "roar"));
		}
	}

	@Override
	protected boolean act() {
		if (state == WANDERING && enemy == null) roared = false;
		return super.act();
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d >= 2 && d <= CONE && Random.Int(2) == 0){
			ArrayList<Integer> cone = cone(enemy.pos);
			if (cone.contains(enemy.pos)){
				//it takes a long breath: a cone is wide at the far end
				warn(FIRE, cone, enemy.pos, 3f);
				return true;
			}
		}
		if (d >= 3 && d <= CHARGE_RANGE){
			int[] dir = toward(enemy.pos);
			ArrayList<Integer> p = path(dir[0], dir[1]);
			LinkedHashSet<Integer> lane = lane(p);
			if (p.size() >= 3 && lane.contains(enemy.pos)){
				//five wide: three steps to get out
				warn(CHARGE, lane, enemy.pos, 3f);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind == FIRE) breathe(cells);
		else charge(aim);
	}

	// *** fire ***

	//where the beak is: the middle of the front side, the way it faces
	private int mouth(){
		int w = Dungeon.level.width();
		int x = right ? left() + size : left() - 1;
		return x + top() * w;
	}

	//a cone of 60 degrees off the beak, as far as the fire reaches
	private ArrayList<Integer> cone( int aim ){
		face(aim);
		ArrayList<Integer> out = new ArrayList<>();
		Level l = Dungeon.level;
		int w = l.width(), from = mouth();
		double fx = from % w, fy = from / w;
		double ax = aim % w - fx, ay = aim / w - fy;
		double len = Math.sqrt(ax * ax + ay * ay);
		if (len < 0.5) return out;
		for (int c : SKCombat.area(from, CONE)){
			if (covers(c) || l.pit[c]) continue;
			double cx = c % w - fx, cy = c / w - fy;
			double cl = Math.sqrt(cx * cx + cy * cy);
			if (cl > CONE + 0.5) continue;
			if (cl >= 0.5 && (cx * ax + cy * ay) / (cl * len) < 0.86) continue;
			if (c != from && new Ballistica(from, c, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos != c) continue;
			out.add(c);
		}
		return out;
	}

	private void breathe( ArrayList<Integer> cells ){
		strikeAnim(FIRE);
		Senses.noise(pos, Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.7f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(FlameParticle.FACTORY, 5);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 75, 100);
			if (ch.isAlive() && !Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 5f);
		}
	}

	// *** charge ***

	private int[] toward( int cell ){
		int w = Dungeon.level.width();
		int dx = cell % w - pos % w, dy = cell / w - pos / w;
		//straight unless it's clearly off to the diagonal
		int sx = Math.abs(dx) * 2 >= Math.abs(dy) ? Integer.signum(dx) : 0;
		int sy = Math.abs(dy) * 2 >= Math.abs(dx) ? Integer.signum(dy) : 0;
		return new int[]{sx, sy};
	}

	private ArrayList<Integer> path( int dx, int dy ){
		ArrayList<Integer> out = new ArrayList<>();
		if (dx == 0 && dy == 0) return out;
		boolean[] f = fits();
		int w = Dungeon.level.width();
		int x = pos % w, y = pos / w;
		for (int k = 1; k <= CHARGE_RANGE; k++){
			int nx = x + dx * k, ny = y + dy * k;
			if (nx < 0 || ny < 0 || nx >= w || ny >= Dungeon.level.height()) break;
			if (!f[nx + ny * w]) break;
			out.add(nx + ny * w);
		}
		return out;
	}

	private LinkedHashSet<Integer> lane( ArrayList<Integer> centers ){
		LinkedHashSet<Integer> out = new LinkedHashSet<>();
		int w = Dungeon.level.width();
		for (int c : centers){
			for (int y = -half(); y <= half(); y++){
				for (int x = -half(); x <= half(); x++){
					int k = c + x + y * w;
					if (k >= 0 && k < Dungeon.level.length() && !covers(k)) out.add(k);
				}
			}
		}
		return out;
	}

	private boolean charging = false;

	private void charge( int aim ){
		int[] dir = toward(aim);
		int start = pos;
		charging = true;
		for (int c : path(dir[0], dir[1])){
			if (!isAlive() || !fitsAt(c)) break;
			walkTo(c);
		}
		charging = false;
		if (!isAlive()) return;
		Senses.noise(pos, Senses.CRASH);
		if (sprite != null && pos != start){
			sprite.move(start, pos);
			strikeAnim(CHARGE);
		}
	}

	@Override
	protected void onTrample( Char ch ) {
		if (!charging || !SKCombat.foes(this, ch)){
			super.onTrample(ch);
			return;
		}
		blast(ch, 80, 110);
		if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("roared", roared);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		roared = bundle.getBoolean("roared");
	}

	//12-13 roar, 14 charging, 15 breathing in, 16 spitting
	public static class RexSprite extends AshColossusSprite {

		@Override
		protected int[] downFrames() {
			return new int[]{17, 18, 19, 20};
		}

		@Override
		protected String tamedSheet() {
			return FaunaAssets.REX_TAMED;
		}

		public RexSprite(){
			super();
			setup(FaunaAssets.REX, 5, 112, 112, 16, 32);
		}

		public void roar(){
			once(clip(6, 12, 13, 12, 13, 13));
		}

		@Override
		public void windup( int kind ) {
			play(kind == FIRE ? loop(4, 15, 15, 15, 1) : loop(3, 14));
		}

		@Override
		public void strike( int kind ) {
			once(kind == FIRE ? clip(8, 16, 16, 16, 16) : clip(10, 14, 7, 8));
		}
	}
}
