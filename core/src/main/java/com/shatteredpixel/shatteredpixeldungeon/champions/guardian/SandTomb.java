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

package com.shatteredpixel.shatteredpixeldungeon.champions.guardian;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//armor ability: 5x5 quicksand. dmg + root + weaken, guardian heals a bit per buried enemy
public class SandTomb extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static final int RANGE = 7;
	public static final int RADIUS = 2;

	public static int minDmg( Hero hero ){
		return Math.round((4 + hero.lvl/2) * (1f + 0.2f*hero.pointsInTalent(Talent.GUA_AGES)));
	}

	public static int maxDmg( Hero hero ){
		return Math.round((9 + hero.lvl) * (1f + 0.2f*hero.pointsInTalent(Talent.GUA_AGES)));
	}

	public static int rootTurns( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.GUA_DEEP_SANDS);
	}

	public static int weakTurns( Hero hero ){
		return 6 + 2*hero.pointsInTalent(Talent.GUA_DEEP_SANDS);
	}

	//% max hp per enemy, max 3
	public static int healPercent( Hero hero ){
		return 3*hero.pointsInTalent(Talent.GUA_THIRSTY_TOMB);
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos( Char user, int dst ) {
		return new Ballistica(user.pos, dst, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET).collisionPos;
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(this, "too_far"));
			return;
		}
		if (Dungeon.level.solid[target]){
			GLog.w(Messages.get(this, "bad_cell"));
			return;
		}
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		ChampionHooks.operateVisual(hero, target);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.8f);
		Sample.INSTANCE.play(Assets.Sounds.DEBUFF, 1f, 0.7f);
		PixelScene.shake(2, 0.5f);

		ArrayList<Char> buried = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (CoopRules.hits(hero, ch) && ch.isAlive() && Dungeon.level.distance(target, ch.pos) <= RADIUS){
				buried.add(ch);
			}
		}
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.heroFOV[i] && Dungeon.level.distance(target, i) <= RADIUS && !Dungeon.level.solid[i]){
				CellEmitter.get(i).burst(Speck.factory(Speck.DUST), 5);
			}
		}
		int caught = 0;
		for (Char ch : buried){
			ch.damage(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)), this);
			if (!ch.isAlive()) continue;
			Buff.prolong(ch, Roots.class, rootTurns(hero));
			Buff.prolong(ch, Weakness.class, weakTurns(hero));
			caught++;
		}
		if (healPercent(hero) > 0 && caught > 0){
			ChampionHooks.heal(hero, Math.round(hero.HT * healPercent(hero) / 100f * Math.min(3, caught)));
		}

		Invisibility.dispel(hero);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.GUARDIAN_SAND_TOMB;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.GUA_DEEP_SANDS, Talent.GUA_THIRSTY_TOMB, Talent.GUA_AGES, Talent.HEROIC_ENERGY};
	}
}
