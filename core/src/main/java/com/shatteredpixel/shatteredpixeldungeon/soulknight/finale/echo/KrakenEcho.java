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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.echo;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken.KrakenAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaFeatures;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

/*
 echo of the kraken. just the head, peeking out of a little whirlpool. it doesn't walk: every so often it
 sinks and comes up somewhere else near the heroes.
 the eye is shut most of the time and hits barely get through (half). every few turns it opens: hits do 1.5x,
 and it glares at someone. otherwise: a huge tentacle comes out of a puddle and slams a marked row, or an ink jet
*/
public class KrakenEcho extends EchoBoss {

	{
		spriteClass = Sprite.class;
		HP = HT = 1800;
		defenseSkill = 18;
		properties.add(Property.IMMOVABLE);
	}

	private static final int SLAM = 0, INK = 1, GLARE = 2;

	public static final int EYE_OPEN = 4;

	public int eyeOpen = 0;
	private int eyeTimer = 6;
	public int diving = 0;
	private int diveTimer = 11;
	private int slamCount = 0;
	private int closedHits = 0;

	//the tentacle being called: its puddle, direction and length. for the drawing too
	public int slamFrom = -1, slamDir = 0, slamLen = 0;

	@Override
	public int world() {
		return SKWorlds.UNDERSEA;
	}

	@Override public int damageRoll() { return Random.NormalIntRange(30, 46); }
	@Override public int attackSkill( Char target ) { return 46; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(2, 10); }

	@Override
	protected int specialCooldown() {
		return eyeOpen > 0 ? 1 : 3;
	}

	@Override
	protected boolean getCloser( int target ) {
		return false;
	}

	@Override
	protected boolean getFurther( int target ) {
		return false;
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return diving > 0 || super.isInvulnerable(effect);
	}

