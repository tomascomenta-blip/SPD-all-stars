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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//W: slow + weaken a nearby enemy
public class Wither extends ChampionAbility {

	public static final Wither INSTANCE = new Wither();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.LOL_WITHER; }
	@Override public int color() { return 0x9966CC; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 5;

	@Override
	public float cooldown( Hero hero ) {
		return 18;
	}

	public static int duration( Hero hero ){
		return 4 + hero.lvl/8 + 2*hero.pointsInTalent(Talent.GUA_LONG_WITHER);
	}

	//also used by colossus hits during the fury
	public static void wither( Hero hero, Char ch, float turns, boolean hex ){
		if (!ch.isAlive()) return;
		if (Char.hasProp(ch, Char.Property.BOSS)) turns = (float) Math.ceil(turns / 2f);
		Buff.prolong(ch, Slow.class, turns);
		Buff.prolong(ch, Weakness.class, turns);
		if (hex) Buff.prolong(ch, Hex.class, turns);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Char enemy = target == null ? null : Actor.findChar(target);
		if (enemy == null || !CoopRules.hits(hero, enemy) || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, enemy.pos) > RANGE){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}

		wither(hero, enemy, duration(hero), hero.pointsInTalent(Talent.GUA_LONG_WITHER) >= 2);
		if (enemy.sprite != null){
			enemy.sprite.emitter().burst(ShadowParticle.UP, 12);
			enemy.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "withered"));
		}
		//visual only
		if (hero.sprite != null) hero.sprite.zap(enemy.pos, new Callback() {
			@Override
			public void call() {
				if (hero.sprite != null) hero.sprite.idle();
			}
		});
		Sample.INSTANCE.play(Assets.Sounds.DEBUFF, 1f, 0.8f);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", RANGE, duration(hero));
		if (hero.pointsInTalent(Talent.GUA_LONG_WITHER) >= 2) d += " " + Messages.get(this, "desc_hex");
		return d;
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
