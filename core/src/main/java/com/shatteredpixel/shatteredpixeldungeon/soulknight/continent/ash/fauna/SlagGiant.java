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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//slag giant. it sleeps half buried in the ash, a hump with a spine. noise, a light close by, a hit
//or someone treading on it wake it up: it takes two turns to get on its feet, roaring, and then it
//tears basalt columns out of the ground and throws them (where it lands is marked two turns ahead).
//left alone long enough it lies down and buries itself again
public class SlagGiant extends AshColossus {

	{
		size = 8;
		spriteClass = GiantSprite.class;

		HP = HT = 2800;
		defenseSkill = 6;
		EXP = 120;
		baseSpeed = 0.75f;

		minDmg = 55; maxDmg = 75; acc = 64;
		armorMin = 18; armorMax = 34;

		properties.add(Property.INORGANIC);
		properties.add(Property.FIERY);
		properties.add(Property.MINIBOSS);

		SLEEPING = new Buried();
		state = SLEEPING;
	}

	private static final int THROW_MIN = 3, THROW_MAX = 14;

	private int rising = 0;
	//turns up and about with nobody to chase
	private int bored = 0;

	public boolean asleep(){
		return state == SLEEPING;
	}

	@Override
	protected int ears() {
		return 2;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	//the chest core, standing
	@Override
	public int[][] weakSpots() {
		return new int[][]{{4, 2}};
	}

	@Override
	protected boolean act() {
		if (paralysed <= 0 && rising > 0){
			if (--rising == 0){
				state = HUNTING;
				if (sprite instanceof GiantSprite) ((GiantSprite) sprite).stand();
			}
			spend(TICK);
			return true;
		}
		if (state == SLEEPING && paralysed <= 0 && !farFromParty()){
			for (Hero h : Party.living()){
				if (Senses.lit(h) && reach(h.pos) <= 5 || reach(h.pos) <= 1 && Random.Int(3) == 0){
					wake(h);
					spend(TICK);
					return true;
				}
			}
		}
		if (state == WANDERING && enemy == null){
			if (++bored >= 25) lieDown();
		} else {
			bored = 0;
		}
		return super.act();
	}

	@Override
	protected void heard( Senses.Noise n ) {
		if (state == SLEEPING) wake(null);
		else super.heard(n);
	}

	@Override
	public void damage( int dmg, Object src ) {
		boolean was = state == SLEEPING;
		super.damage(dmg, src);
		if (was && isAlive()) wake(src instanceof Char ? (Char) src : null);
	}

	private void wake( Char by ){
		if (rising > 0) return;
		rising = 2;
		state = WANDERING;
		if (by != null && SKCombat.foes(this, by)){
			enemy = by;
			target = by.pos;
		}
		Senses.noise(pos, Senses.ROAR);
		if (sprite instanceof GiantSprite) ((GiantSprite) sprite).wake();
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
			GLog.w(Messages.get(this, "wakes"));
			for (int c : cells()) if (c % 6 == 0) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 3);
		}
	}

	private void lieDown(){
		bored = 0;
		state = SLEEPING;
		if (sprite instanceof GiantSprite) ((GiantSprite) sprite).sleep();
	}

	//buried deep: hard to wake by just walking by (distance from the edge of the body)
	private class Buried extends Sleeping {
		@Override
		protected float detectionChance( Char enemy ) {
			return 1 / (reach(enemy.pos) * 2f + enemy.stealth());
		}

		@Override
		protected void awaken( boolean enemyInFOV ) {
			wake(enemyInFOV ? enemy : null);
			spend(TIME_TO_WAKE_UP);
		}
	}

	// *** the columns ***

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d < THROW_MIN || d > THROW_MAX) return false;
		warn(0, square(enemy.pos, 1), enemy.pos, 2f);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim, Senses.CRASH);
		if (sprite != null && sprite.parent != null) throwColumn(aim);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.7f);
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 110, 150);
			if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 3f);
		}
	}

	//only the picture: the column flies from its hands to the marked spot
	private void throwColumn( final int to ){
		final PointF from = new PointF(sprite.x + (right ? 160 : sprite.width() - 160), sprite.y + 76);
		final PointF end = DungeonTilemap.tileCenterToWorld(to);
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (sprite == null || sprite.parent == null) return;
				sprite.parent.add(new Column(from, end, to));
			}
		});
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("rising", rising);
		bundle.put("bored", bored);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		rising = bundle.getInt("rising");
		bored = bundle.getInt("bored");
	}

	public static class Column extends Image {

		private static final float FLIGHT = 0.45f;

		private final PointF from, to;
		private final int cell;
		private final TextureFilm film;
		private float time = 0;

		public Column( PointF from, PointF to, int cell ){
			super(FaunaAssets.COLUMN);
			film = new TextureFilm(texture, 48, 48);
			frame(film.get(0));
			this.from = from;
			this.to = to;
			this.cell = cell;
			origin.set(24, 24);
			place(0);
		}

		private void place( float k ){
			x = from.x + (to.x - from.x) * k - 24;
			//a short arc
			y = from.y + (to.y - from.y) * k - 24 - (float) Math.sin(k * Math.PI) * 40;
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			float k = Math.min(1f, time / FLIGHT);
			place(k);
			frame(film.get((int) (time / 0.08f) % 4));
			if (k >= 1f){
				if (Dungeon.level != null && Dungeon.level.heroFOV[cell]){
					CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 8);
				}
				killAndErase();
			}
		}
	}

	//0-1 asleep, 2-3 getting up, 4-5 standing, 6-9 walking, 10-12 tearing out a column and
	//throwing it, 13-15 falling
	public static class GiantSprite extends AshColossusSprite {

		@Override
		protected int[] downFrames() {
			return new int[]{17, 17, 18, 17};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(94, 60);
		}

		private Animation asleep, awake;

		public GiantSprite(){
			super();
			setup(FaunaAssets.GIANT, 8, 176, 208, 24, 72);
			asleep = loop(1, 0, 0, 1, 1);
			awake = loop(2, 4, 4, 5, 5);
			idle = asleep;
			run = loop(8, 6, 7, 8, 9);
			//21 raises the fist, 22 brings it down (12 is the column throw)
			attack = clip(10, 21, 22, 22, 4);
			die = clip(6, 13, 14, 15);
			play(idle);
		}

		public void wake(){
			idle = awake;
			once(clip(3, 2, 2, 3, 3, 3));
		}

		public void stand(){
			idle = awake;
			idle();
		}

		public void sleep(){
			idle = asleep;
			once(clip(3, 3, 3, 2, 2));
		}

		@Override
		public void windup( int kind ) {
			play(clip(2, 10, 11));
		}

		@Override
		public void strike( int kind ) {
			once(clip(6, 12, 12, 4));
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof SlagGiant && !((SlagGiant) ch).asleep()){
				idle = awake;
				idle();
			}
		}
	}
}
