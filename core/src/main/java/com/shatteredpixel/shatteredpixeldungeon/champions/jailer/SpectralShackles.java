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

package com.shatteredpixel.shatteredpixeldungeon.champions.jailer;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//armor ability: shackles on everything in sight (root, bosses only cripple). shield for the first 3, may grab souls
public class SpectralShackles extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static int radius( Hero hero ){
		return 6 + hero.pointsInTalent(Talent.SH_LONG_CHAINS);
	}

	public static int holdTurns( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.SH_HEAVY_LINKS);
	}

	public static int shieldEach( Hero hero ){
		return 2 + hero.lvl / 4;
	}

	public static int soulCap( Hero hero ){
		return hero.pointsInTalent(Talent.SH_CAPTIVE_SOULS);
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		ArrayList<Char> victims = new ArrayList<>();
		for (Char ch : ChampionHooks.enemiesAround(hero, hero.pos, radius(hero))){
			if (Dungeon.level.heroFOV[ch.pos]) victims.add(ch);
		}
		if (victims.isEmpty()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		int turns = holdTurns(hero);
		int shield = 0;
		int count = 0;
		for (Char ch : victims){
			if (Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)){
				Buff.prolong(ch, Cripple.class, turns);
			} else {
				Buff.prolong(ch, Roots.class, turns);
			}
			if (count < 3) shield += shieldEach(hero);
			count++;
			if (ch.sprite != null && ch.sprite.visible){
				CellEmitter.get(ch.pos).burst(TheBox.SoulParticle.FACTORY, 6);
			}
		}
		Barrier b = Buff.affect(hero, Barrier.class);
		b.setShield(Math.max(b.shielding(), shield));
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
		int souls = Math.min(count, soulCap(hero));
		if (souls > 0) Damnation.gain(hero, souls);

		Sample.INSTANCE.play(Assets.Sounds.CHAINS, 1f, 0.8f);
		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.SPECTRAL_SHACKLES;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SH_LONG_CHAINS, Talent.SH_HEAVY_LINKS, Talent.SH_CAPTIVE_SOULS, Talent.HEROIC_ENERGY};
	}
}
