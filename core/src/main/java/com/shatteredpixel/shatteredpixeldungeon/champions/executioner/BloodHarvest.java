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

package com.shatteredpixel.shatteredpixeldungeon.champions.executioner;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//armor ability: every bleeding enemy in sight takes all its remaining bleed right now
public class BloodHarvest extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static final int RANGE = 8;

	public static float damageFactor( Hero hero ){
		return 1f + 0.25f*hero.pointsInTalent(Talent.HV_DEEP_CUT);
	}

	public static float healShare( Hero hero ){
		return 0.1f*hero.pointsInTalent(Talent.HV_CRIMSON_FEAST);
	}

	public static int newStacks( Hero hero ){
		return hero.pointsInTalent(Talent.HV_RED_MARK);
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		ArrayList<Char> victims = new ArrayList<>();
		for (Char ch : ChampionHooks.enemiesAround(hero.pos, RANGE)){
			if (Dungeon.level.heroFOV[ch.pos] && ch.buff(Hemorrhage.class) != null) victims.add(ch);
		}
		if (victims.isEmpty()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int dealt = 0;
		for (Char ch : victims){
			Hemorrhage h = ch.buff(Hemorrhage.class);
			if (h == null || !ch.isAlive()) continue;
			int dmg = Math.max(1, Math.round(h.totalIncomingDMG() * damageFactor(hero)));
			h.detach();
			int before = ch.HP;
			ch.damage(dmg, hero);
			dealt += Math.max(0, before - Math.max(0, ch.HP));
			if (ch.sprite != null && ch.sprite.visible){
				CellEmitter.center(ch.pos).burst(BloodParticle.BURST, 10);
			}
			if (ch.isAlive() && newStacks(hero) > 0){
				Hemorrhage.apply(hero, ch, newStacks(hero));
			}
		}
		ChampionHooks.heal(hero, Math.round(dealt * healShare(hero)));

		Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 0.6f);
		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.BLOOD_HARVEST;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.HV_DEEP_CUT, Talent.HV_CRIMSON_FEAST, Talent.HV_RED_MARK, Talent.HEROIC_ENERGY};
	}
}
