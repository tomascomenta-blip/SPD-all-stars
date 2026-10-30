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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.gunslinger.BountyTag;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist.Nightmare;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

//marks the new heroes put on enemies (nightmare, bounty) -> +dmg taken.
//also shotFx, a purely visual shot
public class SKMarks {

	public static float damageFactor( Char ch ){
		float f = 1f;
		Nightmare.Mark n = ch.buff(Nightmare.Mark.class);
		if (n != null) f *= n.factor;
		BountyTag.Mark b = ch.buff(BountyTag.Mark.class);
		if (b != null) f *= b.factor;
		//rallied knights take less
		if (ch.buff(KingdomFeatures.Rallied.class) != null) f *= 0.75f;
		return f;
	}

	//just a picture, dmg already happened
	public static void shotFx( Char from, int to, int image ){
		if (from.sprite == null || from.sprite.parent == null) return;
		if (!Dungeon.level.heroFOV[from.pos] && !Dungeon.level.heroFOV[to]) return;
		((MissileSprite) from.sprite.parent.recycle(MissileSprite.class)).reset(
				from.sprite, to, new SKMobSprite.Shot(image), null);
		Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW, 0.8f, Random.Visual.Float(1.3f, 1.5f));
	}
}
