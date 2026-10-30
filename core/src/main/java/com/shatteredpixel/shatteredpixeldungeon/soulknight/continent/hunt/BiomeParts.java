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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.BoneChimera;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.LastStanding;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.Mourner;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.RibDigger;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.SkinlessColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.BloatedBeggar;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.BronzeGuard;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.ChainedOne;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.FurnaceCathedral;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.LastKing;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.LivingCarriage;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.SmelterMother;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.FleshChimera;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.ForestHeart;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.HiveBoar;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.HollowBear;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.InfectedGiraffe;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.RootElk;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.CorruptWyvern;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.FlayedMammoth;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.FrostRaptor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.FrostShepherd;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.IceElk;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.WalkingMountain;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.InkEel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.IslandLizard;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.LighthouseCrab;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.TideOctopus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.YogTrueForm;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.GreatWorm;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;

//the parts of the big ones beyond the ash plains: which beast leaves which (the 5x5 and up), and
//what each does to a body that takes it badly. the guardians and the giants leave the strong ones
public class BiomeParts {

	public static final int FIRST = Mutations.SEWN_HEADS;

	//the beasts in the order of their parts, from FIRST on
	private static final Class<?>[] BEASTS = {
			FleshChimera.class, HiveBoar.class, HollowBear.class, RootElk.class, InfectedGiraffe.class, ForestHeart.class,
			BloatedBeggar.class, BronzeGuard.class, LivingCarriage.class, ChainedOne.class, SmelterMother.class,
			FurnaceCathedral.class, LastKing.class,
			FlayedMammoth.class, FrostRaptor.class, IceElk.class, CorruptWyvern.class, WalkingMountain.class, FrostShepherd.class,
			BoneChimera.class, RibDigger.class, SkinlessColossus.class, Mourner.class, LastStanding.class,
			InkEel.class, LighthouseCrab.class, TideOctopus.class, IslandLizard.class, YogTrueForm.class, GreatWorm.class
	};

	//the parts of each zone, {first, last}
	public static int[] of( int zone ){
		switch (zone){
			case Continent.Z_FLESH: return new int[]{Mutations.SEWN_HEADS, Mutations.AORTA_KNOT};
			case Continent.Z_CITY:  return new int[]{Mutations.GAS_CROP, Mutations.SUNKEN_CROWN};
			case Continent.Z_FROST: return new int[]{Mutations.FROZEN_MUSCLE, Mutations.SHEPHERD_FROST};
			case Continent.Z_BONES: return new int[]{Mutations.HORNED_SKULL, Mutations.RIB_SOUL};
			case Continent.Z_ISLES: return new int[]{Mutations.INK_SAC, Mutations.LAMPREY_MOUTH};
			default:                return new int[]{0, Mutations.THROAT};
		}
	}

	//which part one of these leaves, -1 for none
	public static int of( Colossus c ){
		for (int i = 0; i < BEASTS.length; i++){
			if (BEASTS[i] == c.getClass()) return FIRST + i;
		}
		return -1;
	}

	//taken badly: something of the beast, for a while
	public static void sicken( Hero h, int kind ){
		switch (kind){
			case Mutations.SEWN_HEADS:     Buff.prolong(h, Weakness.class, 30f); break;
			case Mutations.FLESH_COMB:     Buff.affect(h, Poison.class).set(6); break;
			case Mutations.HOLLOW_RIB:     Buff.affect(h, Hunger.class).affectHunger(-120); break;
			case Mutations.ANTLER_EYE:     Senses.noise(h.pos, Senses.STAMPEDE); break;
			case Mutations.TOOTHED_FLOWER: Buff.affect(h, Poison.class).set(8); break;
			case Mutations.AORTA_KNOT:     Buff.prolong(h, Vertigo.class, 10f); break;
			case Mutations.GAS_CROP:       Buff.prolong(h, Slow.class, 8f); break;
			case Mutations.BRONZE_PLATE:   Buff.prolong(h, Cripple.class, 10f); break;
			case Mutations.LIVING_AXLE:    Buff.prolong(h, Vertigo.class, 10f); break;
			case Mutations.SEWN_LIP:       Buff.affect(h, Hunger.class).affectHunger(-150); break;
			case Mutations.CRUCIBLE:       Buff.prolong(h, Weakness.class, 30f); break;
			case Mutations.FURNACE_BELL:   Buff.prolong(h, Vertigo.class, 12f); break;
			case Mutations.SUNKEN_CROWN:   Buff.prolong(h, Weakness.class, 40f); break;
			case Mutations.FROZEN_MUSCLE:  Buff.prolong(h, Slow.class, 6f); break;
			case Mutations.SICKLE_CLAW:    Buff.prolong(h, Cripple.class, 8f); break;
			case Mutations.ICE_ANTLER:     Buff.prolong(h, Chill.class, 5f); break;
			case Mutations.WING_MEMBRANE:  Buff.prolong(h, Vertigo.class, 10f); break;
			case Mutations.GLACIER_HEART:  Buff.prolong(h, Slow.class, 10f); break;
			case Mutations.SHEPHERD_FROST: Buff.affect(h, Hunger.class).affectHunger(-150); break;
			case Mutations.HORNED_SKULL:   Buff.prolong(h, Vertigo.class, 8f); break;
			case Mutations.SPADE_CLAW:     Buff.prolong(h, Blindness.class, 6f); break;
			case Mutations.BARE_SPINE:     Buff.affect(h, Hunger.class).affectHunger(-150); break;
			case Mutations.MIST_TEAR:      Buff.prolong(h, Blindness.class, 5f); break;
			case Mutations.RIB_SOUL:       Continent.taint(h, 10); break;
			case Mutations.INK_SAC:        Buff.affect(h, Poison.class).set(5); break;
			case Mutations.LAMP_GLASS:     Buff.prolong(h, Blindness.class, 4f); break;
			case Mutations.YOG_PLATE:      Continent.taint(h, 10); break;
			case Mutations.ISLAND_HIDE:    Buff.prolong(h, Slow.class, 10f); break;
			case Mutations.TRUE_EYE:       Continent.taint(h, 15); break;
			case Mutations.LAMPREY_MOUTH:  Buff.prolong(h, Blindness.class, 8f); break;
		}
	}
}
