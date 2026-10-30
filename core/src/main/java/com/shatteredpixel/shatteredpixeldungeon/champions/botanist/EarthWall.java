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

package com.shatteredpixel.shatteredpixeldungeon.champions.botanist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

//W: PERMANENT dirt block on a visible cell. it's a wall, saved with the level.
//only the botanist (W on it again) or wall-breaking enemies get rid of it
public class EarthWall extends ChampionAbility {

	public static final EarthWall INSTANCE = new EarthWall();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.BOT_EARTH_WALL; }
	@Override public int color() { return 0x99663A; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 7;

	@Override
	public float cooldown( Hero hero ) {
		return 10 - 2*hero.pointsInTalent(Talent.BOT_DEEP_ROOTS);
	}

	//stored as a blob that never ticks. abuse of Blob but it saves for free
	public static class Blocks extends Blob {

		@Override
		protected void evolve() {
			for (int i = 0; i < cur.length; i++){
				if (cur[i] > 0 && Dungeon.level.map[i] == Terrain.WALL){
					off[i] = cur[i];
					volume += off[i];
				} else {
					off[i] = 0;
				}
			}
		}

		@Override
		public String tileDesc() {
			return null;
		}
	}

	public static boolean isBlock( int cell ){
		Blob b = Dungeon.level.blobs.get(Blocks.class);
		return b != null && b.volume > 0 && b.cur != null && b.cur[cell] > 0 && Dungeon.level.map[cell] == Terrain.WALL;
	}

	private static boolean groundOk( int cell ){
		int t = Dungeon.level.map[cell];
		return t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.GRASS || t == Terrain.HIGH_GRASS
				|| t == Terrain.FURROWED_GRASS || t == Terrain.EMBERS || t == Terrain.EMPTY_SP;
	}

	//don't let people wall off the exit. someone WILL try
	private static boolean keepsPath( Hero hero, int cell ){
		int exit = Dungeon.level.exit();
		if (exit <= 0 || exit >= Dungeon.level.length()) return true;
		boolean[] open = Dungeon.level.passable.clone();
		PathFinder.buildDistanceMap(exit, open);
		boolean before = PathFinder.distance[hero.pos] != Integer.MAX_VALUE;
		if (!before) return true;
		open[cell] = false;
		PathFinder.buildDistanceMap(exit, open);
		return PathFinder.distance[hero.pos] != Integer.MAX_VALUE;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		boolean removing = isBlock(target);
		if (!removing){
			if (!groundOk(target) || Actor.findChar(target) != null || Dungeon.level.heaps.get(target) != null
					|| Dungeon.level.plants.get(target) != null
					|| target == Dungeon.level.entrance() || target == Dungeon.level.exit()){
				GLog.w(Messages.get(this, "no_space"));
				return;
			}
			for (Hero h : Party.heroes()){
				if (!keepsPath(h, target)){
					GLog.w(Messages.get(this, "blocks_path"));
					return;
				}
			}
		}

		if (removing){
			Blob b = Dungeon.level.blobs.get(Blocks.class);
			b.clear(target);
			Level.set(target, Terrain.EMPTY);
		} else {
			Level.set(target, Terrain.WALL);
			GameScene.add(Blob.seed(target, 1, Blocks.class));
		}
		GameScene.updateMap(target);
		if (Dungeon.level.heroFOV[target]){
			CellEmitter.get(target).burst(Speck.factory(Speck.ROCK), 8);
		}
		Sample.INSTANCE.play(removing ? Assets.Sounds.ROCKS : Assets.Sounds.ROCKS, 0.9f, removing ? 1.2f : 0.8f);
		PixelScene.shake(1, 0.3f);
		Dungeon.observe();
		GameScene.updateFog();

		ChampionHooks.operateVisual(hero, target);
		Invisibility.dispel(hero);
		BotanistHooks.afterCast(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE);
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
