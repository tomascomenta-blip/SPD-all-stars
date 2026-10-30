/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

//icons for hero subclasses and abilities atm, maybe add classes?
public class HeroIcon extends Image {

	private static TextureFilm film;
	private static final int SIZE = 16;

	//transparent icon
	public static final int NONE    = 127;

	//subclasses
	public static final int BERSERKER   = 0;
	public static final int GLADIATOR   = 1;
	public static final int BATTLEMAGE  = 2;
	public static final int WARLOCK     = 3;
	public static final int ASSASSIN    = 4;
	public static final int FREERUNNER  = 5;
	public static final int SNIPER      = 6;
	public static final int WARDEN      = 7;
	public static final int CHAMPION    = 8;
	public static final int MONK        = 9;
	public static final int PRIEST      = 10;
	public static final int PALADIN     = 11;

	//champion subclasses
	public static final int CONQUEROR       = 128;
	public static final int HEADSMAN        = 129;
	public static final int TRAPPER         = 130;
	public static final int SKIRMISHER      = 131;
	public static final int TAMER           = 132;
	public static final int INFERNALIST     = 133;
	public static final int SOULREAPER      = 134;
	public static final int LANTERNKEEPER   = 135;
	public static final int PITBOSS         = 136;
	public static final int SHOWMAN         = 137;
	public static final int MISTLORD        = 138;
	public static final int EXHUMER         = 139;
	public static final int CURATOR         = 140;
	public static final int COLOSSUS        = 141;
	public static final int TROUBADOUR      = 142;
	public static final int MINSTREL        = 143;

	//champion abilities
	public static final int LOL_DECIMATE         = 144;
	public static final int LOL_CRIPPLING_STRIKE = 145;
	public static final int LOL_APPREHEND        = 146;
	public static final int LOL_GUILLOTINE       = 147;
	public static final int LOL_BLINDING_DART    = 148;
	public static final int LOL_MOVE_QUICK       = 149;
	public static final int LOL_TOXIC_SHOT       = 150;
	public static final int LOL_NOXIOUS_TRAP     = 151;
	public static final int LOL_DISINTEGRATE     = 152;
	public static final int LOL_INCINERATE       = 153;
	public static final int LOL_MOLTEN_SHIELD    = 154;
	public static final int LOL_SUMMON_BEAR      = 155;
	public static final int LOL_DEATH_SENTENCE   = 156;
	public static final int LOL_DARK_PASSAGE     = 157;
	public static final int LOL_FLAY             = 158;
	public static final int LOL_THE_BOX          = 159;
	public static final int LOL_KNUCKLE_DOWN     = 160;
	public static final int LOL_HAYMAKER         = 161;
	public static final int LOL_FACEBREAKER      = 162;
	public static final int LOL_SHOW_STOPPER     = 163;
	public static final int LOL_LAST_RITES       = 164;
	public static final int LOL_DARK_PROCESSION  = 165;
	public static final int LOL_MOURNING_MIST    = 166;
	public static final int LOL_EULOGY           = 167;
	public static final int LOL_SIPHONING_STRIKE = 168;
	public static final int LOL_WITHER           = 169;
	public static final int LOL_SPIRIT_FIRE      = 170;
	public static final int LOL_FURY_OF_SANDS    = 171;
	public static final int BARD_BALLAD          = 172;
	public static final int BARD_LULLABY         = 173;
	public static final int BARD_HYMN            = 174;
	public static final int BARD_FINALE          = 175;

	//botanico: armor abilities, Botanist abilities and subclasses (rows 24-25 of hero_icons.png)
	public static final int SCOUT_RAIN           = 192;
	public static final int PYRO_SUPERNOVA       = 193;
	public static final int GRAVE_HEADSTONE      = 194;
	public static final int BOT_BLOOM            = 195;
	public static final int BOT_THORN_SHOOTER    = 196;
	public static final int BOT_EARTH_WALL       = 197;
	public static final int BOT_HEALING_MEADOW   = 198;
	public static final int BOT_ANCIENT_TREE     = 199;
	public static final int WARGARDENER          = 200;
	public static final int DRUID                = 201;

	//the Forger
	public static final int FORGER_RIFT          = 216;
	public static final int FORGER_BELLOWS       = 217;
	public static final int FORGER_CHARGE        = 218;
	public static final int FORGER_RAM           = 219;
	public static final int FORGER_ERUPTION      = 220;
	public static final int MASTERSMITH          = 221;
	public static final int LIVINGMOUNTAIN       = 222;
	public static final int GUARDIAN_SAND_TOMB   = 223;
	public static final int BARD_OVERTURE        = 224;
	public static final int FORGER_FORGE         = 225;

	//Ironlord

	//alt world heroes (rows 29-30 of hero_icons.png)
	public static final int SWARM_ENGINEER   = 232;
	public static final int TESLA_COIL       = 233;
	public static final int AUT_OVERLOAD     = 234;
	public static final int AUT_DRONES       = 235;
	public static final int AUT_EMP          = 236;
	public static final int AUT_OMEGA        = 237;
	public static final int AUT_TURRET       = 238;
	public static final int PACK_ALPHA       = 239;
	public static final int FOREST_WARDEN    = 240;
	public static final int BM_FROSTFIRE     = 241;
	public static final int BM_VINES         = 242;
	public static final int BM_BEAR          = 243;
	public static final int BM_STAMPEDE      = 244;
	public static final int BM_ROAR          = 245;
	public static final int SPECTRE_LORD     = 246;
	public static final int PLAGUE_HERALD    = 247;
	public static final int SP_NIGHTMARE     = 248;
	public static final int SP_OMEN          = 249;
	public static final int SP_RAISE         = 250;
	public static final int SP_DANSE         = 251;
	public static final int SP_VEIL          = 252;
	public static final int QUICKDRAW        = 253;
	public static final int BOUNTY_HUNTER    = 254;
	public static final int GS_SPIN          = 255;
	public static final int GS_BOUNTY        = 256;
	public static final int GS_AIRSTRIKE     = 257;
	public static final int GS_HIGH_NOON     = 258;
	public static final int GS_LEAD_RAIN     = 259;

