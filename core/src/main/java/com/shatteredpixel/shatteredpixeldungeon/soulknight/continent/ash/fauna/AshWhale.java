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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
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

import java.util.ArrayList;

//ash whale. it swims under the deep ash: all you see is a fin, a wake of smoke and a furrow. it
//feels whoever walks the ash near it (the roads and the rock are safe) and comes. when it's under
//you the ground cracks and bulges for three turns over a 5x5 patch, then it comes up through it
//with its four jaws open and swallows what's there. it stays up a few turns, the only time it can
//be hurt, and sinks again
public class AshWhale extends AshColossus {

	{
		size = 15;
		spriteClass = WhaleSprite.class;

		HP = HT = 4500;
		defenseSkill = 0;
		EXP = 300;

		minDmg = 50; maxDmg = 70; acc = 60;
		armorMin = 10; armorMax = 26;

		properties.add(Property.MINIBOSS);
	}

	//how far it feels steps on the ash, how far it lunges underground to come up under someone
	private static final int FEEL = 12, LUNGE = 10;
	private static final int UP = 5;

	private boolean under = true;
	private boolean added = false;
	//turns left out of the ash, and before it hunts again
	private int up = 0, rest = 0, baffled = 0;
	private float heardAt = -1;

	public boolean submerged(){
		return under;
	}

	@Override
	public boolean solid() {
		return !under;
	}

	@Override
	protected boolean wades() {
		return false;
	}

	@Override
	protected boolean walkable( int cell ) {
		return AshFauna.deepAsh(cell) && super.walkable(cell);
	}

	//under the ash it can't come up through anyone's feet with its middle
	@Override
	public boolean[] fits() {
		boolean[] f = super.fits();
		if (under){
			for (Char ch : Actor.chars()){
				if (ch != this && ch.pos >= 0 && ch.pos < f.length) f[ch.pos] = false;
			}
		}
		return f;
	}

	@Override
	protected void onAdd() {
		super.onAdd();
		if (!added){
			added = true;
			if (under) invisible++;
		}
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return under || super.isInvulnerable(effect);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return false;
	}

