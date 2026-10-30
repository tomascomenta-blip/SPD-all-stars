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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Yog;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//the lesser eye. floats out of reach and stares a line of cells to ash, one turn after it opens
public class YogEye extends ContinentMob implements Yog {

	{
		spriteClass = EyeSprite.class;

		HP = HT = 120;
		defenseSkill = 32;
		EXP = 16;
		flying = true;

		minDmg = 22; maxDmg = 30; acc = 56;
		armorMin = 0; armorMax = 8;

		properties.add(Property.DEMONIC);
	}

	private static final int RANGE = 7;

	//backs off while the stare recharges, then stands still to use it
	@Override
	protected boolean keepsDistance() {
		return cooldown > 0;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		if (d > RANGE) return false;
		ArrayList<Integer> ray = line(enemy.pos, RANGE + 1);
		if (!ray.contains(enemy.pos)) return false;
		announce(0, ray, enemy.pos);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (cells.isEmpty()) return;
		if (sprite != null && sprite.parent != null){
			Beam ray = new Beam.DeathRay(sprite.center(),
					DungeonTilemap.raisedTileCenterToWorld(cells.get(cells.size() - 1)));
			ray.hardlight(0xFFB040);
			sprite.parent.add(ray);
		}
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.RAY);
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 38, 52);
		}
	}

	public static class EyeSprite extends ContinentMobSprite {
		public EyeSprite(){
			super();
			setup(ContinentAssets.YOG_EYE, 16, 16);
			//the stare: held for the whole warning, idle() ends it
			zap = loop(6, 12, 13);
		}
	}
}
