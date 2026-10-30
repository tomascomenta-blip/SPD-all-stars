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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.gunslinger;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

//armor ability: lead rain on an area, everyone in it gets hit
public class LeadRain extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static final int RANGE = 8;

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	public static int radius( Hero hero ){
		return 2 + hero.pointsInTalent(Talent.LR_WIDE)/2;
	}

	public static int minDmg( Hero hero ){
		return Math.round((6 + hero.lvl/2) * (1f + 0.15f*hero.pointsInTalent(Talent.LR_HEAVY)));
	}

	public static int maxDmg( Hero hero ){
		return Math.round((12 + hero.lvl) * (1f + 0.15f*hero.pointsInTalent(Talent.LR_HEAVY)));
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target] || Dungeon.level.solid[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		ArrayList<Integer> area = SKCombat.openArea(target, radius(hero));
		for (int c : area){
			if (Dungeon.level.heroFOV[c] && Random.Visual.Int(2) == 0) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 2);
		}
		int stun = hero.pointsInTalent(Talent.LR_STUNNING);
		for (Char ch : SKCombat.foesIn(hero, area)){
			GunSpin.shoot(hero, ch, minDmg(hero), maxDmg(hero), this);
			if (ch.isAlive() && stun > 0 && Random.Int(4) < stun) Buff.prolong(ch, Paralysis.class, 1f);
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW, 1f, 0.8f);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.5f, 1.8f);

		ChampionHooks.operateVisual(hero, target);
		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc", RANGE);
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.GUNSLINGER){
			Hero h = Dungeon.hero;
			d += " " + Messages.get(this, "desc_numbers", radius(h), minDmg(h), maxDmg(h));
		}
		return d + "\n\n" + Messages.get(this, "cost", (int) baseChargeUse);
	}

	@Override
	public int icon() {
		return HeroIcon.GS_LEAD_RAIN;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.LR_WIDE, Talent.LR_HEAVY, Talent.LR_STUNNING, Talent.HEROIC_ENERGY};
	}
}