	//the swallowing zone: columns 5-9, rows 8-12 of the body (the same both ways it faces)
	private ArrayList<Integer> mouth( int center ){
		ArrayList<Integer> out = new ArrayList<>();
		int w = Dungeon.level.width();
		for (int dy = 1; dy <= 5; dy++){
			for (int dx = -2; dx <= 2; dx++){
				int c = center + dx + dy * w;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]) out.add(c);
			}
		}
		return out;
	}

	//where its middle has to be for that cell to be in the mouth, or -1
	private int comeUpAt( int cell ){
		boolean[] f = fits();
		boolean[] solidFit = fitsUp();
		int w = Dungeon.level.width();
		int[][] order = {{0, 3}, {0, 2}, {0, 4}, {-1, 3}, {1, 3}, {-1, 2}, {1, 2}, {-1, 4}, {1, 4}, {0, 1}, {0, 5}, {-2, 3}, {2, 3}};
		for (int[] o : order){
			int x = cell % w - o[0], y = cell / w - o[1];
			if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
			int c = x + y * w;
			if (f[c] && solidFit[c]) return c;
		}
		return -1;
	}

	//the plain fit of the body once it's up: other big ones count, small ones get shoved
	private boolean[] fitsUp(){
		return fits(size, this);
	}

	@Override
	protected boolean act() {
		if (paralysed > 0 && !under) return super.act();
		if (pendingKind != -1){
			int[] cells = pendingCells;
			int aim = pendingAim;
			pendingKind = -1;
			pendingCells = new int[0];
			spend(TICK);
			ArrayList<Integer> zone = new ArrayList<>();
			for (int c : cells) zone.add(c);
			emerge(zone, aim);
			return true;
		}
		if (farFromParty()){
			spend(TICK * 3);
			return true;
		}
		if (!under){
			if (--up <= 0) sink();
			spend(TICK);
			return true;
		}
		return swim();
	}

	private boolean swim(){
		if (heardAt < 0) heardAt = Actor.now();
		Hero prey = null;
		int best = Integer.MAX_VALUE;
		for (Hero h : Party.living()){
			if (h.invisible > 0 || !AshFauna.deepAsh(h.pos)) continue;
			int d = reach(h.pos);
			if (d <= FEEL && d < best){
				prey = h;
				best = d;
			}
		}
		Senses.Noise n = Senses.heard(this, heardAt, 6);
		heardAt = Actor.now();
		//resting after coming up it swims off, deaf to everything
		if (rest == 0){
			if (prey != null) target = prey.pos;
			else if (n != null) target = n.cell;
		}

		if (rest > 0) rest--;
		else if (prey != null){
			int spot = comeUpAt(prey.pos);
			if (spot != -1 && Dungeon.level.distance(spot, pos) <= LUNGE){
				baffled = 0;
				dive(spot, prey);
				return true;
			}
			//they stand where it can't come up (against the rock): after a while it loses interest
			if (++baffled >= 10){
				baffled = 0;
				rest = 12;
				int c = fitNear(pos, 20);
				target = c == -1 ? pos : c;
			}
		}

		int old = pos;
		if (target != -1 && target != pos && getCloser(target)){
			furrow(old);
			spend(1 / speed());
			return moveSprite(old, pos);
		}
		int c = fitNear(pos, 20);
		target = c == -1 ? pos : c;
		spend(TICK);
		return true;
	}

	//it slides underground to right under the prey and the ground starts to give
	private void dive( int spot, Hero prey ){
		int old = pos;
		move(spot);
		if (sprite != null) sprite.place(spot);
		furrow(old);
		warn(0, mouth(spot), prey.pos, 3f);
		Senses.noise(spot, Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.4f);
			GLog.w(Messages.get(this, "cracks"));
		}
	}

	private void emerge( ArrayList<Integer> zone, int aim ){
		if (!fitsUp()[pos]){
			//something big got in the way: it stays down
			rest = 3;
			if (sprite != null) sprite.idle();
			return;
		}
		under = false;
		invisible = Math.max(0, invisible - 1);
		up = UP;
		rest = 8;
		if (sprite instanceof WhaleSprite) ((WhaleSprite) sprite).emerge();
		Senses.noise(pos, Senses.ROAR);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.4f);
			for (int c : zone) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 3);
		}
		for (Char ch : SKCombat.foesIn(this, zone)){
			blast(ch, 240, 320);
			if (ch == Dungeon.local() && ch.isAlive()) GLog.n(Messages.get(this, "spat"));
		}
		//whatever is left under the body gets thrown off it
		trample(pos);
	}

	private void sink(){
		under = true;
		invisible++;
		if (sprite instanceof WhaleSprite) ((WhaleSprite) sprite).sink();
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f, 0.5f);
	}

	@Override
	protected void onTrample( Char ch ) {
		if (!under) super.onTrample(ch);
	}

	//only the picture: the ash the fin just stirred
	private void furrow( int center ){
		if (sprite == null || sprite.parent == null || !seen()) return;
		int w = Dungeon.level.width();
		int at = center - half() - half() * w + (right ? 8 : 6) + 10 * w;
		final PointF p = DungeonTilemap.tileCenterToWorld(at);
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (sprite != null && sprite.parent != null) sprite.parent.addToBack(new Furrow(p));
			}
		});
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("under", under);
		bundle.put("up", up);
		bundle.put("rest", rest);
		bundle.put("baffled", baffled);
		bundle.put("heard_at", heardAt);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		under = bundle.getBoolean("under");
		up = bundle.getInt("up");
		rest = bundle.getInt("rest");
		baffled = bundle.getInt("baffled");
		heardAt = bundle.getFloat("heard_at");
		added = true;
		if (under) invisible++;
	}

	public static class Furrow extends Image {

		private float time = 0;
		private final TextureFilm film;

		public Furrow( PointF center ){
			super(FaunaAssets.FURROW);
			film = new TextureFilm(texture, 32, 32);
			frame(film.get(0));
			x = center.x - 16;
			y = center.y - 16;
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			if (time > 6f){
				killAndErase();
			} else {
				frame(film.get(Math.min(2, (int) (time / 2f))));
				if (time > 5f) alpha(6f - time);
			}
		}
	}

	//0-3 the fin, 4 the ground cracking, 5-8 coming up and swallowing, 9 sinking, 10 the crater,
	//11-12 dead
	public static class WhaleSprite extends AshColossusSprite {

		@Override
		protected int[] downFrames() {
			return new int[]{14, 14, 15, 14};
		}

		private Animation fin, out;

		public WhaleSprite(){
			super();
			setup(FaunaAssets.WHALE, 15, 256, 256, 8, 8);
			fin = loop(6, 0, 1, 2, 3);
			out = loop(1, 8);
			idle = fin;
			run = fin;
			die = clip(4, 11, 12, 12);
			play(idle);
		}

		@Override
		public void windup( int kind ) {
			play(loop(3, 4));
		}

		public void emerge(){
			idle = out;
			once(clip(8, 5, 6, 7, 7, 7, 7, 8));
		}

		public void sink(){
			idle = fin;
			once(clip(4, 9, 10, 10, 10));
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof AshWhale && !((AshWhale) ch).submerged()){
				idle = out;
				idle();
			}
		}
	}
}
