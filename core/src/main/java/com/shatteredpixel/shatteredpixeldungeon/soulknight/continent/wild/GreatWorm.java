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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.IslesAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the blind worm. it lives under the whole continent, a column of pale flesh with a lamprey mouth
//and no eyes at all: it only hears. a fight, a roar, anything loud and it comes through the rock
//to there; the ground cracks over a marked patch for three turns and it comes up through it, bites
//whatever is in it, stays out a few turns (the only time it can be hurt) and goes back down.
//only the patch it comes up through is its body (columns 5-9, rows 8-12 of the drawing)
public class GreatWorm extends WildColossus {

	{
		size = 15;
		spriteClass = WormSprite.class;

		HP = HT = 5200;
		defenseSkill = 0;
		EXP = 320;

		minDmg = 55; maxDmg = 75; acc = 70;
		armorMin = 12; armorMax = 26;

		properties.add(Property.MINIBOSS);
	}

	//how far it hears beyond the noise itself, how far it travels under to come up
	private static final int EARS = 10, LUNGE = 14;
	private static final int UP = 4;
	private static final int EMERGE = 0, BITE = 1;

	private boolean under = true, added = false;
	private int up = 0, rest = 0;
	private float heardAt = -1;

	public boolean submerged(){
		return under;
	}

	@Override
	public boolean solid() {
		return !under;
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return under || super.isInvulnerable(effect);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return !under && super.canAttack(enemy);
	}

	// *** its body: the patch it comes up through ***

	private int x0(){
		return pos % Dungeon.level.width() - 2;
	}

	private int y0(){
		return pos / Dungeon.level.width() + 1;
	}

	@Override
	public boolean covers( int cell ) {
		if (cell < 0 || pos < 0) return false;
		int w = Dungeon.level.width();
		int x = cell % w - x0(), y = cell / w - y0();
		return x >= 0 && y >= 0 && x < 5 && y < 5;
	}

	@Override
	public ArrayList<Integer> cells() {
		ArrayList<Integer> out = new ArrayList<>();
		int w = Dungeon.level.width();
		for (int y = 0; y < 5; y++){
			for (int x = 0; x < 5; x++){
				out.add(x0() + x + (y0() + y) * w);
			}
		}
		return out;
	}

	@Override
	public int reach( int cell ) {
		int w = Dungeon.level.width();
		int x = cell % w, y = cell / w;
		int dx = Math.max(0, Math.max(x0() - x, x - (x0() + 4)));
		int dy = Math.max(0, Math.max(y0() - y, y - (y0() + 4)));
		return Math.max(dx, dy);
	}

	//the free cell next to the patch closest to someone who wants to hit it
	@Override
	public int approachCell( int from ) {
		Level l = Dungeon.level;
		int w = l.width(), best = -1, bestD = Integer.MAX_VALUE;
		for (int y = y0() - 1; y <= y0() + 5; y++){
			for (int x = x0() - 1; x <= x0() + 5; x++){
				if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
				int c = x + y * w;
				if (covers(c) || !l.passable[c]) continue;
				int d = l.distance(from, c);
				if (d < bestD){
					bestD = d;
					best = c;
				}
			}
		}
		return best == -1 ? pos : best;
	}

	//it never walks: under the ground it burrows on its own, out of it it stays put
	@Override
	protected boolean getCloser( int target ) {
		return false;
	}

	@Override
	protected boolean getFurther( int target ) {
		return false;
	}

	//under the ground it goes anywhere; to come up the patch has to be open ground
	@Override
	public boolean[] fits() {
		Level l = Dungeon.level;
		int w = l.width(), h = l.height();
		boolean[] f = new boolean[l.length()];
		for (int i = 0; i < f.length; i++){
			int x = i % w, y = i / w;
			if (x - 7 < 0 || y - 7 < 0 || x + 7 >= w || y + 7 >= h) continue;
			f[i] = under || open(i);
		}
		return f;
	}

	private boolean open( int center ){
		Level l = Dungeon.level;
		int w = l.width();
		for (int y = 1; y <= 5; y++){
			for (int x = -2; x <= 2; x++){
				int c = center + x + y * w;
				if (c < 0 || c >= l.length() || !l.passable[c] || l.pit[c]) return false;
				Char ch = Actor.findChar(c);
				if (ch != null && ch != this && ch instanceof WildColossus) return false;
			}
		}
		return true;
	}

