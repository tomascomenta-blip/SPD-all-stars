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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopPings;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//the bird that mimics. something got into it: three human eyes, human lips under the beak. it keeps
//near the worst things on the plain and, out of sight in the dark, it calls in the voice of someone
//from the group ("help, over here!"), a mark on the map and all. alone, it mimics other things
public class MimicBird extends AshCritter {

	{
		spriteClass = BirdSprite.class;

		HP = HT = 55;
		defenseSkill = 38;
		EXP = 6;
		flying = true;
		baseSpeed = 1.5f;

		minDmg = 14; maxDmg = 20; acc = 54;
		armorMin = 0; armorMax = 4;
	}

	//turns to the next call
	private int quiet = 8;

	private static final int CALLS = 4, SOLO_CALLS = 5;

	@Override
	protected boolean act() {
		if (state == WANDERING) perch();
		if (state != HUNTING && --quiet <= 0) call();
		//whoever gets too close finds it out: it flees
		if (state == HUNTING && HP < HT / 2) state = FLEEING;
		return super.act();
	}

	//it keeps close to the biggest thing around that would eat a man
	private void perch(){
		Actor a = leader == -1 ? null : Actor.findById(leader);
		if (a instanceof Char && ((Char) a).isAlive()) return;
		leader = -1;
		Mob best = null;
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (!(m instanceof AshColossus) || ((AshColossus) m).peaceful()) continue;
			if (Dungeon.level.distance(m.pos, pos) > 30) continue;
			if (best == null || Dungeon.level.distance(m.pos, pos) < Dungeon.level.distance(best.pos, pos)) best = m;
		}
		if (best != null) leader = best.id();
	}

	//only when someone is near enough to hear it, and too far to see it
	private void call(){
		Hero heard = null;
		for (Hero h : Party.living()){
			int d = Dungeon.level.distance(h.pos, pos);
			if (d > Continent.vision(h) + 1 && d <= 22){
				heard = h;
				break;
			}
		}
		if (heard == null){
			quiet = 3;
			return;
		}
		quiet = 14 + Random.Int(10);
		boolean ping = Random.Int(3) == 0;
		int line = Random.Int(Party.active() ? CALLS : SOLO_CALLS);
		if (sprite instanceof BirdSprite) ((BirdSprite) sprite).mimic();
		voice(pos, ping, line);
	}

	//what each device hears: the voice of somebody else from the group, so never your own.
	//only words and a mark, nothing the game reads
	private static void voice( int cell, boolean ping, int line ){
		Hero me = Dungeon.local();
		Hero other = null;
		if (Party.active()){
			for (Hero h : Party.living()){
				if (h != me){
					other = h;
					break;
				}
			}
		}
		if (other == null){
			GLog.i(Messages.get(MimicBird.class, "solo_" + line));
			return;
		}
		String who = Party.name(other);
		if (ping){
			CoopPings.show(cell, CoopPings.COME, who, CoopPings.floorKey());
		} else {
			GLog.p(who + ": " + Messages.get(MimicBird.class, "call_" + line));
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("quiet", quiet);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		quiet = bundle.getInt("quiet");
	}

	//12-13 the mouth in its throat talking, 14-15 flying
	public static class BirdSprite extends ContinentMobSprite {

		private Animation talk;

		public BirdSprite(){
			super();
			setup(FaunaAssets.BIRD, 16, 16);
			run = loop(10, 14, 15);
			talk = new Animation(5, false);
			talk.frames(frames, 12, 13, 12, 13, 12, 0);
		}

		public void mimic(){
			if (curAnim == null || curAnim == idle) play(talk);
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == talk){
				idle();
				return;
			}
			super.onComplete(anim);
		}
	}
}
