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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

//R: skeletons out of the ground around him, nearby enemies get terrified. spooky scary
public class DanseMacabre extends ChampionAbility {

	public static final DanseMacabre INSTANCE = new DanseMacabre();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.SP_DANSE; }
	@Override public int color() { return 0xDDDDBB; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final int SKELETONS = 3;
	public static final int FEAR_RANGE = 3;

	@Override
	public float cooldown( Hero hero ) {
		return 80;
	}

	public static int duration( Hero hero ){
		return 20 + 5*hero.pointsInTalent(Talent.SP_BONE_LEGION);
	}

	public static float multi( Hero hero ){
		return 1f + 0.1f*hero.pointsInTalent(Talent.SP_BONE_LEGION);
	}

	public static int fear( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.SP_DREAD_HOWL);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		int made = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			if (made >= SKELETONS) break;
			int c = hero.pos + n;
			if (Actor.findChar(c) != null || Dungeon.level.solid[c] || !Dungeon.level.passable[c] || Dungeon.level.pit[c]) continue;
			Skeleton.spawn(hero, c, duration(hero), multi(hero));
			made++;
		}
		for (Char ch : ChampionHooks.enemiesAround(hero.pos, FEAR_RANGE)){
			if (!Dungeon.level.heroFOV[ch.pos]) continue;
			Buff.affect(ch, Terror.class, fear(hero)).object = hero.id();
		}
		if (hero.sprite != null) hero.sprite.emitter().burst(ShadowParticle.CURSE, 14);
		Sample.INSTANCE.play(Assets.Sounds.BONES);
		Sample.INSTANCE.play(Assets.Sounds.CURSED, 0.6f, 0.8f);
		Dungeon.observe();

		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", SKELETONS, duration(hero), FEAR_RANGE, fear(hero),
				Math.round(Skeleton.minDmg(hero.lvl)*multi(hero)), Math.round(Skeleton.maxDmg(hero.lvl)*multi(hero)));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
