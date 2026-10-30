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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

//armor ability: crowd goes wild. grit maxed + frozen for a while, punches hit harder and heal
public class Ovation extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static final int DAMAGE_PERCENT = 25;

	public static int duration( Hero hero ){
		return 8 + 2*hero.pointsInTalent(Talent.OV_ROAR);
	}

	public static int shield( Hero hero ){
		return Math.round(PitGrit.maxGrit(hero) * 0.2f * hero.pointsInTalent(Talent.OV_IRON_GRIT));
	}

	public static int healPercent( Hero hero ){
		return 15 + 5*hero.pointsInTalent(Talent.OV_BLOOD_SAND);
	}

	public static int proc( Hero hero, int damage ){
		if (hero.buff(Roar.class) == null) return damage;
		damage = Math.round(damage * (1f + DAMAGE_PERCENT / 100f));
		ChampionHooks.heal(hero, Math.round(damage * healPercent(hero) / 100f));
		return damage;
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		PitGrit grit = PitGrit.of(hero);
		if (grit != null){
			grit.grit = PitGrit.maxGrit(hero);
			grit.calm = 0;
		}
		Buff.prolong(hero, Roar.class, duration(hero));
		int s = shield(hero);
		if (s > 0){
			Barrier b = Buff.affect(hero, Barrier.class);
			b.setShield(Math.max(b.shielding(), s));
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(s), FloatingText.SHIELDING);
		}

		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 1.1f);
		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.OVATION;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.OV_ROAR, Talent.OV_IRON_GRIT, Talent.OV_BLOOD_SAND, Talent.HEROIC_ENERGY};
	}

	public static class Roar extends FlavourBuff {

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.FURY;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.8f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			float max = target instanceof Hero ? duration((Hero) target) : 8f;
			return Math.max(0, (max - visualcooldown()) / max);
		}

		@Override
		public String name() {
			return Messages.get(this, "name");
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", DAMAGE_PERCENT, target instanceof Hero ? healPercent((Hero) target) : 15, dispTurns());
		}
	}
}
