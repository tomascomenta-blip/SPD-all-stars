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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//basalt bison. they graze in herds and want nothing to do with anyone. but a racket nearby, a torch
//coming at them or a hit, and the herd bolts: a turn of fright, the way ahead gets marked, and then
//they run two cells a turn over whatever is there. hit one again while it's still jumpy and it fights.
//some carry ember fleas in their wool, which makes them touchier; without fleas they could be tamed
public class BasaltBison extends AshColossus {

	{
		size = 5;
		spriteClass = BisonSprite.class;

		HP = HT = 700;
		defenseSkill = 8;
		EXP = 35;

		minDmg = 36; maxDmg = 50; acc = 56;
		armorMin = 10; armorMax = 24;
	}

	public static final int RUN = 12;

	public boolean infested = false;
	//a herd coming back to the plain: the first one brings the rest on its first turn
	public int calves = 0;

	private int scared = 0, stampede = 0, dirX = 0, dirY = 0;
	//after a run it won't bolt again for a while, and a hit then makes it fight
	private int calm = 0, angry = 0;

	public void infest( int n ){
		infested = true;
		fleas = n;
		spriteClass = FleaBisonSprite.class;
	}

	//freed of its fleas and calm, it lets the collar on
	@Override
	public String tameBlock( Hero h ) {
		if (fleas > 0) return Messages.get(this, "fleas");
		if (state == HUNTING) return Messages.get(this, "wild");
		return null;
	}

	@Override
	public boolean tameable() {
		return !infested;
	}

	@Override
	public boolean peaceful() {
		return angry <= 0;
	}

	@Override
	protected int ears() {
		return infested ? 4 : 2;
	}

	@Override
	protected int roam() {
		return 12;
	}

	@Override
	protected void onFleasLost() {
		if (fleas > 0 || !infested) return;
		infested = false;
		spriteClass = BisonSprite.class;
		if (sprite instanceof BisonSprite) ((BisonSprite) sprite).clean();
	}

	private void bringHerd(){
		int n = calves;
		calves = 0;
		for (int i = 0; i < n; i++){
			BasaltBison b = new BasaltBison();
			int c = b.spotNear(pos, 10);
			if (c == -1) break;
			b.pos = c;
			b.home = c;
			b.leader = id();
			GameScene.add(b);
		}
	}

