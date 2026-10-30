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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Mace;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//the pendulum of the colossus on a chain. heavy (one more strength than the other tier 6 piece),
//hits harder, and one swing in three slows down whatever it hits
public class Pendulum extends MeleeWeapon {

	public static final float SLOW = 3f;

	{
		image = ItemSpriteSheet.PENDULUM;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.8f;

		tier = 6;
		unique = true;
		bones = false;
	}

	@Override
	public int max( int lvl ) {
		return  5*(tier+3) +    //45 base, up from 35
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int STRReq( int lvl ) {
		return super.STRReq(lvl) + 1;
	}

	@Override
	public int proc( Char attacker, Char defender, int damage ) {
		if (defender.isAlive() && Random.Int(3) == 0){
			Buff.affect(defender, Slow.class, SLOW);
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility( Hero hero, Integer target ) {
		//+(7+1.5*lvl), like the war hammer
		int dmgBoost = augment.damageFactor(7 + Math.round(1.5f*buffedLvl()));
		Mace.heavyBlowAbility(hero, target, 1, dmgBoost, this);
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = levelKnown ? 7 + Math.round(1.5f*buffedLvl()) : 7;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+dmgBoost), augment.damageFactor(max()+dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+dmgBoost, max(0)+dmgBoost);
		}
	}

	@Override
	public String upgradeAbilityStat( int level ){
		int dmgBoost = 7 + Math.round(1.5f*level);
		return augment.damageFactor(min(level)+dmgBoost) + "-" + augment.damageFactor(max(level)+dmgBoost);
	}

	@Override
	public String desc() {
		return super.desc() + "\n\n" + Messages.get(this, "swing", Math.round(SLOW));
	}
}
