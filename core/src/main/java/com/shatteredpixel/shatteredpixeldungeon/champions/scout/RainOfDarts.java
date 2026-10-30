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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAreas;
import com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger.GravediggerHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//armor ability: 5x5 dart rain. dmg, poison, blind
public class RainOfDarts extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static final int RANGE = 8;
	public static final int RADIUS = 2;

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos( Char user, int dst ) {
		return new Ballistica(user.pos, dst, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET).collisionPos;
	}

	public static int minDmg( Hero hero ){
		return Math.round((3 + hero.lvl/3) * (1f + 0.2f*hero.pointsInTalent(Talent.SCO_ENDLESS_QUIVER)));
	}

	public static int maxDmg( Hero hero ){
		return Math.round((6 + Math.round(hero.lvl*0.9f)) * (1f + 0.2f*hero.pointsInTalent(Talent.SCO_ENDLESS_QUIVER)));
	}

	public static int poisonTurns( Hero hero ){
		return 4 + hero.lvl/4 + 2*hero.pointsInTalent(Talent.SCO_DEEP_VENOM);
	}

	public static int blindTurns( Hero hero ){
		return 2 + hero.pointsInTalent(Talent.SCO_BLIND_STORM);
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		hero.busy();
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		ArrayList<Integer> area = ChampionAreas.cells(target, RADIUS);
		for (int c : area){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(PoisonParticle.SPLASH, 4);
		}
		Sample.INSTANCE.play(Assets.Sounds.ATK_SPIRITBOW, 1f, 1.3f);
		Sample.INSTANCE.play(Assets.Sounds.GAS, 0.7f, 1.2f);
		PixelScene.shake(1, 0.4f);
		GravediggerHooks.castVisual(hero, target);

		for (Char ch : ChampionAreas.enemiesIn(hero, area)){
			ch.damage(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)), this);
			if (ch.isAlive()){
				Buff.affect(ch, Poison.class).set(poisonTurns(hero));
				Buff.prolong(ch, Blindness.class, blindTurns(hero));
			}
		}

		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc", RADIUS*2 + 1, RANGE);
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.SCOUT){
			Hero h = Dungeon.hero;
			d += " " + Messages.get(this, "desc_numbers", minDmg(h), maxDmg(h), poisonTurns(h), blindTurns(h));
		}
		return d + "\n\n" + Messages.get(this, "cost", (int) baseChargeUse);
	}

	@Override
	public int icon() {
		return HeroIcon.SCOUT_RAIN;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SCO_DEEP_VENOM, Talent.SCO_BLIND_STORM, Talent.SCO_ENDLESS_QUIVER, Talent.HEROIC_ENERGY};
	}
}
