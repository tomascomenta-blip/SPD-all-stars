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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//corrupt wyvern. slate scales crusted with frost and open wounds, wings of torn membrane. it flies
//in circles over its prey, out of reach of any sword, and its shadow runs over the snow. then it
//hangs over a spot (the ground there is marked, its shadow right on it) and comes down on it
//headfirst. once down it stays on the ground a few turns, breathing frost, and can be hurt
public class CorruptWyvern extends WildColossus {

	{
		size = 10;
		spriteClass = WyvernSprite.class;

		HP = HT = 3600;
		defenseSkill = 18;
		EXP = 190;
		baseSpeed = 1.5f;
		flying = true;

		minDmg = 48; maxDmg = 64; acc = 68;
		armorMin = 10; armorMax = 22;
	}

	private static final int DIVE = 0, BREATH = 1;
	private static final int ON_GROUND = 5;

	private boolean aloft = true;
	private int grounded = 0, circle = 0;

	public boolean aloft(){
		return aloft;
	}

	@Override
	public boolean solid() {
		return !aloft;
	}

	//up there nothing reaches it, unless it's just come down and couldn't land (then it hangs low a while)
	@Override
	public boolean isInvulnerable( Class effect ) {
		return aloft && grounded == 0 || super.isInvulnerable(effect);
	}

	@Override
	protected boolean walkable( int cell ) {
		return aloft || super.walkable(cell);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return !aloft && super.canAttack(enemy);
	}

	//the head, at the front
	@Override
	public int[][] weakSpots() {
		return new int[][]{{9, 3}, {9, 4}};
	}

	//its back, between the wings (once it's down)
	@Override
	public int[][] climbable() {
		return new int[][]{{4, 3, 6}, {5, 3, 6}};
	}

	@Override
	protected int eyes() {
		return 8;
	}

	@Override
	protected int specialCooldown() {
		return aloft ? 4 : 3;
	}

	@Override
	protected boolean act() {
		if (grounded > 0 && pendingKind == -1 && down == 0 && --grounded == 0 && !aloft){
			aloft = true;
			if (seen()) GLog.w(Messages.get(this, "takes_off"));
		}
		return super.act();
	}

	//up there it wheels round its prey instead of walking at it
	@Override
	protected boolean getCloser( int target ) {
		if (aloft && state == HUNTING && enemy != null){
			int w = Dungeon.level.width();
			int[][] ring = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
			if (Dungeon.level.distance(pos, target) <= 10) circle = (circle + 1) % 8;
			int x = enemy.pos % w + ring[circle][0] * 9, y = enemy.pos / w + ring[circle][1] * 9;
			x = Math.max(half(), Math.min(w - 1 - half(), x));
			y = Math.max(half(), Math.min(Dungeon.level.height() - 1 - half(), y));
			return super.getCloser(x + y * w);
		}
		return super.getCloser(target);
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (aloft){
			if (d > 16) return false;
			//it hangs right over the spot and comes down on it
			int old = pos, over = enemy.pos - 2 * Dungeon.level.width();
			if (over != pos && fitsAt(over) && Actor.findChar(over) == null){
				move(over);
				moveSprite(old, pos);
			}
			ArrayList<Integer> zone = new ArrayList<>();
			Level l = Dungeon.level;
			for (int c : SKCombat.area(enemy.pos, 2)) if (!l.solid[c] && !l.pit[c]) zone.add(c);
			warn(DIVE, zone, enemy.pos, 3f);
			if (seen()) GLog.w(Messages.get(this, "dives"));
			return true;
		}
		if (d >= 2 && d <= 7){
			ArrayList<Integer> c = cone(enemy.pos, 7);
			if (c.contains(enemy.pos)){
				warn(BREATH, c, enemy.pos, 2f);
				return true;
			}
		}
		return false;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == DIVE ? new int[]{140, 185} : new int[]{70, 95};
	}

	@Override
	protected int dust( int kind ) {
		return kind == BREATH ? Speck.BLIZZARD : Speck.DUST;
	}

	@Override
	protected String blowSound( int kind ) {
		return kind == BREATH ? Assets.Sounds.GAS : Assets.Sounds.BLAST;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		if (kind == BREATH) Buff.prolong(ch, Chill.class, 5f);
	}

	//after the dive it lands as near as there's room, and whatever is under it gets shoved off
	@Override
	protected void afterBlast( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind != DIVE) return;
		aloft = false;
		boolean[] ground = fits();
		int spot = ground[pos] ? pos : nearestFit(ground, pos);
		if (spot == -1 || Dungeon.level.distance(spot, pos) > 6){
			aloft = true;
			grounded = ON_GROUND;
			return;
		}
		if (spot != pos){
			int old = pos;
			move(spot);
			moveSprite(old, spot);
		}
		grounded = ON_GROUND;
		trample(pos);
	}

	@Override
	protected void onTrample( Char ch ) {
		ambush(ch, 20, 35);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("aloft", aloft);
		bundle.put("grounded", grounded);
		bundle.put("circle", circle);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		aloft = !bundle.contains("aloft") || bundle.getBoolean("aloft");
		grounded = bundle.getInt("grounded");
		circle = bundle.getInt("circle");
	}

	//0-1 hovering, 2-5 flying, 6 rears and roars, 7 the dive, 8 frost breath, 9 down, 10 dead,
	//11 landing after the dive, 12 the breath's mist fading.
	//up in the air the drawing goes up and its shadow stays on the ground, on the real cells
	public static class WyvernSprite extends WildSprite {

		private static final float HEIGHT = 56;

		private float lift = 0;
		private Image shadow;

		public WyvernSprite(){
			super();
			beast(FrostAssets.WYVERN, 10, 272, 264, 48, 48);
			idle = loop(6, 0, 1);
			run = loop(10, 2, 3, 4, 5);
			die = clip(4, 9, 10, 10);
			play(idle);
		}

		@Override
		protected int blow( int kind ) {
			return kind == BREATH ? 8 : 7;
		}

		@Override
		public void strike( int kind ) {
			once(kind == BREATH ? clip(10, 8, 8, 8, 12, 0) : clip(10, 7, 7, 11, 11, 0));
		}

		@Override
		protected int[] downFrames() {
			return new int[]{9, 9, 9, 0};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(216, 100);
		}

		@Override
		public PointF worldToCamera( int cell ) {
			PointF p = super.worldToCamera(cell);
			p.y -= lift;
			return p;
		}

		@Override
		public void update() {
			super.update();
			boolean up = ch instanceof CorruptWyvern && ch.isAlive() && ((CorruptWyvern) ch).aloft();
			float want = up ? HEIGHT : 0;
			if (lift != want){
				float step = Game.elapsed * 90f;
				float next = lift < want ? Math.min(want, lift + step) : Math.max(want, lift - step);
				y -= next - lift;
				lift = next;
			}
			if (shadow == null && parent != null){
				shadow = new Image(FrostAssets.SHADOW);
				parent.addToBack(shadow);
			}
			if (shadow != null){
				shadow.frame(frame());
				shadow.x = x;
				shadow.y = y + lift;
				shadow.flipHorizontal = flipHorizontal;
				shadow.visible = visible && lift > 4;
			}
		}

		@Override
		public void kill() {
			super.kill();
			if (shadow != null) shadow.killAndErase();
		}
	}
}
