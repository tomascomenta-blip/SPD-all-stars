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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral.Cathedral;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral.CathedralAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//furnace cathedral. a whole church walking on six legs of yog flesh, a furnace burning where the
//nave was. its front always looks down the street: the bells ring before it vomits fire through
//the portal (a marked cone), behind it the chimneys rain embers (marked too), and now and then
//the doors open and things of the city come out
public class FurnaceCathedral extends CityColossus {

	{
		size = 15;
		spriteClass = CathedralSprite.class;

		HP = HT = 7500;
		defenseSkill = 0;
		EXP = 320;
		baseSpeed = 0.35f;

		minDmg = 60; maxDmg = 80; acc = 64;
		armorMin = 30; armorMax = 50;

		properties.add(Property.FIERY);
		properties.add(Property.INORGANIC);
		properties.add(Property.MINIBOSS);
	}

	private static final int FIRE = 0, DOORS = 1, STAMP = 2, EMBERS = 3;
	private static final int FIRE_RANGE = 9;
	//no more than these of its own brood around at once
	private static final int BROOD = 6;

	private int opened = 0;
	//how many times it went down: walking in is for that one time (Cathedral)
	public int falls = 0;

	//the portal (whoever stands at the doors hits the furnace) and the rose window, from the roof
	@Override
	public int[][] weakSpots() {
		return new int[][]{{7, 14}, {7, 4}};
	}

	//the slate roof, the towers and the gallery over the portal
	@Override
	public int[][] climbable() {
		return new int[][]{{1, 3, 4}, {1, 10, 11}, {2, 3, 11}, {3, 2, 12}, {4, 2, 12}, {5, 2, 12}, {6, 3, 11}, {7, 4, 10}};
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
	protected int roam() {
		return 14;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	//the cell right outside the portal
	public int doorstep(){
		int w = Dungeon.level.width();
		return left() + 7 + (top() + size) * w;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		int touching = SKCombat.foesIn(this, ring(1)).size();
		if (touching >= 2 || touching == 1 && d <= 1 && Random.Int(3) == 0){
			opened++;
			warn(STAMP, ring(2), enemy.pos, 2f);
			return true;
		}
		int w = Dungeon.level.width();
		//the portal looks down the street: the fire reaches the lower half of its sides too
		boolean inFront = enemy.pos / w >= top() + size / 2;
		if (inFront && d <= FIRE_RANGE){
			ArrayList<Integer> cone = cone(doorstep(), enemy.pos, FIRE_RANGE, 0.7);
			if (cone.contains(enemy.pos)){
				opened++;
				warn(FIRE, cone, enemy.pos, 3f);
				Senses.noise(pos, Senses.ROAR);
				return true;
			}
		}
		//behind it or to the far side, the chimneys rain embers on whoever is near
		if (!inFront && d <= FIRE_RANGE && Random.Int(2) == 0){
			LinkedHashSet<Integer> cells = new LinkedHashSet<>();
			for (Hero h : Party.living()){
				if (reach(h.pos) <= FIRE_RANGE) cells.addAll(square(h.pos, 1));
			}
			if (!cells.isEmpty()){
				opened++;
				warn(EMBERS, cells, enemy.pos, 3f);
				return true;
			}
		}
		if ((opened >= 2 || d > FIRE_RANGE && Random.Int(3) == 0) && brood() < BROOD){
			opened = 0;
			ArrayList<Integer> door = square(doorstep(), 1);
			warn(DOORS, door, enemy.pos, 2f);
			if (seen()) GLog.w(Messages.get(this, "doors"));
			return true;
		}
		return false;
	}

	@Override
	protected boolean act() {
		Cathedral.hint(this);
		return super.act();
	}

	//knocked down, its doors hang open: the way in, while it lies there
	@Override
	public void knockDown() {
		super.knockDown();
		falls++;
		Cathedral.openDoors(Dungeon.level, this);
	}

	@Override
	protected void standUp() {
		super.standUp();
		Cathedral.shutDoors(this);
	}

	@Override
	public void die( Object cause ) {
		Cathedral.shutDoors(this);
		super.die(cause);
	}

	private int brood(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m.EXP == 0 && m instanceof CityCritter) n++;
		return n;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(pos, Senses.CRASH);
		if (kind == DOORS){
			for (Char ch : SKCombat.foesIn(this, cells)) knock(ch, 2);
			int n = 2 + Random.Int(2);
			for (int i = 0; i < n; i++){
				int k = Random.Int(3);
				letOut(k == 0 ? new SootRats() : k == 1 ? new FleshGargoyle() : new Lamplighter(), doorstep());
			}
			return;
		}
		if (seen()){
			Sample.INSTANCE.play(kind == FIRE ? Assets.Sounds.BURNING : Assets.Sounds.BLAST, 1f, 0.6f);
			for (int c : cells){
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(kind == STAMP ? Speck.factory(Speck.DUST) : FlameParticle.FACTORY, 4);
			}
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			if (kind == FIRE){
				blast(ch, 150, 200);
				if (ch.isAlive() && !Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 5f);
			} else if (kind == EMBERS){
				blast(ch, 80, 110);
				if (ch.isAlive() && !Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 3f);
			} else {
				blast(ch, 90, 130);
				knock(ch, 3);
			}
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("opened", opened);
		bundle.put("falls", falls);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		opened = bundle.getInt("opened");
		falls = bundle.getInt("falls");
	}

	//6 the bells, 7 fire out of the portal, 8-9 the doors open, 10 leaning, 11 dead. lying down with the
	//way in open, the doors of frame 10 are drawn open over it (CathedralAssets.DOORS)
	public static class CathedralSprite extends Sheet {

		private static final int DOOR_X = 143, DOOR_Y = 238, DOOR_W = 32, DOOR_H = 46;

		private Image doors;
		private float flicker;

		public CathedralSprite(){
			super();
			layout(CityAssets.CATHEDRAL, 15, 304, 364, 32, 96, 10, 11);
			run = loop(4, 2, 3, 4, 5);
		}

		private boolean open(){
			return ch instanceof FurnaceCathedral && ch.isAlive() && ((FurnaceCathedral) ch).down > 0
					&& Dungeon.level != null && Cathedral.doorOf(Dungeon.level, ch.id()) != null;
		}

		@Override
		public void update() {
			super.update();
			boolean open = open();
			if (open && doors == null && parent != null){
				doors = new Image(CathedralAssets.DOORS);
				parent.add(doors);
			}
			if (doors == null) return;
			doors.visible = open && visible;
			if (!doors.visible) return;
			flicker += Game.elapsed;
			doors.frame(((int) (flicker * 5)) % 2 * DOOR_W, 0, DOOR_W, DOOR_H);
			doors.flipHorizontal = flipHorizontal;
			doors.x = x + (flipHorizontal ? frameW - DOOR_X - DOOR_W : DOOR_X);
			doors.y = y + DOOR_Y;
		}

		@Override
		public void kill() {
			super.kill();
			if (doors != null) doors.killAndErase();
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(154, 240);
		}

		@Override
		public void windup( int kind ) {
			play(kind == DOORS ? loop(3, 8, 9) : loop(4, 6, 6, 1));
		}

		@Override
		public void strike( int kind ) {
			if (kind == DOORS) once(clip(3, 9, 9, 8, 0));
			else if (kind == EMBERS) once(clip(4, 6, 1, 6, 0));
			else once(clip(8, 7, 7, 7, 7, 0));
		}
	}
}