	//somewhere this one would fit, near a cell
	private int spotNear( int center, int r ){
		boolean[] f = fits(size, this);
		Level l = Dungeon.level;
		int w = l.width(), cx = center % w, cy = center / w;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int y = Math.max(0, cy - r); y <= Math.min(l.height() - 1, cy + r); y++){
			for (int x = Math.max(0, cx - r); x <= Math.min(w - 1, cx + r); x++){
				int c = x + y * w;
				if (f[c] && l.distance(c, center) >= size + 1 && Actor.findChar(c) == null) cells.add(c);
			}
		}
		return cells.isEmpty() ? -1 : Random.element(cells);
	}

	@Override
	protected boolean act() {
		if (calves > 0) bringHerd();
		if (paralysed > 0) return super.act();
		if (scared > 0 || stampede > 0) return run();
		if (calm > 0) calm--;
		if (angry > 0 && --angry == 0 && state == HUNTING){
			state = WANDERING;
			enemy = null;
		}
		//a light coming close
		int spook = infested ? 9 : 7;
		for (Hero h : Party.living()){
			if (Senses.lit(h) && reach(h.pos) <= spook){
				panic(h.pos);
				break;
			}
		}
		return super.act();
	}

	@Override
	protected void heard( Senses.Noise n ) {
		if (n.radius >= Senses.FIGHT || infested) panic(n.cell);
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage(dmg, src);
		if (!isAlive() || dmg <= 0) return;
		int from = src instanceof Char ? ((Char) src).pos : -1;
		if (from == -1){
			Hero h = Party.nearestLiving(pos);
			if (h != null) from = h.pos;
		}
		if (calm > 0 && stampede == 0 && scared == 0){
			//nowhere left to run: it turns on them
			angry = 15;
			if (src instanceof Char && SKCombat.foes(this, (Char) src)) aggro((Char) src);
		} else if (from != -1){
			panic(from);
			if (state == HUNTING && angry <= 0) state = WANDERING;
		}
	}

	// *** the stampede ***

	//away from that cell, it and the herd around it
	public void panic( int from ){
		if (scared > 0 || stampede > 0 || calm > 0 || !isAlive()) return;
		int w = Dungeon.level.width();
		int dx = Integer.signum(pos % w - from % w), dy = Integer.signum(pos / w - from / w);
		if (dx == 0 && dy == 0){
			dx = Random.Int(2) * 2 - 1;
			dy = Random.Int(3) - 1;
		}
		bolt(dx, dy);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.8f, 0.6f);
			GLog.w(Messages.get(BasaltBison.class, "spooked"));
		}
		//the whole herd goes the same way
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof BasaltBison && m != this && Dungeon.level.distance(m.pos, pos) <= 14){
				BasaltBison b = (BasaltBison) m;
				if (b.scared == 0 && b.stampede == 0 && b.calm == 0) b.bolt(dx, dy);
			}
		}
	}

	private void bolt( int dx, int dy ){
		dirX = dx;
		dirY = dy;
		scared = 1;
		stampede = RUN;
		state = WANDERING;
		if (sprite instanceof BisonSprite) ((BisonSprite) sprite).fright();
		//everything it will run over is marked from now on
		LinkedHashSet<Integer> lane = lane(RUN);
		SKCombat.mark(lane, 1f + RUN / 2f);
	}

	//the ground the body will cover over the next steps the way it's going
	private LinkedHashSet<Integer> lane( int steps ){
		LinkedHashSet<Integer> out = new LinkedHashSet<>();
		int w = Dungeon.level.width(), h = Dungeon.level.height();
		int x = pos % w, y = pos / w;
		for (int k = 1; k <= steps; k++){
			int cx = x + dirX * k, cy = y + dirY * k;
			for (int j = -half(); j <= half(); j++){
				for (int i = -half(); i <= half(); i++){
					int px = cx + i, py = cy + j;
					if (px < 0 || py < 0 || px >= w || py >= h) continue;
					int c = px + py * w;
					if (!covers(c) && !Dungeon.level.solid[c]) out.add(c);
				}
			}
		}
		return out;
	}

	private boolean run(){
		if (scared > 0){
			scared--;
			spend(TICK);
			return true;
		}
		int start = pos, w = Dungeon.level.width();
		boolean[] f = fits();
		for (int i = 0; i < 2 && stampede > 0; i++){
			int next = step(f, w);
			if (next == -1){
				stampede = 0;
				break;
			}
			walkTo(next);
			stampede--;
			f = fits();
		}
		if (pos != start){
			if (sprite != null) sprite.move(start, pos);
			Senses.noise(pos, Senses.STAMPEDE);
			//dust only: no game dice in what only some devices see
			if (seen()) for (int c : cells()) if (Dungeon.level.heroFOV[c] && c % 4 == 0) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 2);
			//the cells right ahead stay marked
			for (int c : lane(Math.min(4, stampede))) GameScene.targetedCell(c, 1f);
		}
		if (stampede == 0){
			calm = 25;
			if (sprite instanceof BisonSprite) ((BisonSprite) sprite).settle();
		}
		spend(TICK);
		return true;
	}

	private static final int[][] ROSE = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};

	//straight on, or bending a little round whatever is in the way
	private int step( boolean[] f, int w ){
		int at = 0;
		for (int i = 0; i < ROSE.length; i++){
			if (ROSE[i][0] == dirX && ROSE[i][1] == dirY) at = i;
		}
		for (int turn : new int[]{0, 1, -1}){
			int[] d = ROSE[(at + turn + ROSE.length) % ROSE.length];
			int c = pos + d[0] + d[1] * w;
			if (c >= 0 && c < f.length && f[c]){
				dirX = d[0];
				dirY = d[1];
				return c;
			}
		}
		return -1;
	}

	//running over someone
	@Override
	protected void onTrample( Char ch ) {
		if (stampede <= 0 || !SKCombat.foes(this, ch)){
			super.onTrample(ch);
			return;
		}
		blast(ch, 70, 95);
		if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("infested", infested);
		bundle.put("calves", calves);
		bundle.put("scared", scared);
		bundle.put("stampede", stampede);
		bundle.put("dirx", dirX);
		bundle.put("diry", dirY);
		bundle.put("calm", calm);
		bundle.put("angry", angry);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		infested = bundle.getBoolean("infested");
		calves = bundle.getInt("calves");
		scared = bundle.getInt("scared");
		stampede = bundle.getInt("stampede");
		dirX = bundle.getInt("dirx");
		dirY = bundle.getInt("diry");
		calm = bundle.getInt("calm");
		angry = bundle.getInt("angry");
		spriteClass = infested ? FleaBisonSprite.class : BisonSprite.class;
	}

	//12 scared, 13-16 galloping, 17 grazing. same frames on the sheet with fleas
	public static class BisonSprite extends AshColossusSprite {

		@Override
		protected int[] downFrames() {
			return new int[]{18, 19, 20, 21};
		}

		@Override
		protected String tamedSheet() {
			return FaunaAssets.BISON_TAMED;
		}

		private Animation grazing, galloping;

		public BisonSprite(){
			this(FaunaAssets.BISON);
		}

		protected BisonSprite( String tex ){
			super();
			setup(tex, 5, 112, 112, 16, 32);
			grazing = loop(2, 0, 0, 1, 1, 17, 17, 17, 1, 0, 17, 17, 0);
			galloping = loop(14, 13, 14, 15, 16);
			idle = grazing;
			play(idle);
		}

		public void fright(){
			play(loop(1, 12));
		}

		public void settle(){
			idle();
		}

		@Override
		public void move( int from, int to ) {
			super.move(from, to);
			if (ch instanceof BasaltBison && ((BasaltBison) ch).stampede > 0) play(galloping);
		}

		public void clean(){
			texture(FaunaAssets.BISON);
		}
	}

	public static class FleaBisonSprite extends BisonSprite {
		public FleaBisonSprite(){
			super(FaunaAssets.BISON_FLEAS);
		}
	}
}