	//paths of the heart of Thal-Vekhorr (end of row 32)
	public static final int YOG_HUNTER       = 260;
	public static final int LIGHTBEARER      = 261;
	public static final int TIME_WANDERER    = 262;
	public static final int IRON_OBLITERATE      = 176;
	public static final int IRON_INDESTRUCTIBLE  = 177;
	public static final int IRON_DEATHS_GRASP    = 178;
	public static final int IRON_REALM           = 179;
	public static final int TYRANT               = 180;
	public static final int REVENANT             = 181;
	public static final int IRON_ECLIPSE         = 182;
	public static final int BLOOD_HARVEST        = 183;
	public static final int SPECTRAL_SHACKLES    = 184;
	public static final int OVATION              = 185;

	//abilities
	public static final int HEROIC_LEAP     = 16;
	public static final int SHOCKWAVE       = 17;
	public static final int ENDURE          = 18;
	public static final int ELEMENTAL_BLAST = 19;
	public static final int WILD_MAGIC      = 20;
	public static final int WARP_BEACON     = 21;
	public static final int SMOKE_BOMB      = 22;
	public static final int DEATH_MARK      = 23;
	public static final int SHADOW_CLONE    = 24;
	public static final int SPECTRAL_BLADES = 25;
	public static final int NATURES_POWER   = 26;
	public static final int SPIRIT_HAWK     = 27;
	public static final int CHALLENGE       = 28;
	public static final int ELEMENTAL_STRIKE= 29;
	public static final int FEINT           = 30;
	public static final int ASCENDED_FORM   = 31;
	public static final int TRINITY         = 32;
	public static final int POWER_OF_MANY   = 33;
	public static final int RATMOGRIFY      = 34;

	//cleric spells
	public static final int GUIDING_LIGHT   = 40;
	public static final int HOLY_WEAPON     = 41;
	public static final int HOLY_WARD       = 42;
	public static final int HOLY_INTUITION  = 43;
	public static final int SHIELD_OF_LIGHT = 44;
	public static final int RECALL_GLYPH    = 45;
	public static final int SUNRAY          = 46;
	public static final int DIVINE_SENSE    = 47;
	public static final int BLESS           = 48;
	public static final int CLEANSE         = 49;
	public static final int RADIANCE        = 50;
	public static final int HOLY_LANCE      = 51;
	public static final int HALLOWED_GROUND = 52;
	public static final int MNEMONIC_PRAYER = 53;
	public static final int SMITE           = 54;
	public static final int LAY_ON_HANDS    = 55;
	public static final int AURA_OF_PROTECTION = 56;
	public static final int WALL_OF_LIGHT   = 57;
	public static final int DIVINE_INTERVENTION = 58;
	public static final int JUDGEMENT       = 59;
	public static final int FLASH           = 60;
	public static final int BODY_FORM       = 61;
	public static final int MIND_FORM       = 62;
	public static final int SPIRIT_FORM     = 63;
	public static final int BEAMING_RAY     = 64;
	public static final int LIFE_LINK       = 65;
	public static final int STASIS          = 66;

	//all cleric spells have a separate icon with no background for the action indicator
	public static final int SPELL_ACTION_OFFSET      = 32;

	//action indicator visuals
	public static final int BERSERK         = 104;
	public static final int COMBO           = 105;
	public static final int PREPARATION     = 106;
	public static final int MOMENTUM        = 107;
	public static final int SNIPERS_MARK    = 108;
	public static final int WEAPON_SWAP     = 109;
	public static final int MONK_ABILITIES  = 110;

	public HeroIcon(int index){
		super( Assets.Interfaces.HERO_ICONS );
		if (film == null){
			film = new TextureFilm(texture, SIZE, SIZE);
		}
		frame(film.get(index));
	}

	public HeroIcon(HeroSubClass subCls){
		super( Assets.Interfaces.HERO_ICONS );
		if (film == null){
			film = new TextureFilm(texture, SIZE, SIZE);
		}
		frame(film.get(subCls.icon()));
	}

	public HeroIcon(ArmorAbility abil){
		super( Assets.Interfaces.HERO_ICONS );
		if (film == null){
			film = new TextureFilm(texture, SIZE, SIZE);
		}
		frame(film.get(abil.icon()));
	}

	public HeroIcon(ActionIndicator.Action action){
		super( Assets.Interfaces.HERO_ICONS );
		if (film == null){
			film = new TextureFilm(texture, SIZE, SIZE);
		}
		frame(film.get(action.actionIcon()));
	}

	public HeroIcon(ClericSpell spell){
		super( Assets.Interfaces.HERO_ICONS );
		if (film == null){
			film = new TextureFilm(texture, SIZE, SIZE);
		}
		frame(film.get(spell.icon()));
	}

}
