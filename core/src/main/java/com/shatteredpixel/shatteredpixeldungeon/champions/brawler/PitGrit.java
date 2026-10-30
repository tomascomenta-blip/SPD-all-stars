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

package com.shatteredpixel.shatteredpixeldungeon.champions.brawler;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//brawler passive: grit fills from damage taken (Haymaker spends it), every 2nd punch hits harder,
//regen goes up the more hurt he is
public class PitGrit extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public float grit = 0;
	public int calm = 0;       //turns w/o getting hit
	public int punches = 0;
	public float regen = 0;

	public static PitGrit of( Hero hero ){
		return hero.buff(PitGrit.class);
	}

	public static float maxGrit( Hero hero ){
		float m = hero.HT * 0.3f;
		if (hero.subClass == HeroSubClass.PITBOSS) m *= 1.5f;
		return m;
	}

	public static int graceTurns( Hero hero ){
		return 3 + 2*hero.pointsInTalent(Talent.BRA_STUBBORN);
	}

	public void addGrit( float amount ){
		if (!(target instanceof Hero)) return;
		Hero hero = (Hero) target;
		amount *= 1f + 0.1f*hero.pointsInTalent(Talent.PB_THICK_HIDE);
		grit = Math.min(maxGrit(hero), grit + amount);
		calm = 0;
		BuffIndicator.refreshHero();
	}

	public boolean highGrit(){
		return target instanceof Hero && grit >= maxGrit((Hero) target) / 2f;
	}

	//left right LEFT right
	public int onPunch( Hero hero, Char enemy, int damage ){
		punches++;
		if (punches % 2 == 0){
			damage = Math.round(damage * (1.25f + 0.1f*hero.pointsInTalent(Talent.BRA_ONE_TWO)));
		}
		if (hero.subClass == HeroSubClass.PITBOSS && highGrit()){
			damage += 2;
		}
		return damage;
	}

	@Override
	public boolean act() {
		if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.BRAWLER){
			detach();
			return true;
		}
		Hero hero = (Hero) target;

		calm++;
		if (calm > graceTurns(hero) && grit > 0 && hero.buff(Ovation.Roar.class) == null){
			grit = Math.max(0, grit - maxGrit(hero) * 0.15f);
		}

		//more hurt = more regen
		if (hero.isAlive() && hero.HP < hero.HT && Regeneration.regenOn() && !hero.isStarving()){
			float missing = 1f - hero.HP / (float) hero.HT;
			regen += missing * 0.2f * (1f + 0.5f*hero.pointsInTalent(Talent.BRA_SECOND_WIND));
			if (regen >= 1f){
				int heal = (int) regen;
				regen -= heal;
				hero.HP = Math.min(hero.HT, hero.HP + heal);
			}
		} else {
			regen = 0;
		}

		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.FURY;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(1f, 0.75f, 0.2f);
	}

	@Override
	public float iconFadePercent() {
		if (!(target instanceof Hero)) return 1;
		return 1f - Math.min(1f, grit / Math.max(1f, maxGrit((Hero) target)));
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(Math.round(grit));
	}

	@Override
	public String name() {
		return Messages.get(PitGrit.class, "name");
	}

	@Override
	public String desc() {
		int max = target instanceof Hero ? Math.round(maxGrit((Hero) target)) : 0;
		return Messages.get(PitGrit.class, "desc", Math.round(grit), max);
	}

	private static final String GRIT = "grit";
	private static final String CALM = "calm";
	private static final String PUNCHES = "punches";
	private static final String REGEN = "regen";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(GRIT, grit);
		bundle.put(CALM, calm);
		bundle.put(PUNCHES, punches);
		bundle.put(REGEN, regen);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		grit = bundle.getFloat(GRIT);
		calm = bundle.getInt(CALM);
		punches = bundle.getInt(PUNCHES);
		regen = bundle.getFloat(REGEN);
	}
}
