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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forest;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//sprites of the Forest monsters
public class ForestSprites {

	public static class BoarSprite extends SKMobSprite {
		public BoarSprite(){
			super();
			setup(SKAssets.BOAR, 18, 15);
		}
	}

	public static class SpearmanSprite extends SKMobSprite {
		public SpearmanSprite(){
			super();
			setup(SKAssets.GOBLIN_SPEAR, 12, 15);
		}
		@Override
		public void update() {
			//row 2 = no spear (it threw it)
			if (ch instanceof GoblinSpearman) row(((GoblinSpearman) ch).hasSpear() ? 0 : 1);
			super.update();
		}
	}

	public static class GunnerSprite extends SKMobSprite {
		public GunnerSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_BULLET;
			shotSound = Assets.Sounds.HIT_ARROW;
			setup(SKAssets.GOBLIN_PISTOL, 12, 15);
		}
	}

	public static class EliteSprite extends SKMobSprite {
		public EliteSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_PELLET;
			shotSound = Assets.Sounds.BLAST;
			setup(SKAssets.GOBLIN_ELITE, 14, 16);
		}
	}

	public static class ShamanSprite extends SKMobSprite {
		public ShamanSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_STAR;
			spinning = true;
			shotSound = Assets.Sounds.ZAP;
			setup(SKAssets.GOBLIN_SHAMAN, 12, 15);
		}
	}

	public static class TrumpetSprite extends SKMobSprite {
		public TrumpetSprite(){
			super();
			setup(SKAssets.TRUMPET, 16, 16);
		}
		@Override
		public void update() {
			//row 2 = closed after bubbling
			if (ch instanceof TrumpetFlower) row(((TrumpetFlower) ch).isClosed() ? 1 : 0);
			super.update();
		}
	}

	public static class GiantSprite extends SKMobSprite {
		public GiantSprite(){
			super();
			setup(SKAssets.GOBLIN_GIANT, 24, 24);
		}
	}

	public static class PriestSprite extends SKMobSprite {
		public PriestSprite(){
			super();
			setup(SKAssets.GOBLIN_PRIEST, 20, 20);
		}
	}
}
