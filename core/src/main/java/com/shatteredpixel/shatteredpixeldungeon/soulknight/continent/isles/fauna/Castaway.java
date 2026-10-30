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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Yog;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshCritter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.CritterSprite;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//castaway: what's left of the expeditions that came before, drowned and walking. barnacles of yog
//flesh on a coat in rags. it shambles after noise and spits black water at whoever is a few
//steps off (marked): it stings and stays on the skin
public class Castaway extends AshCritter implements Yog {

	{
		spriteClass = CastawaySprite.class;

		HP = HT = 110;
		defenseSkill = 22;
		EXP = 9;
		baseSpeed = 0.8f;

		minDmg = 20; maxDmg = 28; acc = 56;
		armorMin = 2; armorMax = 8;

		properties.add(Property.UNDEAD);
	}

	private static final int SPIT = 0;

	@Override
	public int taintOnHit() {
		return 2;
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		if (d < 2 || d > 5) return false;
		ArrayList<Integer> cells = line(enemy.pos, 5);
		if (!cells.contains(enemy.pos)) return false;
		announce(SPIT, cells, enemy.pos);
		if (sprite instanceof CastawaySprite) ((CastawaySprite) sprite).hold(1, 8);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (sprite instanceof CastawaySprite) ((CastawaySprite) sprite).spat();
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.WATER);
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 24, 34);
			if (ch.isAlive()) Buff.affect(ch, Ooze.class).set(Ooze.DURATION);
		}
	}

	public static class CastawaySprite extends CritterSprite {

		private Animation still;

		public CastawaySprite(){
			super();
			critter(IslesAssets.CASTAWAY);
			still = idle;
		}

		public void spat(){
			look(still);
			pose(8, 8, 8, 0);
		}
	}
}
