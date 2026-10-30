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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//pustule moth. yellowish membrane wings dotted with glowing pustules and a furry body. it flutters
//over whoever it finds and shakes its wings: the spores fall on a small marked patch
public class PustuleMoth extends FleshCritter {

	{
		spriteClass = MothSprite.class;

		HP = HT = 40;
		defenseSkill = 42;
		EXP = 4;
		baseSpeed = 1.3f;
		flying = true;

		minDmg = 10; maxDmg = 14; acc = 56;
		armorMin = 0; armorMax = 2;
	}

	//it's drawn to light
	@Override
	protected int eyes() {
		return 2;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (Dungeon.level.distance(pos, enemy.pos) > 3) return false;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : SKCombat.area(enemy.pos, 1)){
			if (!Dungeon.level.solid[c]) cells.add(c);
		}
		announce(0, cells, enemy.pos);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (sprite instanceof MothSprite) ((MothSprite) sprite).special();
		if (Dungeon.level.heroFOV[aim]) Sample.INSTANCE.play(Assets.Sounds.PUFF, 0.7f, 1.3f);
		for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.STENCH), 1);
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 12, 18);
			if (ch.isAlive()) Buff.affect(ch, Poison.class).set(4);
		}
	}

	//2-5 fluttering, 8 the spores
	public static class MothSprite extends Sheet {

		public MothSprite(){
			super();
			layout(FleshAssets.MOTH);
			idle = loop(6, 0, 1);
			play(idle);
		}
	}
}
