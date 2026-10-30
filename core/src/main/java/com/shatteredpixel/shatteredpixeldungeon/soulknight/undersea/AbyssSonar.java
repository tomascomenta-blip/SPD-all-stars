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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.SKGun;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//abyssal sub's sonar gun. pulse at the chosen spot, hurts every enemy within 2 cells and briefly
//shows where all of them are
public class AbyssSonar extends SKGun {

	public static final int RADIUS = 2;

	{
		image = ItemSpriteSheet.ABYSS_SONAR;
		collisionProperties = Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID;
		unique = true;
	}

	@Override
	protected int shotImage() {
		return ItemSpriteSheet.SHOT_BUBBLE;
	}

	@Override
	public int initialCharges() {
		return 2;
	}

	@Override
	public int min( int lvl ) {
		return 3 + lvl;
	}

	@Override
	public int max( int lvl ) {
		return 8 + 2*lvl;
	}

	@Override
	public void onZap( Ballistica bolt ) {
		ArrayList<Integer> area = SKCombat.openArea(bolt.collisionPos, RADIUS);
		for (int c : area){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.BUBBLE), 2);
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.6f);
		for (Char ch : SKCombat.foesIn(curUser, area)){
			shoot(ch, damageRoll());
		}
		Buff.affect(curUser, MindVision.class, 3f);
		Dungeon.observe();
	}
}
