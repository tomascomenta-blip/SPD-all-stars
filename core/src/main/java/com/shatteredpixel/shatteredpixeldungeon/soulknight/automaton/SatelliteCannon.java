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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.automaton;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.SKGun;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//automaton starting gun. thin laser, pierces everything until a wall
public class SatelliteCannon extends SKGun {

	public static final int RANGE = 6;

	{
		image = ItemSpriteSheet.SATELLITE_CANNON;
		collisionProperties = Ballistica.WONT_STOP;
		bones = false;
	}

	@Override
	protected int shotImage() {
		return ItemSpriteSheet.SHOT_BULLET;
	}

	@Override
	public int initialCharges() {
		return 3;
	}

	@Override
	public int min( int lvl ) {
		return 1 + lvl;
	}

	@Override
	public int max( int lvl ) {
		return 4 + 2*lvl;
	}

	private int reach( Ballistica beam ){
		int max = Math.min(RANGE, beam.dist);
		for (int i = 1; i <= max; i++){
			if (Dungeon.level.solid[beam.path.get(i)]) return i - 1;
		}
		return max;
	}

	@Override
	public void onZap( Ballistica beam ) {
		ArrayList<Char> hit = new ArrayList<>();
		for (int i = 1; i <= reach(beam); i++){
			Char ch = Actor.findChar(beam.path.get(i));
			if (ch != null && ch != curUser && (ch.alignment != curUser.alignment || CoopRules.friendlyTarget(curUser, ch))) hit.add(ch);
		}
		for (Char ch : hit){
			shoot(ch, damageRoll());
		}
	}

	@Override
	public void fx( Ballistica beam, Callback callback ) {
		int cell = beam.path.get(Math.max(1, reach(beam)));
		curUser.sprite.parent.add(new Beam.LightRay(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(cell)));
		Sample.INSTANCE.play(Assets.Sounds.RAY, 1f, 1.3f);
		callback.call();
	}
}