	@Override
	protected void onAdd() {
		super.onAdd();
		if (!added){
			added = true;
			if (under) invisible++;
		}
	}

	// *** what it does ***

	@Override
	protected boolean act() {
		if (pendingKind == EMERGE){
			int aim = pendingAim;
			ArrayList<Integer> zone = new ArrayList<>();
			for (int c : pendingCells) zone.add(c);
			pendingKind = -1;
			pendingCells = new int[0];
			spend(TICK);
			emerge(zone, aim);
			return true;
		}
		if (farFromParty()){
			spend(TICK * 3);
			return true;
		}
		if (!under){
			if (pendingKind == -1 && --up <= 0){
				sink();
				spend(TICK);
				return true;
			}
			if (paralysed <= 0) return super.act();
			spend(TICK);
			return true;
		}
		return burrow();
	}

	private boolean burrow(){
		if (heardAt < 0) heardAt = Actor.now();
		//testing: a racket near the hero now and then to call it
		int racket = DevFlags.blindWormNoise();
		if (racket > 0 && (int) Actor.now() % 10 == 0){
			Hero h = nearestHero();
			if (h != null) Senses.noise(Math.min(Dungeon.level.length() - 1, h.pos + racket), Senses.CRASH);
		}
		Senses.Noise n = Senses.heard(this, heardAt, EARS);
		heardAt = Actor.now();
		if (rest > 0){
			rest--;
		} else if (n != null){
			target = n.cell;
			int spot = comeUpAt(n.cell);
			if (spot != -1 && Dungeon.level.distance(spot, pos) <= LUNGE){
				dive(spot, n.cell);
				return true;
			}
		}
		if (target != -1 && target != pos){
			int step = PathFinder.getStep(pos, target, fits());
			if (step != -1){
				move(step);
				if (sprite != null) sprite.place(step);
				spend(1 / speed());
				return true;
			}
		}
		int c = fitNear(pos, 20);
		target = c == -1 ? pos : c;
		spend(TICK);
		return true;
	}

	//where its middle has to be so the noise is right in the patch it comes up through, or -1
	private int comeUpAt( int cell ){
		int w = Dungeon.level.width();
		int[][] order = {{0, 3}, {0, 2}, {0, 4}, {-1, 3}, {1, 3}, {-1, 2}, {1, 2}, {0, 1}, {0, 5}, {-2, 3}, {2, 3}};
		boolean[] f = fits();
		for (int[] o : order){
			int x = cell % w - o[0], y = cell / w - o[1];
			if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
			int c = x + y * w;
			if (f[c] && open(c)) return c;
		}
		return -1;
	}

