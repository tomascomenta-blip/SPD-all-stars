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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.Fauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//a hero up on the back of a big one. the hero stands on a cell of its body and is carried when it
//walks; hits from up there land on the weak spot. every turn the grip wears down a little and
//when the beast shakes itself a lot; at nothing the hero falls. on a tamed one the grip holds and
//it's a place to rest
public class Climbing extends Buff {

	public static final float MAX = 100;
	//the smallest that can be climbed
	public static final int SIZE = 8;

	public int colossus = -1;
	public float grip = MAX;
	//where on the body, from its middle cell
	public int dx, dy;

	{
		type = buffType.NEUTRAL;
	}

	public Colossus target(){
		Actor a = Actor.findById(colossus);
		return a instanceof Colossus && ((Colossus) a).isAlive() ? (Colossus) a : null;
	}

	public static boolean on( Char ch, Colossus c ){
		Climbing b = ch.buff(Climbing.class);
		return b != null && c != null && b.colossus == c.id();
	}

	public static ArrayList<Hero> climbers( Colossus c ){
		ArrayList<Hero> out = new ArrayList<>();
		for (Hero h : Party.living()){
			if (on(h, c)) out.add(h);
		}
		return out;
	}

	public static boolean canClimb( Hero h, Colossus c ){
		return c != null && c.isAlive() && c.size >= SIZE && c.solid() && c.reach(h.pos) <= 1
				&& h.buff(Climbing.class) == null && spot(h, c) != -1;
	}

