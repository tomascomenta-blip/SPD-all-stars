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

package com.shatteredpixel.shatteredpixeldungeon.champions.bard;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//bard passive: songs add notes, enough notes -> next song is Fortissimo and eats them
public class Refrain extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public int notes = 0;

	public static Refrain of( Hero hero ){
		return hero.buff(Refrain.class);
	}

	public static int needed( Hero hero ){
		return hero.pointsInTalent(Talent.BAR_VIRTUOSO) >= 3 ? 2 : 3;
	}

	//0.5 = +50%
	public static float fortePower( Hero hero ){
		return 0.5f + 0.25f*Math.min(2, hero.pointsInTalent(Talent.BAR_VIRTUOSO));
	}

	public boolean charged(){
		return target instanceof Hero && notes >= needed((Hero) target);
	}

	public static boolean charged( Hero hero ){
		Refrain r = of(hero);
		return r != null && r.charged();
	}

	public void gain( int amount ){
		if (!(target instanceof Hero) || amount <= 0) return;
		boolean was = charged();
		notes = Math.min(needed((Hero) target), notes + amount);
		if (!was && charged() && target.sprite != null){
			target.sprite.showStatus(0xFFDD44, Messages.get(Refrain.class, "charged"));
		}
		BuffIndicator.refreshHero();
	}

	//once per song. true = fortissimo (notes spent), false = +1 note
	public static boolean onSong( Hero hero ){
		Refrain r = of(hero);
		if (r == null) return false;
		if (r.charged()){
			r.notes = 0;
			if (hero.sprite != null) hero.sprite.showStatus(0xFFDD44, Messages.get(Refrain.class, "forte"));
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.8f, 1.5f);
			BuffIndicator.refreshHero();
			return true;
		}
		r.gain(1);
		return false;
	}

	@Override
	public boolean act() {
		if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.BARD){
			detach();
			return true;
		}
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.COMBO;
	}

	@Override
	public void tintIcon( Image icon ) {
		if (charged()) icon.hardlight(1f, 0.85f, 0.2f);
		else           icon.hardlight(0.7f, 0.55f, 1f);
	}

	@Override
	public float iconFadePercent() {
		if (!(target instanceof Hero)) return 0;
		return 1f - notes / (float) needed((Hero) target);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(notes);
	}

	@Override
	public String name() {
		return Messages.get(Refrain.class, "name");
	}

	@Override
	public String desc() {
		int need = target instanceof Hero ? needed((Hero) target) : 3;
		int power = target instanceof Hero ? Math.round(100*fortePower((Hero) target)) : 50;
		String d = Messages.get(Refrain.class, "desc", need, power, notes, need);
		if (charged()) d += "\n\n" + Messages.get(Refrain.class, "desc_charged");
		return d;
	}

	private static final String NOTES = "notes";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(NOTES, notes);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		notes = bundle.getInt(NOTES);
	}
}
