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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WraithSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//what lives in the dark between the fires. it only ever comes for someone who's alone: it's a bit
//faster than you, every bite puts corruption in, and once you're back with the others it isn't there
public class Stalker extends ContinentMob {

	{
		spriteClass = StalkerSprite.class;

		HP = HT = 120;
		defenseSkill = 26;
		EXP = 0;
		baseSpeed = 1.25f;
		flying = true;

		minDmg = 12; maxDmg = 20; acc = 50;

		state = HUNTING;

		immunities.add(Amok.class);
		immunities.add(Charm.class);
		immunities.add(Corruption.class);
		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
	}

	public static final int BITE_TAINT = 3;

	public int prey = -1;

	public Hero prey(){
		Actor a = Actor.findById(prey);
		return a instanceof Hero ? (Hero) a : null;
	}

	//the one hunting this hero, if any
	public static Stalker of( Hero h ){
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Stalker && ((Stalker) m).prey == h.id() && m.isAlive()) return (Stalker) m;
		}
		return null;
	}

	//out of the dark 6-9 steps away from whoever's alone. false if there's no room for it
	public static boolean spawn( Hero h ){
		Level l = Dungeon.level;
		PathFinder.buildDistanceMap(h.pos, l.passable, 10);
		ArrayList<Integer> far = new ArrayList<>(), near = new ArrayList<>();
		for (int c = 0; c < l.length(); c++){
			int d = PathFinder.distance[c];
			if (d < 3 || d > 9 || l.pit[c] || Actor.findChar(c) != null || Colossus.at(c) != null) continue;
			(d >= 6 ? far : near).add(c);
		}
		ArrayList<Integer> cells = far.isEmpty() ? near : far;
		if (cells.isEmpty()) return false;

		Stalker s = new Stalker();
		s.prey = h.id();
		s.pos = Random.element(cells);
		GameScene.add(s);
		s.aggro(h);
		s.target = h.pos;
		if (s.sprite != null && s.sprite.visible) CellEmitter.get(s.pos).burst(ShadowParticle.UP, 10);
		CoopLog.line("tether: a stalker comes for hero " + Party.indexOf(h) + " from " + s.pos + " (hero at " + h.pos + ")");
		if (h == Dungeon.local()) GLog.n(Messages.get(Stalker.class, "comes"));
		return true;
	}

	@Override
	protected boolean act() {
		Hero h = prey();
		if (h == null || !h.isAlive() || h.coopDown || !Tether.lost(h)){
			dissolve();
			return true;
		}
		//it always knows where you are
		target = h.pos;
		if (state != FLEEING) state = HUNTING;
		return super.act();
	}

	@Override
	protected Char chooseEnemy() {
		if (buff(Terror.class) != null) return super.chooseEnemy();
		return prey();
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		Continent.taint(enemy, BITE_TAINT);
		return super.attackProc(enemy, damage);
	}

	@Override
	public void die( Object cause ) {
		CoopLog.line("tether: stalker beaten at " + pos);
		super.die(cause);
	}

	//nobody to hunt any more: it just isn't there
	private void dissolve(){
		CoopLog.line("tether: the stalker fades at " + pos);
		if (sprite != null && sprite.visible) CellEmitter.get(pos).burst(ShadowParticle.UP, 12);
		Hero h = prey();
		if (h != null && h == Dungeon.local() && h.isAlive()) GLog.i(Messages.get(this, "fades"));
		EXP = 0;
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	@Override
	public String description() {
		return Messages.get(this, "desc");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("prey", prey);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		prey = bundle.getInt("prey");
	}

	//a wraith's shape gone almost black, with the dark coming off it
	public static class StalkerSprite extends WraithSprite {

		private Emitter smoke;

		public StalkerSprite(){
			super();
			shade();
		}

		private void shade(){
			rm = 0.22f;
			gm = 0.14f;
			bm = 0.32f;
			alpha(0.9f);
		}

		@Override
		public void resetColor() {
			super.resetColor();
			shade();
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (smoke == null){
				smoke = emitter();
				smoke.pour(ShadowParticle.UP, 0.1f);
			}
		}

		@Override
		public void die() {
			super.die();
			if (smoke != null){
				smoke.on = false;
				smoke = null;
			}
		}

		@Override
		public void kill() {
			super.kill();
			if (smoke != null){
				smoke.on = false;
				smoke = null;
			}
		}

		@Override
		public int blood() {
			return 0xFF1A0A24;
		}
	}
}
