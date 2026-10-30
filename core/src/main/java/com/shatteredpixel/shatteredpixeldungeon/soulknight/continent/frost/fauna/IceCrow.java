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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshCritter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.CritterSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

//ice crow. black feathers with frosted tips, a beak of pale ice, a red eye. they fly over everything
//and caw at whoever they find (and the moor listens); the beak opens wounds
public class IceCrow extends AshCritter {

	{
		spriteClass = CrowSprite.class;

		HP = HT = 55;
		defenseSkill = 40;
		EXP = 5;
		baseSpeed = 2f;
		flying = true;

		minDmg = 12; maxDmg = 18; acc = 60;
		armorMin = 0; armorMax = 3;
	}

	@Override
	protected int eyes() {
		return 4;
	}

	@Override
	public void notice() {
		super.notice();
		Senses.noise(pos, Senses.FIGHT);
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.ALERT, 0.5f, 1.9f);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.isAlive() && Random.Int(3) == 0) Buff.affect(enemy, Bleeding.class).set(4);
		return damage;
	}

	//0-1 idle, 2-5 hopping, 6-7 a peck, 8-9 flying
	public static class CrowSprite extends CritterSprite {

		public CrowSprite(){
			super();
			critter(FrostAssets.CROW);
			run = loop(10, 8, 9);
		}
	}
}
