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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.BoneBeetle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.BoneChimera;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.BonesFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.CharredPhoenix;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.LastStanding;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.MarrowCentipede;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.MarrowWorm;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.Mourner;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.OssuaryHyena;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.OssuaryVulture;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.RibDigger;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna.SkinlessColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.BlackIceMantis;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.BlindWolf;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.CorruptWyvern;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.FlayedMammoth;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.FrostFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.FrostHare;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.FrostRaptor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.FrostShepherd;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.IceCrow;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.IceElk;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.IceLarva;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.SealWolf;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.WalkingMountain;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.Castaway;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.InkEel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.IslandLizard;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.IslesFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.LighthouseCrab;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.TideOctopus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.YogJellyfish;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna.YogTrueForm;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;

//testing only, through FaunaTest (-Dspd.test.fauna=... with colpreview): "frost", "bones" or "isles"
//(and a layer digit) fill the plain the way a floor of that biome is filled; the names below put
//those beasts round the hero like the ash ones
public class WildTest {

	public static Mob make( String kind ){
		switch (kind){
			case "hare":     return new FrostHare();
			case "crow":     return new IceCrow();
			case "wolf":     BlindWolf wf = new BlindWolf(); wf.pack = 3; return wf;
			case "larva":    return new IceLarva();
			case "mantis":   return new BlackIceMantis();
			case "seal":     return new SealWolf();
			case "elk":      return new IceElk();
			case "mammoth":  return new FlayedMammoth();
			case "fraptor":  return new FrostRaptor();
			case "wyvern":   return new CorruptWyvern();
			case "mountain": return new WalkingMountain();
			case "shepherd": return new FrostShepherd();
			case "phoenix":  return new CharredPhoenix();
			case "marrow":   MarrowWorm mw = new MarrowWorm(); mw.nest = 2; return mw;
			case "hyena":    OssuaryHyena hy = new OssuaryHyena(); hy.pack = 3; return hy;
			case "bbeetle":  return new BoneBeetle();
			case "centipede":return new MarrowCentipede();
			case "vulture":  return new OssuaryVulture();
			case "digger":   return new RibDigger();
			case "skinless": return new SkinlessColossus();
			case "chimera":  return new BoneChimera();
			case "mourner":  return new Mourner();
			case "last":     return new LastStanding();
			case "castaway": return new Castaway();
			case "jelly":    return new YogJellyfish();
			case "crab":     return new LighthouseCrab();
			case "eel":      return new InkEel();
			case "octopus":  return new TideOctopus();
			case "lizard":   return new IslandLizard();
			case "yog":
				YogTrueForm y = new YogTrueForm();
				if (DevFlags.yogPhase() > 1){
					y.phase = Math.min(3, DevFlags.yogPhase());
					y.HP = y.HT * (4 - y.phase) / 3 - 1;
				}
				return y;
			case "worm":     return new GreatWorm();
		}
		return null;
	}

	//true if it was one of ours
	public static boolean world( Level level, String what ){
		int layer = OpenWorld.SURFACE;
		String biome = what;
		if (!what.isEmpty() && Character.isDigit(what.charAt(what.length() - 1))){
			layer = what.charAt(what.length() - 1) - '0';
			biome = what.substring(0, what.length() - 1);
		}
		WildFauna.Biome b;
		switch (biome){
			case "frost": b = FrostFauna.BIOME; break;
			case "bones": b = BonesFauna.BIOME; break;
			case "isles": b = IslesFauna.BIOME; break;
			default: return false;
		}
		WildFauna.populate(b, level, layer);
		for (int i = 0; i < 6; i++){
			Mob m = WildFauna.next(b, level, layer);
			CoopLog.line("fauna respawn: " + (m == null ? "nothing" : m.getClass().getSimpleName() + " at " + m.pos));
		}
		if (DevFlags.blindWorm()){
			GreatWorm g = GreatWorm.under(level);
			if (g != null) level.mobs.add(g);
		}
		return true;
	}
}
