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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//smoke vulture. circles over the party out of reach and dives, along a marked line, at whoever
//strays furthest from the others. the hit snuffs out their torch. a carrion bird: it trails the
//colossi waiting for leftovers, and smells blood from far (a hurt hero is the first it picks)
public class SmokeVulture extends ContinentMob {

	{
		spriteClass = VultureSprite.class;

		HP = HT = 110;
		defenseSkill = 36;
		EXP = 16;
		flying = true;
		baseSpeed = 1.5f;

		minDmg = 24; maxDmg = 32; acc = 58;
		armorMin = 0; armorMax = 8;

		HUNTING = new Circling();
	}

	private static final int RING = 3;
	private static final int DIVE = 7;

	@Override
	protected int specialCooldown() {
		return 6;
	}

	@Override
	protected boolean act() {
		if (state == WANDERING || state == SLEEPING){
			Hero prey = Senses.scent(this, 18);
			if (prey != null){
				aggro(prey);
				target = prey.pos;
			} else {
				trail();
			}
		}
		return super.act();
	}

	//circles over the nearest big one
	private void trail(){
		Actor a = leader == -1 ? null : Actor.findById(leader);
		if (a instanceof Colossus && ((Colossus) a).isAlive()) return;
		leader = -1;
		Mob best = null;
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (!(m instanceof Colossus) || m.alignment != alignment || Dungeon.level.distance(m.pos, pos) > 30) continue;
			if (best == null || Dungeon.level.distance(m.pos, pos) < Dungeon.level.distance(best.pos, pos)) best = m;
		}
		if (best != null) leader = best.id();
	}

	//the loneliest hero it can see and reach in a straight line
	private Hero mark(){
		Hero best = null;
		int bestAlone = -1;
		for (Hero h : Party.living()){
			if (h.invisible > 0 || fieldOfView == null || !fieldOfView[h.pos]) continue;
			int d = Dungeon.level.distance(pos, h.pos);
			if (d < 2 || d > DIVE) continue;
			if (!line(h.pos, d).contains(h.pos)) continue;
			int alone = 99;
			for (Hero o : Party.living()){
				if (o != h) alone = Math.min(alone, Dungeon.level.distance(o.pos, h.pos));
			}
			//blood first
			if (Senses.bleeding(h)) alone += 50;
			if (best == null || alone > bestAlone
					|| alone == bestAlone && d < Dungeon.level.distance(pos, best.pos)){
				best = h;
				bestAlone = alone;
			}
		}
		return best;
	}

	@Override
	protected boolean special( Char enemy ) {
		Hero prey = mark();
		if (prey == null) return false;
		ArrayList<Integer> cells = line(prey.pos, Dungeon.level.distance(pos, prey.pos));
		this.enemy = prey;
		announce(0, cells, prey.pos);
		if (Dungeon.level.heroFOV[pos]){
			if (prey == Dungeon.local()) GLog.w(Messages.get(this, "dive"));
			else if (prey.sprite != null) prey.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "marked"));
		}
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		int start = pos;
		Char victim = null;
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (SKCombat.foes(this, ch)) victim = ch;
				break;
			}
			if (Dungeon.level.solid[c] || !isAlive()) break;
			move(c, false);
		}
		if (!isAlive()) return;
		if (pos != start && sprite != null){
			sprite.move(start, pos);
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 0.7f);
		}
		if (victim != null && attack(victim, 1.6f, 0, 2f) && victim.isAlive() && victim.buff(Light.class) != null){
			Buff.detach(victim, Light.class);
			if (Dungeon.level.heroFOV[victim.pos]) CellEmitter.get(victim.pos).burst(Speck.factory(Speck.SMOKE), 8);
			if (victim == Dungeon.local()) GLog.n(Messages.get(this, "snuffed"));
			else if (victim.sprite != null) victim.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "dark"));
		}
	}

	//next cell going round the target clockwise, about RING cells away. -1 if there's none
	private int orbit( int center ){
		int w = Dungeon.level.width();
		int ax = pos % w - center % w, ay = pos / w - center / w;
		int best = -1, bestScore = Integer.MAX_VALUE;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c] || Actor.findChar(c) != null) continue;
			if (!Dungeon.level.passable[c] || Dungeon.level.pit[c]) continue;
			int d = Dungeon.level.distance(c, center);
			if (d < 2) continue;
			int bx = c % w - center % w, by = c / w - center / w;
			int turn = ax * by - ay * bx;
			int score = Math.abs(d - RING) * 4 - Integer.signum(turn) * 3;
			if (score < bestScore){
				bestScore = score;
				best = c;
			}
		}
		return best;
	}

	private class Circling extends SKHunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			//one peck right after a dive, then back up
			boolean peck = Dungeon.level.adjacent(pos, enemy == null ? pos : enemy.pos)
					&& cooldown >= specialCooldown() - 1;
			if (pendingKind == -1 && enemyInFOV && enemy != null && cooldown > 0 && !rooted && !peck
					&& Dungeon.level.distance(pos, enemy.pos) <= RING + 2){
				int step = orbit(enemy.pos);
				if (step != -1){
					enemySeen = true;
					int old = pos;
					move(step);
					spend(1 / speed());
					return moveSprite(old, pos);
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	public static class VultureSprite extends ContinentMobSprite {
		public VultureSprite(){
			super();
			setup(ContinentAssets.VULTURE, 20, 16);
			zap = loop(8, 12, 13);
		}
	}
}
