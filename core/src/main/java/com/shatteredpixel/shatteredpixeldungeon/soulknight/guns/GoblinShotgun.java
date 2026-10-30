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

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//goblin shotgun, short cone, hits everyone in it
public class GoblinShotgun extends SKGun {

	private static final int RANGE = 4;
	private static final float SPREAD = 50;

	{
		image = ItemSpriteSheet.GOBLIN_SHOTGUN;
		collisionProperties = Ballistica.WONT_STOP;
	}

	@Override
	protected int shotImage() {
		return ItemSpriteSheet.SHOT_PELLET;
	}

	@Override
	protected String shotSound() {
		return Assets.Sounds.BLAST;
	}

	@Override
	public int initialCharges() {
		return 2;
	}

	@Override
	public int min( int lvl ) {
		return 2 + lvl;
	}

	@Override
	public int max( int lvl ) {
		return 6 + 2*lvl;
	}

	private ConeAOE cone( Ballistica bolt ){
		return new ConeAOE(bolt, RANGE, SPREAD, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
	}

	@Override
	public void onZap( Ballistica bolt ) {
		ArrayList<Char> hit = new ArrayList<>();
		for (int c : cone(bolt).cells){
			Char ch = Actor.findChar(c);
			if (ch != null && ch != curUser && (ch.alignment != curUser.alignment || CoopRules.friendlyTarget(curUser, ch)) && !hit.contains(ch)){
				hit.add(ch);
			}
		}
		for (Char ch : hit){
			shoot(ch, damageRoll());
		}
	}

	@Override
	public void fx( Ballistica bolt, Callback callback ) {
		ConeAOE cone = cone(bolt);
		//pellets on the edges + middle, middle one fires the callback
		for (Ballistica ray : cone.outerRays){
			((MissileSprite) curUser.sprite.parent.recycle(MissileSprite.class)).reset(
					curUser.sprite, ray.path.get(ray.dist), new SKMobSprite.Shot(shotImage()), null);
		}
		((MissileSprite) curUser.sprite.parent.recycle(MissileSprite.class)).reset(
				curUser.sprite, cone.coreRay.path.get(cone.coreRay.dist), new SKMobSprite.Shot(shotImage()), callback);
		Sample.INSTANCE.play(shotSound(), 1f, 1.2f);
	}
}
