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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//R: it's high noon. shoots everything in sight, executes the low ones (not bosses), then duel mode
public class HighNoon extends ChampionAbility {

	public static final HighNoon INSTANCE = new HighNoon();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.GS_HIGH_NOON; }
	@Override public int color() { return 0xFFDD55; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final int RANGE = 8;

	@Override
	public float cooldown( Hero hero ) {
		return 80;
	}

	public static int minDmg( Hero hero ){
		return 8 + hero.lvl/2;
	}

	public static int maxDmg( Hero hero ){
		return 14 + hero.lvl;
	}

	public static float execute( Hero hero ){
		return 0.2f + 0.05f*hero.pointsInTalent(Talent.GS_EXECUTION);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		ArrayList<Char> foes = GunSpin.targets(hero, RANGE);
		if (foes.isEmpty()){
			GLog.w(Messages.get(GunSpin.class, "no_target"));
			return;
		}
		for (Char ch : foes){
			GunSpin.shoot(hero, ch, minDmg(hero), maxDmg(hero), this);
			if (ch.isAlive() && !Char.hasProp(ch, Char.Property.BOSS) && !Char.hasProp(ch, Char.Property.MINIBOSS)
					&& ch.HP <= ch.HT * execute(hero)){
				if (ch.sprite != null) ch.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "executed"));
				ch.damage(ch.HP + ch.shielding(), this);
			}
		}
		Buff.affect(hero, GunslingerHooks.Duel.class).set(5);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.7f, 1.6f);
		PixelScene.shake(1, 0.3f);

		ChampionHooks.operateVisual(hero, foes.get(0).pos);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, minDmg(hero), maxDmg(hero), Math.round(execute(hero)*100));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