	@Override
	protected boolean act() {
		if (emerging() || !isAlive()) return super.act();

		if (diving > 0){
			resurface();
			spend(TICK);
			return true;
		}

		if (eyeOpen > 0){
			if (--eyeOpen == 0){
				eyeTimer = 7;
				if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "shut"));
			}
		} else if (--eyeTimer <= 0 && pendingKind == -1){
			eyeOpen = EYE_OPEN;
			cooldown = 1;
			GLog.w(Messages.get(this, "eye"));
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.7f, 1.5f);
		}

		if (--diveTimer <= 0 && eyeOpen == 0 && pendingKind == -1){
			diveTimer = 11;
			if (dive()){
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!isAlive() || emerging() || diving > 0) return;
		if (dmg > 0){
			if (eyeOpen > 0){
				dmg = Math.round(dmg * 1.5f);
			} else {
				dmg = Math.max(1, dmg / 2);
				if (sprite != null && Dungeon.level.heroFOV[pos] && closedHits++ % 4 == 0){
					sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "closed"));
				}
			}
		}
		super.damage(dmg, src);
	}

	// *** moving around: sink, come up 3 cells from someone ***

	private int diveTo = -1;

	private boolean dive(){
		ArrayList<Hero> living = Party.living();
		if (living.isEmpty()) return false;
		Hero h = living.get(Random.Int(living.size()));
		ArrayList<Integer> spots = new ArrayList<>();
		for (int c : freeNear(h.pos, 2, 3)){
			boolean roomy = true;
			for (int n : PathFinder.NEIGHBOURS8){
				if (Dungeon.level.solid[c + n]) roomy = false;
			}
			if (roomy && Dungeon.level.distance(c, pos) >= 3) spots.add(c);
		}
		if (spots.isEmpty()) return false;
		diveTo = spots.get(Random.Int(spots.size()));
		diving = 1;
		pendingKind = -1;
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.6f);
			CellEmitter.get(pos).burst(Speck.factory(Speck.BUBBLE), 8);
			GLog.w(Messages.get(this, "dive"));
		}
		return true;
	}

	private void resurface(){
		diving = 0;
		if (diveTo != -1 && Actor.findChar(diveTo) == null && Dungeon.level.passable[diveTo]){
			move(diveTo, false);
			if (sprite != null){
				sprite.interruptMotion();
				sprite.place(pos);
			}
		}
		diveTo = -1;
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.8f);
			CellEmitter.get(pos).burst(Speck.factory(Speck.BUBBLE), 10);
		}
		EchoFx.sparks(pos, 8);
		cooldown = Math.min(cooldown, 1);
	}

	// *** attacks ***

	@Override
	protected boolean special( Char enemy ) {
		if (!fieldOfView[enemy.pos] || diving > 0) return false;
		boolean visible = Dungeon.level.heroFOV[pos];
		if (sprite != null) sprite.turnTo(pos, enemy.pos);

		if (eyeOpen > 0){
			ArrayList<Integer> cells = line(enemy.pos, 9);
			if (cells.isEmpty()) return false;
			announce(GLARE, cells, enemy.pos);
			if (visible) GLog.w(Messages.get(this, "glare"));
			return true;
		}

		//two slams for every ink jet
		if (slamCount++ % 3 != 2 && planSlam(enemy)){
			ArrayList<Integer> cells = slamCells();
			announce(SLAM, cells, enemy.pos);
			if (sprite instanceof Sprite) ((Sprite) sprite).rage();
			EchoFx.later(new Runnable() { public void run() { GameScene.effect(new Riser(KrakenEcho.this, slamFrom)); } });
			if (visible) GLog.w(Messages.get(this, "slam"));
			return true;
		}
		ArrayList<Integer> cells = line(enemy.pos, 8);
		if (cells.isEmpty()) return false;
		announce(INK, cells, enemy.pos);
		if (visible) GLog.w(Messages.get(this, "ink"));
		return true;
	}

	private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	//a straight line through the hero, starting from a puddle 2-3 cells before them
	private boolean planSlam( Char enemy ){
		int w = Dungeon.level.width();
		int first = Random.Int(4);
		for (int k = 0; k < 4; k++){
			int[] d = DIRS[(first + k) % 4];
			for (int back = 3; back >= 2; back--){
				int fx = enemy.pos % w - d[0] * back, fy = enemy.pos / w - d[1] * back;
				if (fx < 1 || fy < 1 || fx >= w - 1 || fy >= Dungeon.level.height() - 1) continue;
				int from = fx + fy * w;
				if (Dungeon.level.solid[from]) continue;
				int len = 0;
				for (int i = 1; i <= 8; i++){
					int c = from + (d[0] + d[1] * w) * i;
					if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) break;
					len = i;
				}
				if (len >= back + 1){
					slamFrom = from;
					slamDir = (first + k) % 4;
					slamLen = len;
					return true;
				}
			}
		}
		return false;
	}

	private ArrayList<Integer> slamCells(){
		ArrayList<Integer> cells = new ArrayList<>();
		if (slamFrom == -1) return cells;
		int step = DIRS[slamDir][0] + DIRS[slamDir][1] * Dungeon.level.width();
		for (int i = 0; i <= slamLen; i++) cells.add(slamFrom + step * i);
		return cells;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		switch (kind){
			case SLAM: {
				final int from = slamFrom, dir = slamDir, len = slamLen;
				slamFrom = -1;
				EchoFx.later(new Runnable() { public void run() { GameScene.effect(new Slam(from, dir, len)); } });
				for (int c : cells){
					if (Dungeon.level.heroFOV[c]) CellEmitter.center(c).burst(Speck.factory(Speck.STEAM), 2);
				}
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.6f);
				EchoFx.shake(5, 0.6f);
				for (Char ch : SKCombat.foesIn(this, cells)){
					ch.damage(special(ch, 46, 62), this);
					if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 3f);
				}
				break;
			}
			case INK: {
				jet(cells, 0x301A40);
				for (int i = 0; i < cells.size(); i += 2){
					GameScene.add(Blob.seed(cells.get(i), 25, UnderseaFeatures.InkCloud.class));
				}
				if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 0.5f);
				for (Char ch : SKCombat.foesIn(this, cells)){
					ch.damage(special(ch, 30, 44), this);
					if (ch.isAlive()) Buff.prolong(ch, Blindness.class, 2f);
				}
				break;
			}
			case GLARE: {
				jet(cells, EchoFx.GOLD);
				if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.RAY, 1f, 0.7f);
				hitFoes(cells, 52, 70);
				break;
			}
		}
	}

	private void jet( ArrayList<Integer> cells, final int color ){
		if (cells.isEmpty()) return;
		final int end = cells.get(cells.size() - 1);
		EchoFx.later(new Runnable() { public void run() {
			if (sprite == null || sprite.parent == null) return;
			Beam b = new Beam.DeathRay(sprite.center(), DungeonTilemap.raisedTileCenterToWorld(end));
			b.hardlight(color);
			sprite.parent.add(b);
		} });
	}

	@Override
	public void die( Object cause ) {
		slamFrom = -1;
		super.die(cause);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("eye_open", eyeOpen);
		bundle.put("eye_timer", eyeTimer);
		bundle.put("diving", diving);
		bundle.put("dive_timer", diveTimer);
		bundle.put("dive_to", diveTo);
		bundle.put("slams", slamCount);
		bundle.put("closed_hits", closedHits);
		bundle.put("slam_from", slamFrom);
		bundle.put("slam_dir", slamDir);
		bundle.put("slam_len", slamLen);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		eyeOpen = bundle.getInt("eye_open");
		eyeTimer = bundle.getInt("eye_timer");
		diving = bundle.getInt("diving");
		diveTimer = bundle.getInt("dive_timer");
		diveTo = bundle.getInt("dive_to");
		slamCount = bundle.getInt("slams");
		closedHits = bundle.getInt("closed_hits");
		slamFrom = bundle.getInt("slam_from");
		slamDir = bundle.getInt("slam_dir");
		slamLen = bundle.getInt("slam_len");
	}

	// *** drawing ***

	//tentacle coming up out of its puddle while the row is marked
	public static class Riser extends Image {

		private final KrakenEcho echo;
		private final int cell;
		private final Image pool;
		private float t = 0, gone = -1;

		public Riser( KrakenEcho echo, int cell ){
			super(KrakenAssets.GUARD);
			this.echo = echo;
			this.cell = cell;
			frame(10 * 24, 0, 24, 40);
			scale.set(1.3f);
			pool = new Image(KrakenAssets.WHIRLPOOL);
			pool.frame(0, 0, 352, 208);
			pool.scale.set(0.08f);
			place();
		}

		private void place(){
			PointF c = DungeonTilemap.tileCenterToWorld(cell);
			x = c.x - 12 * scale.x;
			y = c.y + 6 - 40 * scale.y;
			pool.x = c.x - 352 * pool.scale.x / 2f;
			pool.y = c.y + 2 - 208 * pool.scale.y / 2f;
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			pool.update();
			boolean waiting = echo.isAlive() && echo.slamFrom == cell;
			if (!waiting && gone < 0) gone = 0;
			int f;
			if (gone >= 0){
				gone += Game.elapsed;
				f = gone < 0.1f ? 8 : 9;
				if (gone > 0.3f){
					killAndErase();
					return;
				}
			} else if (t < 0.3f){
				int[] up = {10, 9, 8, 3};
				f = up[Math.min(3, (int) (t / 0.075f))];
			} else {
				f = ((int) (t * 3)) % 2;
			}
			frame(f * 24, 0, 24, 40);
			int pf = ((int) (t * 8)) % 6;
			pool.frame(pf * 352, 0, 352, 208);
			hardlight(0.55f, 0.95f, 1.1f);
			alpha(0.8f);
			pool.alpha(0.7f);
			place();
		}

		@Override
		public void draw() {
			pool.draw();
			super.draw();
		}
	}

	//the tentacle lying across the row for a moment
	public static class Slam extends Image {

		private float t = 0;
		private final int from, dir, len;

		public Slam( int from, int dir, int len ){
			super(KrakenAssets.lie(1));
			this.from = from;
			this.dir = dir;
			this.len = len;
			frame(0, 0, 208, 64);
			origin.set(0, 32);
			float s = (len * DungeonTilemap.SIZE + 10) / 208f;
			scale.set(s, Math.max(0.5f, s));
			PointF c = DungeonTilemap.tileCenterToWorld(from);
			x = c.x - 4;
			y = c.y - 32;
			angle = dir == 0 ? 0 : dir == 1 ? 180 : dir == 2 ? 90 : -90;
			hardlight(0.6f, 0.95f, 1.1f);
			alpha(0.85f);
			int step = DIRS[dir][0] + DIRS[dir][1] * Dungeon.level.width();
			for (int i = 0; i <= len; i += 2){
				int cell = from + step * i;
				if (EchoFx.seen(cell)) CellEmitter.center(cell).burst(Speck.factory(Speck.BUBBLE), 3);
			}
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			int f = t < 0.45f ? ((int) (t * 10)) % 3 : (t < 0.6f ? 3 : 4);
			frame(f * 208, 0, 208, 64);
			am = t < 0.45f ? 0.85f : Math.max(0f, 0.85f - (t - 0.45f) * 1.6f);
			if (t > 1f) killAndErase();
		}
	}

	/* kraken_head.png, 256x176 a frame: 0 eye shut, 1 half open, 2 open, 3 red, 4 open + tentacles up.
	   drawn small over a little whirlpool */
	public static class Sprite extends EchoSprite {

		private Animation shut, opening, open, rage;
		private Image pool;
		private float sink = 0;
		private boolean wasOpen = false;

		public Sprite(){
			super();
			texture(KrakenAssets.HEAD);
			TextureFilm film = new TextureFilm(texture, 256, 176);
			shut = anim(film, 1, true, 0);
			opening = anim(film, 6, false, 0, 1, 2);
			open = anim(film, 2, true, 2, 2, 2, 4);
			rage = anim(film, 6, false, 4, 3, 4, 3);
			idle = shut;
			run = shut;
			attack = anim(film, 10, false, 3, 4, 3);
			zap = rage;
			die = anim(film, 4, false, 1, 0);
			echo(0.15f);
			perspectiveRaise = -2 / 16f;
			play(idle);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (pool == null && parent != null){
				pool = new Image(KrakenAssets.WHIRLPOOL);
				pool.frame(0, 0, 352, 208);
				pool.scale.set(0.11f);
				parent.addToBack(pool);
			}
		}

		public void rage(){
			play(rage);
		}

		@Override
		public void update() {
			KrakenEcho k = ch instanceof KrakenEcho ? (KrakenEcho) ch : null;
			if (k != null && k.isAlive()){
				boolean isOpen = k.eyeOpen > 0;
				if (isOpen != wasOpen){
					wasOpen = isOpen;
					idle = isOpen ? open : shut;
					play(isOpen ? opening : idle);
				}
			}
			super.update();
			//sinking and coming back up
			float want = k != null && k.diving > 0 ? 1f : 0f;
			if (sink != want){
				sink = want > sink ? Math.min(want, sink + Game.elapsed * 3) : Math.max(want, sink - Game.elapsed * 2);
			}
			am *= 1f - 0.85f * sink;
			if (pool != null){
				pool.visible = visible;
				pool.frame(((int) (t * 8) % 6) * 352, 0, 352, 208);
				PointF c = center();
				pool.x = c.x - 352 * pool.scale.x / 2f;
				pool.y = y + height() - 208 * pool.scale.y / 2f - 2;
				pool.hardlight(0.45f, 0.75f, 0.95f);
				pool.alpha(0.55f * Math.min(1f, am / Math.max(0.01f, baseAlpha) + 0.4f));
			}
		}

		@Override
		public void onComplete( Animation anim ) {
			super.onComplete(anim);
			if (anim == opening && ch != null && ch.isAlive()) play(idle);
		}

		@Override
		public void kill() {
			super.kill();
			if (pool != null){
				pool.killAndErase();
				pool = null;
			}
		}

		@Override
		public int blood() {
			return 0xFF7FE8FF;
		}
	}
}
