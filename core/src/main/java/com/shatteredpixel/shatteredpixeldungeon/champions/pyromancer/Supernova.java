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

package com.shatteredpixel.shatteredpixeldungeon.champions.pyromancer;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAreas;
import com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger.GravediggerHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//armor ability: fire nova around her. enemies burn, friends are fine, she keeps an ember shield
public class Supernova extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static final int RADIUS = 2;

	public static int minDmg( Hero hero ){
		return Math.round((4 + hero.lvl/3) * (1f + 0.2f*hero.pointsInTalent(Talent.PYR_BLAZING_CORE)));
	}

	public static int maxDmg( Hero hero ){
		return Math.round((8 + Math.round(hero.lvl*1.1f)) * (1f + 0.2f*hero.pointsInTalent(Talent.PYR_BLAZING_CORE)));
	}

	public static int shield( Hero hero ){
		return hero.pointsInTalent(Talent.PYR_AFTERGLOW) * (2 + hero.lvl/3);
	}

	public static int crippleTurns( Hero hero ){
		return 2 * hero.pointsInTalent(Talent.PYR_BLAST_WAVE);
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		hero.busy();
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		final boolean stun = hero.heroClass == HeroClass.PYROMANCER && Pyromania.onOffensiveCast(hero);
		ArrayList<Integer> area = ChampionAreas.cells(hero.pos, RADIUS);
		for (int c : area){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(FlameParticle.FACTORY, 5);
		}
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.8f);
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 1f);
		PixelScene.shake(2, 0.6f);
		GravediggerHooks.castVisual(hero, hero.pos);

		for (Char ch : ChampionAreas.enemiesIn(hero, area)){
			ch.damage(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)), this);
			if (ch.isAlive()){
				Buff.affect(ch, Burning.class).reignite(ch);
				if (crippleTurns(hero) > 0) Buff.prolong(ch, Cripple.class, crippleTurns(hero));
				if (stun) Pyromania.stun(hero, ch);
			}
		}
		int sh = shield(hero);
		if (sh > 0){
			Buff.affect(hero, Barrier.class).setShield(sh);
			if (hero.sprite != null) hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(sh), FloatingText.SHIELDING);
		}

		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc", RADIUS);
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.PYROMANCER){
			Hero h = Dungeon.hero;
			d += " " + Messages.get(this, "desc_numbers", minDmg(h), maxDmg(h));
		}
		return d + "\n\n" + Messages.get(this, "cost", (int) baseChargeUse);
	}

	@Override
	public int icon() {
		return HeroIcon.PYRO_SUPERNOVA;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.PYR_BLAZING_CORE, Talent.PYR_AFTERGLOW, Talent.PYR_BLAST_WAVE, Talent.HEROIC_ENERGY};
	}
}
