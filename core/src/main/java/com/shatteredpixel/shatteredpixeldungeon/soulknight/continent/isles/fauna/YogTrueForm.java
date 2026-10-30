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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Yog;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.Awakening;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//yog, its true form: the last thing on the isles. a mountain of yog flesh in three moods.
//shut, it's plated in bone and only slams the ground and calls its brood. at two thirds the plates
//crack and the great eye opens: it looks at you (a marked beam) and the eye can be hit. at one
//third it splits under the eye and blooms: light comes down on everyone at once and it never rests.
//when it dies with seven drops of essence in the party, the true ending can begin
public class YogTrueForm extends WildColossus implements Yog {

	{
		size = 15;
		spriteClass = YogTrueSprite.class;

		HP = HT = 14000;
		defenseSkill = 12;
		EXP = 800;
		baseSpeed = 0.6f;

		minDmg = 55; maxDmg = 75; acc = 80;
		armorMin = 20; armorMax = 40;

		territory = 12;

		properties.add(Property.BOSS);
		properties.add(Property.DEMONIC);
	}

	private static final int SLAM = 0, GAZE = 1, BLOOM = 2, LASH = 3;

	public int phase = 1;
	//turns left of a change of mood, untouchable while it shifts
	private int shifting = 0;
	private int broodT = 0;

	@Override
	public int taintOnHit() {
		return 3 + phase * 2;
	}

	@Override
	public int[][] weakSpots() {
		if (phase == 1) return new int[0][];
		if (phase == 2) return new int[][]{{6, 3}, {7, 3}};
		return new int[][]{{6, 3}, {7, 3}, {6, 14}, {7, 14}, {8, 14}};
	}

	@Override
	public int[][] climbable() {
		return new int[][]{{5, 3, 11}, {6, 3, 11}, {7, 3, 11}};
	}

	@Override
	protected int ears() {
		return 6;
	}

	@Override
	protected int eyes() {
		return phase == 1 ? 2 : 8;
	}

	@Override
	protected int specialCooldown() {
		return phase == 3 ? 2 : 3;
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return shifting > 0 || super.isInvulnerable(effect);
	}

	//shut, the bone plates soak most of it
	@Override
	public void damage( int dmg, Object src ) {
		if (phase == 1 && dmg > 0) dmg = dmg * 2 / 3;
		super.damage(dmg, src);
	}

	@Override
	protected boolean act() {
		if (shifting > 0){
			shifting--;
			spend(TICK);
			return true;
		}
		if (isAlive() && phase == 1 && HP < HT * 2 / 3) return shift(2);
		if (isAlive() && phase == 2 && HP < HT / 3) return shift(3);
		return super.act();
	}

	private boolean shift( int to ){
		phase = to;
		shifting = 2;
		pendingKind = -1;
		pendingCells = new int[0];
		down = 0;
		stagger = 0;
		CoopLog.line("yog: true form phase " + to + " t=" + (int) Actor.now());
		Senses.noise(pos, Senses.STAMPEDE);
		if (sprite instanceof YogTrueSprite) ((YogTrueSprite) sprite).phase(to, true);
		Sample.INSTANCE.play(Assets.Sounds.BOSS);
		GLog.n(Messages.get(this, "phase" + to));
		spend(TICK);
		return true;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (++broodT >= 4 && d <= 12){
			broodT = 0;
			if (brood()) return true;
		}
		if (d <= 2 && Random.Int(2) == 0){
			warn(SLAM, ring(3), enemy.pos, 3f);
			return true;
		}
		if (phase == 3 && Random.Int(3) > 0){
			//light comes down on every hero in sight at once
			ArrayList<Integer> cells = new ArrayList<>();
			for (Hero h : Party.living()){
				if (reach(h.pos) > 16 || !fieldOfView[h.pos]) continue;
				for (int c : square(h.pos, 1)) if (!cells.contains(c)) cells.add(c);
			}
			if (!cells.isEmpty()){
				warn(BLOOM, cells, enemy.pos, 3f);
				return true;
			}
		}
		if (phase >= 2 && d <= 16){
			warn(GAZE, lane(enemy.pos, 16, 3), enemy.pos, 3f);
			return true;
		}
		if (d <= 6){
			face(enemy.pos);
			ArrayList<Integer> f = front(5, 2);
			if (f.contains(enemy.pos)){
				warn(LASH, f, enemy.pos, 3f);
				return true;
			}
			warn(SLAM, ring(3), enemy.pos, 3f);
			return true;
		}
		return false;
	}

