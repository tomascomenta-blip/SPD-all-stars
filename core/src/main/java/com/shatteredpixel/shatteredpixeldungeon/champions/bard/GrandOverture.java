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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//bard armor ability: everyone in sight charmed (bosses half), allies get haste + shield
public class GrandOverture extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static final int RANGE = 8;

	public static int charmTurns( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.BAR_ENCORE);
	}

	public static int hasteTurns( Hero hero ){
		return 3 + 2*hero.pointsInTalent(Talent.BAR_OVATION);
	}

	public static int shield( Hero hero ){
		return hero.pointsInTalent(Talent.BAR_ECHO) * (2 + hero.lvl/3);
	}

	//standing ovation: cd reduction for allies
	public static int refresh( Hero hero ){
		return 4*hero.pointsInTalent(Talent.BAR_OVATION);
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		ChampionHooks.operateVisual(hero, hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS, 1f, 0.8f);
		Sample.INSTANCE.play(Assets.Sounds.MASTERY, 1f, 1.1f);

		ArrayList<Char> enemies = new ArrayList<>();
		ArrayList<Hero> friends = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (!ch.isAlive() || !Dungeon.level.heroFOV[ch.pos] || Dungeon.level.distance(hero.pos, ch.pos) > RANGE) continue;
			if (ch.alignment == Char.Alignment.ENEMY){
				enemies.add(ch);
			} else if (ch instanceof Hero && ch != hero){
				friends.add((Hero) ch);
			}
		}
		friends.add(hero);

		for (Char ch : enemies){
			float turns = charmTurns(hero);
			if (Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)) turns = (float) Math.ceil(turns / 2f);
			Charm c = Buff.affect(ch, Charm.class, turns);
			c.object = hero.id();
			if (ch.sprite != null) ch.sprite.emitter().burst(Speck.factory(Speck.HEART), 4);
		}
		for (Hero friend : friends){
			Buff.prolong(friend, Haste.class, hasteTurns(hero));
			if (shield(hero) > 0){
				Barrier b = Buff.affect(friend, Barrier.class);
				if (b.shielding() < shield(hero)){
					b.setShield(shield(hero));
					if (friend.sprite != null) friend.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield(hero)), FloatingText.SHIELDING);
				}
			}
			if (refresh(hero) > 0) ChampionHooks.reduceCooldowns(friend, refresh(hero));
			if (friend.sprite != null) friend.sprite.emitter().burst(Speck.factory(Speck.NOTE), 5);
		}

		Invisibility.dispel(hero);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.BARD_OVERTURE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.BAR_ENCORE, Talent.BAR_ECHO, Talent.BAR_OVATION, Talent.HEROIC_ENERGY};
	}
}