	//a free cell of its back near the hero: the climbable rows when the drawing says which
	private static int spot( Hero h, Colossus c ){
		int w = Dungeon.level.width();
		ArrayList<Integer> cells = new ArrayList<>();
		if (c instanceof Fauna && ((Fauna) c).climbable().length > 0){
			boolean right = !(c instanceof AshColossus) || ((AshColossus) c).right;
			for (int[] r : ((Fauna) c).climbable()){
				for (int col = r[1]; col <= r[2]; col++){
					int x = right ? col : c.size - 1 - col;
					cells.add(c.origin() + x + r[0] * w);
				}
			}
		} else {
			//its upper half, off the very edge
			for (int y = 1; y <= c.size / 2; y++){
				for (int x = 1; x < c.size - 1; x++) cells.add(c.origin() + x + y * w);
			}
		}
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int cell : cells){
			if (cell < 0 || cell >= Dungeon.level.length()) continue;
			Char there = Actor.findChar(cell);
			if (there != null && there != c) continue;
			int d = Dungeon.level.distance(cell, h.pos);
			if (d < bestD){
				bestD = d;
				best = cell;
			}
		}
		return best;
	}

	public static void climb( final Hero h, Colossus c ){
		int cell = spot(h, c);
		if (cell == -1) return;
		int w = Dungeon.level.width();
		Climbing b = Buff.affect(h, Climbing.class);
		b.colossus = c.id();
		b.grip = MAX;
		b.dx = cell % w - c.pos % w;
		b.dy = cell / w - c.pos / w;
		int from = h.pos;
		h.pos = cell;
		CoopLog.line("hunt: hero " + Party.indexOf(h) + " climbs " + c.getClass().getSimpleName() + " at " + cell);
		if (h == Dungeon.local()) GLog.p(Messages.get(Climbing.class, "up", c.name()));
		Sample.INSTANCE.play(Assets.Sounds.GRASS);
		if (h.sprite != null && h.sprite.parent != null){
			h.sprite.jump(from, cell, 12, 0.25f, new Callback() {
				@Override
				public void call() {
					h.sprite.place(h.pos);
				}
			});
		}
		Dungeon.observe();
	}

	//called by the colossus each time it takes a step: whoever is up there goes along
	public static void carry( Colossus c ){
		int w = Dungeon.level.width();
		for (Hero h : climbers(c)){
			Climbing b = h.buff(Climbing.class);
			h.pos = c.pos + b.dx + b.dy * w;
			if (h.sprite != null) h.sprite.place(h.pos);
		}
		if (!climbers(c).isEmpty()) Dungeon.observe();
	}

	//it shakes itself: every grip takes a blow
	public static void shake( Colossus c, float amount ){
		for (Hero h : climbers(c)){
			Climbing b = h.buff(Climbing.class);
			b.grip -= amount;
			if (h == Dungeon.local()){
				GLog.w(Messages.get(Climbing.class, "shaken", c.name()));
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						PixelScene.shake(2f, 0.4f);
					}
				});
			}
			if (b.grip <= 0) b.fall();
		}
	}

	//a cell clicked while up there: on the body it's a hit, even on a part out of sight in the dark,
	//anywhere else it's a jump down towards it. true when it was handled here (Hero.handle)
	public static boolean handle( Hero h, int cell ){
		Climbing b = h.buff(Climbing.class);
		if (b == null) return false;
		Colossus c = b.target();
		if (c == null){
			b.detach();
			return false;
		}
		if (c.covers(cell)){
			Char there = Actor.findChar(cell);
			if (there != null && there != c) return false;
			h.curAction = new HeroAction.Attack(c);
			return true;
		}
		b.getDown(cell);
		h.curAction = null;
		h.spend(TICK);
		return true;
	}

	public void getDown( int toward ){
		Hero h = (Hero) target;
		int land = landing(toward);
		detach();
		if (land == -1) return;
		int from = h.pos;
		h.pos = land;
		Dungeon.level.occupyCell(h);
		if (h.sprite != null) h.sprite.jump(from, land, 10, 0.2f, null);
		CoopLog.line("hunt: hero " + Party.indexOf(h) + " gets down at " + land);
		Dungeon.observe();
	}

	public void fall(){
		Hero h = (Hero) target;
		Colossus c = target();
		CoopLog.line("hunt: hero " + Party.indexOf(h) + " falls off");
		if (h == Dungeon.local()) GLog.n(Messages.get(Climbing.class, "fell", c == null ? "" : c.name()));
		getDown(c == null ? h.pos : c.pos + (h.pos - c.pos) * 2);
		h.damage(Math.max(1, h.HT / 10), this);
	}

	//the nearest open cell just off the body, towards where the hero wants to go
	private int landing( int toward ){
		Hero h = (Hero) target;
		Colossus c = target();
		Level l = Dungeon.level;
		int w = l.width();
		if (c == null) return h.pos;
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int y = c.top() - 1; y <= c.top() + c.size; y++){
			for (int x = c.left() - 1; x <= c.left() + c.size; x++){
				if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
				int cell = x + y * w;
				if (c.covers(cell) || !l.passable[cell] || l.pit[cell] || Actor.findChar(cell) != null) continue;
				int d = l.distance(cell, toward < 0 || toward >= l.length() ? h.pos : toward);
				if (d < bestD){
					bestD = d;
					best = cell;
				}
			}
		}
		return best;
	}

	@Override
	public boolean act() {
		Colossus c = target();
		Hero h = (Hero) target;
		if (c == null || !h.isAlive()){
			if (h.isAlive()) getDown(h.pos);
			else detach();
			return true;
		}
		AshColossus a = c instanceof AshColossus ? (AshColossus) c : null;
		if (a != null && a.tamed){
			//resting on its back
			grip = Math.min(MAX, grip + 10);
			if (h.HP < h.HT && h.resting) h.HP = Math.min(h.HT, h.HP + 1);
		} else if (a != null && a.down > 0){
			grip = Math.min(MAX, grip + 4);
		} else {
			grip -= 2.5f;
		}
		if (grip <= 0) fall();
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("colossus", colossus);
		bundle.put("grip", grip);
		bundle.put("dx", dx);
		bundle.put("dy", dy);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		colossus = bundle.getInt("colossus");
		grip = bundle.getFloat("grip");
		dx = bundle.getInt("dx");
		dy = bundle.getInt("dy");
	}
}
