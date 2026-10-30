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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//the minute hand of the colossus, sharpened into a sword. tier 6, plain numbers, and every
//4th hit that lands "strikes the hour" for +50%
public class MinuteHand extends MeleeWeapon {

	public static final int HOUR = 4;

	{
		image = ItemSpriteSheet.MINUTE_HAND;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.85f;

		tier = 6;
		unique = true;
		bones = false;
	}

	private int ticks = 0;

	@Override
	public int proc( Char attacker, Char defender, int damage ) {
		ticks++;
		if (ticks >= HOUR){
			ticks = 0;
			damage = Math.round(damage * 1.5f);
			if (defender.sprite != null){
				defender.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "hour"));
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.8f);
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected int baseChargeUse( Hero hero, Char target ){
		return hero.buff(Sword.CleaveTracker.class) != null ? 0 : 1;
	}

	@Override
	protected void duelistAbility( Hero hero, Integer target ) {
		//+(8+lvl), a bit more than the greatsword
		int dmgBoost = augment.damageFactor(8 + buffedLvl());
		Sword.cleaveAbility(hero, target, 1, dmgBoost, this);
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = levelKnown ? 8 + buffedLvl() : 8;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+dmgBoost), augment.damageFactor(max()+dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+dmgBoost, max(0)+dmgBoost);
		}
	}

	@Override
	public String upgradeAbilityStat( int level ){
		int dmgBoost = 8 + level;
		return augment.damageFactor(min(level)+dmgBoost) + "-" + augment.damageFactor(max(level)+dmgBoost);
	}

	@Override
	public String desc() {
		return super.desc() + "\n\n" + Messages.get(this, "ticks", HOUR - ticks);
	}

	private static final String TICKS = "ticks";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(TICKS, ticks);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ticks = bundle.getInt(TICKS);
	}
}
