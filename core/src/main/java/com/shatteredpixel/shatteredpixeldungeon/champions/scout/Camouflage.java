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

package com.shatteredpixel.shatteredpixeldungeon.champions.scout;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//invis while standing still. extends Invisibility so anything that dispels invis kills it too, for free
public class Camouflage extends Invisibility {

	public static final float LONG = 100000f;

	//talent: free steps
	public int freeSteps = 0;

	public static void apply( Hero hero ){
		if (hero.buff(Camouflage.class) != null) return;
		Camouflage c = Buff.affect(hero, Camouflage.class, LONG);
		c.freeSteps = hero.pointsInTalent(Talent.SK_SHADOW_STEP);
	}

	public static void onMove( Hero hero ){
		Camouflage c = hero.buff(Camouflage.class);
		if (c == null) return;
		if (c.freeSteps > 0){
			c.freeSteps--;
		} else {
			c.detach();
		}
	}

	@Override
	public void detach() {
		Char t = target;
		super.detach();
		if (t instanceof Hero && ((Hero) t).hasTalent(Talent.SCO_AMBUSH)){
			Buff.prolong(t, Ambush.class, 2f);
		}
		if (t instanceof Hero){
			GuerrillaTracker g = t.buff(GuerrillaTracker.class);
			if (g != null) g.still = 0;
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.INVISIBLE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0.4f, 1f, 0.3f);
	}

	@Override
	public float iconFadePercent() {
		return 0;
	}

	@Override
	public String iconTextDisplay() {
		return "";
	}

	@Override
	public String name() {
		return Messages.get(Camouflage.class, "name");
	}

	@Override
	public String desc() {
		return Messages.get(Camouflage.class, "desc");
	}

	private static final String FREE_STEPS = "free_steps";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(FREE_STEPS, freeSteps);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		freeSteps = bundle.getInt(FREE_STEPS);
	}

	//first hit out of camo bonus
	public static class Ambush extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.4f, 1f, 0.3f);
		}
	}

	//stand still a few turns -> camo
	public static class GuerrillaTracker extends Buff {

		{
			revivePersists = true;
		}

		public int lastPos = -1;
		public int still = 0;

		public static int turnsNeeded( Hero hero ){
			return Math.max(1, 3 - hero.pointsInTalent(Talent.SCO_QUICK_CAMO));
		}

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.SCOUT){
				detach();
				return true;
			}
			Hero hero = (Hero) target;
			if (hero.pos == lastPos && hero.isAlive()){
				still++;
			} else {
				still = 0;
				lastPos = hero.pos;
			}
			if (still >= turnsNeeded(hero) && hero.buff(Camouflage.class) == null){
				Camouflage.apply(hero);
			}
			spend(TICK);
			return true;
		}

		private static final String LAST_POS = "last_pos";
		private static final String STILL = "still";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LAST_POS, lastPos);
			bundle.put(STILL, still);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			lastPos = bundle.getInt(LAST_POS);
			still = bundle.getInt(STILL);
		}
	}
}
