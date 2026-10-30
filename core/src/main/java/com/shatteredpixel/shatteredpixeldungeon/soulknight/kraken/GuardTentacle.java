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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//assault 3: 2 small tentacles by the bowsprit, eye stays shut while they live. spit bubbles at the bow
public class GuardTentacle extends SKMob {

	{
		spriteClass = GuardSprite.class;
		HP = HT = 45;
		defenseSkill = 8;
		EXP = 0;
		lootChance = 0;
		state = HUNTING;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	@Override
	protected void onAdd() {
		super.onAdd();
		if (DevFlags.krakenWeak()) HP = HT = 1;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(6, 13);
	}

	@Override
	public int attackSkill( Char target ) {
		return 30;
	}

	@Override
	protected int shotRange() {
		return 5;
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
		KrakenVisuals.splash(pos);
		if (!Kraken.guardsAlive()){
			KrakenHud.banner(Messages.get(Kraken.class, "banner_eye"));
		}
	}

	public static class GuardSprite extends SKMobSprite {
		public GuardSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_BUBBLE;
			setup(KrakenAssets.GUARD, 24, 40);
		}
		@Override
		public int blood() {
			return 0xFF7A1A4A;
		}
	}
}
