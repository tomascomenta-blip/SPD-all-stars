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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the last one standing, guardian of the graveyard: the skeleton of the oldest colossus. it lies as a
//heap of bones with the skull on top until someone comes close, then it gets up (two turns, nothing
//hurts it meanwhile). it stamps a marked patch in front of it, sweeps everything around it, and
//calls the small bones of the graveyard up out of the dust. its soul glows inside the ribs
public class LastStanding extends WildColossus {

	{
		size = 15;
		spriteClass = LastSprite.class;

		HP = HT = 9500;
		defenseSkill = 6;
		EXP = 520;
		baseSpeed = 0.7f;

		minDmg = 58; maxDmg = 78; acc = 72;
		armorMin = 22; armorMax = 38;

		territory = 16;

		properties.add(Property.MINIBOSS);
		properties.add(Property.UNDEAD);
	}

	private static final int STAMP = 0, SWEEP = 1;

	public boolean heap = true;
	private int rising = 0, callT = 0;

	//the skull at the front, and the soul in the ribs (climb it)
	@Override
	public int[][] weakSpots() {
		return new int[][]{{14, 3}, {14, 4}, {8, 4}};
	}

	//the spine
	@Override
	public int[][] climbable() {
		return new int[][]{{2, 1, 12}, {3, 1, 12}};
	}

	@Override
	public boolean peaceful() {
		return heap;
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return rising > 0 || super.isInvulnerable(effect);
	}

	@Override
	protected int ears() {
		return 4;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean act() {
		if (rising > 0){
			if (--rising == 0) state = HUNTING;
			spend(TICK);
			return true;
		}
		if (heap){
			for (Hero h : Party.living()){
				if (reach(h.pos) <= 4){
					rise(h);
					return true;
				}
			}
			spend(TICK);
			return true;
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (heap && isAlive() && src instanceof Char){
			rise((Char) src);
			return;
		}
		super.damage(dmg, src);
	}

	private void rise( Char by ){
		heap = false;
		rising = 2;
		aggro(by);
		CoopLog.line("fauna: LastStanding rises t=" + (int) Actor.now());
		Senses.noise(pos, Senses.STAMPEDE);
		if (sprite instanceof LastSprite) ((LastSprite) sprite).rise();
		Sample.INSTANCE.play(Assets.Sounds.BONES, 1f, 0.5f);
		if (seen()) GLog.n(Messages.get(this, "rises"));
		spend(TICK);
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (++callT >= 5 && d <= 10){
			callT = 0;
			if (call()) return true;
		}
		if (d <= 2 && Random.Int(2) == 0){
			warn(SWEEP, ring(2), enemy.pos, 3f);
			return true;
		}
		if (d <= 5){
			face(enemy.pos);
			ArrayList<Integer> f = front(5, 1);
			if (f.contains(enemy.pos)){
				warn(STAMP, f, enemy.pos, 3f);
				return true;
			}
			warn(SWEEP, ring(2), enemy.pos, 3f);
			return true;
		}
		return false;
	}

	//small bones of the graveyard climb out of the dust round it
	private boolean call(){
		ArrayList<Integer> cells = ring(3);
		int n = 0;
		for (int i = 0; i < 3 && !cells.isEmpty(); i++){
			int c = cells.remove(Random.Int(cells.size()));
			if (Actor.findChar(c) != null) continue;
			SKMob m = Random.Int(2) == 0 ? new BoneBeetle() : new OssuaryHyena();
			m.pos = c;
			m.summoned();
			m.state = m.HUNTING;
			GameScene.add(m);
			n++;
		}
		if (n == 0) return false;
		if (sprite instanceof LastSprite) ((LastSprite) sprite).pose(4, 6, 6, 0);
		if (seen()) GLog.w(Messages.get(this, "calls"));
		spend(TICK);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == STAMP ? new int[]{135, 175} : new int[]{90, 120};
	}

	@Override
	protected int dust( int kind ) {
		return kind == STAMP ? Speck.DUST : Speck.BONE;
	}

	@Override
	protected String blowSound( int kind ) {
		return kind == STAMP ? Assets.Sounds.ROCKS : Assets.Sounds.BONES;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		if (kind == SWEEP) knock(ch, 3);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("heap", heap);
		bundle.put("rising", rising);
		bundle.put("call_t", callT);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		heap = !bundle.contains("heap") || bundle.getBoolean("heap");
		rising = bundle.getInt("rising");
		callT = bundle.getInt("call_t");
	}

	//0-1 idle, 2-5 walk, 6 skull up and jaws open, 7 the stamp, 8 a heap of bones, 9 half up,
	//10 fallen into the heap, 11 dead
	public static class LastSprite extends WildSprite {

		private Animation standing;

		public LastSprite(){
			super();
			beast(BonesAssets.LAST, 15, 328, 368, 40, 112);
			standing = idle;
			idle = loop(1, 8);
			die = clip(4, 10, 11, 11);
			play(idle);
		}

		public void rise(){
			idle = standing;
			once(clip(2, 8, 9, 9, 0));
		}

		@Override
		protected int ready( int kind ) {
			return 6;
		}

		@Override
		protected int blow( int kind ) {
			return 7;
		}

		@Override
		protected int[] downFrames() {
			return new int[]{10, 10, 10, 9};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(176, 190);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof LastStanding && !((LastStanding) ch).heap){
				idle = standing;
				idle();
			}
		}
	}
}
