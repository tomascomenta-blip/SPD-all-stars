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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//dark grand knight's blade. now and then a hit lets out a fan of 3 shadow blades that keep flying
//and hurt whatever's behind
public class ShadowBlade extends MeleeWeapon {

	{
		image = ItemSpriteSheet.SHADOW_BLADE;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;

		tier = 3;
		unique = true;
		bones = false;
	}

	public static final int CHANCE = 3; //1 in 3 hits

	@Override
	public int proc( Char attacker, Char defender, int damage ) {
		if (Random.Int(CHANCE) == 0 && defender != null){
			int min = Math.max(1, damage/4);
			int max = Math.max(min, damage/2);
			float aim = SKBulletField.angleTo(attacker.pos, defender.pos);
			SKBulletField field = SKBulletField.get();
			for (int i = -1; i <= 1; i++){
				SKBulletField.Bullet b = field.fire(attacker, attacker.pos, aim + 20*i, 2f, ItemSpriteSheet.SHOT_BLADE, min, max);
				b.life = 4;
			}
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String desc() {
		return super.desc() + "\n\n" + Messages.get(this, "blades");
	}
}