	//two of the small brood out of its flesh, next to it
	private boolean brood(){
		ArrayList<Integer> cells = ring(2);
		int n = 0;
		for (int i = 0; i < 2 + phase && !cells.isEmpty(); i++){
			int c = cells.remove(Random.Int(cells.size()));
			if (Actor.findChar(c) != null || !Dungeon.level.passable[c]) continue;
			Mob m = WildFauna.yog();
			m.pos = c;
			if (m instanceof SKMob) ((SKMob) m).summoned();
			m.state = m.HUNTING;
			GameScene.add(m);
			n++;
		}
		if (n == 0) return false;
		if (seen()) GLog.w(Messages.get(this, "brood"));
		if (sprite instanceof YogTrueSprite) ((YogTrueSprite) sprite).rear();
		spend(TICK);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		switch (kind){
			case SLAM: return new int[]{100 + 20 * phase, 140 + 20 * phase};
			case GAZE: return new int[]{150, 200};
			case BLOOM: return new int[]{120, 170};
			default: return new int[]{110, 150};
		}
	}

	@Override
	protected int dust( int kind ) {
		return kind == GAZE || kind == BLOOM ? Speck.LIGHT : Speck.DUST;
	}

	@Override
	protected String blowSound( int kind ) {
		return kind == GAZE || kind == BLOOM ? Assets.Sounds.RAY : Assets.Sounds.BLAST;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		if (kind == SLAM) knock(ch, 2);
		if (kind == GAZE) Buff.prolong(ch, Blindness.class, 2f);
		if (kind == GAZE || kind == BLOOM) Continent.taint(ch, 6 * phase);
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
		int e = Continent.essence;
		CoopLog.line("yog: true form dies, party carries " + e + "/" + Continent.NEEDED + " essence");
		if (e >= Continent.NEEDED){
			GLog.p(Messages.get(this, "true_end", e));
			onDefeated();
		} else {
			GLog.n(Messages.get(this, "not_enough", e, Continent.NEEDED));
		}
	}

	//the true ending starts here (seven drops or more carried when it falls): the continent wakes up
	public static void onDefeated(){
		Awakening.wake();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("phase", phase);
		bundle.put("shifting", shifting);
		bundle.put("brood_t", broodT);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		phase = Math.max(1, bundle.getInt("phase"));
		shifting = bundle.getInt("shifting");
		broodT = bundle.getInt("brood_t");
	}

	//0-1 shut idle, 2-3 crawl, 4 ready, 5 slam, 6 the eye opening; 7-8 awake idle, 9 ready, 10 the gaze,
	//11 splitting open; 12-13 in bloom, 14 its blow; 15-16 dying
	public static class YogTrueSprite extends WildSprite {

		private int phase = 1;

		public YogTrueSprite(){
			super();
			beast(IslesAssets.YOG, 15, 400, 408, 80, 104);
			die = clip(4, 15, 16, 16, 16);
			looks(false);
		}

		private void looks( boolean shifting ){
			if (phase == 1){
				idle = loop(2, 0, 0, 1, 1);
				run = loop(4, 2, 3);
			} else if (phase == 2){
				idle = loop(2, 7, 7, 8, 8);
				run = loop(3, 7, 8);
			} else {
				idle = loop(4, 12, 13);
				run = loop(4, 12, 13);
			}
			attack = phase == 1 ? clip(10, 4, 5, 5) : phase == 2 ? clip(10, 9, 10, 10) : clip(10, 13, 14, 14);
			zap = attack.clone();
			if (shifting) once(phase == 2 ? clip(3, 6, 6, 6, 7) : clip(3, 11, 11, 11, 12));
			else idle();
		}

		public void phase( int p, boolean shifting ){
			phase = p;
			looks(shifting);
		}

		public void rear(){
			once(clip(4, ready(0), ready(0), 0));
		}

		@Override
		protected int ready( int kind ) {
			return phase == 1 ? 4 : phase == 2 ? 9 : 13;
		}

		@Override
		protected int blow( int kind ) {
			return phase == 1 ? 5 : phase == 2 ? 10 : 14;
		}

		@Override
		public void strike( int kind ) {
			int rest = phase == 1 ? 0 : phase == 2 ? 7 : 12;
			once(clip(10, blow(kind), blow(kind), blow(kind), rest));
		}

		@Override
		protected int[] downFrames() {
			int rest = phase == 1 ? 0 : phase == 2 ? 7 : 12;
			return new int[]{15, 15, 15, rest};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(186, 166);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof YogTrueForm) phase(((YogTrueForm) ch).phase, false);
		}
	}

	//so the isles' guardian can be put down by the level itself (an arena), with nothing else
	public static AshColossus at( int cell ){
		YogTrueForm y = new YogTrueForm();
		y.pos = cell;
		y.home = cell;
		return y;
	}
}
