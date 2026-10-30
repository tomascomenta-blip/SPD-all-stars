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

package com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SpectralWallParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//W: ring of gravestones around a cell, nobody gets through. sinks once nothing hostile is left inside
public class DarkProcession extends ChampionAbility {

	public static final DarkProcession INSTANCE = new DarkProcession();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.LOL_DARK_PROCESSION; }
	@Override public int color() { return 0x8899AA; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 5;
	//5x5 ring, 3x3 inside
	public static final int RADIUS = 2;

	@Override
	public float cooldown( Hero hero ) {
		return 25;
	}

	public static int duration( Hero hero ){
		return 4 + hero.pointsInTalent(Talent.GRA_LONG_PROCESSION);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}

		if (!GraveWall.enemyInside(target)){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		Level l = Dungeon.level;
		int w = l.width();
		int cx = target % w;
		int cy = target / w;
		int dur = duration(hero);
		GraveWall wall = null;
		for (int dy = -RADIUS; dy <= RADIUS; dy++){
			for (int dx = -RADIUS; dx <= RADIUS; dx++){
				if (Math.max(Math.abs(dx), Math.abs(dy)) != RADIUS) continue;
				int x = cx + dx;
				int y = cy + dy;
				if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
				int c = x + y*w;
				if (!l.insideMap(c) || l.solid[c] || l.pit[c]) continue;
				//don't bury people alive. occupied cells stay open
				if (Actor.findChar(c) != null) continue;
				wall = Blob.seed(c, dur, GraveWall.class);
				GameScene.add(wall);
				if (l.heroFOV[c]) CellEmitter.get(c).burst(MistParticle.UP, 4);
			}
		}
		if (wall == null){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}
		wall.addRing(target);

		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f, 0.8f);
		Sample.INSTANCE.play(Assets.Sounds.TOMB, 0.6f, 0.7f);
		GravediggerHooks.castVisual(hero, target);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, duration(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	//solid but see-through. each ring keeps its center and sinks when it's empty of enemies
	public static class GraveWall extends Blob {

		{
			alwaysVisible = true;
		}

		//ring centers, raise order
		private ArrayList<Integer> rings = new ArrayList<>();

		public void addRing( int center ){
			if (!rings.contains(center)) rings.add(center);
		}

		public static boolean enemyInside( int center ){
			for (Char ch : Actor.chars()){
				if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
						&& Dungeon.level.distance(center, ch.pos) < RADIUS){
					return true;
				}
			}
			return false;
		}

		@Override
		protected void evolve() {
			int cell;
			Level l = Dungeon.level;

			//empty rings go down right away
			ArrayList<Integer> standing = new ArrayList<>();
			for (int center : rings){
				if (enemyInside(center)) standing.add(center);
			}
			rings = standing;

			for (int i = area.left; i < area.right; i++){
				for (int j = area.top; j < area.bottom; j++){
					cell = i + j*l.width();
					boolean held = false;
					if (cur[cell] > 0){
						for (int center : rings){
							if (l.distance(center, cell) == RADIUS){
								held = true;
								break;
							}
						}
					}
					off[cell] = cur[cell] > 0 && held ? cur[cell] - 1 : 0;
					//cap 9, double casting doesn't stack. ya sé, 9 es raro, it just feels right
					off[cell] = Math.min(off[cell], 9);
					volume += off[cell];
					if (off[cell] == 0 && cur[cell] > 0){
						cellsToFlagUpdate.add(cell);
					}
				}
			}
		}

		@Override
		public void seed( Level level, int cell, int amount ) {
			super.seed(level, cell, amount);
			level.updateCellFlags(cell);
		}

		@Override
		public void clear( int cell ) {
			super.clear(cell);
			if (cur == null) return;
			Dungeon.level.updateCellFlags(cell);
		}

		@Override
		public void fullyClear() {
			super.fullyClear();
			rings.clear();
			Dungeon.level.buildFlagMaps();
		}

		@Override
		public void onBuildFlagMaps( Level l ) {
			if (volume > 0){
				for (int i = 0; i < l.length(); i++){
					onUpdateCellFlags(l, i);
				}
			}
		}

		@Override
		public void onUpdateCellFlags( Level l, int cell ) {
			if (volume > 0 && cur[cell] > 0){
				l.solid[cell] = true;
				l.passable[cell] = false;
				l.avoid[cell] = false;
				//level fixes openSpace right after, don't do it here
			}
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use(emitter);
			emitter.pour(SpectralWallParticle.FACTORY, 0.05f);
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}

		private static final String RINGS = "rings";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			int[] r = new int[rings.size()];
			for (int i = 0; i < r.length; i++) r[i] = rings.get(i);
			bundle.put(RINGS, r);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			rings = new ArrayList<>();
			if (bundle.contains(RINGS)){
				for (int c : bundle.getIntArray(RINGS)) rings.add(c);
			}
		}
	}
}
