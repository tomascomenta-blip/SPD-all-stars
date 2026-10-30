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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

//armor ability, big group roar. enemies stunned (bosses just weakened), allies healed, beasts go feral
public class PackRoar extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static int radius( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.PR_THUNDEROUS);
	}

	public static int healPercent( Hero hero ){
		return 20 + 10*hero.pointsInTalent(Talent.PR_RALLY);
	}

	public static int fury( Hero hero ){
		return 5 + 3*hero.pointsInTalent(Talent.PR_FEROCITY);
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int r = radius(hero);
		for (Char ch : Actor.chars().toArray(new Char[0])){
			if (!ch.isAlive() || Dungeon.level.distance(hero.pos, ch.pos) > r) continue;
			if (ch.alignment == Char.Alignment.ENEMY){
				if (Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)){
					Buff.prolong(ch, Weakness.class, 5f);
				} else {
					Buff.prolong(ch, Paralysis.class, 2f);
				}
			} else if (ch.alignment == Char.Alignment.ALLY){
				ChampionHooks.heal(ch, Math.round(ch.HT * healPercent(hero) / 100f));
				if (ch instanceof Wolf || ch instanceof FuzzyBear.Bear){
					Buff.affect(ch, Ferocity.class, fury(hero));
				}
			}
		}
		if (hero.sprite != null) hero.sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.3f, 3);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
		PixelScene.shake(2, 0.5f);

		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc");
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.BEASTMASTER){
			Hero h = Dungeon.hero;
			d += " " + Messages.get(this, "desc_numbers", radius(h), healPercent(h), fury(h));
		}
		return d + "\n\n" + Messages.get(this, "cost", (int) baseChargeUse);
	}

	@Override
	public int icon() {
		return HeroIcon.BM_ROAR;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.PR_THUNDEROUS, Talent.PR_RALLY, Talent.PR_FEROCITY, Talent.HEROIC_ENERGY};
	}

	//+dmg
	public static class Ferocity extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.FURY;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.5f, 0.2f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
