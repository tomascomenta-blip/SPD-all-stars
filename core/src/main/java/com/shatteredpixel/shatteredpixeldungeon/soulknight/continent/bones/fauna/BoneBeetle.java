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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna;

import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshCritter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.CritterSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//bone beetle: a domed shell of old porous bone, legs of dark tendon and a yellow eye. hurt badly, it
//pulls the legs in and shuts for a few turns: then almost nothing gets through the shell
public class BoneBeetle extends AshCritter {

	{
		spriteClass = BeetleSprite.class;

		HP = HT = 90;
		defenseSkill = 16;
		EXP = 6;
		baseSpeed = 0.9f;

		minDmg = 16; maxDmg = 24; acc = 56;
		armorMin = 8; armorMax = 16;
	}

	private int shut = 0;
	private boolean shutOnce = false;

	@Override
	protected boolean act() {
		if (shut > 0){
			if (--shut == 0 && sprite instanceof BeetleSprite) ((BeetleSprite) sprite).shut(false);
			spend(TICK);
			return true;
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage(dmg, src);
		if (isAlive() && !shutOnce && HP < HT / 2){
			shutOnce = true;
			shut = 4;
			if (sprite instanceof BeetleSprite) ((BeetleSprite) sprite).shut(true);
		}
	}

	@Override
	public int drRoll() {
		return super.drRoll() + (shut > 0 ? Random.NormalIntRange(25, 40) : 0);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("shut", shut);
		bundle.put("shut_once", shutOnce);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		shut = bundle.getInt("shut");
		shutOnce = bundle.getBoolean("shut_once");
	}

	//0-1 idle, 2-5 walk, 6-7 the bite, 8 shut tight
	public static class BeetleSprite extends CritterSprite {

		public void shut( boolean on ){
			if (on) hold(1, 8);
			else hold(2, 0, 0, 1, 0, 1, 1);
		}

		public BeetleSprite(){
			super();
			critter(BonesAssets.BEETLE);
		}
	}
}
