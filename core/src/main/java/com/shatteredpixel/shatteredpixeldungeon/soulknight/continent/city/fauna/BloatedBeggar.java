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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//bloated beggar. a beggar swollen to the size of a house with the gas of all he ate. slow, he
//slaps whoever is near; hurt enough, or with people round him, he swells up (marked, three
//turns) and bursts, poisoning everything around. bursting kills him
public class BloatedBeggar extends CityColossus {

	{
		size = 5;
		spriteClass = BeggarSprite.class;

		HP = HT = 650;
		defenseSkill = 0;
		EXP = 45;
		baseSpeed = 0.5f;

		minDmg = 30; maxDmg = 42; acc = 52;
		armorMin = 4; armorMax = 14;
	}

	//the belly
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 4}, {3, 4}};
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	@Override
	protected boolean special( Char enemy ) {
		int near = SKCombat.foesIn(this, ring(2)).size();
		if (HP * 5 < HT * 2 || near >= 2 || near == 1 && Random.Int(3) == 0){
			warn(0, ring(3), enemy.pos, 3f);
			if (seen()) GLog.w(Messages.get(this, "swells"));
			return true;
		}
		return false;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		Senses.noise(pos, Senses.ROAR);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.8f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.TOXIC), 2);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 120, 160);
			if (ch.isAlive()) Buff.affect(ch, Poison.class).set(6);
		}
		die(this);
	}

	//6 swelling, 7 the slap, 8 down, 9 dead, 10 bursting
	public static class BeggarSprite extends Sheet {

		public BeggarSprite(){
			super();
			layout(CityAssets.BEGGAR, 5, 112, 112, 16, 32, 8, 9);
			die = clip(6, 10, 10, 9, 9);
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(58, 70);
		}

		@Override
		public void windup( int kind ) {
			play(loop(4, 6, 6, 10));
		}
	}
}
