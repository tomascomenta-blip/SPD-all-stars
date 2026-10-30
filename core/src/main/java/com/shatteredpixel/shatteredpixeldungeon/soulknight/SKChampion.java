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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKArmory;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinGiant;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaMobs;
import com.watabou.utils.Random;

//elite mobs for the new worlds: purple aura, 2x hp, meaner (1.5x bullets, harder hits, special sooner).
//drop gold and sometimes a gun. not related to the champion heroes, bad naming, sorry
public class SKChampion extends ChampionEnemy {

	{
		color = 0xC040FF;
		rays = 6;
	}

	//1 in 10
	private static final int ODDS = 10;

	@Override
	public float meleeDamageFactor() {
		return 1.25f;
	}

	@Override
	public float evasionAndAccuracyFactor() {
		return 1.1f;
	}

	//rolled on every new-world mob creation (levelgen, respawn, ambush).
	//ALWAYS roll, even when it can't be a champion, so the Random call count stays the same. seeds break otherwise
	public static void roll( Mob m ){
		if (!SKWorlds.isSK(Dungeon.depth) || !isWorldMonster(m)) return;
		//-Dspd.test.skchampion=N -> 1 de cada N
		boolean lucky = Random.Int(DevFlags.skChampion(ODDS)) == 0;
		if (!lucky || m.buff(ChampionEnemy.class) != null) return;
		if (m instanceof GoblinGiant || m instanceof KingdomMobs.DarkKnight || m instanceof UnderseaMobs.Subject004) return;
		make(m);
	}

	public static void make( Mob m ){
		Buff.affect(m, SKChampion.class);
		m.HT *= 2;
		m.HP = m.HT;
	}

	private static boolean isWorldMonster( Mob m ){
		return m.getClass().getName().startsWith("com.shatteredpixel.shatteredpixeldungeon.soulknight.")
				&& m.alignment == Char.Alignment.ENEMY
				&& !(m instanceof SKProps.SKProp)
				&& !m.properties().contains(Char.Property.BOSS)
				&& !m.properties().contains(Char.Property.MINIBOSS);
	}

	public static boolean is( Char c ){
		return c != null && c.buff(SKChampion.class) != null;
	}

	//x1.5 bullets
	public static int more( Char c, int n ){
		return is(c) ? n + (n+1)/2 : n;
	}

	//gold always, gun 1/3
	public static void onDeath( Mob m ){
		if (!is(m)) return;
		Dungeon.level.drop(new Gold().random(), m.pos).sprite.drop();
		if (Random.Int(3) == 0){
			Dungeon.level.drop(SKArmory.random(SKArmory.tierForDepth(Dungeon.depth)), m.pos).sprite.drop();
		}
	}
}
