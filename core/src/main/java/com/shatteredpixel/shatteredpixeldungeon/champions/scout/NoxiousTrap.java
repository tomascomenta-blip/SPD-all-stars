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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//R: plant a shroom. charges instead of a cd
public class NoxiousTrap extends ChampionAbility {

	public static final NoxiousTrap INSTANCE = new NoxiousTrap();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.LOL_NOXIOUS_TRAP; }
	@Override public int color() { return 0x9944CC; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return null; }
	@Override public boolean targeted() { return true; }

	@Override
	public float cooldown( Hero hero ) {
		return 0;
	}

	public static int range( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.TR_LONG_THROW);
	}

	public static int maxCharges( Hero hero ){
		int max = 3 + hero.pointsInTalent(Talent.SCO_MORE_SHROOMS);
		if (hero.subClass == HeroSubClass.TRAPPER) max += 2;
		return max;
	}

	public static float turnsPerCharge( Hero hero ){
		return hero.subClass == HeroSubClass.TRAPPER ? 12f : 25f;
	}

	@Override
	public boolean ready( Hero hero ) {
		Charges c = hero.buff(Charges.class);
		return c != null && c.charges >= 1;
	}

	@Override
	public String notReadyText( Hero hero ) {
		return Messages.get(this, "no_charges");
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > range(hero) || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		if (!NoxiousShroom.canPlantAt(target)){
			GLog.w(Messages.get(this, "bad_cell"));
			return;
		}
		Charges c = hero.buff(Charges.class);
		if (c == null || c.charges < 1) return;
		c.charges--;

		NoxiousShroom.plantFor(hero, target);
		ChampionHooks.operateVisual(hero, target);
		if (Dungeon.level.heroFOV[target]){
			CellEmitter.get(target).burst(LeafParticle.GENERAL, 6);
		}
		Sample.INSTANCE.play(Assets.Sounds.PLANT);
		kit.updateQuickslot();
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		Charges c = hero.buff(Charges.class);
		return Messages.get(this, "desc", range(hero), c == null ? 0 : c.charges, maxCharges(hero),
				Math.round(turnsPerCharge(hero)));
	}

	//charges buff, always on
	public static class Charges extends Buff {

		{
			revivePersists = true;
		}

		public int charges = 1;
		public float partial = 0;

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.SCOUT){
				detach();
				return true;
			}
			Hero hero = (Hero) target;
			if (hero.lvl >= INSTANCE.unlockLevel() && charges < maxCharges(hero)){
				partial += 1f / turnsPerCharge(hero);
				if (partial >= 1f){
					partial = 0;
					charges++;
				}
			} else if (charges >= maxCharges(hero)){
				partial = 0;
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			if (!(target instanceof Hero) || ((Hero) target).lvl < INSTANCE.unlockLevel()) return BuffIndicator.NONE;
			return BuffIndicator.POISON;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.7f, 0.3f, 0.9f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(charges);
		}

		@Override
		public float iconFadePercent() {
			if (!(target instanceof Hero)) return 0;
			return charges >= maxCharges((Hero) target) ? 0 : 1f - partial;
		}

		@Override
		public String name() {
			return Messages.get(Charges.class, "name");
		}

		@Override
		public String desc() {
			int max = target instanceof Hero ? maxCharges((Hero) target) : 3;
			return Messages.get(Charges.class, "desc", charges, max);
		}

		private static final String CHARGES = "charges";
		private static final String PARTIAL = "partial";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(CHARGES, charges);
			bundle.put(PARTIAL, partial);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			charges = bundle.getInt(CHARGES);
			partial = bundle.getFloat(PARTIAL);
		}
	}
}
