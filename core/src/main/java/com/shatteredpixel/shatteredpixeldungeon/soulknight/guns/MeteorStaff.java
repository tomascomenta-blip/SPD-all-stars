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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.guns;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//goblin high priest staff. 3 meteors around the target, splash on landing
public class MeteorStaff extends SKGun {

	private static final int METEORS = 3;

	{
		image = ItemSpriteSheet.METEOR_STAFF;
		collisionProperties = Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID;
		unique = true;
	}

	@Override
	protected int shotImage() {
		return ItemSpriteSheet.SHOT_METEOR;
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
		return 7 + 2*lvl;
	}

	@Override
	public void onZap( Ballistica bolt ) {
		int center = bolt.collisionPos;
		ArrayList<Integer> landings = new ArrayList<>();
		landings.add(center);
		ArrayList<Integer> near = SKCombat.area(center, 2);
		near.remove((Integer) center);
		for (int i = 1; i < METEORS && !near.isEmpty(); i++){
			landings.add(near.remove(Random.Int(near.size())));
		}

		boolean visible = false;
		for (int c : landings){
			ArrayList<Integer> blast = SKCombat.openArea(c, 1);
			for (int b : blast){
				if (Dungeon.level.heroFOV[b]){
					visible = true;
					CellEmitter.center(b).burst(BlastParticle.FACTORY, 2);
					CellEmitter.get(b).burst(FlameParticle.FACTORY, 2);
				}
			}
			for (Char ch : SKCombat.foesIn(curUser, blast)){
				shoot(ch, damageRoll());
			}
		}
		if (visible){
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
			PixelScene.shake(1, 0.3f);
		}
	}

	@Override
	public void fx( Ballistica bolt, Callback callback ) {
		//falls from above
		PointF to = DungeonTilemap.tileCenterToWorld(bolt.collisionPos);
		PointF from = new PointF(to.x + 24, to.y - 64);
		((MissileSprite) curUser.sprite.parent.recycle(MissileSprite.class)).reset(
				from, to, new SKMobSprite.Shot(shotImage()), callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP, 1f, 0.7f);
	}
}
