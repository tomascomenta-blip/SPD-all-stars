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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//the big beasts of the moor, the graveyard and the isles: the ash ones, plus the shapes their blows
//take. every heavy blow is marked first (warn) and lands in release; an ambush may come unmarked but
//never takes the last bit of anyone's life
public abstract class WildColossus extends AshColossus {

	//the ones that keep to a patch of ground don't even look at whoever is far out of it: seeing them
	//would make it notice, give up at once and notice again without ever spending a turn
	@Override
	protected Char chooseEnemy() {
		Char e = super.chooseEnemy();
		if (e != null && !tamed && territory > 0 && home != -1 && Dungeon.level.distance(home, e.pos) > territory + 6) return null;
		return e;
	}

	//what a marked blow of that kind takes, {min, max}. by default half again a plain hit
	protected int[] power( int kind ){
		return new int[]{minDmg * 3 / 2, maxDmg * 3 / 2};
	}

	//what else that blow does to whoever it caught (still alive)
	protected void onBlast( int kind, Char ch ){
	}

	//what's left to do once the blow has landed (moving, calling others...)
	protected void afterBlast( int kind, ArrayList<Integer> cells, int aim ){
	}

	//the particle the blow raises on each cell, -1 for none
	protected int dust( int kind ){
		return Speck.DUST;
	}

	protected String blowSound( int kind ){
		return Assets.Sounds.HIT_STRONG;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim >= 0 ? aim : pos, Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(blowSound(kind), 1f, 0.7f);
			int d = dust(kind);
			if (d >= 0) for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(d), 2);
		}
		int[] p = power(kind);
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, p[0], p[1]);
			if (ch.isAlive()) onBlast(kind, ch);
		}
		afterBlast(kind, cells, aim);
	}

	//an unmarked hit out of hiding: it hurts, but it never kills
	protected void ambush( Char ch, int min, int max ){
		int d = Math.min(SKCombat.specialDamage(this, ch, min, max), ch.HP - 1);
		if (d > 0) ch.damage(d, this);
		if (ch == Dungeon.local() && ch.isAlive()) GLog.w(Messages.get(WildColossus.class, "ambush", name()));
	}

	//walked over, a hero gets bruised but never finished off: that's not a marked blow
	@Override
	protected void onTrample( Char ch ) {
		if (!(ch instanceof Hero)){
			super.onTrample(ch);
			return;
		}
		int d = Math.min(Math.max(1, damageRoll() * Math.min(size, 10) / 20), ch.HP - 1);
		if (d > 0) ch.damage(d, this);
	}

	// *** shapes ***

	//a band in front of the body, the way it faces: depth cells deep, as tall as the body plus spread
	protected ArrayList<Integer> front( int depth, int spread ){
		ArrayList<Integer> out = new ArrayList<>();
		Level l = Dungeon.level;
		int w = l.width();
		int x0 = right ? left() + size : left() - depth;
		for (int y = top() - spread; y < top() + size + spread; y++){
			for (int x = x0; x < x0 + depth; x++){
				if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
				int c = x + y * w;
				if (!covers(c) && !l.solid[c]) out.add(c);
			}
		}
		return out;
	}

	//a strip width cells wide from the body towards aim, len cells past its edge
	protected ArrayList<Integer> lane( int aim, int len, int width ){
		LinkedHashSet<Integer> out = new LinkedHashSet<>();
		Level l = Dungeon.level;
		int w = l.width();
		float dx = aim % w - pos % w, dy = aim / w - pos / w;
		float n = (float) Math.sqrt(dx * dx + dy * dy);
		if (n < 0.5f) return new ArrayList<>(out);
		dx /= n;
		dy /= n;
		int r = width / 2;
		for (int t = 1; t <= half() + len; t++){
			int cx = Math.round(pos % w + dx * t), cy = Math.round(pos / w + dy * t);
			for (int y = cy - r; y <= cy + r; y++){
				for (int x = cx - r; x <= cx + r; x++){
					if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
					int c = x + y * w;
					if (!covers(c) && !l.solid[c] && reach(c) <= len) out.add(c);
				}
			}
		}
		return new ArrayList<>(out);
	}

	//the middle of the side it faces, just off the body
	protected int mouth(){
		int w = Dungeon.level.width();
		int x = right ? left() + size : left() - 1;
		return x + (pos / w) * w;
	}

	//a cone off the front of the body towards aim, about 60 degrees wide, as far as range
	protected ArrayList<Integer> cone( int aim, int range ){
		face(aim);
		ArrayList<Integer> out = new ArrayList<>();
		Level l = Dungeon.level;
		int w = l.width(), from = mouth();
		if (from < 0 || from >= l.length()) return out;
		double fx = from % w, fy = from / w;
		double ax = aim % w - fx, ay = aim / w - fy;
		double len = Math.sqrt(ax * ax + ay * ay);
		if (len < 0.5) return out;
		for (int c : SKCombat.area(from, range)){
			if (covers(c) || l.pit[c] || l.solid[c]) continue;
			double cx = c % w - fx, cy = c / w - fy;
			double cl = Math.sqrt(cx * cx + cy * cy);
			if (cl > range + 0.5) continue;
			if (cl >= 0.5 && (cx * ax + cy * ay) / (cl * len) < 0.86) continue;
			if (c != from && new Ballistica(from, c, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos != c) continue;
			out.add(c);
		}
		return out;
	}

	//the hero nearest to the body, for the ones that look for prey themselves
	protected Hero prey( int range ){
		Hero h = nearestHero();
		return h != null && reach(h.pos) <= range ? h : null;
	}
}
