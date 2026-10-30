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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.armory;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//the 20 guns. tier 1 shows up around the first world, tier 4 in the last.
//numbers are mostly vibes + a spreadsheet i lost
public class ArmoryGuns {

	// tier 1

	public static class OldPistol extends ArmoryGun {
		{
			image = ItemSpriteSheet.OLD_PISTOL;
			tier = 1; charges = 5;
			baseMin = 2; lvlMin = 1; baseMax = 5; lvlMax = 1;
		}
	}

	public static class Smg extends ArmoryGun {
		{
			image = ItemSpriteSheet.SMG;
			tier = 1; kind = BURST; count = 3; charges = 3;
			baseMin = 1; lvlMin = 0; baseMax = 3; lvlMax = 1;
		}
	}

	public static class Bow extends ArmoryGun {
		{
			image = ItemSpriteSheet.SK_BOW;
			tier = 1; charges = 4;
			baseMin = 3; lvlMin = 1; baseMax = 6; lvlMax = 2;
			shot = ItemSpriteSheet.SHOT_ARROW;
		}
	}

	public static class BubbleGun extends ArmoryGun {
		{
			image = ItemSpriteSheet.BUBBLE_GUN;
			tier = 1; charges = 4; effect = ROOT;
			baseMin = 1; lvlMin = 1; baseMax = 4; lvlMax = 1;
			shot = ItemSpriteSheet.SHOT_BUBBLE;
			sound = Assets.Sounds.WATER;
		}
	}

	public static class StunPistol extends ArmoryGun {
		{
			image = ItemSpriteSheet.STUN_PISTOL;
			tier = 1; charges = 3; effect = STUN;
			baseMin = 2; lvlMin = 1; baseMax = 4; lvlMax = 1;
			sound = Assets.Sounds.ZAP;
		}
	}

	// ---- tier 2

	public static class DesertEagle extends ArmoryGun {
		{
			image = ItemSpriteSheet.DESERT_EAGLE;
			tier = 2; charges = 2;
			baseMin = 5; lvlMin = 1; baseMax = 11; lvlMax = 3;
		}
	}

	public static class Shotgun extends ArmoryGun {
		{
			image = ItemSpriteSheet.SHOTGUN;
			tier = 2; kind = CONE; reach = 4; charges = 2;
			baseMin = 3; lvlMin = 1; baseMax = 7; lvlMax = 2;
			shot = ItemSpriteSheet.SHOT_PELLET;
			sound = Assets.Sounds.BLAST;
			collisionProperties = Ballistica.WONT_STOP;
		}
	}

	public static class Crossbow extends ArmoryGun {
		{
			image = ItemSpriteSheet.SK_CROSSBOW;
			tier = 2; kind = PIERCE; reach = 6; charges = 3;
			baseMin = 4; lvlMin = 1; baseMax = 8; lvlMax = 2;
			shot = ItemSpriteSheet.SHOT_ARROW;
			collisionProperties = Ballistica.WONT_STOP;
		}
	}

	public static class ToxicBlaster extends ArmoryGun {
		{
			image = ItemSpriteSheet.TOXIC_BLASTER;
			tier = 2; kind = BLAST; count = 1; charges = 2; effect = POISON;
			baseMin = 2; lvlMin = 1; baseMax = 5; lvlMax = 1;
			shot = ItemSpriteSheet.SHOT_ORB;
		}
	}

	public static class FrostGun extends ArmoryGun {
		{
			image = ItemSpriteSheet.FROST_GUN;
			tier = 2; charges = 3; effect = CHILL;
			baseMin = 3; lvlMin = 1; baseMax = 7; lvlMax = 2;
			shot = ItemSpriteSheet.SHOT_STAR;
			sound = Assets.Sounds.SHATTER;
		}
	}

	public static class AssaultRifle extends ArmoryGun {
		{
			image = ItemSpriteSheet.ASSAULT_RIFLE;
			tier = 3; kind = BURST; count = 3; charges = 3;
			baseMin = 3; lvlMin = 1; baseMax = 6; lvlMax = 1;
		}
	}

	public static class Flamethrower extends ArmoryGun {
		{
			image = ItemSpriteSheet.FLAMETHROWER;
			tier = 3; kind = CONE; reach = 3; charges = 3; effect = BURN;
			baseMin = 3; lvlMin = 1; baseMax = 6; lvlMax = 1;
			shot = ItemSpriteSheet.SHOT_METEOR;
			sound = Assets.Sounds.BURNING;
			collisionProperties = Ballistica.WONT_STOP;
		}
	}

	public static class ThunderStaff extends ArmoryGun {
		{
			image = ItemSpriteSheet.THUNDER_STAFF;
			tier = 3; kind = CHAIN; count = 3; charges = 3;
			baseMin = 4; lvlMin = 1; baseMax = 9; lvlMax = 2;
			shot = ItemSpriteSheet.SHOT_STAR;
			sound = Assets.Sounds.ZAP;
		}
	}

	public static class GrenadeLauncher extends ArmoryGun {
		{
			image = ItemSpriteSheet.GRENADE_LAUNCHER;
			tier = 3; kind = BLAST; count = 1; charges = 2;
			baseMin = 6; lvlMin = 2; baseMax = 12; lvlMax = 3;
			shot = ItemSpriteSheet.SHOT_BIG_ORB;
		}
	}

	public static class FireworkLauncher extends ArmoryGun {
		{
			image = ItemSpriteSheet.FIREWORK_LAUNCHER;
			tier = 3; kind = VOLLEY; count = 4; reach = 6; charges = 2;
			baseMin = 3; lvlMin = 1; baseMax = 7; lvlMax = 2;
			shot = ItemSpriteSheet.SHOT_ROCKET;
		}
	}

	// ---- tier 4 (the fun ones)

	public static class SniperRifle extends ArmoryGun {
		{
			image = ItemSpriteSheet.SNIPER_RIFLE;
			tier = 4; kind = PIERCE; reach = 9; charges = 2;
			baseMin = 10; lvlMin = 2; baseMax = 20; lvlMax = 4;
			collisionProperties = Ballistica.WONT_STOP;
		}
	}

	public static class Gatling extends ArmoryGun {
		{
			image = ItemSpriteSheet.GATLING;
			tier = 4; kind = BURST; count = 6; charges = 2;
			baseMin = 2; lvlMin = 1; baseMax = 5; lvlMax = 1;
		}
	}

	public static class Bazooka extends ArmoryGun {
		{
			image = ItemSpriteSheet.BAZOOKA;
			tier = 4; kind = BLAST; count = 2; charges = 1;
			baseMin = 10; lvlMin = 2; baseMax = 18; lvlMax = 4;
			shot = ItemSpriteSheet.SHOT_ROCKET;
		}
	}

	public static class LaserRifle extends ArmoryGun {
		{
			image = ItemSpriteSheet.LASER_RIFLE;
			tier = 4; kind = PIERCE; reach = 8; charges = 3; beam = true;
			baseMin = 6; lvlMin = 2; baseMax = 12; lvlMax = 3;
			shot = ItemSpriteSheet.SHOT_LASER;
			collisionProperties = Ballistica.WONT_STOP;
		}
	}

	public static class PlasmaCannon extends ArmoryGun {
		{
			image = ItemSpriteSheet.PLASMA_CANNON;
			tier = 4; kind = BLAST; count = 1; charges = 2; effect = SLOW;
			baseMin = 8; lvlMin = 2; baseMax = 16; lvlMax = 3;
			shot = ItemSpriteSheet.SHOT_BIG_ORB;
		}
	}
}
