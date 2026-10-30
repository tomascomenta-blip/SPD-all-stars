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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//R: core overload. zaps + short stun on everything in sight, leftover juice -> shield
public class OmegaProtocol extends ChampionAbility {

	public static final OmegaProtocol INSTANCE = new OmegaProtocol();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.AUT_OMEGA; }
	@Override public int color() { return 0xAA66FF; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final int RANGE = 8;

	@Override
	public float cooldown( Hero hero ) {
		return 70 * AutomatonHooks.cooldownFactor(hero);
	}

	public static int damage( Hero hero ){
		int d = 6 + hero.lvl;
		if (hero.subClass == HeroSubClass.TESLA_COIL) d += 2;
		return d;
	}

	public static int shield( Hero hero ){
		return 5 + hero.lvl;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		ArrayList<Char> foes = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
					&& Dungeon.level.distance(hero.pos, ch.pos) <= RANGE && Dungeon.level.heroFOV[ch.pos]){
				foes.add(ch);
			}
		}
		if (foes.isEmpty()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		for (Char ch : foes){
			if (hero.sprite != null && ch.sprite != null) arcs.add(new Lightning.Arc(hero.sprite.center(), ch.sprite.center()));
			ch.damage(damage(hero), this);
			if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
		}
		if (hero.sprite != null && hero.sprite.parent != null){
			hero.sprite.parent.addToFront(new Lightning(arcs, null));
		}
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f, 1.4f);
		PixelScene.shake(2, 0.5f);

		int s = shield(hero);
		Buff.affect(hero, Barrier.class).setShield(s);
		if (hero.sprite != null) hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(s), FloatingText.SHIELDING);

		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		startCooldown(hero);
		AutomatonHooks.afterAbility(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, damage(hero), shield(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
