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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//E: heal everyone the party can see + 1 debuff off each
public class Hymn extends BardSong {

	public static final Hymn INSTANCE = new Hymn();

	//dissonance range
	public static final int DISSONANCE_RANGE = 3;
	//song of life HoT duration
	public static final int LIFE_SONG_TURNS = 10;

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.BARD_HYMN; }
	@Override public int color() { return 0x55FF88; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		float cd = 25;
		if (hero.subClass == HeroSubClass.TROUBADOUR) cd *= 0.7f;
		return cd;
	}

	public static float healFactor( Hero hero ){
		float f = 1f + 0.25f*hero.pointsInTalent(Talent.BAR_SOOTHING);
		if (hero.subClass == HeroSubClass.TROUBADOUR) f *= 1.2f;
		return f;
	}

	//15% max hp + bard lvl
	public static int heal( Hero hero, Char ch, float power ){
		return Math.round((0.15f*ch.HT + hero.lvl) * healFactor(hero) * power);
	}

	public static int lifeSong( Hero hero ){
		if (hero.subClass != HeroSubClass.TROUBADOUR) return 0;
		return hero.pointsInTalent(Talent.TB_LIFE_SONG) * (1 + hero.lvl/3);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		boolean forte = Refrain.onSong(hero);
		ArrayList<Char> friends = friendsInView(hero);
		float power = power(hero, forte, duet(friends));
		int regen = Math.round(lifeSong(hero) * power);

		for (Char ch : friends){
			ChampionHooks.heal(ch, heal(hero, ch, power));
			cleanse(ch, 1);
			harmony(hero, ch);
			if (regen > 0){
				Buff.affect(ch, Healing.class).setHeal(regen, 0f, (regen + LIFE_SONG_TURNS - 1) / LIFE_SONG_TURNS);
			}
			if (ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
			notes(ch);
		}
		dissonance(hero, enemiesInView(hero.pos, DISSONANCE_RANGE), this);

		Sample.INSTANCE.play(Assets.Sounds.DRINK, 0.6f, 1.3f);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS, 0.8f, 0.9f);
		gesture(hero, hero.pos, false);
		finish(hero, friends.size());
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", heal(hero, hero, 1f), Math.round(15*healFactor(hero)), Math.round(hero.lvl*healFactor(hero)));
		if (lifeSong(hero) > 0) d += " " + Messages.get(this, "desc_life", lifeSong(hero), LIFE_SONG_TURNS);
		if (dissonanceDamage(hero) > 0) d += " " + Messages.get(this, "desc_dissonance", dissonanceDamage(hero), DISSONANCE_RANGE);
		return d + forteDesc(hero, fortePercent(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
