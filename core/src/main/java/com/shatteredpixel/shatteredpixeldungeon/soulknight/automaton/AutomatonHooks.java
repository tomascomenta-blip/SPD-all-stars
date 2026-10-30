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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.automaton;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

//automaton hooks.
//passive "armored chassis": armor scaling with level + half dmg from the new-world bullets
public class AutomatonHooks {

	public static void initHero( Hero hero ){
		(hero.belongings.weapon = new Wrench()).identify();
		hero.belongings.weapon.activate(hero);

		AutomatonKit kit = new AutomatonKit();
		kit.identify().collect();
		SatelliteCannon cannon = new SatelliteCannon();
		cannon.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);
		Dungeon.quickslot.setSlot(1, cannon);

		new PotionOfHealing().identify();
		new ScrollOfRecharging().identify();
	}

	public static void ensurePassives( Hero hero ){
	}

	public static boolean is( Char ch ){
		return ch instanceof Hero && ((Hero) ch).heroClass == HeroClass.AUTOMATON;
	}

	public static int drBonus( Hero hero ){
		int top = 1 + hero.lvl/6 + hero.pointsInTalent(Talent.AUT_PLATING);
		return Random.NormalIntRange(0, top);
	}

	public static int bulletDamage( Char ch, int dmg ){
		return is(ch) ? Math.max(1, dmg/2) : dmg;
	}

	//overclock
	public static float cooldownFactor( Hero hero ){
		return 1f - 0.1f*hero.pointsInTalent(Talent.AUT_OVERCLOCK);
	}

	public static void afterAbility( Hero hero ){
		if (hero.hasTalent(Talent.AUT_SELF_REPAIR)){
			ChampionHooks.heal(hero, 1 + hero.pointsInTalent(Talent.AUT_SELF_REPAIR));
		}
	}

	public static void onFoodEaten( Hero hero ){
		if (!hero.hasTalent(Talent.AUT_COOLANT)) return;
		SatelliteCannon cannon = hero.belongings.getItem(SatelliteCannon.class);
		if (cannon != null){
			cannon.gainCharge(hero.pointsInTalent(Talent.AUT_COOLANT), true);
			Item.updateQuickslot();
		}
	}

	public static void onPotionUsed( Hero hero, float factor ){
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		if (hero.heroClass == HeroClass.AUTOMATON && (item instanceof Armor || item instanceof Wand)){
			return 1f + 0.75f*hero.pointsInTalent(Talent.AUT_DIAGNOSTICS);
		}
		return 1f;
	}

	//failsafe: free EMP at low hp (has its own cd, don't worry)
	public static void onHeroDamaged( Hero hero, int effectiveDamage, Object src ){
		int p = hero.pointsInTalent(Talent.AUT_FAILSAFE);
		if (p <= 0 || !hero.isAlive() || hero.HP > hero.HT*0.3f) return;
		if (hero.buff(FailsafeCooldown.class) != null) return;
		Buff.affect(hero, FailsafeCooldown.class, 200 - 50*p);
		if (hero.sprite != null) hero.sprite.showStatus(CharSprite.WARNING, Messages.get(AutomatonHooks.class, "failsafe"));
		EMP.discharge(hero);
	}

	public static class FailsafeCooldown extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
			revivePersists = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.4f, 0.8f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, visualcooldown() / 150f);
		}
	}
}