	//right under the noise, and the ground starts to crack
	private void dive( int spot, int aim ){
		move(spot);
		if (sprite != null) sprite.place(spot);
		invisible = Math.max(0, invisible - 1);
		ArrayList<Integer> zone = new ArrayList<>();
		for (int c : cells()) if (!Dungeon.level.solid[c]) zone.add(c);
		CoopLog.line("fauna: GreatWorm comes up at " + spot + " t=" + (int) Actor.now());
		warn(EMERGE, zone, aim, 3f);
		Senses.noise(spot, Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.35f);
			GLog.w(Messages.get(this, "cracks"));
		}
	}

	private void emerge( ArrayList<Integer> zone, int aim ){
		if (!open(pos)){
			//something big got in the way: it stays down
			rest = 4;
			invisible++;
			if (sprite instanceof WormSprite) ((WormSprite) sprite).sink();
			return;
		}
		under = false;
		invisible = 0;
		CoopLog.line("fauna: GreatWorm out at " + pos + " t=" + (int) Actor.now());
		up = UP;
		rest = 10;
		if (sprite instanceof WormSprite) ((WormSprite) sprite).emerge();
		Senses.noise(pos, Senses.ROAR);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.4f);
			for (int c : zone) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
		}
		for (Char ch : SKCombat.foesIn(this, zone)){
			blast(ch, 210, 280);
		}
		trample(pos);
	}

	private void sink(){
		under = true;
		pendingKind = -1;
		pendingCells = new int[0];
		invisible++;
		down = 0;
		state = WANDERING;
		enemy = null;
		if (sprite instanceof WormSprite) ((WormSprite) sprite).sink();
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f, 0.5f);
	}

	@Override
	protected boolean special( Char enemy ) {
		//out of the ground it bites once more whoever stays in front of it
		if (under || reach(enemy.pos) > 2) return false;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : square(pos + 3 * Dungeon.level.width(), 4)) if (reach(c) <= 2) cells.add(c);
		warn(BITE, cells, enemy.pos, 2f);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return new int[]{120, 160};
	}

	@Override
	protected void onTrample( Char ch ) {
		if (!under) ambush(ch, 20, 40);
	}

	//the throat, at the top of the column
	@Override
	public int[][] weakSpots() {
		return new int[][]{{6, 8}, {7, 8}, {8, 8}};
	}

	@Override
	public int[][] climbable() {
		return new int[][]{{9, 6, 8}, {10, 6, 8}};
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("under", under);
		bundle.put("up", up);
		bundle.put("rest", rest);
		bundle.put("heard_at", heardAt);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		under = !bundle.contains("under") || bundle.getBoolean("under");
		up = bundle.getInt("up");
		rest = bundle.getInt("rest");
		heardAt = bundle.contains("heard_at") ? bundle.getFloat("heard_at") : -1;
		added = true;
		if (under) invisible++;
	}

	// *** coming ***

	public static GreatWorm on( Level level ){
		for (Mob m : level.mobs) if (m instanceof GreatWorm) return (GreatWorm) m;
		return null;
	}

	//every so often, from the open floors of any biome (their respawner, say): if it isn't down there
	//already it may come, far from everyone, and it only shows once it hears something. true if it came
	public static boolean maybeEmerge( Level level ){
		if (!(level instanceof OpenWorld) && !DevFlags.blindWorm() || on(level) != null) return false;
		if (!DevFlags.blindWorm() && Random.Int(4) != 0) return false;
		GreatWorm g = under(level);
		if (g == null) return false;
		if (level == Dungeon.level) GameScene.add(g);
		else level.mobs.add(g);
		return true;
	}

	//a worm placed under that floor far from the heroes, not added yet, or null
	public static GreatWorm under( Level level ){
		Level was = Dungeon.level;
		Dungeon.level = level;
		try {
			int w = level.width(), cell = -1;
			for (int tries = 0; tries < 60 && cell == -1; tries++){
				int c = Random.Int(level.length());
				int x = c % w, y = c / w;
				if (x < 7 || y < 7 || x + 7 >= w || y + 7 >= level.height()) continue;
				boolean far = true;
				for (Hero h : Party.living()) if (level.distance(h.pos, c) < 24) far = false;
				if (far || DevFlags.blindWorm()) cell = c;
			}
			if (cell == -1) return null;
			GreatWorm g = new GreatWorm();
			g.pos = cell;
			g.home = cell;
			CoopLog.line("fauna: GreatWorm under the floor at " + cell);
			return g;
		} finally {
			Dungeon.level = was;
		}
	}

	//0 the hole, 1 it shakes, 2-3 coming up, 4-5 out, 6 rears back, 7 bites, 8 listens, 9 going down,
	//10 down, 11 dead
	public static class WormSprite extends WildSprite {

		private Animation hole, out;

		public WormSprite(){
			super();
			beast(IslesAssets.WORM, 15, 256, 256, 8, 8);
			hole = loop(1, 0);
			out = loop(2, 4, 4, 5, 8, 4, 5);
			idle = hole;
			run = hole;
			attack = clip(10, 6, 7, 7);
			zap = attack.clone();
			die = clip(4, 10, 11, 11);
			play(idle);
		}

		@Override
		public void windup( int kind ) {
			play(loop(6, kind == EMERGE ? 1 : 6, kind == EMERGE ? 0 : 6));
		}

		@Override
		public void strike( int kind ) {
			once(clip(10, 7, 7, 4));
		}

		public void emerge(){
			idle = out;
			once(clip(8, 2, 3, 3, 7, 7, 4));
		}

		public void sink(){
			idle = hole;
			once(clip(4, 9, 9, 0));
		}

		@Override
		protected int[] downFrames() {
			return new int[]{10, 10, 10, 4};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(128, 98);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof GreatWorm && !((GreatWorm) ch).submerged()){
				idle = out;
				idle();
			}
		}
	}
}
