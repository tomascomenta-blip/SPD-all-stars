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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.BurningFist;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogEye;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog.YogLarva;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//every hero, while the continent is awake: the brood keeps crawling out of the ground round them,
//out of sight. a few at once on every floor they get to, then one every few turns while there
//aren't too many already close
public class Hunted extends Buff {

	{
		type = buffType.NEGATIVE;
		announced = false;
		revivePersists = true;
	}

	//turns between two of them, the burst on arriving and how many is too many round one hero
	public static final int EVERY = 15;
	public static final int ARRIVAL = 2;
	public static final int CROWD = 5;
	//how far from the hero they come out, and the ring that counts as close
	public static final int NEAR = 7, FAR = 12, CLOSE = 14;

	private int clock = 0;
	//the floor it saw last (depth and branch), to know when the party got somewhere new
	private int floor = -1;

	@Override
	public boolean act() {
		Hero h = (Hero) target;
		if (!Awakening.on() || Awakening.sealed() || !Continent.in() || !h.isAlive() || h.coopDown){
			spend(TICK);
			return true;
		}
		int here = Dungeon.depth * 16 + Dungeon.branch;
		if (here != floor){
			//on the floor where it woke up the big scene already said it
			boolean told = floor == -1;
			floor = here;
			clock = 0;
			int n = 0;
			for (int i = 0; i < ARRIVAL; i++) if (crawl(h)) n++;
			CoopLog.line("awake: " + n + " out of the ground round hero " + Party.indexOf(h) + " on floor " + Continent.floor());
			if (h == Dungeon.local() && !told) GLog.n(Messages.get(Awakening.class, "still_awake"));
		} else if (++clock >= EVERY){
			clock = 0;
			if (crowd(h) < CROWD) crawl(h);
		}
		spend(TICK);
		return true;
	}

	//enemies close to that hero
	private static int crowd( Hero h ){
		int n = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m.isAlive() && m.alignment == Char.Alignment.ENEMY && m.state != m.PASSIVE
					&& Dungeon.level.distance(m.pos, h.pos) <= CLOSE) n++;
		}
		return n;
	}

	private static Mob brood(){
		int k = Random.Int(7);
		if (k < 3) return new YogEye();
		if (k < 5) return new YogLarva();
		return new BurningFist();
	}

	//one of the brood out of the ground near that hero, past what anyone in the party can see in the
	//dark (by their sight, not the screen: every device has to pick the same cell)
	private static boolean crawl( Hero h ){
		Level l = Dungeon.level;
		int sight = 0;
		for (Hero o : Party.living()) sight = Math.max(sight, o.viewDistance);
		int near = Math.max(NEAR, sight + 2), far = Math.max(FAR, near + 5);
		int w = l.width(), hx = h.pos % w, hy = h.pos / w;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int y = Math.max(1, hy - far); y <= Math.min(l.height() - 2, hy + far); y++){
			for (int x = Math.max(1, hx - far); x <= Math.min(w - 2, hx + far); x++){
				int c = x + y * w;
				int d = l.distance(c, h.pos);
				if (d < near || d > far) continue;
				if (!l.passable[c] || l.pit[c] || l.avoid[c] || l.water[c] || Actor.findChar(c) != null) continue;
				if (closeToParty(l, c, near)) continue;
				cells.add(c);
			}
		}
		if (cells.isEmpty()) return false;
		Mob m = brood();
		m.pos = Random.element(cells);
		if (m instanceof SKMob) ((SKMob) m).summoned();
		m.state = m.HUNTING;
		m.aggro(h);
		GameScene.add(m);
		if (m instanceof YogLarva) ((YogLarva) m).hatch();
		return true;
	}

	private static boolean closeToParty( Level l, int cell, int near ){
		for (Hero o : Party.living()){
			if (l.distance(o.pos, cell) < near) return true;
		}
		return false;
	}

	@Override
	public int icon() {
		return Awakening.on() && Continent.in() && !Awakening.sealed() ? BuffIndicator.TARGETED : BuffIndicator.NONE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0xFF3020);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", Continent.essence, Continent.NEEDED);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("clock", clock);
		bundle.put("floor", floor);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		clock = bundle.getInt("clock");
		floor = bundle.getInt("floor");
	}
}
